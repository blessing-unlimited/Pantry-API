using System.Text.Json;

namespace Pantry.Api.Services;

/// <summary>
/// Builds the HTTP v1 Cloud Messaging body. Kept separate so the JSON shape
/// can be tested without a service account (Firebase, 2026).
/// </summary>
public static class FcmMessageFactory
{
    public static string ToJson(string token, string title, string body)
    {
        return JsonSerializer.Serialize(new
        {
            message = new
            {
                token,
                notification = new
                {
                    title,
                    body
                }
            }
        });
    }
}
