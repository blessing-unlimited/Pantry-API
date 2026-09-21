using Microsoft.AspNetCore.Mvc;
using Pantry.Api.Models;
using Pantry.Api.Services;

namespace Pantry.Api.Controllers;

[ApiController]
[Route("api/v1/ingredients")]
public sealed class IngredientsController : ControllerBase
{
    private readonly IRecipeMatchingService _matching;

    public IngredientsController(IRecipeMatchingService matching)
    {
        _matching = matching;
    }

    [HttpGet("search")]
    [ProducesResponseType(typeof(ApiEnvelope<IReadOnlyList<IngredientDto>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<IReadOnlyList<IngredientDto>>>> Search(
        [FromQuery] string q = "",
        CancellationToken cancellationToken = default)
    {
        if (User.GetUserId() is null)
        {
            return Unauthorized(ApiEnvelope<IReadOnlyList<IngredientDto>>.Fail("Authentication is required."));
        }

        var results = await _matching.SearchIngredientsAsync(q, cancellationToken);
        return Ok(ApiEnvelope<IReadOnlyList<IngredientDto>>.Ok(results));
    }
}
