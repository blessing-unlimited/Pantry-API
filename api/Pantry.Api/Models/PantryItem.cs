namespace Pantry.Api.Models;

public sealed class PantryItem
{
    public string Id { get; set; } = string.Empty;

    public string IngredientName { get; set; } = string.Empty;

    public decimal? Quantity { get; set; }

    public string? Unit { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public DateTimeOffset UpdatedAt { get; set; }

    /// <summary>Download URL for a scan photo stored in blob storage (Firebase, 2026).</summary>
    public string? ImageUrl { get; set; }
}

public sealed class CreatePantryItemRequest
{
    public string IngredientName { get; set; } = string.Empty;

    public decimal? Quantity { get; set; }

    public string? Unit { get; set; }

    public string? ImageUrl { get; set; }
}

public sealed class UpdatePantryItemRequest
{
    public string? IngredientName { get; set; }

    public decimal? Quantity { get; set; }

    public string? Unit { get; set; }

    public string? ImageUrl { get; set; }
}
