# PratibhaScan — Installation and Setup Guide

**Project:** PratibhaScan  
**Platform:** Android (Kotlin)  
**Hackathon:** MY Bharat — Hack for Social Cause  
**Team:** Idea Ignite

## 1. Prerequisites

Install the following software:

- Android Studio
- Android SDK
- Java Development Kit compatible with the project's Gradle configuration
- Git
- An Android emulator or physical Android device

## 2. Clone the Repository

```bash
git clone https://github.com/KUNDRAPU-THARUN/pratibhascan-final.git
cd pratibhascan-final
```

Open the project in Android Studio and allow Gradle synchronization to complete.

## 3. Android SDK Configuration

If Gradle cannot find the Android SDK, create or update `local.properties` in the project root.

Example for Windows:

```properties
sdk.dir=C:/Users/YOUR_USERNAME/AppData/Local/Android/Sdk
```

Replace `YOUR_USERNAME` with your Windows username.

Do not commit `local.properties` to GitHub.

## 4. Firebase Configuration

PratibhaScan uses Firebase services for authentication, cloud data, and storage.

To configure Firebase:

1. Open the Firebase Console.
2. Create or select an authorized development project.
3. Register the Android application using the package/application ID specified in the Gradle configuration.
4. Download the appropriate `google-services.json`.
5. Place the configuration file in the `app/` directory.
6. Configure the authentication providers and required cloud services.
7. Deploy appropriate Firestore and Storage Security Rules.

Never publish service-account credentials or private keys.

## 5. MediaPipe Configuration

The project contains MediaPipe pose-landmarker model assets.

MediaPipe is used for detecting body landmarks during supported exercise assessments.

The exact analysis behavior depends on the implemented exercise-specific logic.

## 6. Gemini AI Configuration

The current development configuration may read `GEMINI_API_KEY` from `local.properties`.

Example:

```properties
GEMINI_API_KEY=YOUR_DEVELOPMENT_KEY
```

Do not publish a real API key.

Embedding a private API key into an Android APK is not secure for production use. A protected backend is recommended before public deployment.

## 7. Google Maps Configuration

Configure the Maps API key according to the project's Android manifest and Google Cloud settings.

Restrict the key to the authorized Android application ID and signing certificate fingerprints, and restrict permitted APIs.

## 8. Build the Application

From the project root on Windows:

```powershell
.\gradlew.bat assembleDebug
```

A successful build displays:

```text
BUILD SUCCESSFUL
```

The debug APK is normally generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 9. Run Unit Tests

```powershell
.\gradlew.bat testDebugUnitTest
```

## 10. Run Instrumentation Tests

Start an Android emulator or connect a physical device.

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## 11. Demonstration Workflow

Evaluators can explore the three stakeholder dashboards using authorized demonstration accounts:

- Athlete: profile, supported assessments, coaching, and progress
- Academy: athlete and organization management
- Institution: student-athlete and sports-management workflows

Only demonstrate features available in the checked-out version.

## 12. Troubleshooting

**Java not recognized:** Configure `JAVA_HOME` and update the terminal PATH.

**Android SDK not found:** Set the correct `sdk.dir` in `local.properties`.

**Gradle build failure:** Review the complete Gradle error output and verify installed SDK components.

**Firebase errors:** Verify Firebase project configuration, enabled services, authorization, and security rules.

**Camera issues:** Check device permissions and supported hardware.

## 13. Current Verification

A local debug build and Kotlin compilation have completed successfully during development.

Successful compilation does not independently confirm that every application feature, backend permission, or device workflow has been tested.