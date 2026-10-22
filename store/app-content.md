# Play Console answers

The answers for each form in Play Console, based on how the app behaves (see [PRIVACY.md](../PRIVACY.md)). Google can change the wording of its forms. If a question is worded differently, answer it to match the facts here.

## App details

| Field | Answer |
| --- | --- |
| App name | Turnfall (see `en-US/title.txt`) |
| Default language | English (United States) |
| App or game | Game |
| Free or paid | Free |
| Developer contact email | **Fill in your own address.** It is shown publicly on the store page, so it is not written in this repository. |
| Website | Optional. `https://github.com/giamat13/turnfall`. The repository is public. |

## Store listing

| Field | File |
| --- | --- |
| Short description | `en-US/short-description.txt` |
| Full description | `en-US/full-description.txt` |
| App icon, 512 × 512 | `graphics/icon-512.png` |
| Feature graphic, 1024 × 500 | `graphics/feature-graphic-1024x500.jpg` |
| Phone screenshots | `screenshots/phone/` (four screens, 1080 × 1920) |
| Category | Games > Puzzle |

## App content

| Question | Answer |
| --- | --- |
| Privacy policy URL | `https://github.com/giamat13/turnfall/blob/main/PRIVACY.md`. Checked: it returns HTTP 200 now that the repository is public. |
| Contains ads | No |
| App access | All functionality is available without special access. There is no sign-in. |
| Content rating | Answer the IARC questionnaire: no violence, no sexual content, no profanity, no drugs, no gambling with real money, no user-generated content or chat, no location sharing, no in-app purchases. |
| Target audience | 6+. In Play Console, select every band from 6–8 up to 18 and over. See the Families section below. |
| News app | No |
| COVID-19 contact tracing or status app | No |
| Government app | No |
| Financial features | No |
| Health | No |

## Families policy (the audience includes children)

Once a child age band is chosen, Google's Families policy applies to the whole app. What the Families policy asks for, and how Turnfall meets it:

| Requirement | Turnfall |
| --- | --- |
| Ads only through Families-certified ad SDKs | No ads and no ad SDKs |
| No advertising ID, device serial, or similar identifiers sent for children | None are used. The app has no network access. |
| No precise location for child-only apps | No location permission |
| Privacy policy that matches the app | `PRIVACY.md` says the app collects nothing |
| Store images that are not aimed at toddlers | The icon and feature graphic show an abstract board and a ball, with no young characters |
| Accurate audience answers | All bands from 6 and up are selected, because the app suits that whole range. "Ages 5 & Under" is not selected, because the app is a reading-based puzzle. |

Google can review the chosen audience and reject the listing if the answers do not match the app. Check the current requirements before submitting: [Google Play Families Policies](https://support.google.com/googleplay/android-developer/answer/9893335) and [Manage target audience and app content settings](https://support.google.com/googleplay/android-developer/answer/9867159).

## Data safety

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | No |

Why: the app has no network access (its only permission is `VIBRATE`). Progress and settings are stored only on the device. Nothing leaves the phone, so nothing is collected under Google's definition. No questions about encryption in transit or data deletion follow from that answer.

If the form asks about advertising ID: the app does not use one.

## Release

| Field | Value |
| --- | --- |
| Version | 1.0.0 (`versionCode` 1, from `app/build.gradle.kts`) |
| Release notes | `en-US/release-notes-1.0.0.txt` |
| Bundle | `app/build/outputs/bundle/release/app-release.aab`, built with `.\gradlew.bat :app:bundleRelease` after `keystore.properties` exists |
| Track | A personal developer account must first run a closed test with at least 12 testers for 14 days before it can apply for production access. An organization account is not covered by that rule. |
