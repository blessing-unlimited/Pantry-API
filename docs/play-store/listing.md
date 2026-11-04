# Google Play listing — Pantry

**Package name:** `za.ac.vc.emgppt.pantry`  
**Version:** 1.0.0 (version code 2)  
**Default language:** English (South Africa). The app also includes isiZulu and Afrikaans.

## Title

Pantry

## Short description

Cook from the ingredients you already have.

## Full description

Pantry keeps a digital kitchen and ranks recipes by what is already on the shelf. Sign in with Google, optionally unlock the app with a fingerprint or face, and add ingredients by typing or by photographing them. The app works offline for pantry edits and shopping-list ticks, then syncs those changes when the network returns. Alerts can be delivered with Firebase Cloud Messaging. The interface is available in English, isiZulu, and Afrikaans.

## Graphic assets

| Asset | File |
| --- | --- |
| High-resolution icon (512×512) | [icon-512.png](icon-512.png) |
| Feature graphic (1024×500) | [feature-1024x500.png](feature-1024x500.png) |
| Phone screenshots | The device captures embedded in the repository README |

The launcher icon in the APK is the adaptive terracotta plate mark in `android/app/src/main/res`.

## Content and permissions

The app requests internet, network state, camera (optional), biometric unlock, and notifications. The camera is used only when the user scans food. Biometric unlock is optional.

## Release binary

The signed production APK is [../../releases/pantry-1.0.0-release.apk](../../releases/pantry-1.0.0-release.apk). It is signed with the upload key described in the README and targets Android 15 (`targetSdk` 35) with a minimum of Android 8 (`minSdk` 26).
