namespace Pantry.Api.Models;

public sealed class PantryItem
{
    public string Id { get; set; } = string.Empty;

    public string IngredientName { get; set; } = string.Empty;

    public decimal? Quantity { get; set; }

    public string? Unit { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public DateTimeOffset UpdatedAt { get; set; }
}

public sealed class CreatePantryItemRequest
{
    public string IngredientName { get; set; } = string.Empty;

    public decimal? Quantity { get; set; }

    public string? Unit { get; set; }
}

public sealed class UpdatePantryItemRequest
{
    public string? IngredientName { get; set; }

    public decimal? Quantity { get; set; }

    public string? Unit { get; set; }
}
