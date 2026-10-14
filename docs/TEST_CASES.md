# PratibhaScan — Software Testing and Validation

**Project:** PratibhaScan  
**Team:** Idea Ignite  
**Hackathon:** MY Bharat — Hack for Social Cause  
**Platform:** Android (Kotlin)

## 1. Testing Objective

The purpose of testing is to verify that PratibhaScan works correctly across its three stakeholder dashboards: Athlete, Academy, and Institution.

Testing focuses on authentication, AI-powered sports assessment, movement analysis, performance tracking, data security, and application stability.

## 2. Testing Environment

- Operating system: Windows 11
- Development tools: VS Code and Android Studio
- Programming language: Kotlin
- Build system: Gradle
- Android SDK: Installed
- Java: JDK 21
- AI framework: MediaPipe Pose Landmarker
- Backend: Firebase
- Testing device: To be recorded

## 3. Functional Test Cases

| Test ID | Module | Test Scenario | Expected Result | Status |
|---|---|---|---|---|
| TC-01 | Authentication | Login using valid athlete credentials | Athlete dashboard opens | Not Tested |
| TC-02 | Authentication | Login using incorrect credentials | Appropriate error appears | Not Tested |
| TC-03 | Athlete | Open athlete profile | Authorized profile information displays | Not Tested |
| TC-04 | AI Assessment | Open a supported sports assessment | Assessment screen opens | Not Tested |
| TC-05 | Camera | Deny camera permission | App handles denial without crashing | Not Tested |
| TC-06 | MediaPipe | Process a supported exercise video | Pose landmarks are detected when conditions permit | Not Tested |
| TC-07 | Movement Analysis | Perform a complete valid repetition | Repetition is counted according to implemented rules | Not Tested |
| TC-08 | Movement Analysis | Perform an incomplete repetition | No false completed repetition is counted | Not Tested |
| TC-09 | AI Coach | Open AI Coach | Available coaching interface loads | Not Tested |
| TC-10 | Progress | Open assessment history | Saved records display if available | Not Tested |
| TC-11 | Academy | Login using authorized academy account | Academy dashboard opens | Not Tested |
| TC-12 | Academy | Open athlete management | Authorized athlete records display | Not Tested |
| TC-13 | Institution | Login using authorized institution account | Institution dashboard opens | Not Tested |
| TC-14 | Institution | Open student-athlete records | Authorized records display | Not Tested |
| TC-15 | Security | Attempt unauthorized access to another user's data | Server-side access is denied | Not Tested |
| TC-16 | Network | Disconnect network during a cloud operation | App handles failure without crashing | Not Tested |
| TC-17 | Maps | Open implemented map functionality | Available map screen loads | Not Tested |
| TC-18 | Navigation | Switch between implemented screens | Navigation works without unexpected crashes | Not Tested |

## 4. AI Assessment Validation

AI assessment testing should use authorized videos representing different movement conditions.

Recommended test categories:

1. Correct exercise movement.
2. Incomplete exercise movement.
3. Different camera angles.
4. Different lighting conditions.
5. Partial body visibility.
6. Multiple people in the frame.
7. Low-quality video.
8. Fast or irregular movement.

Record actual observed results before reporting model accuracy, repetition-counting reliability, or assessment performance.

## 5. Automated Testing

### Debug Build

Command:

```powershell
.\gradlew.bat assembleDebug
```

Observed result during development:

**BUILD SUCCESSFUL**

### Kotlin Compilation

Command:

```powershell
.\gradlew.bat :app:compileDebugKotlin --rerun-tasks
```

Observed result during development:

**BUILD SUCCESSFUL**

### Unit Tests

Command:

```powershell
.\gradlew.bat testDebugUnitTest
```

Status: Not yet verified.

### Instrumentation Tests

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Status: Not yet verified.

## 6. Test Result Recording

For each executed test, record:

- Test ID
- Test date
- Device or emulator
- Input or test conditions
- Expected result
- Actual result
- Pass/Fail status
- Screenshot or log reference, if available

## 7. Known Limitations

- Successful compilation does not guarantee every runtime feature works.
- AI assessment accuracy depends on input conditions and implemented exercise logic.
- Firebase operations depend on network connectivity, configuration, and authorization.
- GIS-based talent discovery enhancements are planned and should not be reported as completed functionality without implementation evidence.

## 8. Testing Conclusion

The Android application has successfully completed a local debug build and Kotlin compilation.

Functional validation, automated unit tests, device testing, and security testing will be documented as they are completed.

No unverified test is claimed as passed.