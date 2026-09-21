namespace Pantry.Api.Models;

public sealed class RecipeIngredient
{
    public string Name { get; set; } = string.Empty;

    public string Measure { get; set; } = string.Empty;

    public bool InPantry { get; set; }
}

public sealed class RecipeSummary
{
    public string Id { get; set; } = string.Empty;

    public string Title { get; set; } = string.Empty;

    public string? ThumbnailUrl { get; set; }

    public string? Category { get; set; }

    public string? Area { get; set; }

    public int MatchPercent { get; set; }

    public int PantryHaveCount { get; set; }

    public int PantryNeedCount { get; set; }

    public List<string> MissingIngredients { get; set; } = [];
}

public sealed class RecipeDetail
{
    public string Id { get; set; } = string.Empty;

    public string Title { get; set; } = string.Empty;

    public string? ThumbnailUrl { get; set; }

    public string? Category { get; set; }

    public string? Area { get; set; }

    public string? Tags { get; set; }

    public string Instructions { get; set; } = string.Empty;

    public string? YoutubeUrl { get; set; }

    public string? SourceUrl { get; set; }

    public List<RecipeIngredient> Ingredients { get; set; } = [];

    public List<string> Steps { get; set; } = [];

    public int MatchPercent { get; set; }

    public int PantryHaveCount { get; set; }

    public int PantryNeedCount { get; set; }
}

public sealed class RecipeMatchRequest
{
    public List<string>? IngredientNames { get; set; }

    public List<string>? DietaryPreferences { get; set; }

    public int? MaxMissingIngredients { get; set; }

    public string? Query { get; set; }
}

public sealed class IngredientDto
{
    public string Id { get; set; } = string.Empty;

    public string Name { get; set; } = string.Empty;

    public string? Description { get; set; }

    public string? ThumbnailUrl { get; set; }
}
