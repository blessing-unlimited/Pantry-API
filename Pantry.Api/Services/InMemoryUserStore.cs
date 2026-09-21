using System.Collections.Concurrent;
using Pantry.Api.Models;

namespace Pantry.Api.Services;

/// <summary>
/// Process-local store used when Firebase:BaseUrl is not configured so the
/// prototype can be demonstrated on localhost. Production and the Part 2 video
/// should point this API at Firebase Realtime Database (Firebase, 2026).
/// </summary>
public sealed class InMemoryUserStore : IUserStore
{
    private readonly ConcurrentDictionary<string, UserProfile> _profiles = new();
    private readonly ConcurrentDictionary<string, UserSettings> _settings = new();
    private readonly ConcurrentDictionary<string, ConcurrentDictionary<string, PantryItem>> _pantries = new();
    private readonly ConcurrentDictionary<string, ConcurrentDictionary<string, ShoppingList>> _shoppingLists = new();
    private readonly ILogger<InMemoryUserStore> _logger;

    public InMemoryUserStore(ILogger<InMemoryUserStore> logger)
    {
        _logger = logger;
        _logger.LogWarning("Using in-memory user store. Configure Firebase:BaseUrl to persist pantry and settings.");
    }

    public Task<UserProfile?> GetProfileAsync(string userId, CancellationToken cancellationToken)
    {
        _profiles.TryGetValue(userId, out var profile);
        return Task.FromResult(profile);
    }

    public Task SaveProfileAsync(UserProfile profile, CancellationToken cancellationToken)
    {
        _profiles[profile.UserId] = profile;
        return Task.CompletedTask;
    }

    public Task<UserSettings> GetSettingsAsync(string userId, CancellationToken cancellationToken)
    {
        var settings = _settings.GetOrAdd(userId, _ => new UserSettings());
        return Task.FromResult(CloneSettings(settings));
    }

    public Task<UserSettings> SaveSettingsAsync(string userId, UserSettings settings, CancellationToken cancellationToken)
    {
        _settings[userId] = CloneSettings(settings);
        return Task.FromResult(CloneSettings(settings));
    }

    public Task<IReadOnlyList<PantryItem>> GetPantryAsync(string userId, CancellationToken cancellationToken)
    {
        var pantry = _pantries.GetOrAdd(userId, _ => new ConcurrentDictionary<string, PantryItem>());
        var items = pantry.Values.OrderBy(i => i.IngredientName, StringComparer.OrdinalIgnoreCase).ToList();
        return Task.FromResult<IReadOnlyList<PantryItem>>(items);
    }

    public Task<PantryItem?> GetPantryItemAsync(string userId, string itemId, CancellationToken cancellationToken)
    {
        if (_pantries.TryGetValue(userId, out var pantry) && pantry.TryGetValue(itemId, out var item))
        {
            return Task.FromResult<PantryItem?>(item);
        }

        return Task.FromResult<PantryItem?>(null);
    }

    public Task<PantryItem> AddPantryItemAsync(string userId, PantryItem item, CancellationToken cancellationToken)
    {
        var pantry = _pantries.GetOrAdd(userId, _ => new ConcurrentDictionary<string, PantryItem>());
        item.Id = Guid.NewGuid().ToString("N");
        pantry[item.Id] = item;
        return Task.FromResult(item);
    }

    public Task<PantryItem?> UpdatePantryItemAsync(string userId, string itemId, PantryItem item, CancellationToken cancellationToken)
    {
        if (!_pantries.TryGetValue(userId, out var pantry) || !pantry.TryGetValue(itemId, out var existing))
        {
            return Task.FromResult<PantryItem?>(null);
        }

        item.Id = itemId;
        item.CreatedAt = existing.CreatedAt;
        pantry[itemId] = item;
        return Task.FromResult<PantryItem?>(item);
    }

    public Task<bool> DeletePantryItemAsync(string userId, string itemId, CancellationToken cancellationToken)
    {
        if (!_pantries.TryGetValue(userId, out var pantry))
        {
            return Task.FromResult(false);
        }

        return Task.FromResult(pantry.TryRemove(itemId, out _));
    }

    public Task<IReadOnlyList<ShoppingList>> GetShoppingListsAsync(string userId, CancellationToken cancellationToken)
    {
        var userLists = _shoppingLists.GetOrAdd(userId, _ => new ConcurrentDictionary<string, ShoppingList>());
        var lists = userLists.Values.OrderByDescending(l => l.CreateAt).ToList();
        return Task.FromResult<IReadOnlyList<ShoppingList>>(lists);
    }

    public Task<ShoppingList?> GetShoppingListAsync(string userId, string listId, CancellationToken cancellationToken)
    {
        if (_shoppingLists.TryGetValue(userId, out var lists) && lists.TryGetValue(listId, out var list))
        {
            return Task.FromResult<ShoppingList?>(list);
        }

        return Task.FromResult<ShoppingList?>(null);
    }

    public Task<ShoppingList> AddShoppingListAsync(string userId, ShoppingList list, CancellationToken cancellationToken)
    {
        var userLists = _shoppingLists.GetOrAdd(userId, _ => new ConcurrentDictionary<string, ShoppingList>());
        list.Id = Guid.NewGuid().ToString("N");
        userLists[list.Id] = list;
        return Task.FromResult(list);
    }

    public Task<ShoppingList?> UpdateShoppingListAsync(string userId, string listId, ShoppingList list, CancellationToken cancellationToken)
    {
        if (!_shoppingLists.TryGetValue(userId, out var lists) || !lists.TryGetValue(listId, out var existing))
        {
            return Task.FromResult<ShoppingList?>(null);
        }

        list.Id = listId;
        list.CreateAt = existing.CreateAt;
        lists[listId] = list;
        return Task.FromResult<ShoppingList?>(list);
    }

    public Task<bool> DeleteShoppingListAsync(string userId, string listId, CancellationToken cancellationToken)
    {
        if (!_shoppingLists.TryGetValue(userId, out var lists))
        {
            return Task.FromResult(false);
        }

        return Task.FromResult(lists.TryRemove(listId, out _));
    }

    private static UserSettings CloneSettings(UserSettings settings) => new()
    {
        LanguageCode = settings.LanguageCode,
        DietaryPreferences = [.. settings.DietaryPreferences],
        CuisinePreferences = [.. settings.CuisinePreferences],
        Allergens = [.. settings.Allergens],
        NotificationsEnabled = settings.NotificationsEnabled,
        MaxMissingIngredients = settings.MaxMissingIngredients
    };
}
