using Pantry.Api.DTO;
using SkiaSharp;
using System.Net.Http.Headers;
using System.Text.Json;

namespace Pantry.Api.Services
{
    public class LogMealService : ILogMealService
    {
        private readonly HttpClient _httpClient;
        private readonly ILogger<LogMealService> _logger;

        public LogMealService(HttpClient httpClient, ILogger<LogMealService> logger)
        {
            _httpClient = httpClient;
            _logger = logger;
        }

        public async Task<LogMealRecognitionResult?> RecognizeImageAsync(IFormFile image, CancellationToken cancellationToken = default)
        {
            //If the image is null then return null
            if (image == null || image.Length == 0)
            {
                _logger.LogError("RecognizeImageAsync received a null or empty image (length: {Length}).", image?.Length ?? 0);
                return null;
            }

            //If image is not null, log the metadata about the image.
            _logger.LogInformation("RecognizeImageAsync received image {FileName}, ContentType {ContentType}, size {Size} bytes.", image.FileName, image.ContentType, image.Length);

            // LogMeal only accepts JPEG ('jpeg'/'jpg'). Re-encode anything else.
            byte[] imageBytes;
            string fileName;

            var contentType = image.ContentType ?? string.Empty;
            if (contentType.Equals("image/jpeg", StringComparison.OrdinalIgnoreCase) ||
                contentType.Equals("image/jpg", StringComparison.OrdinalIgnoreCase))
            {
                using var ms = new MemoryStream();
                await image.CopyToAsync(ms, cancellationToken);
                imageBytes = ms.ToArray();
                fileName = image.FileName;
            }
            else
            {
                // Decode PNG/WebP/etc. and re-encode as JPEG via SkiaSharp
                using var skBitmap = SKBitmap.Decode(image.OpenReadStream());
                if (skBitmap == null)
                {
                    _logger.LogError("Could not decode uploaded image {FileName} (content type {ContentType}).", image.FileName, image.ContentType);
                    return null;
                }

                using var skImage = SKImage.FromBitmap(skBitmap);
                if (skImage == null)
                {
                    _logger.LogError("Could not create SKImage from decoded bitmap for {FileName}.", image.FileName);
                    return null;
                }

                using var jpegData = skImage.Encode(SKEncodedImageFormat.Jpeg, 90);
                if (jpegData == null || jpegData.IsEmpty)
                {
                    _logger.LogError("JPEG encoding returned no data for {FileName}.", image.FileName);
                    return null;
                }

                imageBytes = jpegData.ToArray();
                fileName = Path.ChangeExtension(image.FileName, ".jpg");
            }

            // Build multipart/form-data content
            using var content = new MultipartFormDataContent();

            var fileContent = new ByteArrayContent(imageBytes);

            // LogMeal expects the file field to be named "image"
            fileContent.Headers.ContentType = new MediaTypeHeaderValue("image/jpeg");

            content.Add(fileContent, "image", fileName);

            // Build the request
            var request = new HttpRequestMessage(HttpMethod.Post, "https://api.logmeal.com/v2/image/segmentation/complete")
            {
                Content = content
            };

            // Authorization header is already set on the HttpClient
            request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

            // Send the request
            HttpResponseMessage response;
            try
            {
                response = await _httpClient.SendAsync(request, cancellationToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Exception while calling LogMeal API: {Message}", ex.Message);
                return null;
            }

            if (!response.IsSuccessStatusCode)
            {
                var errorBody = await response.Content.ReadAsStringAsync(cancellationToken);
                _logger.LogError(
                    "LogMeal API returned {StatusCode}: {ErrorBody}",
                    (int)response.StatusCode,
                    errorBody);
                return null;
            }

            // Deserialize the response (Microsoft, 2026b)
            var json = await response.Content.ReadAsStringAsync(cancellationToken);
            var result = JsonSerializer.Deserialize<LogMealRecognitionResult>(
                json,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

            return result;
        }

    }
}
