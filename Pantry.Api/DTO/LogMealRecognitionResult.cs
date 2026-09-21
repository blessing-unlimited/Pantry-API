using System.Text.Json.Serialization;

namespace Pantry.Api.DTO
{
    public class LogMealRecognitionResult
    {
        [JsonPropertyName("imageId")]
        public long ImageId { get; set; }

        [JsonPropertyName("segmentation_results")]
        public List<SegmentationResult>? SegmenationResults { get; set; }
    }

    public class SegmentationResult
    {
        [JsonPropertyName("foodName")]
        public string FoodName { get; set; } = string.Empty;

        [JsonPropertyName("foodId")]
        public int FoodId { get; set; }

        [JsonPropertyName("probability")]
        public double Probability { get; set; }

        // LogMeal may return the recognized dish under different keys
        [JsonPropertyName("recognition_results")]
        public List<RecognitionCandidate> RecognitionResults { get; set; } = new();
    }

    public class RecognitionCandidate
    {
        [JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;

        [JsonPropertyName("id")]
        public int Id { get; set; }

        [JsonPropertyName("prob")]
        public double Probability { get; set; }
    }
}
