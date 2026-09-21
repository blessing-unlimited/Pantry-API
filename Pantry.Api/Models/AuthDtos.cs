namespace Pantry.Api.Models;

public sealed class SsoRequest
{
    /// <summary>Firebase ID token issued after Google Sign-In (Firebase, 2026).</summary>
    public string IdToken { get; set; } = string.Empty;

    public string? DisplayName { get; set; }

    public string? Email { get; set; }
}

public sealed class SsoResponse
{
    public string UserId { get; set; } = string.Empty;

    public string? Email { get; set; }

    public string? DisplayName { get; set; }

    public bool IsNewUser { get; set; }
}
