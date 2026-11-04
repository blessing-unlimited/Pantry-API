using Pantry.Api.Services;

namespace Pantry.Api.Tests;

public class FcmMessageFactoryTests
{
    [Fact]
    public void Message_includes_token_title_and_body()
    {
        var json = FcmMessageFactory.ToJson("device-token", "Pantry", "Milk was added");

        Assert.Contains("device-token", json);
        Assert.Contains("Pantry", json);
        Assert.Contains("Milk was added", json);
        Assert.Contains("\"message\"", json);
    }
}
