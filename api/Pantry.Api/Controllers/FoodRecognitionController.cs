using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Pantry.Api.DTO;
using Pantry.Api.Services;
using System.Collections.Concurrent;

namespace Pantry.Api.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class FoodRecognitionController : ControllerBase
    {
        private readonly ILogMealService _logMealService;
        private readonly ILogger<FoodRecognitionController> _logger;

        //Caches the candidate options of each recognition so the user can confirm the correct one. 
        private sealed record CachedCandidates(List<RecognitionCandidate> Candidates, DateTimeOffset ExpiresAt);

        //(Microsoft,2026a)
        private static readonly ConcurrentDictionary<long, CachedCandidates> _candidateCache = new();

        // Entries expire so memory does not grow forever.
        private static readonly TimeSpan CacheLifetime = TimeSpan.FromMinutes(15);

        public FoodRecognitionController(ILogMealService logMealService, ILogger<FoodRecognitionController> logger)
        {
            _logMealService = logMealService;
            _logger = logger;
        }

        //Upload an image and then get an identified food(fruit or vegitable)
        [HttpPost("identify")]
        [RequestSizeLimit(10 * 1024 * 1024)] // 10 MB limit
        public async Task<IActionResult> IdentifyFood(IFormFile image, CancellationToken cancellationToken)
        {
            if (image == null || image.Length == 0)
            {
                return BadRequest(new { error = "No image file provided." });
            }

            // Optional: validate file type
            var allowedTypes = new[] { "image/jpeg", "image/png", "image/webp" };
            if (!allowedTypes.Contains(image.ContentType.ToLower()))
            {
                return BadRequest(new { error = "Unsupported image format. Use JPEG, PNG, or WebP." });
            }

            var result = await _logMealService.RecognizeImageAsync(image, cancellationToken);

            if (result == null)
            {
                return StatusCode(502, new { error = "Food recognition service failed." });
            }

            // Extract the top recognized food names (Highest confidence score)
            var identifiedFoods = result.SegmenationResults
                .SelectMany(sr => sr.RecognitionResults
                    .OrderByDescending(r => r.Probability)
                    .Select(r => r.Name))
                .Where(name => !string.IsNullOrWhiteSpace(name))
                .Distinct()
                .ToList();

            var allCandidates = result.SegmenationResults
                .SelectMany(sr => sr.RecognitionResults)
                .Where(r => !string.IsNullOrWhiteSpace(r.Name))
                .GroupBy(r => r.Name, StringComparer.OrdinalIgnoreCase)
                .Select(g => g.OrderByDescending(r => r.Probability).First())
                .ToList();

            if (identifiedFoods.Count == 0)
            {
                return Ok(new
                {
                    message = "No food items were recognized in the image.",
                    foods = Array.Empty<string>()
                });
            }

            // Cache the options so they can be confirmed via /confirm afterwards.
            _candidateCache[result.ImageId] = new CachedCandidates(allCandidates,DateTimeOffset.UtcNow.Add(CacheLifetime));

            return Ok(new
            {
                imageId = result.ImageId,
                foods = identifiedFoods,
                // Optionally return the top match with confidence
                topMatch = result.SegmenationResults
                    .SelectMany(sr => sr.RecognitionResults)
                    .OrderByDescending(r => r.Probability)
                    .Select(r => new { name = r.Name, confidence = r.Probability })
                    .FirstOrDefault()
            });
        }

        //This confirms whether the food recognized in the picture is correct, passes of the recognized names from
        //from /identify, or it passes the custome name(none of the recognized foods are correct).
        //
        [HttpPost("confirm")]
        public IActionResult ConfirmFood([FromBody] ConfirmFoodRequest request)
        {
            if (request == null || string.IsNullOrWhiteSpace(request.SelectedFood))
            {
                return BadRequest(new { error = "selectedFood is required." });
            }

            // Purge expired entries.
            foreach (var kvp in _candidateCache)
            {
                if (kvp.Value.ExpiresAt < DateTimeOffset.UtcNow)
                {
                    _candidateCache.TryRemove(kvp.Key, out _);
                }
            }

            _candidateCache.TryGetValue(request.ImageId, out var cached);

            if (cached == null)
            {
                // Candidates expired or were never cached. Accept the pick only
                // if the caller explicitly allows custom names.
                if (!request.AllowCustomName)
                {
                    return NotFound(new
                    {
                        error = $"No recognition results found for imageId {request.ImageId}. They expire after {CacheLifetime.TotalMinutes} minutes - re-identify the image, or set allowCustomName=true to confirm manually."
                    });
                }

                return Ok(new ConfirmFoodResponse
                {
                    ImageId = request.ImageId,
                    ConfirmedFood = request.SelectedFood.Trim(),
                    WasFromSuggestions = false,
                    Confidence = null,
                    Message = "Food confirmed manually (no cached suggestions for this imageId)."
                });
            }

            // Case-insensitive match against the cached candidates.
            var match = cached.Candidates.FirstOrDefault(c => string.Equals(c.Name, request.SelectedFood.Trim(), StringComparison.OrdinalIgnoreCase));

            if (match == null)
            {
                if (!request.AllowCustomName)
                {
                    return BadRequest(new
                    {
                        error = $"'{request.SelectedFood}' is not one of the recognized options for this image.",
                        availableOptions = cached.Candidates
                            .OrderByDescending(c => c.Probability)
                            .Select(c => new { name = c.Name, confidence = c.Probability })
                    });
                }

                return Ok(new ConfirmFoodResponse
                {
                    ImageId = request.ImageId,
                    ConfirmedFood = request.SelectedFood.Trim(),
                    WasFromSuggestions = false,
                    Confidence = null,
                    Message = "Food confirmed manually (did not match any suggestion)."
                });
            }

            // Confirmed successfully - drop the cached entry.
            _candidateCache.TryRemove(request.ImageId, out _);

            return Ok(new ConfirmFoodResponse
            {
                ImageId = request.ImageId,
                ConfirmedFood = match.Name,
                WasFromSuggestions = true,
                Confidence = match.Probability,
                Message = $"'{match.Name}' confirmed as the food in the image."
            });
        }
    }
}
