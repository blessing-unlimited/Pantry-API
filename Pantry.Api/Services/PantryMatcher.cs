using Pantry.Api.Models;
using Pantry.Api.Models.MealDb;

namespace Pantry.Api.Services;

/// <summary>
/// Deterministic reverse-pantry matching helpers. Ranking is performed on the
/// hosted API so the Android client stays a thin presenter (Part 1 design).
/// Ingredient comparison is case-insensitive and treats substring matches as
/// hits so "chicken breast" matches pantry item "chicken".
/// </summary>
public static class PantryMatcher
{
    private static readonly HashSet<string> AnimalProteins =
    [
        "chicken", "beef", "pork", "lamb", "bacon", "ham", "sausage", "steak",
        "turkey", "duck", "veal", "goat", "anchovy", "fish", "salmon", "tuna",
        "prawn", "shrimp", "crab", "lobster", "mussel", "oyster", "squid"
    ];

    private static readonly HashSet<string> AnimalProducts =
    [
        "milk", "cheese", "butter", "cream", "yogurt", "yoghurt", "egg", "honey",
        "mayonnaise", "ghee"
    ];

    public static string Normalize(string? value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return string.Empty;
        }

        return value.Trim().ToLowerInvariant().Replace('_', ' ');
    }

    public static bool NamesMatch(string recipeIngredient, string pantryIngredient)
    {
        var recipe = Normalize(recipeIngredient);
        var pantry = Normalize(pantryIngredient);
        if (recipe.Length == 0 || pantry.Length == 0)
        {
            return false;
        }

        return recipe == pantry
               || recipe.Contains(pantry, StringComparison.Ordinal)
               || pantry.Contains(recipe, StringComparison.Ordinal);
    }

    public static IReadOnlyList<RecipeIngredient> AnnotateIngredients(
        IEnumerable<RecipeIngredient> ingredients,
        IEnumerable<string> pantryNames)
    {
        var pantry = pantryNames.Select(Normalize).Where(n => n.Length > 0).ToList();
        var annotated = new List<RecipeIngredient>();
        foreach (var ingredient in ingredients)
        {
            var inPantry = pantry.Any(p => NamesMatch(ingredient.Name, p));
            annotated.Add(new RecipeIngredient
            {
                Name = ingredient.Name,
                Measure = ingredient.Measure,
                InPantry = inPantry
            });
        }

        return annotated;
    }

    public static int ComputeMatchPercent(IReadOnlyList<RecipeIngredient> annotated)
    {
        if (annotated.Count == 0)
        {
            return 0;
        }

        var have = annotated.Count(i => i.InPantry);
        return (int)Math.Round(have * 100.0 / annotated.Count, MidpointRounding.AwayFromZero);
    }

    public static List<string> MissingNames(IReadOnlyList<RecipeIngredient> annotated) =>
        annotated.Where(i => !i.InPantry).Select(i => i.Name).Distinct(StringComparer.OrdinalIgnoreCase).ToList();

    /// <summary>
    /// Applies onboarding dietary chips to a MealDB recipe using category, tags
    /// and ingredient names. Vegetarian/vegan filtering is heuristic because
    /// TheMealDB does not expose a first-class diet field (TheMealDB, 2026).
    /// </summary>
    public static bool MatchesDietary(MealDbMeal meal, IEnumerable<RecipeIngredient> ingredients, IEnumerable<string> preferences)
    {
        var prefs = preferences.Select(Normalize).Where(p => p.Length > 0).ToHashSet();
        if (prefs.Count == 0 || prefs.Contains("no restriction"))
        {
            return true;
        }

        var category = Normalize(meal.StrCategory);
        var tags = Normalize(meal.StrTags);
        var names = ingredients.Select(i => Normalize(i.Name)).ToList();

        if (prefs.Contains("vegan"))
        {
            if (category == "vegan" || tags.Contains("vegan"))
            {
                return true;
            }

            if (ContainsAny(names, AnimalProteins) || ContainsAny(names, AnimalProducts) || category is "beef" or "chicken" or "pork" or "lamb" or "seafood")
            {
                return false;
            }
        }

        if (prefs.Contains("vegetarian"))
        {
            if (category is "vegetarian" or "vegan" || tags.Contains("vegetarian") || tags.Contains("vegan"))
            {
                return true;
            }

            if (ContainsAny(names, AnimalProteins) || category is "beef" or "chicken" or "pork" or "lamb" or "seafood")
            {
                return false;
            }
        }

        if (prefs.Contains("halal") && (ContainsToken(names, "pork") || ContainsToken(names, "bacon") || ContainsToken(names, "ham") || category == "pork"))
        {
            return false;
        }

        return true;
    }

    public static List<string> SplitInstructionSteps(string? instructions)
    {
        if (string.IsNullOrWhiteSpace(instructions))
        {
            return [];
        }

        return instructions
            .Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Where(line => line.Length > 0)
            .ToList();
    }

    public static IReadOnlyList<RecipeIngredient> ExtractIngredients(MealDbMeal meal)
    {
        var items = new List<RecipeIngredient>();
        for (var index = 1; index <= 20; index++)
        {
            var name = GetIngredient(meal, index);
            var measure = GetMeasure(meal, index);
            if (string.IsNullOrWhiteSpace(name))
            {
                continue;
            }

            items.Add(new RecipeIngredient
            {
                Name = name.Trim(),
                Measure = measure?.Trim() ?? string.Empty
            });
        }

        return items;
    }

    private static bool ContainsAny(IEnumerable<string> names, HashSet<string> tokens) =>
        names.Any(name => tokens.Any(token => name.Contains(token, StringComparison.Ordinal)));

    private static bool ContainsToken(IEnumerable<string> names, string token) =>
        names.Any(name => name.Contains(token, StringComparison.Ordinal));

    private static string? GetIngredient(MealDbMeal meal, int index) => index switch
    {
        1 => meal.StrIngredient1,
        2 => meal.StrIngredient2,
        3 => meal.StrIngredient3,
        4 => meal.StrIngredient4,
        5 => meal.StrIngredient5,
        6 => meal.StrIngredient6,
        7 => meal.StrIngredient7,
        8 => meal.StrIngredient8,
        9 => meal.StrIngredient9,
        10 => meal.StrIngredient10,
        11 => meal.StrIngredient11,
        12 => meal.StrIngredient12,
        13 => meal.StrIngredient13,
        14 => meal.StrIngredient14,
        15 => meal.StrIngredient15,
        16 => meal.StrIngredient16,
        17 => meal.StrIngredient17,
        18 => meal.StrIngredient18,
        19 => meal.StrIngredient19,
        20 => meal.StrIngredient20,
        _ => null
    };

    private static string? GetMeasure(MealDbMeal meal, int index) => index switch
    {
        1 => meal.StrMeasure1,
        2 => meal.StrMeasure2,
        3 => meal.StrMeasure3,
        4 => meal.StrMeasure4,
        5 => meal.StrMeasure5,
        6 => meal.StrMeasure6,
        7 => meal.StrMeasure7,
        8 => meal.StrMeasure8,
        9 => meal.StrMeasure9,
        10 => meal.StrMeasure10,
        11 => meal.StrMeasure11,
        12 => meal.StrMeasure12,
        13 => meal.StrMeasure13,
        14 => meal.StrMeasure14,
        15 => meal.StrMeasure15,
        16 => meal.StrMeasure16,
        17 => meal.StrMeasure17,
        18 => meal.StrMeasure18,
        19 => meal.StrMeasure19,
        20 => meal.StrMeasure20,
        _ => null
    };
}
