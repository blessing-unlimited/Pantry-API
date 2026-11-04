# Use of generative AI

Generative AI tooling was used while preparing the final Pantry release for PROG7314. It was used as a drafting aid. The group remains responsible for the behaviour that is submitted.

The assistant was asked to propose a structure for three features that the Part 2 prototype had deferred: biometric unlock on top of the existing Google session, a Room database with an outbox for pantry and shopping-list edits, and an upload of scan photos to cloud object storage. The drafts were then checked against the official Android, Firebase, and Play documentation listed in the project README, and they were edited so they match the Retrofit client and the ASP.NET API that were already in the repository. In-text comments in the new Kotlin and C# files cite those sources. The assistant did not replace that reading.

The same tooling was used while fixing compile and design mistakes: activity types required by the biometric prompt, locale resources for English, isiZulu, and Afrikaans, and the JSON body of a Firebase Cloud Messaging HTTP v1 request. Suggested code was not pasted in bulk and left unreviewed. Naming, logging, and error messages were adjusted so a failed network call, a cancelled biometric prompt, or a missing server credential shows a message instead of closing the app.

Documentation was also drafted with the tool and then rewritten. That includes this note, the release notes, and parts of the technical README. The demonstration video is not generated. It still has to be recorded on a physical phone with a voice-over.

No secrets were given to the tool. The Firebase service account, the LogMeal token, `google-services.json`, and the upload keystore are not in the repository. Generated text that did not match the running code was discarded. Unit tests for the sync outbox and the messaging payload were added so the result can be checked without a device.

Word count: 278.
