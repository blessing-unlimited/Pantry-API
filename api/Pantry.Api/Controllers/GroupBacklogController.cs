using Microsoft.AspNetCore.Mvc;
using Pantry.Api.Models;

namespace Pantry.Api.Controllers;

/// <summary>
/// Assigned to Blessing. These routes are documented in Part 1 and must be
/// implemented against Firebase plus pantry/recipe data. Returning 501 keeps
/// the contract visible in Swagger without pretending the feature is done.
/// </summary>
[ApiController]
[Route("api/v1")]
public sealed class GroupBacklogController : ControllerBase
{
    /// <summary>Blessing: generate a de-duplicated shopping list from recipe ids.</summary>
    [HttpPost("shopping-lists/generate")]
    [ProducesResponseType(StatusCodes.Status501NotImplemented)]
    public IActionResult GenerateShoppingList() =>
        StatusCode(StatusCodes.Status501NotImplemented,
            ApiEnvelope<string>.Fail("Assigned to Blessing: implement shopping-list generation."));

    [HttpGet("shopping-lists")]
    [ProducesResponseType(StatusCodes.Status501NotImplemented)]
    public IActionResult ListShoppingLists() =>
        StatusCode(StatusCodes.Status501NotImplemented,
            ApiEnvelope<string>.Fail("Assigned to Blessing: implement shopping-list CRUD."));

    /// <summary>Blessing: return pantry-aware substitutions for a missing ingredient.</summary>
    [HttpGet("substitutions/{ingredientId}")]
    [ProducesResponseType(StatusCodes.Status501NotImplemented)]
    public IActionResult Substitutions(string ingredientId) =>
        StatusCode(StatusCodes.Status501NotImplemented,
            ApiEnvelope<string>.Fail($"Assigned to Blessing: substitutions for '{ingredientId}'."));
}
