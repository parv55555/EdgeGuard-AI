# EdgeGuard AI: On-Device Real-Time Scam Detection & Interception

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](./LICENSE)

An edge-computing mobile security application engineered strictly for **on-device inference** and **real-time User Behavior Analysis (UBA)**. EdgeGuard AI delivers **sub-millisecond (<0.1ms–185ms) scam interception with zero cloud-latency and absolute user data privacy**. 

Unlike conventional anti-fraud tools that stream user messages to remote servers, **EdgeGuard AI decouples inference completely from the cloud**, preventing server-side data harvesting and functioning seamlessly in offline environments.

---

## 📑 Table of Contents

- [Key Architectural Features](#-key-architectural-features)
- [System Architecture](#-system-architecture)
- [User Behavior Analysis (UBA) & Multi-App Funnels](#-user-behavior-analysis-uba--multi-app-funnels)
- [Scam Vectors Protected](#-scam-vectors-protected)
- [Performance Benchmarks](#-performance-benchmarks)
- [Prerequisites & Environment Variables](#-prerequisites--environment-variables)
- [Step-by-Step Setup & Build Guide](#-step-by-step-setup--build-guide)
- [Device Permissions & Onboarding](#-device-permissions--onboarding)
- [Running Automated Tests](#-running-automated-tests)
- [Sample Data & Test Cases](#-sample-data--test-cases)
- [Technical Design Document](#-technical-design-document)
- [Privacy Policy & Zero-Cloud Guarantee](#-privacy-policy--zero-cloud-guarantee)
- [License & Attribution](#-license--attribution)

---

## ⚡ Key Architectural Features

* **Zero-Cloud Data Sovereignty:** User messages, notifications, call logs, and telemetry never leave the device. All processing occurs locally within the Android hardware sandbox.
* **Dual-Pillar Defense Engine:**
  * **Pillar 1: NLP & Deterministic Classification:** Tier 1 ultra-fast token matching (<0.1ms) + Tier 2 quantized MobileBERT transformer (~185ms) for text analysis.
  * **Pillar 2: Real-Time User Behavior Analysis (UBA):** Contextual tracking across apps (Instagram $\rightarrow$ Telegram/WhatsApp), temporal OTP-to-call correlation, unknown video call monitoring, and remote desktop installation watchdogs.
* **Non-Blocking System Overlays:** Displays high-priority warning cards directly above targeted applications (WhatsApp, SMS, Telegram, Phone, Browsers) using Android `WindowManager` overlay channels without interrupting normal OS navigation.
* **Transient Local Persistence:** Encrypted Room SQLite database with automatic 14-day record pruning and in-memory deduplication caching to prevent storage and RAM bloat.

---

## 🚀 System Architecture

```mermaid
flowchart TB
    subgraph INGESTION["1. Multi-Modal Ingestion Layer (100% On-Device)"]
        A1["📱 Social Media & Sponsored Ads<br/>Instagram Reels, Facebook Ads, Chrome Browsers"] --> B1[MessageReaderAccessibilityService]
        A2["💬 Chats & Community Groups<br/>WhatsApp Groups, Telegram Channels, SMS"] --> B1
        A3["📦 Sideloaded APK Files<br/>Traffic e-Challan & Wedding Card APKs"] --> B1 & B3[AppInstallReceiver]
        A4["📞 Cellular & VoIP Telephony<br/>Incoming Unknown Calls & Video Calls"] --> B2[AppPhoneStateReceiver]
        A5["🔐 Bank Transaction OTPs<br/>Incoming SMS Verification Codes"] --> B4[OtpManager]
    end

    subgraph ENGINE["2. EdgeGuard Dual Threat Engine"]
        B1 & B2 & B3 & B4 --> C1{Event Dispatcher & Behavioral Router}
        
        subgraph NLP_ENGINE["Pillar 1: NLP & Deterministic Pipeline"]
            C1 -->|Raw Message Text| D1["Tier 1: Deterministic Rule Matrix (&lt;0.1ms)<br/>Keyword trie, regex, phonetics & homoglyphs"]
            C1 -->|Novel / Ambiguous Variants| D2["Tier 2: Quantized TFLite SLM (~185ms)<br/>Local MobileBERT 8-bit quantized weights"]
        end

        subgraph UBA_ENGINE["Pillar 2: Real-Time User Behavior Analysis (UBA)"]
            C1 -->|Cross-App Ad Click| E1["📈 Meta / Instagram Trading Ad Interceptor<br/>Instagram/Facebook Ads &rarr; Telegram/WhatsApp VIP Group"]
            C1 -->|Sideload APK Click| E2["📦 APK Fraud & Pre-Install Trojan Shield<br/>e-Challan / Wedding APK Intercept + Installer Dismissal"]
            C1 -->|Group Chat Feed| E3["💬 WhatsApp & Telegram Group Phishing Scanner<br/>Mass-Broadcast Scam Links, Airdrops & Group Payloads"]
            C1 -->|Temporal Correlation| E4["🔐 OTP Theft Interceptor<br/>Unknown Call &le; 7 mins after Bank OTP SMS"]
            C1 -->|VoIP Video Call| E5["📹 Sextortion Watchdog<br/>Unknown Incoming Video Call Detector"]
            C1 -->|Package Added| E6["💻 Remote Access (RAT) Watchdog<br/>AnyDesk / TeamViewer Install During Active Call"]
            C1 -->|Bank App Launch| E7["🏛️ Digital Arrest Shield<br/>Bank App Opened &le; 2h of Police Extortion Threat"]
            C1 -->|Query Bar Intent| E8["🔍 Pre-Emptive Search Scanner<br/>Fake Customer Care, Loan & Booking Queries"]
        end
    end

    subgraph PERSISTENCE["3. Local Persistence Layer"]
        D1 & D2 & E1 & E2 & E3 & E4 & E5 & E6 & E7 & E8 --> F1[("Local Room Database<br/>Encrypted &bull; 14-Day Auto-Purge")]
    end

    subgraph INTERVENTION["4. Instant Intervention Layer"]
        D1 & D2 & E1 & E2 & E3 & E4 & E5 & E6 & E7 & E8 -->|Threat Confirmed| G1{Overlay Permission?}
        G1 -->|Granted| H1["🚨 Non-Blocking Compose Warning Overlay<br/>Custom Floating Dialog via WindowManager"]
        G1 -->|Denied / Background| H2["⚠️ High-Priority System Notification<br/>Channel: SCAM_ALERTS with Sound/Vibration"]
        E2 -->|Trojan Detected| H3["🛑 Android Package Installer Auto-Dismiss<br/>Simulates GLOBAL_ACTION_BACK to block install"]
    end

    style INGESTION fill:#1e293b,stroke:#3b82f6,stroke-width:2px,color:#fff
    style ENGINE fill:#0f172a,stroke:#8b5cf6,stroke-width:2px,color:#fff
    style NLP_ENGINE fill:#1e1b4b,stroke:#a855f7,stroke-width:2px,color:#fff
    style UBA_ENGINE fill:#14532d,stroke:#22c55e,stroke-width:2px,color:#fff
    style PERSISTENCE fill:#1e1e24,stroke:#10b981,stroke-width:2px,color:#fff
    style INTERVENTION fill:#311b92,stroke:#ec4899,stroke-width:2px,color:#fff
```

For complete sequence diagrams, state machines, and mathematical formulations, refer to the [**Technical Design Document**](./TECHNICAL_DESIGN_DOCUMENT.md).

---

## 🧠 User Behavior Analysis (UBA) & Multi-App Funnels

A critical innovation of EdgeGuard AI is its ability to detect **behavioral multi-app attack funnels** before financial damage occurs:

```mermaid
graph TD
    subgraph Behavioral Threat Interceptors
        T1["📈 1. Fake Instagram / Meta Trading Ads Interceptor<br/>Instagram / Facebook Sponsored Ads &rarr; Telegram / WhatsApp VIP Group<br/>Blocks Fake Stock Tips, Crypto Schemes & VIP Trading Funnels"]
        T2["📦 2. Universal APK Frauds & Pre-Install Trojan Shield<br/>Fake Traffic e-Challan & Wedding Card .apk Files<br/>Dismisses Package Installer to Prevent Malware Execution"]
        T3["💬 3. WhatsApp & Telegram Group Phishing Scanner<br/>Phishing Links, Bogus Giveaways & Crypto Airdrops in Groups<br/>Inspects Group Feeds in Real-Time Without Compromising Privacy"]
        T4["📹 4. Sextortion Video Call Watchdog<br/>Unknown VoIP / Cellular Video Calls<br/>Alerts User to Cover Camera & Decline Blackmail Traps"]
        T5["🔐 5. Temporal OTP Theft Interceptor<br/>Bank OTP Received + Unknown Call Within 7 Mins<br/>Alerts User NEVER to Share OTP Over Call"]
        T6["💻 6. Remote Access (RAT) Watchdog<br/>AnyDesk / TeamViewer Installed During/After Call<br/>Alerts Against Total Device Hijacking"]
        T7["🏛️ 7. Digital Arrest Shield<br/>Banking App Opened &le; 2 Hours After Fake Police Threat<br/>Prevents Coerced RTGS / IMPS Extortion Transfers"]
        T8["🔍 8. Pre-Emptive Search Scanner<br/>Searching for Customer Care, Loans, or Trading in Browser<br/>Warns Before Tapping Sponsored Fraudulent Listings"]
    end
```

### 1. Fake Instagram / Meta Trading Ads Interceptor (Instagram / Facebook &rarr; Telegram / WhatsApp)
* **Attack Scenario:** A user browsing Instagram feeds, Reels, or Facebook taps a sponsored Meta ad featuring deepfake celebrity endorsements, bogus SEBI certificates, or promising *"Guaranteed 500% Stock Trading Returns"*. The ad funnels the victim directly into WhatsApp or Telegram to join an unverified "VIP Trading Channel".
* **EdgeGuard AI Intervention:** `MessageReaderAccessibilityService` tracks cross-app intent transitions. When it detects a redirect from Instagram (`com.instagram.android`), Facebook (`com.facebook.katana`), or mobile browsers into a Telegram or WhatsApp group join screen within 60 seconds, it intercepts the screen with:
  > **🚨 BEWARE OF FAKE TRADING GROUPS!**  
  > *You were redirected from social media to join this group. Scammers create fake investment channels to steal your savings. Never transfer money for stock tips or crypto schemes!*

### 2. Universal APK Frauds & Pre-Install Shield (e-Challan, Wedding Card & Sideload Trojans)
* **Attack Scenario:** Attackers send disguised `.apk` trojans via WhatsApp, Telegram, SMS, or malicious landing pages—disguised as **Traffic e-Challan APKs** (`echallan.apk`), **Wedding Invitation Cards** (`Wedding_Card.apk`), **PM-Kisan updates**, or banking security patches.
* **EdgeGuard AI Intervention:** 
  * **Click Interception:** Intercepts clicks on `.apk` files inside WhatsApp, Telegram, Chrome, and Samsung Internet.
  * **Pre-Install Shield:** Automatically simulates Android `GLOBAL_ACTION_BACK` to dismiss unverified package installer prompts, preventing clickjacking and displaying an unmistakable red trojan alert before the package can be installed.

### 3. WhatsApp & Telegram Group Phishing Scanner
* **Attack Scenario:** Scammers flood public and private community groups, broadcast channels, and college/work groups with deceptive phishing links, fake cryptocurrency airdrops, and credential-harvesting portals.
* **EdgeGuard AI Intervention:** Continuously inspects group messages and shared links in real-time without compromising user group privacy. If an uncatalogued phishing link or malicious attachment is displayed, an immediate on-screen warning card warns the user before they tap.

### 4. Unknown Video Call Watchdog (Sextortion Defense)
* **Attack Scenario:** Scammers initiate an unexpected video call from an unsaved number on WhatsApp or cellular networks. Once the victim answers, scammers record the victim's face alongside explicit video to manufacture blackmail material.
* **EdgeGuard AI Intervention:** Cross-references the incoming video call against the device's local phonebook (`ContactUtils.isContactExists`). If the caller is unknown, EdgeGuard AI triggers `OverlayUtils.showSextortionWarningOverlay`:
  > **⚠️ SEXTORTION ALERT: UNKNOWN VIDEO CALL**  
  > *Scammers use video calls from unknown numbers to record your face and blackmail you. Keep your camera covered and decline calls from unknown callers!*

### 5. Bank OTP + Unknown Call Correlation (OTP Theft Interceptor)
* **Attack Scenario:** A victim receives a bank transaction OTP. Within seconds, a scammer calls from an unknown number posing as a bank manager or credit card officer, demanding the OTP to "reverse a fraudulent charge."
* **EdgeGuard AI Intervention:** `OtpManager` logs the timestamp of incoming bank OTPs. When `AppPhoneStateReceiver` detects an incoming call from an unknown number within **7 minutes** of receiving an OTP, it immediately pops `OverlayUtils.showOtpWarningOverlay` directly over the ringing call screen:
  > **🛑 CRITICAL: DO NOT SHARE YOUR OTP!**  
  > *You received a bank OTP recently, and now an unknown caller is ringing your phone. Bank officials NEVER ask for OTPs or passwords over the phone!*

### 6. Remote Access / RAT Watchdog (AnyDesk & TeamViewer Interceptor)
* **Attack Scenario:** Over an active phone call, a scammer convinces the victim to download a remote access app (AnyDesk, TeamViewer, RustDesk, QuickSupport) to "solve a technical issue" or "unfreeze an account," allowing the scammer to remotely control the phone and drain bank accounts.
* **EdgeGuard AI Intervention:** `AppInstallReceiver` listens for package installations (`ACTION_PACKAGE_ADDED`). If a blacklisted remote access app is installed **while an unknown call is active or within 1 hour of an unknown call**, EdgeGuard AI pops `OverlayUtils.showRatWarningOverlay`:
  > **🚨 SEVERE WARNING: DEVICE HIJACKING ATTEMPT!**  
  > *You installed a remote desktop app while talking to an unknown caller. This allows them to see your screen and steal your bank passwords. Hang up and uninstall immediately!*

### 7. Digital Arrest &rarr; Banking App Protection
* **Attack Scenario:** Scammers impersonate CBI, Police, or Customs officers, accusing the victim of money laundering and placing them under "digital arrest" via Skype or WhatsApp, ordering them to transfer money to a "court verification account."
* **EdgeGuard AI Intervention:** When a digital arrest threat is intercepted, a 2-hour protection window is activated. If the panicked user launches any banking app (SBI, HDFC, ICICI, PhonePe, Paytm), EdgeGuard AI intervenes with `showDigitalArrestWarningOverlay`:
  > **🛑 STOP! DIGITAL ARREST IS A 100% FAKE SCAM!**  
  > *Law enforcement agencies NEVER arrest people via video calls, nor do they ever demand money transfers. Do not transfer any funds!*

### 8. Pre-Emptive Browser Search Intent Scanner
* **Attack Scenario:** Users search Google for customer care numbers, loan approvals, or travel bookings and click sponsored scam ads containing fake helpline numbers.
* **EdgeGuard AI Intervention:** Inspects browser query intents and displays pre-emptive safety cards warning users before they dial fraudulent customer care numbers or pay advance booking fees over WhatsApp QR codes.

---

## 🛡️ Scam Vectors Protected

| Threat Category | Attack Vector Description | Example Indicators |
| :--- | :--- | :--- |
| **Fake Instagram / Meta Trading Ads** | Sponsored social media ads on Reels/feeds redirecting to scam Telegram/WhatsApp trading groups. | *"Earn 500% daily returns in our SEBI certified Telegram channel", "VIP Stock Tips"* |
| **Universal APK Frauds & Trojans** | Disguised APK malware (e-Challan, wedding invitations, PM Kisan) hijacking device permissions. | *"Unpaid traffic challan: echallan.apk", "Wedding_Card.apk", "security_update.apk"* |
| **WhatsApp & Telegram Group Phishing** | Mass broadcast phishing links, free recharge coupons, and crypto giveaways in community groups. | *"Join urgent group airdrop: claim-free-eth.xyz", "3-month free recharge", "Tap here to claim gift"* |
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

* **Tier 1 (Deterministic) Latency:** `< 0.08 ms`
* **Tier 2 (TFLite NLP) Latency:** `~ 185 ms`
* **Behavioral Funnel Latency:** `< 0.05 ms`
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

For EdgeGuard AI to protect your communications and user behavior in real time, the following permissions must be enabled:

1. **Accessibility Service (Required for Text Inspection & Cross-App Funnel Tracking):**
   * Navigate to: `Settings > Accessibility > Downloaded Apps > EdgeGuard AI` (or `SafeTexts`).
   * Toggle the service to **ON** and confirm.
2. **Display Over Other Apps (Required for Warning Card Overlays):**
   * Navigate to: `Settings > Apps > Special App Access > Display Over Other Apps > EdgeGuard AI`.
   * Enable **Allow display over other apps**.
3. **Phone & Call State Permission (Required for OTP & Call Correlation):**
   * Enables `AppPhoneStateReceiver` to detect unknown callers when OTPs are received.
4. **Notification Access (Fallback Alerts):**
   * Enables high-priority fallback security notifications if overlays are restricted.

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

## 🔒 Privacy Policy & Zero-Cloud Guarantee

EdgeGuard AI is engineered strictly for **100% on-device inference** with **zero cloud egress**. No private chat messages, call logs, contacts, OTP verification codes, or behavioral telemetry ever leave the mobile device.

👉 **[Read the Full Privacy Policy](https://parv55555.github.io/EdgeGuard-AI-website/privacy-policy.html)**

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
