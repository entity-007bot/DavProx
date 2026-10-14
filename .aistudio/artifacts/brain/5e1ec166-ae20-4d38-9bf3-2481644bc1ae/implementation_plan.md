# DavProx Application ID & Build Configuration Plan

Configure DavProx with the custom Application ID `com.entity007.davprox` and release versioning `1.0 (versionCode 1)` while preserving existing namespace integrity and network debugging features.

### User Review & Critical Decisions

> [!IMPORTANT]
> The Application ID uniquely identifies the app on device installations and package managers. The internal Kotlin namespace `com.example` will remain unchanged to prevent build breaks with generated `R` classes and resources.

- **Confirmed Decision 1**: Set `applicationId = "com.entity007.davprox"` in `app/build.gradle.kts`.
- **Confirmed Decision 2**: Set `versionCode = 1` and `versionName = "1.0"` in `app/build.gradle.kts`.
- **Preserved Constraint**: Keep `namespace = "com.example"` to ensure backward compatibility and stable build symbols without breaking existing Compose UI and network engine imports.

---

### 1. Overview & Core Concept

- **What It Does**: Establishes a professional, customized package identity (`com.entity007.davprox`) for the DavProx mobile network traffic inspector on Android devices, ensuring seamless APK installation and collision-free sideloading on Tecno and modern Android 15 smartphones.
- **Target Audience / Persona**: Chukwuyem David (ENTITY_007) and mobile developers debugging API payloads, headers, latency, and status codes on real Android devices.
- **Key Value**: Enables clean device installation, export, and debugging identification with zero naming conflicts.

---

### 2. User Experience & Visual Design

- **App Identity & Manifest Alignment**:
  - The launcher label remains **DavProx** in `res/values/strings.xml` and `metadata.json`.
  - The Android system package installer displays `com.entity007.davprox` with Version `1.0`.
- **In-App Branding**:
  - App header explicitly displays **DavProx** with sub-header **by ENTITY_007 (Chukwuyem David)**.
  - JSON session exports and cURL headers retain the attribution signature `DavProx (Android; Tecno Device Inspector)`.
- **Design System**:
  - Material Design 3 theme with dynamic color tokens, monospace code viewers for JSON payloads, status badges (2xx green, 4xx orange, 5xx red), and responsive bottom sheets for transaction inspection.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Application ID vs. Kotlin Namespace Separation**
  - *Chosen Approach*: Update `applicationId = "com.entity007.davprox"` inside `defaultConfig` while keeping `namespace = "com.example"`.
  - *Why*: Modern Android Gradle Plugin allows separating the OS application identifier from the source code namespace. This eliminates unnecessary package renaming across dozens of Kotlin files and guarantees zero build regressions.
  - *Alternatives Considered*: Renaming the entire directory structure from `com/example/` to `com/entity007/davprox/`. Rejected because it creates churn and can break generated build resources without any functional advantage.
- **Decision 2: Cleartext Traffic & Debugging Configuration**
  - *Chosen Approach*: Maintain `network_security_config.xml` with cleartext permitted for localhost/local network and user CA trust anchors.
  - *Why*: Allows real API testing and local proxy socket capture on Android 15 without certificate rejection on loopback addresses.

---

### 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────┐
│               Android OS & Package Manager                   │
│   Package ID: com.entity007.davprox  (Version 1.0, Code 1)    │
└───────────────────────────────┬───────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────┐
│                 DavProx Application Process                   │
│                                                               │
│   ┌───────────────────────────┐   ┌───────────────────────┐   │
│   │    ProxyServerEngine      │   │   LiveNetworkTester   │   │
│   │   127.0.0.1:8080 Socket   │   │  OkHttp Live Dispatch │   │
│   └─────────────┬─────────────┘   └───────────┬───────────┘   │
│                 │                             │               │
│                 ▼                             ▼               │
│   ┌───────────────────────────────────────────────────────┐   │
│   │          TrafficRepository (StateFlow Store)          │   │
│   └───────────────────────────┬───────────────────────────┘   │
│                               │                               │
│                               ▼                               │
│   ┌───────────────────────────────────────────────────────┐   │
│   │             Jetpack Compose M3 UI Layer               │   │
│   │     DavProxScreen • DetailSheet • ConfigDialog        │   │
│   └───────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────┘
```

- **Execution Steps**:
  1. Update `app/build.gradle.kts`:
     - Change `applicationId` to `"com.entity007.davprox"`.
     - Confirm `versionCode = 1` and `versionName = "1.0"`.
  2. Verify all references and ensure clean project configuration.
