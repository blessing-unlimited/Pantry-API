using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.Extensions.Options;
using Pantry.Api.Models;
using Pantry.Api.Options;

namespace Pantry.Api.Services;

public interface IUserStore
{
    Task<UserProfile?> GetProfileAsync(string userId, CancellationToken cancellationToken);

    Task SaveProfileAsync(UserProfile profile, CancellationToken cancellationToken);

    Task<UserSettings> GetSettingsAsync(string userId, CancellationToken cancellationToken);

    Task<UserSettings> SaveSettingsAsync(string userId, UserSettings settings, CancellationToken cancellationToken);

    Task<IReadOnlyList<PantryItem>> GetPantryAsync(string userId, CancellationToken cancellationToken);

    Task<PantryItem?> GetPantryItemAsync(string userId, string itemId, CancellationToken cancellationToken);

    Task<PantryItem> AddPantryItemAsync(string userId, PantryItem item, CancellationToken cancellationToken);

    Task<PantryItem?> UpdatePantryItemAsync(string userId, string itemId, PantryItem item, CancellationToken cancellationToken);

    Task<bool> DeletePantryItemAsync(string userId, string itemId, CancellationToken cancellationToken);

    Task<IReadOnlyList<ShoppingList>> GetShoppingListsAsync(string userId, CancellationToken cancellationToken);

    Task<ShoppingList?> GetShoppingListAsync(string userId, string listId, CancellationToken cancellationToken);

    Task<ShoppingList> AddShoppingListAsync(string userId, ShoppingList list, CancellationToken cancellationToken);

    Task<ShoppingList?> UpdateShoppingListAsync(string userId, string listId, ShoppingList list, CancellationToken cancellationToken);

    Task<bool> DeleteShoppingListAsync(string userId, string listId, CancellationToken cancellationToken);
}

/// <summary>
/// Firebase Realtime Database REST client. Paths are scoped per authenticated
/// user so one account cannot read another user's pantry (OWASP, 2023).
/// </summary>
public sealed class FirebaseUserStore : IUserStore
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        PropertyNameCaseInsensitive = true,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
    };

    private readonly HttpClient _httpClient;
    private readonly FirebaseOptions _options;
    private readonly ILogger<FirebaseUserStore> _logger;

    public FirebaseUserStore(HttpClient httpClient, IOptions<FirebaseOptions> options, ILogger<FirebaseUserStore> logger)
    {
        _httpClient = httpClient;
        _options = options.Value;
        _logger = logger;

        var baseUrl = (_options.BaseUrl ?? string.Empty).Trim();
        if (Uri.TryCreate(baseUrl.TrimEnd('/') + "/", UriKind.Absolute, out var uri))
        {
            _httpClient.BaseAddress = uri;
        }
    }

    public async Task<UserProfile?> GetProfileAsync(string userId, CancellationToken cancellationToken)
    {
        return await GetJsonAsync<UserProfile>($"users/{userId}/profile.json", cancellationToken);
    }

    public async Task SaveProfileAsync(UserProfile profile, CancellationToken cancellationToken)
    {
        await PutJsonAsync($"users/{profile.UserId}/profile.json", profile, cancellationToken);
    }

    public async Task<UserSettings> GetSettingsAsync(string userId, CancellationToken cancellationToken)
    {
        var settings = await GetJsonAsync<UserSettings>($"users/{userId}/settings.json", cancellationToken);
        return settings ?? new UserSettings();
    }

    public async Task<UserSettings> SaveSettingsAsync(string userId, UserSettings settings, CancellationToken cancellationToken)
    {
        await PutJsonAsync($"users/{userId}/settings.json", settings, cancellationToken);
        return settings;
    }

    public async Task<IReadOnlyList<PantryItem>> GetPantryAsync(string userId, CancellationToken cancellationToken)
    {
        var nodes = await GetJsonAsync<Dictionary<string, PantryItem>>($"users/{userId}/pantryItems.json", cancellationToken);
        if (nodes is null || nodes.Count == 0)
        {
            return [];
        }

        var items = new List<PantryItem>();
        foreach (var (id, item) in nodes)
        {
            item.Id = id;
            items.Add(item);
        }

        return items.OrderBy(i => i.IngredientName, StringComparer.OrdinalIgnoreCase).ToList();
    }

    public async Task<PantryItem?> GetPantryItemAsync(string userId, string itemId, CancellationToken cancellationToken)
    {
        var item = await GetJsonAsync<PantryItem>($"users/{userId}/pantryItems/{itemId}.json", cancellationToken);
        if (item is null)
        {
            return null;
        }

        item.Id = itemId;
        return item;
    }

    public async Task<PantryItem> AddPantryItemAsync(string userId, PantryItem item, CancellationToken cancellationToken)
    {
        EnsureConfigured();
        var response = await _httpClient.PostAsJsonAsync(
            BuildPath($"users/{userId}/pantryItems.json"),
            item,
            JsonOptions,
            cancellationToken);
        var body = await ReadBodyAsync(response, cancellationToken);
        var created = JsonSerializer.Deserialize<FirebaseNameResponse>(body, JsonOptions);
        if (string.IsNullOrWhiteSpace(created?.Name))
        {
            throw new InvalidOperationException("Firebase did not return a generated pantry item id.");
        }

        item.Id = created.Name;
        _logger.LogInformation("Created pantry item {ItemId} for user {UserId}", item.Id, userId);
        return item;
    }

    public async Task<PantryItem?> UpdatePantryItemAsync(string userId, string itemId, PantryItem item, CancellationToken cancellationToken)
    {
        var existing = await GetPantryItemAsync(userId, itemId, cancellationToken);
        if (existing is null)
        {
            return null;
        }

        item.Id = itemId;
        item.CreatedAt = existing.CreatedAt;
        await PutJsonAsync($"users/{userId}/pantryItems/{itemId}.json", item, cancellationToken);
        return item;
    }

    public async Task<bool> DeletePantryItemAsync(string userId, string itemId, CancellationToken cancellationToken)
    {
        var existing = await GetPantryItemAsync(userId, itemId, cancellationToken);
        if (existing is null)
        {
            return false;
        }

        EnsureConfigured();
        var response = await _httpClient.DeleteAsync(BuildPath($"users/{userId}/pantryItems/{itemId}.json"), cancellationToken);
        await ReadBodyAsync(response, cancellationToken);
        return true;
    }

    public async Task<IReadOnlyList<ShoppingList>> GetShoppingListsAsync(string userId, CancellationToken cancellationToken)
    {
        var nodes = await GetJsonAsync<Dictionary<string, ShoppingList>>($"users/{userId}/shoppingLists.json", cancellationToken);
        if (nodes is null || nodes.Count == 0)
        {
            return [];
        }

        var lists = new List<ShoppingList>();
        foreach (var (id, list) in nodes)
        {
            list.Id = id;
            lists.Add(list);
        }

        return lists.OrderByDescending(l => l.CreateAt).ToList();
    }

    public async Task<ShoppingList?> GetShoppingListAsync(string userId, string listId, CancellationToken cancellationToken)
    {
        var list = await GetJsonAsync<ShoppingList>($"users/{userId}/shoppingLists/{listId}.json", cancellationToken);
        if (list is null)
        {
            return null;
        }

        list.Id = listId;
        return list;
    }

    public async Task<ShoppingList> AddShoppingListAsync(string userId, ShoppingList list, CancellationToken cancellationToken)
    {
        EnsureConfigured();
        var response = await _httpClient.PostAsJsonAsync(
            BuildPath($"users/{userId}/shoppingLists.json"),
            list,
            JsonOptions,
            cancellationToken);
        var body = await ReadBodyAsync(response, cancellationToken);
        var created = JsonSerializer.Deserialize<FirebaseNameResponse>(body, JsonOptions);
        if (string.IsNullOrWhiteSpace(created?.Name))
        {
            throw new InvalidOperationException("Firebase did not return a generated shopping list id.");
        }

        list.Id = created.Name;
        _logger.LogInformation("Created shopping list {ListId} for user {UserId}", list.Id, userId);
        return list;
    }

    public async Task<ShoppingList?> UpdateShoppingListAsync(string userId, string listId, ShoppingList list, CancellationToken cancellationToken)
    {
        var existing = await GetShoppingListAsync(userId, listId, cancellationToken);
        if (existing is null)
        {
            return null;
        }

        list.Id = listId;
        list.CreateAt = existing.CreateAt;
        await PutJsonAsync($"users/{userId}/shoppingLists/{listId}.json", list, cancellationToken);
        return list;
    }

    public async Task<bool> DeleteShoppingListAsync(string userId, string listId, CancellationToken cancellationToken)
    {
        var existing = await GetShoppingListAsync(userId, listId, cancellationToken);
        if (existing is null)
        {
            return false;
        }

        EnsureConfigured();
        var response = await _httpClient.DeleteAsync(BuildPath($"users/{userId}/shoppingLists/{listId}.json"), cancellationToken);
        await ReadBodyAsync(response, cancellationToken);
        return true;
    }

    private async Task<T?> GetJsonAsync<T>(string path, CancellationToken cancellationToken)
    {
        EnsureConfigured();
        var response = await _httpClient.GetAsync(BuildPath(path), cancellationToken);
        var body = await ReadBodyAsync(response, cancellationToken);
        if (string.IsNullOrWhiteSpace(body) || body.Trim() == "null")
        {
            return default;
        }

        return JsonSerializer.Deserialize<T>(body, JsonOptions);
    }

    private async Task PutJsonAsync<T>(string path, T payload, CancellationToken cancellationToken)
    {
        EnsureConfigured();
        var response = await _httpClient.PutAsJsonAsync(BuildPath(path), payload, JsonOptions, cancellationToken);
        await ReadBodyAsync(response, cancellationToken);
    }

    private void EnsureConfigured()
    {
        if (_httpClient.BaseAddress is null || string.IsNullOrWhiteSpace(_options.BaseUrl))
        {
            throw new InvalidOperationException(
                "Firebase Realtime Database is not configured. Set Firebase:BaseUrl in appsettings or user secrets.");
        }
    }

    private string BuildPath(string path)
    {
        if (string.IsNullOrWhiteSpace(_options.AuthToken))
        {
            return path;
        }

        return $"{path}?auth={Uri.EscapeDataString(_options.AuthToken)}";
    }

    private static async Task<string> ReadBodyAsync(HttpResponseMessage response, CancellationToken cancellationToken)
    {
        var body = await response.Content.ReadAsStringAsync(cancellationToken);
        if (!response.IsSuccessStatusCode)
        {
            throw new HttpRequestException(
                $"Firebase request failed ({(int)response.StatusCode} {response.ReasonPhrase}): {body}");
        }

        return body;
    }

    private sealed class FirebaseNameResponse
    {
        public string? Name { get; set; }
    }
}
