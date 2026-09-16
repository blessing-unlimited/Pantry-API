# Group work split — Pantry Part 2

Use **your own GitHub account** when you commit so the contributor graph lists Sihle, Blessing, Lesedi, and Thami separately.

| Person | GitHub | Own these files / endpoints |
| --- | --- | --- |
| Sihle Nathi Simelane | `Sihl3s` | Android architecture, Google/dev SSO, pantry CRUD UI, reverse-pantry match client, settings, Retrofit, API matching engine, GitHub Actions |
| Blessing Makhukhula | `blessing-unlimited` | `POST /api/v1/shopping-lists/generate`, shopping-list CRUD, `GET /api/v1/substitutions/{ingredientId}`, [ScanScreen](android/app/src/main/java/za/ac/vc/emgppt/pantry/ui/group/GroupScreens.kt), host the API in the cloud |
| Lesedi Khutoane | `ST10443296` | Hands-free [CookModeScreen](android/app/src/main/java/za/ac/vc/emgppt/pantry/ui/group/GroupScreens.kt) (`FLAG_KEEP_SCREEN_ON`, timer, swipe), onboarding chips, profile XP/badges |
| Nomathamsanqa Tame | `NomathamsanqaTame` | Physical-device demo video with voice-over, unlisted YouTube link in README, screenshots, app icon polish |

Blessing's 501 endpoints currently live in [GroupBacklogController.cs](api/Pantry.Api/Controllers/GroupBacklogController.cs). Replace the 501 responses with real implementations; do not delete the routes.

Production SSO: copy [android/app/google-services.json.example](android/app/google-services.json.example) to `android/app/google-services.json`, enable Google Sign-In in Firebase, and put the Web client ID in `android/app/src/main/res/values/strings.xml` (`default_web_client_id`). Set `Firebase:BaseUrl` and `Firebase:ProjectId` in the API before the video so Thami can show Auth + Realtime Database in the Firebase console.
