using Microsoft.AspNetCore.Mvc;
using Pantry.Api.Models;
using Pantry.Api.Services;

namespace Pantry.Api.Controllers;

[ApiController]
[Route("api/v1/pantry-items")]
public sealed class PantryItemsController : ControllerBase
{
    private readonly IUserStore _store;
    private readonly IPushNotificationSender _push;
    private readonly ILogger<PantryItemsController> _logger;

    public PantryItemsController(
        IUserStore store,
        IPushNotificationSender push,
        ILogger<PantryItemsController> logger)
    {
        _store = store;
        _push = push;
        _logger = logger;
    }

    [HttpGet]
    [ProducesResponseType(typeof(ApiEnvelope<IReadOnlyList<PantryItem>>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<IReadOnlyList<PantryItem>>>> List(CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<IReadOnlyList<PantryItem>>.Fail("Authentication is required."));
        }

        var items = await _store.GetPantryAsync(userId, cancellationToken);
        return Ok(ApiEnvelope<IReadOnlyList<PantryItem>>.Ok(items));
    }

    [HttpPost]
    [ProducesResponseType(typeof(ApiEnvelope<PantryItem>), StatusCodes.Status201Created)]
    public async Task<ActionResult<ApiEnvelope<PantryItem>>> Create(
        [FromBody] CreatePantryItemRequest request,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<PantryItem>.Fail("Authentication is required."));
        }

        if (string.IsNullOrWhiteSpace(request.IngredientName))
        {
            return BadRequest(ApiEnvelope<PantryItem>.Fail("ingredientName is required."));
        }

        if (request.Quantity is < 0)
        {
            return BadRequest(ApiEnvelope<PantryItem>.Fail("quantity cannot be negative."));
        }

        var now = DateTimeOffset.UtcNow;
        var created = await _store.AddPantryItemAsync(userId, new PantryItem
        {
            IngredientName = request.IngredientName.Trim(),
            Quantity = request.Quantity,
            Unit = string.IsNullOrWhiteSpace(request.Unit) ? null : request.Unit.Trim(),
            ImageUrl = string.IsNullOrWhiteSpace(request.ImageUrl) ? null : request.ImageUrl.Trim(),
            CreatedAt = now,
            UpdatedAt = now
        }, cancellationToken);

        _logger.LogInformation("User {UserId} added pantry item {Name}", userId, created.IngredientName);
        await TryNotifyAddedAsync(userId, created.IngredientName, cancellationToken);
        return CreatedAtAction(nameof(GetById), new { id = created.Id }, ApiEnvelope<PantryItem>.Ok(created));
    }

    [HttpGet("{id}")]
    [ProducesResponseType(typeof(ApiEnvelope<PantryItem>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PantryItem>>> GetById(string id, CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<PantryItem>.Fail("Authentication is required."));
        }

        var item = await _store.GetPantryItemAsync(userId, id, cancellationToken);
        if (item is null)
        {
            return NotFound(ApiEnvelope<PantryItem>.Fail("Pantry item was not found."));
        }

        return Ok(ApiEnvelope<PantryItem>.Ok(item));
    }

    [HttpPatch("{id}")]
    [ProducesResponseType(typeof(ApiEnvelope<PantryItem>), StatusCodes.Status200OK)]
    public async Task<ActionResult<ApiEnvelope<PantryItem>>> Update(
        string id,
        [FromBody] UpdatePantryItemRequest request,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<PantryItem>.Fail("Authentication is required."));
        }

        var existing = await _store.GetPantryItemAsync(userId, id, cancellationToken);
        if (existing is null)
        {
            return NotFound(ApiEnvelope<PantryItem>.Fail("Pantry item was not found."));
        }

        if (request.Quantity is < 0)
        {
            return BadRequest(ApiEnvelope<PantryItem>.Fail("quantity cannot be negative."));
        }

        existing.IngredientName = string.IsNullOrWhiteSpace(request.IngredientName)
            ? existing.IngredientName
            : request.IngredientName.Trim();
        existing.Quantity = request.Quantity ?? existing.Quantity;
        existing.Unit = request.Unit ?? existing.Unit;
        if (request.ImageUrl is not null)
        {
            existing.ImageUrl = string.IsNullOrWhiteSpace(request.ImageUrl) ? null : request.ImageUrl.Trim();
        }
        existing.UpdatedAt = DateTimeOffset.UtcNow;

        var updated = await _store.UpdatePantryItemAsync(userId, id, existing, cancellationToken);
        return Ok(ApiEnvelope<PantryItem>.Ok(updated!));
    }

    [HttpDelete("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> Delete(string id, CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
        {
            return Unauthorized(ApiEnvelope<PantryItem>.Fail("Authentication is required."));
        }

        var deleted = await _store.DeletePantryItemAsync(userId, id, cancellationToken);
        if (!deleted)
        {
            return NotFound(ApiEnvelope<PantryItem>.Fail("Pantry item was not found."));
        }

        _logger.LogInformation("User {UserId} deleted pantry item {Id}", userId, id);
        return NoContent();
    }

    /// <summary>
    /// A pantry write can raise a Cloud Messaging alert when the user opted in
    /// and a device token is stored (Firebase, 2026). Failure to send does not
    /// fail the write.
    /// </summary>
    private async Task TryNotifyAddedAsync(string userId, string ingredientName, CancellationToken cancellationToken)
    {
        if (!_push.IsConfigured)
        {
            return;
        }

        try
        {
            var settings = await _store.GetSettingsAsync(userId, cancellationToken);
            if (!settings.NotificationsEnabled)
            {
                return;
            }

            var profile = await _store.GetProfileAsync(userId, cancellationToken);
            if (string.IsNullOrWhiteSpace(profile?.FcmToken))
            {
                _logger.LogInformation("Push skipped for {UserId}: no device token", userId);
                return;
            }

            await _push.SendAsync(
                profile.FcmToken,
                "Pantry",
                $"{ingredientName} was added to your pantry.",
                cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Push notification was not sent for {UserId}", userId);
        }
    }
}
