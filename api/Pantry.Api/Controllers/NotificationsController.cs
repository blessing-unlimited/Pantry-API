using Microsoft.AspNetCore.Mvc;
using Pantry.Api.Models;
using Pantry.Api.Services;

namespace Pantry.Api.Controllers;

/// <summary>
/// Sends a Cloud Messaging alert to the signed-in user's registered device.
/// The server credential is optional so the API still runs in class and in CI
/// (Firebase, 2026).
/// </summary>
[ApiController]
[Route("api/v1/notifications")]
public sealed class NotificationsController : ControllerBase
{
    private readonly IUserStore _store;
    private readonly IPushNotificationSender _push;
    private readonly ILogger<NotificationsController> _logger;

    public NotificationsController(
        IUserStore store,
        IPushNotificationSender push,
        ILogger<NotificationsController> logger)
    {
        _store = store;
        _push = push;
        _logger = logger;
    }

    [HttpPost("test")]
    [ProducesResponseType(typeof(ApiEnvelope<string>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiEnvelope<string>), StatusCodes.Status503ServiceUnavailable)]
    public async Task<ActionResult<ApiEnvelope<string>>> SendTest(CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<string>.Fail("Authentication is required."));
        }

        if (!_push.IsConfigured)
        {
            _logger.LogWarning("FCM test requested but the service account is not configured");
            return StatusCode(
                StatusCodes.Status503ServiceUnavailable,
                ApiEnvelope<string>.Fail("Firebase Cloud Messaging is not configured on the server."));
        }

        var profile = await _store.GetProfileAsync(userId, cancellationToken);
        if (string.IsNullOrWhiteSpace(profile?.FcmToken))
        {
            return BadRequest(ApiEnvelope<string>.Fail("This account has no registered device token yet."));
        }

        await _push.SendAsync(profile.FcmToken, "Pantry", "This is a test alert from Pantry.", cancellationToken);
        _logger.LogInformation("Test notification sent for {UserId}", userId);
        return Ok(ApiEnvelope<string>.Ok("Test notification sent."));
    }
}
