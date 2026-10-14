# EdgeGuard AI: On-Device Real-Time Scam Detection & Interception

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](./LICENSE)

An edge-computing mobile security application engineered strictly for **on-device inference** to deliver **sub-millisecond (<0.1ms–200ms) scam detection with zero cloud-latency and absolute user data privacy**. 

Unlike conventional anti-fraud tools that stream user messages to remote LLMs, **EdgeGuard AI decouples inference completely from the cloud**, preventing server-side data harvesting and functioning seamlessly in offline environments.

---

## 📑 Table of Contents

- [Key Architectural Features](#-key-architectural-features)
- [System Architecture](#-system-architecture)
- [Scam Vectors Protected](#-scam-vectors-protected)
- [Performance Benchmarks](#-performance-benchmarks)
- [Prerequisites & Environment Variables](#-prerequisites--environment-variables)
- [Step-by-Step Setup & Build Guide](#-step-by-step-setup--build-guide)
- [Device Permissions & Onboarding](#-device-permissions--onboarding)
- [Running Automated Tests](#-running-automated-tests)
- [Sample Data & Test Cases](#-sample-data--test-cases)
- [Technical Design Document](#-technical-design-document)
- [License & Attribution](#-license--attribution)

---

## ⚡ Key Architectural Features

* **Zero-Cloud Data Sovereignty:** User messages, notifications, and telemetry never leave the device. All processing occurs locally within the Android hardware sandbox.
* **Tiered Hybrid Inference Engine:**
  * **Tier 1 (<0.1 ms):** Zero-latency deterministic pattern matcher for critical, instant-threat patterns (e.g., utility shutoff threats, KYC freezes, direct `.apk` downloads).
  * **Tier 2 (~185 ms):** 8-bit quantized MobileBERT / TinyBERT model deployed via TensorFlow Lite for complex, novel linguistic social engineering cues.
  * **Tier 3 (Heuristic):** Real-time monitoring of malicious APK installations, remote desktop tools (AnyDesk, TeamViewer), and digital arrest extortion.
* **Non-Blocking Compose Overlays:** Displays high-priority warning cards directly above targeted applications (WhatsApp, SMS, Telegram) using Android `WindowManager` overlay channels without interrupting normal OS navigation.
* **Transient Local Persistence:** Encrypted Room SQLite database with automatic 14-day record pruning and in-memory deduplication caching to prevent storage and RAM bloat.

---

## 🚀 System Architecture

```mermaid
flowchart LR
    A[Incoming Message / SMS / WhatsApp] --> B[MessageReaderAccessibilityService]
    B --> C{ScamAnalyzer Engine}
    C -->|Deterministic Match &lt;0.1ms| D[Tier 1: Rule Engine]
    C -->|Complex NLP ~185ms| E[Tier 2: Quantized TFLite]
    D --> F{Threat &ge; 0.90?}
    E --> F
    F -->|Yes| G[Jetpack Compose Overlay Card]
    F -->|No| H[(Local Room DB)]
    G --> H
```

For complete architectural diagrams, state machine specifications, and sequence charts, refer to the [**Technical Design Document**](./TECHNICAL_DESIGN_DOCUMENT.md).

---

## 🛡️ Scam Vectors Protected

| Threat Category | Attack Vector Description | Example Indicators |
| :--- | :--- | :--- |
| **Electricity Disconnection** | Immediate power shutoff threat directing victims to private phone numbers. | *"Electricity disconnected tonight at 9:30 PM due to unpaid bill"* |
| **Bank / KYC Suspension** | Threats of bank account, PAN, or debit card deactivation via phishing links. | *"Dear customer, your bank account blocked today due to pending KYC"* |
| **Fake Lottery & Lucky Draw** | Fabricated KBC or cash prizes demanding advance fees or WhatsApp communication. | *"Congratulations! Won Rs 25 Lakh in KBC Lucky Draw"* |
| **Task / Work-from-Home Fraud** | Unrealistic daily payouts for liking YouTube videos or completing Telegram tasks. | *"Earn Rs 3000-8000 daily per day by simple YouTube tasks"* |
| **Traffic E-Challan APK Trojan** | Disguised government traffic fine notifications delivering malicious `.apk` packages. | *"Unpaid traffic challan against vehicle. Pay or court summon: challan.apk"* |
| **Digital Arrest & Law Enforcement** | Psychological intimidation impersonating police, CBI, or customs officials. | *"Officer from Cyber Crime / CBI. Illegal narcotics parcel intercepted. Digital arrest warrant"* |
| **Postal Delivery Failure** | Impersonating India Post claiming parcel delivery failed to steal credentials. | *"India Post: Package delivery failed due to address. Update within 24h"* |
| **Remote Access & OTP Fraud** | Persuading users to install AnyDesk/TeamViewer or forward OTP verification codes. | *"Install AnyDesk from PlayStore and share code to unfreeze transaction"* |
| **Pre-Approved Loan / Refund** | Unsolicited high-value loan or tax refund approvals via shortened links. | *"Pre-approved personal loan credited. Click bit.ly link to claim"* |
| **SIM Card Deactivation** | False telecom warnings claiming SIM cancellation due to expired biometric data. | *"SIM card will be deactivated within 2 hours. Call 9988776655 to verify"* |

---

## ⚙️ Performance Benchmarks

*Recorded on physical Android device (Snapdragon 8 Gen 2, 8 GB RAM, Android 14):*

* **Tier 1 Inference Latency:** `< 0.08 ms`
* **Tier 2 NLP Inference Latency:** `~ 185 ms`
* **Peak Memory Allocation:** `~ 190 MB RAM` during active neural evaluation
* **Quantized Model Size:** `30 MB` (INT8 Quantized MobileBERT / FastText payload)
* **Battery Impact:** `< 0.8%` battery consumption per 24 hours of background monitoring
* **Network Egress:** `0 Bytes` (100% offline edge processing)

---

## 🛠️ Prerequisites & Environment Variables

### 1. Development Prerequisites
Ensure the following tools are installed on your workstation:
* **Operating System:** Windows 10/11, macOS (Apple Silicon/Intel), or Linux (Ubuntu 20.04+)
* **JDK:** Java Development Kit (OpenJDK or Oracle JDK) **version 17**
* **Android Studio:** Android Studio Ladybug (2024.2.1) or higher
* **Android SDK:** 
  * `compileSdk`: **36**
  * `targetSdk`: **35**
  * `minSdk`: **26** (Android 8.0 Oreo)
* **Android NDK:** **`26.0.10792818`**
* **CMake:** **`3.22.1`** (Required for native C++ JNI inference libraries)

### 2. Environment Variables Configuration

Set the following environment variables on your system:

#### Windows (PowerShell / System Environment Variables):
```powershell
# Set ANDROID_HOME (adjust to your SDK directory)
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")

# Set JAVA_HOME (must point to JDK 17)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "User")

# Add Platform Tools to PATH
$currentPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
[System.Environment]::SetEnvironmentVariable("Path", "$currentPath;$env:LOCALAPPDATA\Android\Sdk\platform-tools", "User")
```

#### macOS / Linux (`~/.bashrc` or `~/.zshrc`):
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk # macOS
# export ANDROID_HOME=$HOME/Android/Sdk        # Linux

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" # macOS
# export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"                       # Linux

export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
```

### 3. `local.properties` Configuration
Create or verify the `local.properties` file in the root directory of the project:
```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
ndk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk\\ndk\\26.0.10792818
```
*(On macOS/Linux, specify forward slashes, e.g. `sdk.dir=/Users/username/Library/Android/sdk`)*

---

## 📦 Step-by-Step Setup & Build Guide

### 1. Clone the Repository
```bash
git clone https://github.com/parv55555/EdgeGuard-AI.git
cd EdgeGuard-AI
```

### 2. Open Project in Android Studio
1. Launch **Android Studio**.
2. Select **File > Open...** and navigate to the `EdgeGuard-AI` directory.
3. Allow Gradle to download dependencies and sync the project structure automatically.

### 3. Build via Command Line (Gradle Wrapper)

#### Build Debug APK:
```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```
*Generated APK location:* `app/build/outputs/apk/debug/app-debug.apk`

#### Build Release APK:
```bash
# Windows
.\gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleRelease
```
*Generated APK location:* `app/build/outputs/apk/release/app-release-unsigned.apk`

### 4. Install & Launch on Connected Device
Ensure **USB Debugging** is enabled on your physical device or start an Android Emulator:
```bash
# Verify device connection
adb devices

# Install directly via Gradle
./gradlew installDebug
```

---

## 📱 Device Permissions & Onboarding

For EdgeGuard AI to protect your communications in real time, the following permissions must be enabled on the target device:

1. **Accessibility Service (Required for Real-Time Text Inspection):**
   * Navigate to: `Settings > Accessibility > Downloaded Apps > EdgeGuard AI` (or `SafeTexts`).
   * Toggle the service to **ON** and confirm.
2. **Display Over Other Apps (Required for Warning Card Overlays):**
   * Navigate to: `Settings > Apps > Special App Access > Display Over Other Apps > EdgeGuard AI`.
   * Enable **Allow display over other apps**.
3. **Notification Access (Fallback Alerts):**
   * Navigate to: `Settings > Apps > Special App Access > Notification Access`.
   * Enable access to allow high-priority security notifications when overlay permissions are restricted.

---

## 🧪 Running Automated Tests

EdgeGuard AI includes unit and instrumentation test suites validating detection accuracy and false-positive prevention across all catalogued social engineering vectors:

```bash
# Run local JVM unit tests
./gradlew testDebugUnitTest

# Run on physical connected device or emulator
./gradlew connectedDebugAndroidTest
```

---

## 📂 Sample Data & Test Cases

Standardized sample data and evaluation benchmarks are located in the [`sample_data/`](./sample_data/) directory:

* [`sample_data/scam_test_cases.json`](./sample_data/scam_test_cases.json): Full structured JSON test suite with threat categories, confidence thresholds, and indicator tokens.
* [`sample_data/scam_test_cases.csv`](./sample_data/scam_test_cases.csv): Tabular CSV format for external benchmark pipelines.
* [`sample_data/README.md`](./sample_data/README.md): Detailed documentation of the threat taxonomy and test methodologies.

To run a quick test against the rule engine:
```kotlin
val result = ScamAnalyzer.checkFastDeterministicScam(
    "Dear consumer, your electricity power will be disconnected tonight at 9:30 PM due to unpaid bill. Call 9876543210."
)
// Result: Pair("Alert", 0.98f)
```

---

## 📖 Technical Design Document

A full architectural specification covering the mathematical formulation of on-device quantization, thread synchronization, Room database schema, and security threat modeling is available in:

👉 [**Technical Design Document (`TECHNICAL_DESIGN_DOCUMENT.md`)**](./TECHNICAL_DESIGN_DOCUMENT.md)

---

## 📄 License & Attribution

This project is licensed under the **Apache License, Version 2.0**. See the [**LICENSE**](./LICENSE) file for terms and conditions.

```
Copyright 2026 Parv Khandelwal

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

**Architected & Engineered by:** [Parv Khandelwal](https://github.com/parv55555)
