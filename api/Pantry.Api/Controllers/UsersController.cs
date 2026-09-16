using Microsoft.AspNetCore.Mvc;
using Pantry.Api.Models;
using Pantry.Api.Services;

namespace Pantry.Api.Controllers;

[ApiController]
[Route("api/v1/users")]
public sealed class UsersController : ControllerBase
{
    private readonly IUserStore _store;
    private readonly ILogger<UsersController> _logger;

    public UsersController(IUserStore store, ILogger<UsersController> logger)
    {
        _store = store;
        _logger = logger;
    }

    [HttpGet("me")]
    [ProducesResponseType(typeof(ApiEnvelope<UserProfile>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<UserProfile>>> Me(CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<UserProfile>.Fail("Authentication is required."));
        }

        var profile = await _store.GetProfileAsync(userId, cancellationToken)
                      ?? new UserProfile { UserId = userId, CreatedAt = DateTimeOffset.UtcNow };
        return Ok(ApiEnvelope<UserProfile>.Ok(profile));
    }

    [HttpGet("me/settings")]
    [ProducesResponseType(typeof(ApiEnvelope<UserSettings>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<UserSettings>>> GetSettings(CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<UserSettings>.Fail("Authentication is required."));
        }

        var settings = await _store.GetSettingsAsync(userId, cancellationToken);
        return Ok(ApiEnvelope<UserSettings>.Ok(settings));
    }

    [HttpPatch("me/settings")]
    [ProducesResponseType(typeof(ApiEnvelope<UserSettings>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<UserSettings>>> UpdateSettings(
        [FromBody] UserSettings request,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<UserSettings>.Fail("Authentication is required."));
        }

        if (request.MaxMissingIngredients is < 0 or > 20)
        {
            return BadRequest(ApiEnvelope<UserSettings>.Fail("maxMissingIngredients must be between 0 and 20."));
        }

        if (string.IsNullOrWhiteSpace(request.LanguageCode))
        {
            request.LanguageCode = "en";
        }

        request.DietaryPreferences = request.DietaryPreferences
            .Where(v => !string.IsNullOrWhiteSpace(v))
            .Select(v => v.Trim().ToLowerInvariant())
            .Distinct()
            .ToList();
        request.CuisinePreferences = request.CuisinePreferences
            .Where(v => !string.IsNullOrWhiteSpace(v))
            .Select(v => v.Trim())
            .Distinct()
            .ToList();
        request.Allergens = request.Allergens
            .Where(v => !string.IsNullOrWhiteSpace(v))
            .Select(v => v.Trim())
            .Distinct()
            .ToList();

        var saved = await _store.SaveSettingsAsync(userId, request, cancellationToken);
        _logger.LogInformation("Updated settings for {UserId}", userId);
        return Ok(ApiEnvelope<UserSettings>.Ok(saved));
    }
}
