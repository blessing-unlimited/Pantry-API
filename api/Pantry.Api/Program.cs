using System.Net.Http.Headers;
using System.Reflection;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using Microsoft.OpenApi.Models;
using Pantry.Api.Auth;
using Pantry.Api.Options;
using Pantry.Api.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<FirebaseOptions>(builder.Configuration.GetSection(FirebaseOptions.SectionName));
builder.Services.Configure<MealDbOptions>(builder.Configuration.GetSection(MealDbOptions.SectionName));
builder.Services.Configure<AuthenticationOptions>(builder.Configuration.GetSection(AuthenticationOptions.SectionName));

builder.Services.AddHttpClient<IMealDbClient, MealDbClient>();
builder.Services.AddHttpClient<ITokenIdentityService, TokenIdentityService>();
builder.Services.AddHttpClient<FirebaseUserStore>();
builder.Services.AddSingleton<InMemoryUserStore>();
builder.Services.AddScoped<IUserStore>(sp =>
{
    var firebase = sp.GetRequiredService<Microsoft.Extensions.Options.IOptions<FirebaseOptions>>().Value;
    if (!string.IsNullOrWhiteSpace(firebase.BaseUrl) &&
        !firebase.BaseUrl.Contains("YOUR-PROJECT", StringComparison.OrdinalIgnoreCase))
    {
        return sp.GetRequiredService<FirebaseUserStore>();
    }

    return sp.GetRequiredService<InMemoryUserStore>();
});
builder.Services.AddScoped<IRecipeMatchingService, RecipeMatchingService>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new()
    {
        Title = "Pantry REST API",
        Version = "v1",
        Description = "Custom hosted API for the Pantry Android prototype. Recipe catalogue data is sourced from TheMealDB; pantry, settings and profiles are stored in Firebase Realtime Database when configured (TheMealDB, 2026; Firebase, 2026)."
    });
    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Description = "Firebase / Google ID token, or Development bypass token `dev-token`. Example: Bearer dev-token",
        Name = "Authorization",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT"
    });
    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });

    var xmlFile = $"{Assembly.GetExecutingAssembly().GetName().Name}.xml";
    var xmlPath = Path.Combine(AppContext.BaseDirectory, xmlFile);
    if (File.Exists(xmlPath))
    {
        options.IncludeXmlComments(xmlPath);
    }
});

builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy => policy.AllowAnyOrigin().AllowAnyHeader().AllowAnyMethod());
});

builder.Services.AddHttpClient<ILogMealService, LogMealService>(client =>
{
    client.BaseAddress = new Uri("https://api.logmeal.com/");
    var token = builder.Configuration["LogMeal:ApiToken"];
    if (!string.IsNullOrWhiteSpace(token))
    {
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
    }

    client.Timeout = TimeSpan.FromSeconds(60);
});

var app = builder.Build();

app.UseExceptionHandler(errorApp =>
{
    errorApp.Run(async context =>
    {
        var feature = context.Features.Get<IExceptionHandlerFeature>();
        var error = feature?.Error;
        context.Response.ContentType = "application/problem+json";

        if (error is InvalidOperationException invalid &&
            invalid.Message.Contains("Firebase", StringComparison.OrdinalIgnoreCase))
        {
            context.Response.StatusCode = StatusCodes.Status503ServiceUnavailable;
            await context.Response.WriteAsJsonAsync(new ProblemDetails
            {
                Title = "Firebase is not configured",
                Detail = invalid.Message,
                Status = StatusCodes.Status503ServiceUnavailable
            });
            return;
        }

        if (error is HttpRequestException httpError)
        {
            context.Response.StatusCode = StatusCodes.Status502BadGateway;
            await context.Response.WriteAsJsonAsync(new ProblemDetails
            {
                Title = "Upstream request failed",
                Detail = httpError.Message,
                Status = StatusCodes.Status502BadGateway
            });
            return;
        }

        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        await context.Response.WriteAsJsonAsync(new ProblemDetails
        {
            Title = "Unexpected error",
            Status = StatusCodes.Status500InternalServerError
        });
    });
});

app.UseSwagger();
app.UseSwaggerUI(options =>
{
    options.SwaggerEndpoint("/swagger/v1/swagger.json", "Pantry API v1");
    options.RoutePrefix = "swagger";
});

app.UseCors();
if (!app.Environment.IsDevelopment())
{
    app.UseHttpsRedirection();
}

app.UseMiddleware<BearerAuthenticationMiddleware>();
app.MapControllers();
app.MapGet("/api/v1/health", () => Results.Ok(new
{
    success = true,
    data = new { status = "ok", service = "Pantry.Api" }
}));

app.Run();
