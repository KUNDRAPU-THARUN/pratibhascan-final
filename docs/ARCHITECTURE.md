# PratibhaScan — Technical Architecture

**Project:** PratibhaScan  
**Hackathon:** MY Bharat — Hack for Social Cause  
**Team:** Idea Ignite  
**Theme:** AI, GIS & Emerging Technologies

## 1. Project Overview

PratibhaScan is an AI-powered Android sports talent assessment and development platform built using Kotlin.

It connects three stakeholders:

1. Athlete
2. Academy
3. Institution

The objective is to make sports performance assessment more accessible, support athlete development, and improve pathways to sports opportunities.

## 2. Technology Stack

| Component | Technology |
|---|---|
| Mobile application | Kotlin, Android SDK |
| Build system | Gradle |
| Pose estimation | MediaPipe Pose Landmarker |
| Body tracking | 33 pose landmarks |
| Local database | Room |
| Authentication | Firebase Authentication |
| Cloud database | Cloud Firestore |
| File storage | Firebase Storage |
| Maps | Google Maps |
| AI assistance | Application AI Coach integration |

## 3. System Architecture

```text
                    PRATIBHASCAN
                          |
                   Android Application
                          |
          +---------------+---------------+
          |               |               |
        Athlete         Academy       Institution
       Dashboard       Dashboard       Dashboard
          |               |               |
          +---------------+---------------+
                          |
                   Application Logic
                          |
          +---------------+---------------+
          |               |               |
      AI Assessment   Local Storage   Cloud Services
          |               |               |
       MediaPipe         Room           Firebase
          |
     Pose Landmarks
          |
     Movement Analysis
          |
     Feedback and Results
```

## 4. AI Assessment Workflow

1. Athlete selects a supported assessment.
2. The application captures or receives video.
3. MediaPipe detects body landmarks.
4. Exercise-specific logic analyzes movement.
5. The application calculates supported metrics such as repetitions, joint angles, and movement quality.
6. Assessment feedback is displayed.
7. Available results may be stored for progress tracking.

## 5. Athlete Dashboard

Supports athlete profiles, sports assessments, AI-assisted guidance, performance history, and sports opportunities.

## 6. Academy Dashboard

Supports athlete and team management, training-related workflows, and performance information.

## 7. Institution Dashboard

Supports student-athlete management, sports teams, and institutional assessment records.

## 8. Data Security

Firebase Authentication provides user authentication.

Cloud data access should be protected through Firebase Security Rules and appropriate role-based authorization.

Sensitive athlete information must be handled with suitable privacy and consent controls.

## 9. AI and GIS Roadmap

The project proposes expanding privacy-conscious GIS talent discovery to help connect consenting athletes with relevant sports opportunities.

Initial proposed pilot regions include Rajasthan, Andhra Pradesh, and Telangana.

These are proposed enhancements and pilot locations, not claims of completed deployment.

## 10. Validation

The application has undergone a successful local Gradle debug build.

Functional tests, device testing, and security verification must be documented separately as they are completed.