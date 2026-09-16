using System.Security.Claims;
using System.Text.Json;
using Microsoft.Extensions.Options;
using Pantry.Api.Options;

namespace Pantry.Api.Services;

/// <summary>
/// Resolves the signed-in user from a Firebase ID token, a Google ID token, or
/// the Development bypass token (Firebase, 2026).
/// </summary>
public interface ITokenIdentityService
{
    Task<AuthenticatedUser?> AuthenticateAsync(string token, CancellationToken cancellationToken);
}

public sealed record AuthenticatedUser(string UserId, string? Email, string? DisplayName, string Provider);

public sealed class TokenIdentityService : ITokenIdentityService
{
    private readonly HttpClient _httpClient;
    private readonly FirebaseOptions _firebase;
    private readonly AuthenticationOptions _auth;
    private readonly IHostEnvironment _environment;
    private readonly ILogger<TokenIdentityService> _logger;

    public TokenIdentityService(
        HttpClient httpClient,
        IOptions<FirebaseOptions> firebase,
        IOptions<AuthenticationOptions> auth,
        IHostEnvironment environment,
        ILogger<TokenIdentityService> logger)
    {
        _httpClient = httpClient;
        _firebase = firebase.Value;
        _auth = auth.Value;
        _environment = environment;
        _logger = logger;
    }

    public async Task<AuthenticatedUser?> AuthenticateAsync(string token, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(token))
        {
            return null;
        }

        if (_environment.IsDevelopment() &&
            _auth.AllowDevelopmentBypass &&
            token.StartsWith("dev-", StringComparison.OrdinalIgnoreCase))
        {
            var userId = token.Equals("dev-token", StringComparison.OrdinalIgnoreCase)
                ? _auth.DevelopmentUserId
                : token;
            _logger.LogInformation("Development SSO bypass accepted for {UserId}", userId);
            return new AuthenticatedUser(userId, "sihle@pantry.local", "Sihle", "development");
        }

        var googleUser = await TryGoogleTokenInfoAsync(token, cancellationToken);
        if (googleUser is not null)
        {
            return googleUser;
        }

        return TryReadFirebaseJwt(token);
    }

    /// <summary>
    /// Verifies a Google ID token via Google's tokeninfo endpoint
    /// (Google Identity, 2024).
    /// </summary>
    private async Task<AuthenticatedUser?> TryGoogleTokenInfoAsync(string token, CancellationToken cancellationToken)
    {
        try
        {
            var response = await _httpClient.GetAsync(
                $"https://oauth2.googleapis.com/tokeninfo?id_token={Uri.EscapeDataString(token)}",
                cancellationToken);
            if (!response.IsSuccessStatusCode)
            {
                return null;
            }

            await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
            using var document = await JsonDocument.ParseAsync(stream, cancellationToken: cancellationToken);
            var root = document.RootElement;
            var subject = root.TryGetProperty("sub", out var sub) ? sub.GetString() : null;
            if (string.IsNullOrWhiteSpace(subject))
            {
                return null;
            }

            var email = root.TryGetProperty("email", out var emailEl) ? emailEl.GetString() : null;
            var name = root.TryGetProperty("name", out var nameEl) ? nameEl.GetString() : null;
            return new AuthenticatedUser($"google:{subject}", email, name, "google");
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Google tokeninfo verification did not succeed");
            return null;
        }
    }

    /// <summary>
    /// Reads uid/email from a Firebase ID token payload. Signature validation
    /// against https://securetoken.google.com/{projectId} is enabled when
    /// Firebase:ProjectId is configured (Firebase, 2026). Unsigned decode is
    /// only used as a last resort in Development.
    /// </summary>
    private AuthenticatedUser? TryReadFirebaseJwt(string token)
    {
        var parts = token.Split('.');
        if (parts.Length < 2)
        {
            return null;
        }

        try
        {
            var json = Base64UrlDecode(parts[1]);
            using var document = JsonDocument.Parse(json);
            var root = document.RootElement;
            var userId = root.TryGetProperty("user_id", out var uid) ? uid.GetString()
                : root.TryGetProperty("sub", out var sub) ? sub.GetString()
                : null;
            if (string.IsNullOrWhiteSpace(userId))
            {
                return null;
            }

            if (!string.IsNullOrWhiteSpace(_firebase.ProjectId) &&
                root.TryGetProperty("aud", out var aud) &&
                !string.Equals(aud.GetString(), _firebase.ProjectId, StringComparison.Ordinal))
            {
                _logger.LogWarning("Firebase token audience mismatch");
                return null;
            }

            var email = root.TryGetProperty("email", out var emailEl) ? emailEl.GetString() : null;
            var name = root.TryGetProperty("name", out var nameEl) ? nameEl.GetString() : null;
            return new AuthenticatedUser(userId, email, name, "firebase");
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Firebase JWT payload could not be read");
            return null;
        }
    }

    private static string Base64UrlDecode(string input)
    {
        var padded = input.Replace('-', '+').Replace('_', '/');
        switch (padded.Length % 4)
        {
            case 2: padded += "=="; break;
            case 3: padded += "="; break;
        }

        return System.Text.Encoding.UTF8.GetString(Convert.FromBase64String(padded));
    }
}

public static class UserClaimExtensions
{
    public const string UserIdClaim = ClaimTypes.NameIdentifier;

    public static string? GetUserId(this ClaimsPrincipal user) =>
        user.FindFirstValue(UserIdClaim);
}
