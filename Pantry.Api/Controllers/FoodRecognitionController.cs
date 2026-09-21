using System.Collections.Concurrent;
using Microsoft.AspNetCore.Mvc;
using Pantry.Api.DTO;
using Pantry.Api.Models;
using Pantry.Api.Services;

namespace Pantry.Api.Controllers;

/// <summary>
/// Blessing's LogMeal food recognition. The Android camera uploads a photo;
/// this API keeps the LogMeal token server-side (LogMeal API, 2026).
/// </summary>
[ApiController]
[Route("api/v1/food-recognition")]
public sealed class FoodRecognitionController : ControllerBase
{
    private sealed record CachedCandidates(List<RecognitionCandidate> Candidates, DateTimeOffset ExpiresAt);

    private static readonly ConcurrentDictionary<long, CachedCandidates> CandidateCache = new();
    private static readonly TimeSpan CacheLifetime = TimeSpan.FromMinutes(15);

    private readonly ILogMealService _logMealService;
    private readonly ILogger<FoodRecognitionController> _logger;

    public FoodRecognitionController(ILogMealService logMealService, ILogger<FoodRecognitionController> logger)
    {
        _logMealService = logMealService;
        _logger = logger;
    }

    [HttpPost("identify")]
    [RequestSizeLimit(10 * 1024 * 1024)]
    [ProducesResponseType(typeof(ApiEnvelope<IdentifyFoodResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<IdentifyFoodResponse>>> IdentifyFood(
        IFormFile? image,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<IdentifyFoodResponse>.Fail("Authentication is required."));
        }

        if (image == null || image.Length == 0)
        {
            return BadRequest(ApiEnvelope<IdentifyFoodResponse>.Fail("No image file provided."));
        }

        var contentType = image.ContentType?.ToLowerInvariant() ?? string.Empty;
        var allowed = contentType is "image/jpeg" or "image/jpg" or "image/png" or "image/webp" or "application/octet-stream" or "";
        if (!allowed)
        {
            return BadRequest(ApiEnvelope<IdentifyFoodResponse>.Fail("Unsupported image format. Use JPEG, PNG, or WebP."));
        }

        var result = await _logMealService.RecognizeImageAsync(image, cancellationToken);
        if (result is null)
        {
            return StatusCode(
                StatusCodes.Status502BadGateway,
                ApiEnvelope<IdentifyFoodResponse>.Fail("Food recognition service failed. Check LogMeal:ApiToken."));
        }

        var segments = result.SegmenationResults ?? [];
        var allCandidates = segments
            .SelectMany(sr =>
            {
                var names = sr.RecognitionResults ?? [];
                if (names.Count == 0 && !string.IsNullOrWhiteSpace(sr.FoodName))
                {
                    return [new RecognitionCandidate { Name = sr.FoodName, Probability = sr.Probability }];
                }

                return names;
            })
            .Where(r => !string.IsNullOrWhiteSpace(r.Name))
            .GroupBy(r => r.Name, StringComparer.OrdinalIgnoreCase)
            .Select(g => g.OrderByDescending(r => r.Probability).First())
            .ToList();

        var identifiedFoods = allCandidates
            .OrderByDescending(c => c.Probability)
            .Select(c => c.Name)
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();

        if (identifiedFoods.Count == 0)
        {
            return Ok(ApiEnvelope<IdentifyFoodResponse>.Ok(new IdentifyFoodResponse
            {
                ImageId = result.ImageId,
                Foods = [],
                Message = "No food items were recognized in the image."
            }));
        }

        CandidateCache[result.ImageId] = new CachedCandidates(allCandidates, DateTimeOffset.UtcNow.Add(CacheLifetime));
        _logger.LogInformation("User {UserId} identified {Count} foods from image {ImageId}", userId, identifiedFoods.Count, result.ImageId);

        var top = allCandidates.OrderByDescending(c => c.Probability).First();
        return Ok(ApiEnvelope<IdentifyFoodResponse>.Ok(new IdentifyFoodResponse
        {
            ImageId = result.ImageId,
            Foods = identifiedFoods,
            TopMatch = new IdentifyTopMatch { Name = top.Name, Confidence = top.Probability }
        }));
    }

    [HttpPost("confirm")]
    [ProducesResponseType(typeof(ApiEnvelope<ConfirmFoodResponse>), StatusCodes.Status200OK)]
    public ActionResult<ApiEnvelope<ConfirmFoodResponse>> ConfirmFood([FromBody] ConfirmFoodRequest request)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<ConfirmFoodResponse>.Fail("Authentication is required."));
        }

        if (request is null || string.IsNullOrWhiteSpace(request.SelectedFood))
        {
            return BadRequest(ApiEnvelope<ConfirmFoodResponse>.Fail("selectedFood is required."));
        }

        foreach (var kvp in CandidateCache)
        {
            if (kvp.Value.ExpiresAt < DateTimeOffset.UtcNow)
            {
                CandidateCache.TryRemove(kvp.Key, out _);
            }
        }

        CandidateCache.TryGetValue(request.ImageId, out var cached);
        if (cached is null)
        {
            if (!request.AllowCustomName)
            {
                return NotFound(ApiEnvelope<ConfirmFoodResponse>.Fail(
                    $"No recognition results found for imageId {request.ImageId}. Re-identify the image, or set allowCustomName=true."));
            }

            return Ok(ApiEnvelope<ConfirmFoodResponse>.Ok(new ConfirmFoodResponse
            {
                ImageId = request.ImageId,
                ConfirmedFood = request.SelectedFood.Trim(),
                WasFromSuggestions = false,
                Message = "Food confirmed manually (no cached suggestions for this imageId)."
            }));
        }

        var match = cached.Candidates.FirstOrDefault(c =>
            string.Equals(c.Name, request.SelectedFood.Trim(), StringComparison.OrdinalIgnoreCase));

        if (match is null)
        {
            if (!request.AllowCustomName)
            {
                return BadRequest(ApiEnvelope<ConfirmFoodResponse>.Fail(
                    $"'{request.SelectedFood}' is not one of the recognized options for this image."));
            }

            return Ok(ApiEnvelope<ConfirmFoodResponse>.Ok(new ConfirmFoodResponse
            {
                ImageId = request.ImageId,
                ConfirmedFood = request.SelectedFood.Trim(),
                WasFromSuggestions = false,
                Message = "Food confirmed manually (did not match any suggestion)."
            }));
        }

        CandidateCache.TryRemove(request.ImageId, out _);
        return Ok(ApiEnvelope<ConfirmFoodResponse>.Ok(new ConfirmFoodResponse
        {
            ImageId = request.ImageId,
            ConfirmedFood = match.Name,
            WasFromSuggestions = true,
            Confidence = match.Probability,
            Message = $"'{match.Name}' confirmed as the food in the image."
        }));
    }
}
