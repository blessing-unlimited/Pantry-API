namespace Pantry.Api.Models;

/// <summary>
/// Uniform JSON envelope so the Android client can parse success and error
/// payloads consistently (Fielding, Nottingham and Reschke, 2022).
/// </summary>
public sealed class ApiEnvelope<T>
{
    public bool Success { get; init; }

    public T? Data { get; init; }

    public string? Error { get; init; }

    public static ApiEnvelope<T> Ok(T data) => new() { Success = true, Data = data };

    public static ApiEnvelope<T> Fail(string error) => new() { Success = false, Error = error };
}
