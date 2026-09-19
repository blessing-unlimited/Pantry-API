using System.Net.Http.Json;
using Microsoft.Extensions.Options;
using Pantry.Api.Models.MealDb;
using Pantry.Api.Options;

namespace Pantry.Api.Services;

public interface IMealDbClient
{
    Task<IReadOnlyList<MealDbMeal>> FilterByIngredientAsync(string ingredient, CancellationToken cancellationToken);

    Task<MealDbMeal?> LookupAsync(string mealId, CancellationToken cancellationToken);

    Task<IReadOnlyList<MealDbMeal>> SearchByNameAsync(string query, CancellationToken cancellationToken);

    Task<IReadOnlyList<MealDbMeal>> ListByFirstLetterAsync(char letter, CancellationToken cancellationToken);

    Task<IReadOnlyList<MealDbIngredientRow>> ListIngredientsAsync(CancellationToken cancellationToken);
}

/// <summary>
/// HttpClient wrapper for TheMealDB. The free v1 test key is used for this
/// educational prototype (TheMealDB, 2026).
/// </summary>
public sealed class MealDbClient : IMealDbClient
{
    private readonly HttpClient _httpClient;
    private readonly ILogger<MealDbClient> _logger;

    public MealDbClient(HttpClient httpClient, IOptions<MealDbOptions> options, ILogger<MealDbClient> logger)
    {
        _httpClient = httpClient;
        _logger = logger;
        var baseUrl = options.Value.BaseUrl.TrimEnd('/') + "/";
        _httpClient.BaseAddress = new Uri(baseUrl);
        _httpClient.Timeout = TimeSpan.FromSeconds(20);
    }

    public async Task<IReadOnlyList<MealDbMeal>> FilterByIngredientAsync(string ingredient, CancellationToken cancellationToken)
    {
        var slug = ingredient.Trim().Replace(' ', '_');
        var response = await _httpClient.GetFromJsonAsync<MealDbMealsResponse>(
            $"filter.php?i={Uri.EscapeDataString(slug)}",
            cancellationToken);
        return response?.Meals ?? [];
    }

    public async Task<MealDbMeal?> LookupAsync(string mealId, CancellationToken cancellationToken)
    {
        var response = await _httpClient.GetFromJsonAsync<MealDbMealsResponse>(
            $"lookup.php?i={Uri.EscapeDataString(mealId)}",
            cancellationToken);
        return response?.Meals?.FirstOrDefault();
    }

    public async Task<IReadOnlyList<MealDbMeal>> SearchByNameAsync(string query, CancellationToken cancellationToken)
    {
        var response = await _httpClient.GetFromJsonAsync<MealDbMealsResponse>(
            $"search.php?s={Uri.EscapeDataString(query)}",
            cancellationToken);
        return response?.Meals ?? [];
    }

    public async Task<IReadOnlyList<MealDbMeal>> ListByFirstLetterAsync(char letter, CancellationToken cancellationToken)
    {
        var response = await _httpClient.GetFromJsonAsync<MealDbMealsResponse>(
            $"search.php?f={letter}",
            cancellationToken);
        _logger.LogDebug("MealDB letter search {Letter} returned {Count} meals", letter, response?.Meals?.Count ?? 0);
        return response?.Meals ?? [];
    }

    public async Task<IReadOnlyList<MealDbIngredientRow>> ListIngredientsAsync(CancellationToken cancellationToken)
    {
        var response = await _httpClient.GetFromJsonAsync<MealDbIngredientListResponse>(
            "list.php?i=list",
            cancellationToken);
        return response?.Meals ?? [];
    }
}
