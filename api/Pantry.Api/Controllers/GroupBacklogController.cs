using Microsoft.AspNetCore.Mvc;
using Pantry.Api.Models;
using Pantry.Api.Services;

namespace Pantry.Api.Controllers;

/// <summary>
/// Shopping list generation, CRUD, and ingredient substitutions. Recipe
/// ingredient data comes live from TheMealDB via injected
/// IRecipeMatchingService; persistence flows through IUserStore
/// (TheMealDB, 2026; Firebase, 2026).
/// </summary>
[ApiController]
[Route("api/v1")]
public sealed class GroupBacklogController : ControllerBase
{
    private readonly IUserStore _store;
    private readonly IRecipeMatchingService _recipeService;
    private readonly ILogger<GroupBacklogController> _logger;

    public GroupBacklogController(IUserStore store, IRecipeMatchingService recipeService, ILogger<GroupBacklogController> logger)
    {
        _store = store;
        _recipeService = recipeService;
        _logger = logger;
    }

    /// <summary>Generate a de-duplicated shopping list from MealDB recipe ids.</summary>
    [HttpPost("shopping-lists/generate")]
    [ProducesResponseType(typeof(ApiEnvelope<ShoppingList>), StatusCodes.Status201Created)]
    public async Task<ActionResult<ApiEnvelope<ShoppingList>>> GenerateShoppingList(
        [FromBody] GenerateShoppingListRequest request,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<ShoppingList>.Fail("Authentication is required."));
        }

        if (request.RecipeIds is null || request.RecipeIds.Count == 0)
        {
            return BadRequest(ApiEnvelope<ShoppingList>.Fail("At least one recipeId is required."));
        }

        var pantry = await _store.GetPantryAsync(userId, cancellationToken);
        var pantryNames = pantry
            .Select(p => ShoppingListGenerator.Normalize(p.IngredientName))
            .ToHashSet(StringComparer.OrdinalIgnoreCase);

        var recipesWithTitles = new List<(RecipeDetail Detail, string Title)>();
        foreach (var rid in request.RecipeIds.Distinct())
        {
            RecipeDetail? detail;
            try
            {
                detail = await _recipeService.GetRecipeAsync(rid, pantryNames.ToList(), cancellationToken);
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "MealDB lookup failed for meal {MealId}", rid);
                continue;
            }

            if (detail is null)
            {
                continue;
            }

            recipesWithTitles.Add((detail, detail.Title));
        }

        if (recipesWithTitles.Count == 0)
        {
            return NotFound(ApiEnvelope<ShoppingList>.Fail("No valid recipes were found for the provided ids."));
        }

        var mergedItems = ShoppingListGenerator.MergeIngredients(recipesWithTitles, pantryNames);
        var now = DateTimeOffset.UtcNow;
        var list = new ShoppingList
        {
            Name = string.IsNullOrWhiteSpace(request.Name)
                ? $"Shopping list - {now:yyyy-MM-dd}"
                : request.Name.Trim(),
            SourceRecipeIds = request.RecipeIds.Distinct().ToList(),
            Items = mergedItems,
            CreateAt = now,
            UpdatedAt = now
        };

        var saved = await _store.AddShoppingListAsync(userId, list, cancellationToken);
        _logger.LogInformation(
            "User {UserId} generated shopping list {ListId} with {Count} items",
            userId,
            saved.Id,
            saved.Items.Count);

        return CreatedAtAction(nameof(GetShoppingList), new { id = saved.Id }, ApiEnvelope<ShoppingList>.Ok(saved));
    }

    [HttpGet("shopping-lists")]
    [ProducesResponseType(typeof(ApiEnvelope<IReadOnlyList<ShoppingList>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<IReadOnlyList<ShoppingList>>>> ListShoppingLists(
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<IReadOnlyList<ShoppingList>>.Fail("Authentication is required."));
        }

        var lists = await _store.GetShoppingListsAsync(userId, cancellationToken);
        return Ok(ApiEnvelope<IReadOnlyList<ShoppingList>>.Ok(lists));
    }

    [HttpGet("shopping-lists/{id}")]
    [ProducesResponseType(typeof(ApiEnvelope<ShoppingList>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<ShoppingList>>> GetShoppingList(
        string id,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<ShoppingList>.Fail("Authentication is required."));
        }

        var list = await _store.GetShoppingListAsync(userId, id, cancellationToken);
        if (list is null)
        {
            return NotFound(ApiEnvelope<ShoppingList>.Fail("Shopping list was not found."));
        }

        return Ok(ApiEnvelope<ShoppingList>.Ok(list));
    }

    [HttpPatch("shopping-lists/{id}")]
    [ProducesResponseType(typeof(ApiEnvelope<ShoppingList>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<ShoppingList>>> UpdateShoppingList(
        string id,
        [FromBody] UpdateShoppingListRequest request,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<ShoppingList>.Fail("Authentication is required."));
        }

        var existing = await _store.GetShoppingListAsync(userId, id, cancellationToken);
        if (existing is null)
        {
            return NotFound(ApiEnvelope<ShoppingList>.Fail("Shopping list was not found."));
        }

        if (!string.IsNullOrWhiteSpace(request.Name))
        {
            existing.Name = request.Name.Trim();
        }

        if (request.Items is not null)
        {
            existing.Items = request.Items;
        }

        existing.UpdatedAt = DateTimeOffset.UtcNow;
        var updated = await _store.UpdateShoppingListAsync(userId, id, existing, cancellationToken);
        return Ok(ApiEnvelope<ShoppingList>.Ok(updated!));
    }

    [HttpDelete("shopping-lists/{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> DeleteShoppingList(string id, CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<ShoppingList>.Fail("Authentication is required."));
        }

        var deleted = await _store.DeleteShoppingListAsync(userId, id, cancellationToken);
        if (!deleted)
        {
            return NotFound(ApiEnvelope<ShoppingList>.Fail("Shopping list was not found."));
        }

        _logger.LogInformation("User {UserId} deleted shopping list {ListId}", userId, id);
        return NoContent();
    }

    /// <summary>Return pantry-aware substitutions for a missing ingredient.</summary>
    [HttpGet("substitutions/{ingredientId}")]
    [ProducesResponseType(typeof(ApiEnvelope<IReadOnlyList<SubstitutionDto>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<IReadOnlyList<SubstitutionDto>>>> Substitutions(
        string ingredientId,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<IReadOnlyList<SubstitutionDto>>.Fail("Authentication is required."));
        }

        if (string.IsNullOrWhiteSpace(ingredientId))
        {
            return BadRequest(ApiEnvelope<IReadOnlyList<SubstitutionDto>>.Fail("ingredientId is required."));
        }

        var pantry = await _store.GetPantryAsync(userId, cancellationToken);
        var pantryNames = pantry
            .Select(p => ShoppingListGenerator.Normalize(p.IngredientName))
            .ToHashSet(StringComparer.OrdinalIgnoreCase);

        var result = new List<SubstitutionDto>();
        foreach (var (alt, ratio) in SubstitutionLookup.GetAlternatives(ingredientId))
        {
            result.Add(new SubstitutionDto
            {
                IngredientName = ingredientId.Trim(),
                AlternativeName = alt,
                InPantry = pantryNames.Contains(ShoppingListGenerator.Normalize(alt)),
                RatioHint = ratio
            });
        }

        return Ok(ApiEnvelope<IReadOnlyList<SubstitutionDto>>.Ok(
            result.OrderByDescending(s => s.InPantry).ThenBy(s => s.AlternativeName).ToList()));
    }
}
