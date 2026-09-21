using Microsoft.Extensions.Options;
using Pantry.Api.Models;
using Pantry.Api.Options;

namespace Pantry.Api.Services;

public interface IRecipeMatchingService
{
    Task<IReadOnlyList<RecipeSummary>> MatchAsync(
        IReadOnlyList<string> pantryNames,
        IReadOnlyList<string> dietaryPreferences,
        int? maxMissing,
        string? query,
        CancellationToken cancellationToken);

    Task<RecipeDetail?> GetRecipeAsync(string mealId, IReadOnlyList<string> pantryNames, CancellationToken cancellationToken);

    Task<IReadOnlyList<RecipeSummary>> SearchAsync(
        string query,
        IReadOnlyList<string> pantryNames,
        IReadOnlyList<string> dietaryPreferences,
        CancellationToken cancellationToken);

    Task<IReadOnlyList<IngredientDto>> SearchIngredientsAsync(string query, CancellationToken cancellationToken);
}

/// <summary>
/// Orchestrates TheMealDB calls and reverse-pantry ranking. The free MealDB
/// filter supports one ingredient at a time, so multi-ingredient ranking is
/// performed here (TheMealDB, 2026).
/// </summary>
public sealed class RecipeMatchingService : IRecipeMatchingService
{
    private readonly IMealDbClient _mealDb;
    private readonly MealDbOptions _options;
    private readonly ILogger<RecipeMatchingService> _logger;

    public RecipeMatchingService(IMealDbClient mealDb, IOptions<MealDbOptions> options, ILogger<RecipeMatchingService> logger)
    {
        _mealDb = mealDb;
        _options = options.Value;
        _logger = logger;
    }

    public async Task<IReadOnlyList<RecipeSummary>> MatchAsync(
        IReadOnlyList<string> pantryNames,
        IReadOnlyList<string> dietaryPreferences,
        int? maxMissing,
        string? query,
        CancellationToken cancellationToken)
    {
        var pantry = pantryNames
            .Select(PantryMatcher.Normalize)
            .Where(n => n.Length > 0)
            .Distinct()
            .Take(_options.MaxFilterIngredients)
            .ToList();

        if (pantry.Count == 0)
        {
            return [];
        }

        var scores = new Dictionary<string, int>(StringComparer.Ordinal);
        var thumbs = new Dictionary<string, Models.MealDb.MealDbMeal>(StringComparer.Ordinal);

        foreach (var ingredient in pantry)
        {
            IReadOnlyList<Models.MealDb.MealDbMeal> filtered;
            try
            {
                filtered = await _mealDb.FilterByIngredientAsync(ingredient, cancellationToken);
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "MealDB filter failed for ingredient {Ingredient}", ingredient);
                continue;
            }

            foreach (var meal in filtered)
            {
                if (string.IsNullOrWhiteSpace(meal.IdMeal))
                {
                    continue;
                }

                scores[meal.IdMeal] = scores.GetValueOrDefault(meal.IdMeal) + 1;
                thumbs[meal.IdMeal] = meal;
            }
        }

        var rankedIds = scores
            .OrderByDescending(pair => pair.Value)
            .Select(pair => pair.Key)
            .Take(_options.MaxRecipeLookups)
            .ToList();

        var results = new List<RecipeSummary>();
        foreach (var id in rankedIds)
        {
            var detail = await GetRecipeAsync(id, pantry, cancellationToken);
            if (detail is null)
            {
                continue;
            }

            if (!string.IsNullOrWhiteSpace(query) &&
                detail.Title.Contains(query, StringComparison.OrdinalIgnoreCase) == false)
            {
                continue;
            }

            var mealStub = thumbs.GetValueOrDefault(id) ?? new Models.MealDb.MealDbMeal
            {
                IdMeal = detail.Id,
                StrMeal = detail.Title,
                StrCategory = detail.Category,
                StrArea = detail.Area,
                StrTags = detail.Tags
            };

            if (!PantryMatcher.MatchesDietary(mealStub, detail.Ingredients, dietaryPreferences))
            {
                continue;
            }

            // Treat 20 as "no cap" so a small pantry still returns ranked recipes.
            if (maxMissing is > 0 and < 20 && detail.PantryNeedCount > maxMissing.Value)
            {
                continue;
            }

            results.Add(ToSummary(detail));
        }

        return results
            .OrderByDescending(r => r.MatchPercent)
            .ThenBy(r => r.PantryNeedCount)
            .ThenBy(r => r.Title)
            .ToList();
    }

    public async Task<RecipeDetail?> GetRecipeAsync(string mealId, IReadOnlyList<string> pantryNames, CancellationToken cancellationToken)
    {
        var meal = await _mealDb.LookupAsync(mealId, cancellationToken);
        if (meal is null || string.IsNullOrWhiteSpace(meal.IdMeal))
        {
            return null;
        }

        var extracted = PantryMatcher.ExtractIngredients(meal);
        var annotated = PantryMatcher.AnnotateIngredients(extracted, pantryNames);
        return new RecipeDetail
        {
            Id = meal.IdMeal,
            Title = meal.StrMeal ?? "Untitled meal",
            ThumbnailUrl = meal.StrMealThumb,
            Category = meal.StrCategory,
            Area = meal.StrArea,
            Tags = meal.StrTags,
            Instructions = meal.StrInstructions ?? string.Empty,
            YoutubeUrl = meal.StrYoutube,
            SourceUrl = meal.StrSource,
            Ingredients = annotated.ToList(),
            Steps = PantryMatcher.SplitInstructionSteps(meal.StrInstructions),
            MatchPercent = PantryMatcher.ComputeMatchPercent(annotated),
            PantryHaveCount = annotated.Count(i => i.InPantry),
            PantryNeedCount = annotated.Count(i => !i.InPantry)
        };
    }

    public async Task<IReadOnlyList<RecipeSummary>> SearchAsync(
        string query,
        IReadOnlyList<string> pantryNames,
        IReadOnlyList<string> dietaryPreferences,
        CancellationToken cancellationToken)
    {
        var meals = await _mealDb.SearchByNameAsync(query, cancellationToken);
        var results = new List<RecipeSummary>();
        foreach (var meal in meals)
        {
            if (string.IsNullOrWhiteSpace(meal.IdMeal))
            {
                continue;
            }

            var detail = await GetRecipeAsync(meal.IdMeal, pantryNames, cancellationToken);
            if (detail is null)
            {
                continue;
            }

            if (!PantryMatcher.MatchesDietary(meal, detail.Ingredients, dietaryPreferences))
            {
                continue;
            }

            results.Add(ToSummary(detail));
        }

        return results.OrderByDescending(r => r.MatchPercent).ToList();
    }

    public async Task<IReadOnlyList<IngredientDto>> SearchIngredientsAsync(string query, CancellationToken cancellationToken)
    {
        var all = await _mealDb.ListIngredientsAsync(cancellationToken);
        var needle = PantryMatcher.Normalize(query);
        return all
            .Where(row => !string.IsNullOrWhiteSpace(row.StrIngredient))
            .Where(row => string.IsNullOrWhiteSpace(needle) || PantryMatcher.Normalize(row.StrIngredient).Contains(needle, StringComparison.Ordinal))
            .Take(25)
            .Select(row => new IngredientDto
            {
                Id = row.IdIngredient ?? row.StrIngredient!,
                Name = row.StrIngredient!,
                Description = row.StrDescription,
                ThumbnailUrl = $"https://www.themealdb.com/images/ingredients/{Uri.EscapeDataString(row.StrIngredient!.Replace(' ', '_'))}-Small.png"
            })
            .ToList();
    }

    private static RecipeSummary ToSummary(RecipeDetail detail) => new()
    {
        Id = detail.Id,
        Title = detail.Title,
        ThumbnailUrl = detail.ThumbnailUrl,
        Category = detail.Category,
        Area = detail.Area,
        MatchPercent = detail.MatchPercent,
        PantryHaveCount = detail.PantryHaveCount,
        PantryNeedCount = detail.PantryNeedCount,
        MissingIngredients = PantryMatcher.MissingNames(detail.Ingredients)
    };
}
