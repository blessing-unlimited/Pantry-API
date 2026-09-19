namespace Pantry.Api.Models;

/// <summary>Pantry-aware ingredient substitution result (The Spruce Eats, 2023).</summary>
public sealed class SubstitutionDto
{
    public string IngredientName { get; set; } = string.Empty;
    public string AlternativeName { get; set; } = string.Empty;
    /// <summary>True if the user already owns this alternative in their pantry.</summary>
    public bool InPantry { get; set; }
    public string? RatioHint { get; set; }
}
