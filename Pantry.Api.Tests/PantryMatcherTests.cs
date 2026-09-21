using Pantry.Api.Models;
using Pantry.Api.Models.MealDb;
using Pantry.Api.Services;

namespace Pantry.Api.Tests;

/// <summary>
/// Unit tests for reverse-pantry ranking. These tests do not call TheMealDB
/// or Firebase so GitHub Actions can run them without secrets.
/// </summary>
public class PantryMatcherTests
{
    [Fact]
    public void Normalize_Trims_And_Lowercases_Underscores()
    {
        Assert.Equal("chicken breast", PantryMatcher.Normalize("  Chicken_Breast "));
    }

    [Fact]
    public void NamesMatch_Treats_Substring_As_Hit()
    {
        Assert.True(PantryMatcher.NamesMatch("chicken breast", "chicken"));
        Assert.True(PantryMatcher.NamesMatch("garlic", "fresh garlic"));
        Assert.False(PantryMatcher.NamesMatch("tomato", "onion"));
    }

    [Fact]
    public void ComputeMatchPercent_AllPresent_Is100()
    {
        var annotated = PantryMatcher.AnnotateIngredients(
            [
                new RecipeIngredient { Name = "Tomato" },
                new RecipeIngredient { Name = "Garlic" }
            ],
            ["tomato", "garlic"]);

        Assert.Equal(100, PantryMatcher.ComputeMatchPercent(annotated));
        Assert.Empty(PantryMatcher.MissingNames(annotated));
    }

    [Fact]
    public void ComputeMatchPercent_HalfPresent_Is50()
    {
        var annotated = PantryMatcher.AnnotateIngredients(
            [
                new RecipeIngredient { Name = "Tomato" },
                new RecipeIngredient { Name = "Soy Sauce" }
            ],
            ["tomato"]);

        Assert.Equal(50, PantryMatcher.ComputeMatchPercent(annotated));
        Assert.Equal(["Soy Sauce"], PantryMatcher.MissingNames(annotated));
    }

    [Fact]
    public void ComputeMatchPercent_EmptyRecipe_Is0()
    {
        Assert.Equal(0, PantryMatcher.ComputeMatchPercent([]));
    }

    [Fact]
    public void MatchesDietary_Vegetarian_Rejects_Chicken()
    {
        var meal = new MealDbMeal { StrCategory = "Chicken" };
        var ingredients = new[] { new RecipeIngredient { Name = "Chicken Breast" } };
        Assert.False(PantryMatcher.MatchesDietary(meal, ingredients, ["vegetarian"]));
    }

    [Fact]
    public void MatchesDietary_Vegetarian_Allows_Vegetable_Dish()
    {
        var meal = new MealDbMeal { StrCategory = "Vegetarian", StrTags = "Vegetarian" };
        var ingredients = new[] { new RecipeIngredient { Name = "Tomato" } };
        Assert.True(PantryMatcher.MatchesDietary(meal, ingredients, ["vegetarian"]));
    }

    [Fact]
    public void MatchesDietary_NoRestriction_Allows_Meat()
    {
        var meal = new MealDbMeal { StrCategory = "Beef" };
        var ingredients = new[] { new RecipeIngredient { Name = "Beef" } };
        Assert.True(PantryMatcher.MatchesDietary(meal, ingredients, ["no restriction"]));
    }

    [Fact]
    public void SplitInstructionSteps_Ignores_Blank_Lines()
    {
        var steps = PantryMatcher.SplitInstructionSteps("Heat oil.\n\nAdd garlic.\r\nServe.");
        Assert.Equal(["Heat oil.", "Add garlic.", "Serve."], steps);
    }

    [Fact]
    public void ExtractIngredients_Skips_Empty_Slots()
    {
        var meal = new MealDbMeal
        {
            StrIngredient1 = "Tomato",
            StrMeasure1 = "2",
            StrIngredient2 = " ",
            StrMeasure2 = "1 tsp",
            StrIngredient3 = "Garlic",
            StrMeasure3 = "1 clove"
        };

        var extracted = PantryMatcher.ExtractIngredients(meal);
        Assert.Equal(2, extracted.Count);
        Assert.Equal("Tomato", extracted[0].Name);
        Assert.Equal("Garlic", extracted[1].Name);
    }
}
