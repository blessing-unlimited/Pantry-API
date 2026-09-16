namespace Pantry.Api.Options;

/// <summary>
/// Authentication switches. Production must verify Firebase ID tokens
/// (Firebase, 2026). Development may use a documented bypass so Swagger and
/// the emulator can be demonstrated before google-services.json is installed.
/// </summary>
public sealed class AuthenticationOptions
{
    public const string SectionName = "Authentication";

    /// <summary>
    /// When true (Development only) the API accepts Bearer tokens that start with
    /// "dev-" and maps them to a local user id. Disabled in Production.
    /// </summary>
    public bool AllowDevelopmentBypass { get; set; }

    public string DevelopmentUserId { get; set; } = "dev-user-sihle";
}
