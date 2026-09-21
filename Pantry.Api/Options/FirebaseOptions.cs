namespace Pantry.Api.Options;

/// <summary>
/// Firebase Realtime Database connection settings.
/// Persistence follows the official REST surface (Firebase, 2026).
/// </summary>
public sealed class FirebaseOptions
{
    public const string SectionName = "Firebase";

    /// <summary>Realtime Database URL, for example https://project-id-default-rtdb.region.firebasedatabase.app/</summary>
    public string BaseUrl { get; set; } = string.Empty;

    /// <summary>Firebase / Google Cloud project id used as the JWT audience.</summary>
    public string ProjectId { get; set; } = string.Empty;

    /// <summary>Optional auth token when security rules are not in open test mode.</summary>
    public string AuthToken { get; set; } = string.Empty;
}
