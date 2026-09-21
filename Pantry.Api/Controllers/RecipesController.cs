using Microsoft.AspNetCore.Mvc;
using Pantry.Api.Models;
using Pantry.Api.Services;

namespace Pantry.Api.Controllers;

[ApiController]
[Route("api/v1/recipes")]
public sealed class RecipesController : ControllerBase
{
    private readonly IRecipeMatchingService _matching;
    private readonly IUserStore _store;
    private readonly ILogger<RecipesController> _logger;

    public RecipesController(IRecipeMatchingService matching, IUserStore store, ILogger<RecipesController> logger)
    {
        _matching = matching;
        _store = store;
        _logger = logger;
    }

    [HttpGet]
    [ProducesResponseType(typeof(ApiEnvelope<IReadOnlyList<RecipeSummary>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<IReadOnlyList<RecipeSummary>>>> List(
        [FromQuery] string? q,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<IReadOnlyList<RecipeSummary>>.Fail("Authentication is required."));
        }

        var pantry = await _store.GetPantryAsync(userId, cancellationToken);
        var settings = await _store.GetSettingsAsync(userId, cancellationToken);
        var pantryNames = pantry.Select(i => i.IngredientName).ToList();

        IReadOnlyList<RecipeSummary> results;
        if (!string.IsNullOrWhiteSpace(q))
        {
            results = await _matching.SearchAsync(q, pantryNames, settings.DietaryPreferences, cancellationToken);
        }
        else
        {
            results = await _matching.MatchAsync(
                pantryNames,
                settings.DietaryPreferences,
                settings.MaxMissingIngredients,
                null,
                cancellationToken);
        }

        return Ok(ApiEnvelope<IReadOnlyList<RecipeSummary>>.Ok(results));
    }

    [HttpGet("{id}")]
    [ProducesResponseType(typeof(ApiEnvelope<RecipeDetail>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<RecipeDetail>>> GetById(string id, CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<RecipeDetail>.Fail("Authentication is required."));
        }

        var pantry = await _store.GetPantryAsync(userId, cancellationToken);
        var detail = await _matching.GetRecipeAsync(id, pantry.Select(i => i.IngredientName).ToList(), cancellationToken);
        if (detail is null)
        {
            return NotFound(ApiEnvelope<RecipeDetail>.Fail("Recipe was not found."));
        }

        return Ok(ApiEnvelope<RecipeDetail>.Ok(detail));
    }

    /// <summary>
    /// Reverse-pantry match: compare owned ingredients with TheMealDB recipes
    /// and return ranked results with missing-ingredient counts.
    /// </summary>
    [HttpPost("match")]
    [ProducesResponseType(typeof(ApiEnvelope<IReadOnlyList<RecipeSummary>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<IReadOnlyList<RecipeSummary>>>> Match(
        [FromBody] RecipeMatchRequest? request,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<IReadOnlyList<RecipeSummary>>.Fail("Authentication is required."));
        }

        var pantry = await _store.GetPantryAsync(userId, cancellationToken);
        var settings = await _store.GetSettingsAsync(userId, cancellationToken);
        var names = request?.IngredientNames is { Count: > 0 }
            ? request.IngredientNames
            : pantry.Select(i => i.IngredientName).ToList();
        var diets = request?.DietaryPreferences is { Count: > 0 }
            ? request.DietaryPreferences
            : settings.DietaryPreferences;
        var maxMissing = request?.MaxMissingIngredients ?? settings.MaxMissingIngredients;

        _logger.LogInformation("Matching {Count} pantry ingredients for {UserId}", names.Count, userId);
        var results = await _matching.MatchAsync(names, diets, maxMissing, request?.Query, cancellationToken);
        return Ok(ApiEnvelope<IReadOnlyList<RecipeSummary>>.Ok(results));
    }
}
