using System.Globalization;
using Pantry.Api.Models;

namespace Pantry.Api.Services;

/// <summary> Blessing
/// Pure-logic helpers for shopping-list generation and substitutions.
/// RecipeDetail input is produced by RecipeMatchingService from live
/// TheMealDB data; this class stays network-free for testability
/// (Microsoft, 2024; TheMealDB, 2026).
/// </summary>
public static class ShoppingListGenerator
{
    public static string Normalize(string name) =>
        name.Trim().Replace('_', ' ').ToLowerInvariant();

    /// <summary>
    /// Merge ingredients across recipes, de-duplicate by normalized name,
    /// accumulate quantities with matching units, and tag source recipes.
    /// </summary>
    public static List<ShoppingListItem> MergeIngredients(IEnumerable<(RecipeDetail Recipe, string Title)> recipes, ISet<string> pantryNormalizedNames)
    {
        var merged = new Dictionary<string, ShoppingListItem>(StringComparer.OrdinalIgnoreCase);

        foreach (var (recipe, title) in recipes)
        {
            foreach (var ing in recipe.Ingredients)
            {
                if (ing.InPantry || pantryNormalizedNames.Contains(Normalize(ing.Name)))
                    continue;

                var key = Normalize(ing.Name);
                TryParseQuantityAndUnit(ing.Measure, out var qty, out var unit);

                if (merged.TryGetValue(key, out var existing))
                {
                    if (existing.Quantity is not null && qty is not null &&
                        string.Equals(existing.Unit, unit, StringComparison.OrdinalIgnoreCase))
                    {
                        existing.Quantity += qty;
                    }
                    if (!existing.SourceRecipeTitles.Contains(title))
                        existing.SourceRecipeTitles.Add(title);
                }
                else
                {
                    merged[key] = new ShoppingListItem
                    {
                        IngredientName = ing.Name,
                        Quantity = qty,
                        Unit = unit,
                        IsChecked = false,
                        SourceRecipeTitles = [title]
                    };
                }
            }
        }

        return merged.Values.OrderBy(i => i.IngredientName, StringComparer.OrdinalIgnoreCase).ToList();
    }

    /// <summary>Best-effort parser for TheMealDB measure strings like "2 cups" or "1/2 kg".</summary>
    public static bool TryParseQuantityAndUnit(string? measure, out decimal? quantity, out string? unit)
    {
        quantity = null;
        unit = null;
        if (string.IsNullOrWhiteSpace(measure)) return false;

        var parts = measure.Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        if (parts.Length == 0) return false;

        if (decimal.TryParse(parts[0], NumberStyles.Any, CultureInfo.InvariantCulture, out var q))
            quantity = q;
        else if (parts[0].Contains('/'))
        {
            var frac = parts[0].Split('/');
            if (frac.Length == 2 && decimal.TryParse(frac[0], out var num) &&
                decimal.TryParse(frac[1], out var den) && den != 0)
                quantity = num / den;
        }

        if (parts.Length > 1)
            unit = string.Join(' ', parts.Skip(1));

        return quantity is not null || unit is not null;
    }
}

/// <summary>
/// Static lookup for common cooking substitutions (The Spruce Eats, 2023).
/// </summary>
public static class SubstitutionLookup
{
    private static readonly Dictionary<string, (string Alt, string Ratio)[]> Table = new(StringComparer.OrdinalIgnoreCase)
        {
            ["butter"] = [("margarine", "1:1"), ("coconut oil", "1:1"), ("olive oil", "3/4 cup oil per 1 cup butter")],
            ["milk"] = [("almond milk", "1:1"), ("soy milk", "1:1"), ("oat milk", "1:1")],
            ["egg"] = [("applesauce", "1/4 cup per egg"), ("flax meal", "1 tbsp flax + 3 tbsp water per egg")],
            ["sugar"] = [("honey", "3/4 cup honey per 1 cup sugar + reduce liquid"), ("maple syrup", "3/4 cup per 1 cup sugar"), ("coconut sugar", "1:1")],
            ["flour"] = [("almond flour", "1:1 (may need extra binder)"), ("oat flour", "1:1")],
            ["baking powder"] = [("baking soda + cream of tartar", "1/4 tsp soda + 1/2 tsp tartar per 1 tsp powder")],
            ["soy sauce"] = [("tamari", "1:1"), ("coconut aminos", "1:1")],
            ["onion"] = [("onion powder", "1 tbsp powder per 1 medium onion"), ("shallots", "3 shallots per 1 onion")],
            ["garlic"] = [("garlic powder", "1/4 tsp powder per 1 clove")]
        };

    public static IEnumerable<(string Alt, string Ratio)> GetAlternatives(string ingredientName)
    {
        foreach (var key in Table.Keys)
        {
            if (ingredientName.Contains(key, StringComparison.OrdinalIgnoreCase) ||
                key.Contains(ingredientName.Trim(), StringComparison.OrdinalIgnoreCase))
            {
                return Table[key];
            }
        }
        return [];
    }
}
