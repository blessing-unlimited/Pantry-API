using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Options;
using Pantry.Api.Options;

namespace Pantry.Api.Services;

public interface IPushNotificationSender
{
    bool IsConfigured { get; }

    Task SendAsync(string deviceToken, string title, string body, CancellationToken cancellationToken);
}

/// <summary>
/// Sends one Firebase Cloud Messaging HTTP v1 message. The service-account
/// JSON stays in host configuration and is never written to the repository
/// (Firebase, 2026).
/// </summary>
public sealed class FcmPushNotificationSender : IPushNotificationSender
{
    private readonly HttpClient _http;
    private readonly FirebaseOptions _options;
    private readonly ILogger<FcmPushNotificationSender> _logger;
    private readonly object _gate = new();
    private string? _accessToken;
    private DateTimeOffset _accessTokenExpiry = DateTimeOffset.MinValue;

    public FcmPushNotificationSender(
        HttpClient http,
        IOptions<FirebaseOptions> options,
        ILogger<FcmPushNotificationSender> logger)
    {
        _http = http;
        _options = options.Value;
        _logger = logger;
    }

    public bool IsConfigured => LoadAccount() is not null;

    public async Task SendAsync(string deviceToken, string title, string body, CancellationToken cancellationToken)
    {
        var account = LoadAccount()
            ?? throw new InvalidOperationException("Firebase Cloud Messaging is not configured.");

        var accessToken = await GetAccessTokenAsync(account, cancellationToken);
        var url = $"https://fcm.googleapis.com/v1/projects/{account.ProjectId}/messages:send";
        using var request = new HttpRequestMessage(HttpMethod.Post, url);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
        request.Content = new StringContent(FcmMessageFactory.ToJson(deviceToken, title, body), Encoding.UTF8, "application/json");

        var response = await _http.SendAsync(request, cancellationToken);
        if (!response.IsSuccessStatusCode)
        {
            _logger.LogWarning("FCM send failed with status {Status}", (int)response.StatusCode);
            throw new InvalidOperationException($"Firebase Cloud Messaging rejected the message ({(int)response.StatusCode}).");
        }

        _logger.LogInformation("FCM message accepted");
    }

    private async Task<string> GetAccessTokenAsync(ServiceAccount account, CancellationToken cancellationToken)
    {
        lock (_gate)
        {
            if (!string.IsNullOrWhiteSpace(_accessToken) && _accessTokenExpiry > DateTimeOffset.UtcNow.AddMinutes(1))
            {
                return _accessToken;
            }
        }

        var assertion = CreateAssertion(account);
        using var content = new FormUrlEncodedContent(new Dictionary<string, string>
        {
            ["grant_type"] = "urn:ietf:params:oauth:grant-type:jwt-bearer",
            ["assertion"] = assertion
        });
        var response = await _http.PostAsync("https://oauth2.googleapis.com/token", content, cancellationToken);
        var json = await response.Content.ReadAsStringAsync(cancellationToken);
        if (!response.IsSuccessStatusCode)
        {
            _logger.LogWarning("FCM OAuth token request failed with status {Status}", (int)response.StatusCode);
            throw new InvalidOperationException("Firebase Cloud Messaging could not obtain an access token.");
        }

        using var document = JsonDocument.Parse(json);
        var token = document.RootElement.GetProperty("access_token").GetString()
            ?? throw new InvalidOperationException("Firebase Cloud Messaging access token was empty.");
        var expiresIn = document.RootElement.TryGetProperty("expires_in", out var expires)
            ? expires.GetInt32()
            : 3600;

        lock (_gate)
        {
            _accessToken = token;
            _accessTokenExpiry = DateTimeOffset.UtcNow.AddSeconds(expiresIn);
        }

        return token;
    }

    private static string CreateAssertion(ServiceAccount account)
    {
        var header = Base64Url(Encoding.UTF8.GetBytes("{\"alg\":\"RS256\",\"typ\":\"JWT\"}"));
        var now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var payloadJson = JsonSerializer.Serialize(new
        {
            iss = account.ClientEmail,
            scope = "https://www.googleapis.com/auth/firebase.messaging",
            aud = "https://oauth2.googleapis.com/token",
            iat = now,
            exp = now + 3600
        });
        var payload = Base64Url(Encoding.UTF8.GetBytes(payloadJson));
        var unsigned = $"{header}.{payload}";
        using var rsa = RSA.Create();
        rsa.ImportFromPem(account.PrivateKey);
        var signature = rsa.SignData(Encoding.UTF8.GetBytes(unsigned), HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
        return $"{unsigned}.{Base64Url(signature)}";
    }

    private ServiceAccount? LoadAccount()
    {
        var json = _options.ServiceAccountJson;
        if (string.IsNullOrWhiteSpace(json) &&
            !string.IsNullOrWhiteSpace(_options.ServiceAccountPath) &&
            File.Exists(_options.ServiceAccountPath))
        {
            json = File.ReadAllText(_options.ServiceAccountPath);
        }

        if (string.IsNullOrWhiteSpace(json))
        {
            return null;
        }

        try
        {
            using var document = JsonDocument.Parse(json);
            var root = document.RootElement;
            var email = root.TryGetProperty("client_email", out var emailElement) ? emailElement.GetString() : null;
            var key = root.TryGetProperty("private_key", out var keyElement) ? keyElement.GetString() : null;
            var project = root.TryGetProperty("project_id", out var projectElement) ? projectElement.GetString() : null;
            if (string.IsNullOrWhiteSpace(project))
            {
                project = _options.ProjectId;
            }

            if (string.IsNullOrWhiteSpace(email) || string.IsNullOrWhiteSpace(key) || string.IsNullOrWhiteSpace(project))
            {
                _logger.LogWarning("Firebase service account is missing client_email, private_key, or project_id");
                return null;
            }

            return new ServiceAccount(email, key, project);
        }
        catch (JsonException)
        {
            _logger.LogWarning("Firebase service account JSON could not be parsed");
            return null;
        }
    }

    private static string Base64Url(byte[] data) =>
        Convert.ToBase64String(data).TrimEnd('=').Replace('+', '-').Replace('/', '_');

    private sealed record ServiceAccount(string ClientEmail, string PrivateKey, string ProjectId);
}
