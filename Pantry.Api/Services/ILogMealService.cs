using Pantry.Api.DTO;

namespace Pantry.Api.Services
{
    public interface ILogMealService
    {
        Task<LogMealRecognitionResult?> RecognizeImageAsync(IFormFile image, CancellationToken cancellationToken = default);
    }
}
