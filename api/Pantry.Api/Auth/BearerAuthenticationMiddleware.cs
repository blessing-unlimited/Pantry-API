using System.Security.Claims;
using Pantry.Api.Services;

namespace Pantry.Api.Auth;

/// <summary>
/// Reads the Bearer token on each request and assigns a ClaimsPrincipal so
/// controllers can enforce per-user access (OWASP, 2023).
/// </summary>
public sealed class BearerAuthenticationMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<BearerAuthenticationMiddleware> _logger;

    public BearerAuthenticationMiddleware(RequestDelegate next, ILogger<BearerAuthenticationMiddleware> logger)
    {
        _next = next;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext context, ITokenIdentityService tokens)
    {
        var header = context.Request.Headers.Authorization.ToString();
        if (!string.IsNullOrWhiteSpace(header) && header.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
        {
            var token = header["Bearer ".Length..].Trim();
            var identity = await tokens.AuthenticateAsync(token, context.RequestAborted);
            if (identity is not null)
            {
                var claims = new List<Claim>
                {
                    new(ClaimTypes.NameIdentifier, identity.UserId),
                    new("provider", identity.Provider)
                };
                if (!string.IsNullOrWhiteSpace(identity.Email))
                {
                    claims.Add(new Claim(ClaimTypes.Email, identity.Email));
                }

                if (!string.IsNullOrWhiteSpace(identity.DisplayName))
                {
                    claims.Add(new Claim(ClaimTypes.Name, identity.DisplayName));
                }

                context.User = new ClaimsPrincipal(new ClaimsIdentity(claims, "Bearer"));
                _logger.LogDebug("Authenticated {UserId} via {Provider}", identity.UserId, identity.Provider);
            }
        }

        await _next(context);
    }
}
