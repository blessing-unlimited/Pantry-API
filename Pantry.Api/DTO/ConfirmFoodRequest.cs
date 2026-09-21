using System.ComponentModel.DataAnnotations;

namespace Pantry.Api.DTO
{

    /// <summary>
    /// Request body for confirming which food is correct
    /// </summary>
    public class ConfirmFoodRequest
    {
        //The imageId is returned by the /identify endpoint.
        [Required]
        public long ImageId { get; set; }

        //The food the user picked, from the list of identified candidate names form /identify
        [Required]
        public string SelectedFood { get; set; } = string.Empty;

        //Set to true to accept a food name the API did not suggets. All the suggestions were wrong.
        public bool AllowCustomName { get; set; } = false;
    }

    /// <summary>
    /// The response returned after the user confirms a food.
    /// </summary>
    public class ConfirmFoodResponse
    {
        public long ImageId { get; set; }
        public string ConfirmedFood { get; set; } = string.Empty;
        public bool WasFromSuggestions { get; set; }
        public double? Confidence { get; set; }
        public string Message { get; set; } = string.Empty;
    }

    public class IdentifyFoodResponse
    {
        public long ImageId { get; set; }
        public List<string> Foods { get; set; } = [];
        public IdentifyTopMatch? TopMatch { get; set; }
        public string? Message { get; set; }
    }

    public class IdentifyTopMatch
    {
        public string Name { get; set; } = string.Empty;
        public double Confidence { get; set; }
    }
}
