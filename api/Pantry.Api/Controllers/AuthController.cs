using Microsoft.AspNetCore.Mvc;
using Pantry.Api.Models;
using Pantry.Api.Services;

namespace Pantry.Api.Controllers;

[ApiController]
[Route("api/v1/auth")]
public sealed class AuthController : ControllerBase
{
    private readonly ITokenIdentityService _tokens;
    private readonly IUserStore _store;
    private readonly ILogger<AuthController> _logger;

    public AuthController(ITokenIdentityService tokens, IUserStore store, ILogger<AuthController> logger)
    {
        _tokens = tokens;
        _store = store;
        _logger = logger;
    }

    /// <summary>
    /// Exchanges a Google / Firebase ID token for an application session.
    /// Identity verification is performed by the external SSO provider
    /// (Firebase, 2026).
    /// </summary>
    [HttpPost("sso")]
    [ProducesResponseType(typeof(ApiEnvelope<SsoResponse>), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<ActionResult<ApiEnvelope<SsoResponse>>> Sso([FromBody] SsoRequest request, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(request.IdToken))
        {
            return BadRequest(ApiEnvelope<SsoResponse>.Fail("idToken is required."));
        }

        var identity = await _tokens.AuthenticateAsync(request.IdToken, cancellationToken);
        if (identity is null)
        {
            _logger.LogWarning("SSO token was rejected");
            return Unauthorized(ApiEnvelope<SsoResponse>.Fail("SSO token could not be verified."));
        }

        var existing = await _store.GetProfileAsync(identity.UserId, cancellationToken);
        var isNew = existing is null;
        var profile = existing ?? new UserProfile
        {
            UserId = identity.UserId,
            CreatedAt = DateTimeOffset.UtcNow
        };
        profile.Email = identity.Email ?? request.Email ?? profile.Email;
        profile.DisplayName = identity.DisplayName ?? request.DisplayName ?? profile.DisplayName;
        await _store.SaveProfileAsync(profile, cancellationToken);

        if (isNew)
        {
            await _store.SaveSettingsAsync(identity.UserId, new UserSettings(), cancellationToken);
        }

        _logger.LogInformation("SSO {Action} for {UserId}", isNew ? "registered" : "signed in", identity.UserId);

        return Ok(ApiEnvelope<SsoResponse>.Ok(new SsoResponse
        {
            UserId = identity.UserId,
            Email = profile.Email,
            DisplayName = profile.DisplayName,
            IsNewUser = isNew
        }));
    }

    [HttpPost("logout")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public IActionResult Logout()
    {
        // Tokens are client-held (Firebase / Google). Logout is a client-side
        // sign-out plus this acknowledgement for the Android logging flow.
        _logger.LogInformation("Logout requested for {UserId}", User.GetUserId() ?? "anonymous");
        return NoContent();
    }
}
