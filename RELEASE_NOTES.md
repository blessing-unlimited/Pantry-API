# Release notes

Changes since the PROG7314 Part 2 prototype. The prototype already shipped Google sign-in, the custom API, Firebase Realtime Database, pantry matching, scan, shopping lists, cook mode, and continuous integration.

## 1.0.0 — Final POE

**Biometric unlock.** After Google single sign-on, the user can enrol fingerprint or face unlock. The next cold start asks for that check before the pantry opens. Cancelling the prompt stays on the lock screen. The biometric sample is never stored by the app (Android Developers, 2025).

**Offline mode with synchronisation.** Pantry creates and deletes, and shopping-list ticks, are stored in Room and in an outbox. The outbox collapses to the latest write per row. A connectivity callback replays it to the custom API when the network returns. The last matched recipe list can be read offline (Android Developers, 2024).

**Blob storage.** A confirmed scan photo is uploaded to Firebase Storage under `users/{userId}/scans/`. The download URL is saved on the pantry item, which is stored in Firebase Realtime Database through the API (Firebase, 2026). If Storage is not configured, the ingredient is still saved and the failure is logged.

**Languages.** English, isiZulu, and Afrikaans. The choice is saved with the other settings and applied with the per-app locale API (Android Developers, 2026).

**Real-time notifications.** The client registers a Firebase Cloud Messaging token. The API can send a test alert, and can notify the user when a pantry item is added, once a service-account credential is present on the host and notifications are enabled in settings (Firebase, 2026).

**Publication.** Application version 1.0.0, HTTPS API in release builds, adaptive launcher icon, Play listing copy, and a signed release APK in `releases/` (Google Play, 2026).

**Quality.** New unit tests for the outbox and for the Cloud Messaging JSON body. GitHub Actions still builds both projects on every push.

## References

Android Developers (2024) *Save data in a local database using Room*. Available at: https://developer.android.com/training/data-storage/room (Accessed: 4 November 2026).

Android Developers (2025) *Show a biometric authentication dialog*. Available at: https://developer.android.com/identity/sign-in/biometric-auth (Accessed: 4 November 2026).

Android Developers (2026) *Per-app language preferences*. Available at: https://developer.android.com/guide/topics/resources/app-languages (Accessed: 4 November 2026).

Firebase (2026) *Cloud Storage* and *Firebase Cloud Messaging*. Available at: https://firebase.google.com/docs (Accessed: 4 November 2026).

Google Play (2026) *Sign your app*. Available at: https://developer.android.com/studio/publish/app-signing (Accessed: 4 November 2026).
