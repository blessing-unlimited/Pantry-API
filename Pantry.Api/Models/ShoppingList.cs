namespace Pantry.Api.Models;

/// <summary>
/// A user-owned shopping list containing de-duplicated ingredients needed
/// for one or more selected recipes. Stored under users/{userId}/shoppingLists
/// in Firebase Realtime Database (Firebase, 2026).
/// </summary>
public sealed class ShoppingList
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public List<string> SourceRecipeIds { get; set; } = [];
    public List<ShoppingListItem> Items { get; set; } = [];
    public DateTimeOffset CreateAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
}

public sealed class ShoppingListItem
{
    public string IngredientName { get; set; } = string.Empty;
    public decimal? Quantity { get; set; }
    public string? Unit { get; set; }
    public bool IsChecked { get; set; }

    public List<string> SourceRecipeTitles { get; set; } = [];
}

public sealed class GenerateShoppingListRequest
{
    public List<string> RecipeIds { get; set; } = [];
    public string? Name { get; set; }

}

public sealed class UpdateShoppingListRequest
{
    public string? Name { get; set; }
    public List<ShoppingListItem>? Items { get; set; }
}

public sealed class UpdateShoppingListItemRequest
{
    public bool? IsChecked { get; set; }
    public decimal? Quantity { get; set; }
}
