namespace Pantry.Api.Models;

public sealed class UserSettings
{
    public string LanguageCode { get; set; } = "en";

    public List<string> DietaryPreferences { get; set; } = [];

    public List<string> CuisinePreferences { get; set; } = [];

    public List<string> Allergens { get; set; } = [];

    public bool NotificationsEnabled { get; set; }

    public int MaxMissingIngredients { get; set; } = 20;
}
