namespace Pantry.Api.Models;

public sealed class UserProfile
{
    public string UserId { get; set; } = string.Empty;

    public string? Email { get; set; }

    public string? DisplayName { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public bool OnboardingCompleted { get; set; }
}
