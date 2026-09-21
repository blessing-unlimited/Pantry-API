using Pantry.Api.Models;
using Pantry.Api.Services;

namespace Pantry.Api.Tests;

/// <summary>
/// Network-free tests for Blessing's shopping-list merge and substitutions
/// (TheMealDB, 2026; The Spruce Eats, 2023).
/// </summary>
public class ShoppingListGeneratorTests
{
    [Fact]
    public void MergeIngredients_SkipsItemsAlreadyInPantry()
    {
        var recipe = new RecipeDetail
        {
            Title = "Tomato pasta",
            Ingredients =
            [
                new RecipeIngredient { Name = "Tomato", Measure = "2", InPantry = true },
                new RecipeIngredient { Name = "Garlic", Measure = "3 cloves", InPantry = false },
                new RecipeIngredient { Name = "Olive oil", Measure = "1 tbsp", InPantry = false }
            ]
        };

        var items = ShoppingListGenerator.MergeIngredients(
            [(recipe, recipe.Title)],
            new HashSet<string>(StringComparer.OrdinalIgnoreCase) { "olive oil" });

        Assert.Single(items);
        Assert.Equal("Garlic", items[0].IngredientName);
        Assert.Equal(3, items[0].Quantity);
        Assert.Equal("cloves", items[0].Unit);
        Assert.Contains("Tomato pasta", items[0].SourceRecipeTitles);
    }

    [Fact]
    public void MergeIngredients_DeduplicatesAndAddsQuantities()
    {
        var first = new RecipeDetail
        {
            Title = "Soup",
            Ingredients = [new RecipeIngredient { Name = "Onion", Measure = "1", InPantry = false }]
        };
        var second = new RecipeDetail
        {
            Title = "Stew",
            Ingredients = [new RecipeIngredient { Name = "onion", Measure = "2", InPantry = false }]
        };

        var items = ShoppingListGenerator.MergeIngredients(
            [(first, first.Title), (second, second.Title)],
            new HashSet<string>(StringComparer.OrdinalIgnoreCase));

        Assert.Single(items);
        Assert.Equal(3, items[0].Quantity);
        Assert.Equal(2, items[0].SourceRecipeTitles.Count);
    }

    [Fact]
    public void SubstitutionLookup_PrefersKnownKeyInsideName()
    {
        var alts = SubstitutionLookup.GetAlternatives("unsalted butter").ToList();
        Assert.Contains(alts, a => a.Alt == "margarine");
    }

    [Fact]
    public void SubstitutionLookup_UnknownIngredient_IsEmpty()
    {
        Assert.Empty(SubstitutionLookup.GetAlternatives("dragon fruit"));
    }
}
