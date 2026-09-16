namespace Pantry.Api.Options;

/// <summary>
/// TheMealDB catalogue settings. The free developer key "1" is documented for
/// educational use (TheMealDB, 2026).
/// </summary>
public sealed class MealDbOptions
{
    public const string SectionName = "MealDb";

    public string BaseUrl { get; set; } = "https://www.themealdb.com/api/json/v1/1/";

    /// <summary>Maximum pantry ingredients sent to TheMealDB filter endpoint per match request.</summary>
    public int MaxFilterIngredients { get; set; } = 6;

    /// <summary>Maximum full recipe lookups after candidate collection, to keep latency predictable.</summary>
    public int MaxRecipeLookups { get; set; } = 20;
}
