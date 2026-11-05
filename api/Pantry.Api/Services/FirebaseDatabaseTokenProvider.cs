using Google.Apis.Auth.OAuth2;
using Microsoft.Extensions.Options;
using Pantry.Api.Options;

namespace Pantry.Api.Services
{
    public class FirebaseDatabaseTokenProvider
    {
        private static readonly string[] Scopes =
            [
                "https://www.googleapis.com/auth/userinfo.email",
                "https://www.googleapis.com/auth/firebase.database"
            ];

        private readonly Lazy<GoogleCredential> _credential;

        public FirebaseDatabaseTokenProvider(IOptions<FirebaseOptions> options)
        {
            var firebase = options.Value;

            _credential = new Lazy<GoogleCredential>(() =>
            {
                GoogleCredential credential;

                if (!string.IsNullOrWhiteSpace(firebase.ServiceAccountJson))
                {
                    credential = GoogleCredential.FromJson(firebase.ServiceAccountJson);
                }
                else if (!string.IsNullOrWhiteSpace(firebase.ServiceAccountPath) && File.Exists(firebase.ServiceAccountPath))
                {
                    credential = GoogleCredential.FromFile(firebase.ServiceAccountPath);
                }
                else
                {
                    throw new InvalidOperationException("Set Firebase:ServiceAccountJson or Firebase:ServiceAccountPath for database access.");
                }

                return credential.CreateScoped(Scopes);
            });
        }

        public Task<string> GetAccessTokenAsync(CancellationToken cancellationToken)
        {
            var tokenAccess = _credential.Value.UnderlyingCredential as ITokenAccess
                ?? throw new InvalidOperationException(
                    "The configured Google credential cannot provide OAuth access tokens.");

            return tokenAccess.GetAccessTokenForRequestAsync(
                cancellationToken: cancellationToken);
        }
    }
}
