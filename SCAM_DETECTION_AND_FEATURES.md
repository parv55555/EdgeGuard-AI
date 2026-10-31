# EdgeGuard — Features & Scam Detection Capabilities

This document provides a comprehensive inventory of all system features, scam types diagnosed, behavioral patterns analyzed, and real-time alerts implemented in **EdgeGuard**.

---

## 1. Core Architectural Features

* **100% On-Device AI / NLP Engine**: Executes text classification and NLP inference locally using TensorFlow Lite (fine-tuned `A0908i` SLM). Zero message content is ever transmitted to cloud servers.
* **Complex Event Processing (CEP) / User Behaviour Analysis (UBA)**: Correlates asynchronous Android system events (calls, notifications, app transitions, installation events, search inputs, and temporal delays) to detect multi-stage social engineering.
* **Real-Time Floating Overlay Warning System**: Utilizes Android's `WindowManager` overlay service to display high-visibility, explanatory alert cards directly over active messaging chats, phone dialers, or browsers without closing the current app.
* **Non-Intrusive Background Services**:
  * `NotificationListenerService` (`ScamDetectionService`): Scans incoming notification text, identifies OTP arrivals, and detects VoIP calls across supported apps.
  * `AccessibilityService` (`MessageReaderAccessibilityService`): Performs strictly read-only inspection of messaging nodes, package installer dialogs, and browser search intent.
  * `BroadcastReceiver` (`AppPhoneStateReceiver`, `AppInstallReceiver`): Monitors telecom call states and package installation events.
* **Zero-Egress Security Boundary**: No external network permissions used during detection. All analysis runs in volatile device memory.
* **Local Encrypted Room Database with Auto-Purge**: Threat records and alert events are stored locally in an encrypted Room SQLite database and automatically purged after **12 days** for data privacy.

---

## 2. Complete List of Scams Diagnosed & Alerted

### A. Behavioral, Call & Social Engineering Scams

#### 1. Digital Arrest Scam
* **Threat Mechanism**: Fraudsters impersonate law enforcement (Police, CBI, ED, Narcotics) on a video call, intimidate the victim into staying on the call, and instruct them to open a banking app to "verify" or transfer funds.
* **Detection Criteria**:
  * Active VoIP video call lasting **> 15 minutes** with an unknown caller (not in contacts).
  * User opens a banking, UPI, or payment application during the call or within **2 hours** of the call.
* **Alert Triggered**: **Digital Arrest Warning Overlay** advising the user to hang up immediately and stating that Indian law enforcement never conducts interrogations or arrests over video calls.

#### 2. OTP Theft / Vishing (Voice Phishing) Scam
* **Threat Mechanism**: Scammers call pretending to be bank executives, telecom agents, or courier support, triggering an OTP and tricking the user into speaking the OTP over the phone.
* **Detection Criteria**:
  * High-value OTP arrives (containing banking/Aadhaar/PAN/KYC keywords and a 4–8 digit code).
  * Evaluated against 3 temporal conditions:
    1. Phone is on an active cellular or VoIP call with an unsaved number when the OTP arrives.
    2. An unknown caller dials within **7 minutes** of receiving an OTP.
    3. A high-value OTP arrives within **1 hour** of ending a call with an unknown caller.
* **Alert Triggered**: **OTP Theft Warning Overlay** directly over the call screen: *"NEVER share your OTP over the phone! Banks never call to ask for OTPs."*

#### 3. Remote Access Trojan (RAT) / Screen-Sharing Scam
* **Threat Mechanism**: Scammers instruct the user over a call to install screen-sharing software (AnyDesk, TeamViewer, RustDesk) under the guise of "customer support" or "KYC completion" to view screens and steal banking credentials.
* **Detection Criteria**:
  * Android package manager broadcast (`ACTION_PACKAGE_ADDED`) detects installation of blacklisted remote control apps:
    * `AnyDesk` (`com.anydesk.anydeskandroid`)
    * `TeamViewer QuickSupport / Host`
    * `RustDesk` (`com.carriez.rustdesk`)
    * `Apowersoft Mirror` (`com.apowersoft.mirror`)
    * `AirDroid` (`com.sand.airdroid`)
    * `Chrome Remote Desktop`, `RealVNC`, `Microsoft Remote Desktop`
  * Co-occurs during an active call or within **1 hour** of a call with an unsaved number.
* **Alert Triggered**: **RAT Watchdog Warning Overlay** urging immediate app deletion and hanging up the call.

#### 4. Sextortion / Video Call Blackmail Trap
* **Threat Mechanism**: Unknown contacts place video calls. Once answered, compromised explicit media is played and the victim's face is recorded to extort money.
* **Detection Criteria**:
  * Incoming VoIP video call notification in WhatsApp, Telegram, Skype, Google Meet, or Zoom.
  * Caller is verified as not present in the local contact address book.
* **Alert Triggered**: **Sextortion Warning Overlay** warning the user before answering: *"Unknown video call detected. Scammers record your screen to blackmail you."*

---

### B. Malicious App, APK & Sideloading Scams

#### 5. Pre-Install Malicious APK Shield (e-Challan, Wedding Cards, PM-Kisan)
* **Threat Mechanism**: Trojan APK files sent via WhatsApp or browser downloads masquerading as utility bills, government schemes, wedding invitations, or traffic challans.
* **Detection Criteria**:
  * System Package Installer (`packageinstaller`) window appears on screen with install/update prompts.
  * Evaluates whether the user is on an active call (escalated severity).
* **Alert Triggered**: **Pre-Install Shield Warning Overlay** advising the user to abort installation of unverified sideloaded apps outside Google Play.

#### 6. Post-Install Sideload Watchdog
* **Threat Mechanism**: Sideloaded APKs successfully installed from browsers, WhatsApp, Telegram, or unknown file managers.
* **Detection Criteria**:
  * Package install receiver inspects `getInstallSourceInfo()`.
  * Flags apps installed from non-official sources (non-Google Play, non-OEM official store).
* **Alert Triggered**: **Post-Install Warning Overlay** detailing the unverified package origin.

#### 7. Browser Malicious APK Download Interceptor
* **Threat Mechanism**: Deceptive websites or drive-by downloads pushing malicious APKs directly in Chrome or Samsung Internet.
* **Detection Criteria**:
  * Accessibility reader inspects browser DOM / download prompts containing `.apk` filenames combined with download/harmful/install keywords.
* **Alert Triggered**: **Malicious APK Download Alert Card**.

---

### C. Social Media Funnels & Search Intent Scams

#### 8. Meta / Instagram Ad Scam Funnel Interceptor
* **Threat Mechanism**: Sponsored Instagram/Facebook ads promising 500% trading profits or stock tips that redirect users into WhatsApp or Telegram groups.
* **Detection Criteria**:
  * Application switching velocity: User transitions from a browser, Facebook, or Instagram into WhatsApp or Telegram within **60 seconds**.
  * Destination screen displays group invite UI (`"Join Group"`, `"Join Channel"`, `"Request to Join"`).
* **Alert Triggered**: **Scam Funnel Warning Overlay**: *"You were redirected here from a social media ad. Beware of fraudulent investment/trading groups."*

#### 9. Direct Message (DM) Funnel Interceptor
* **Threat Mechanism**: Sponsored ads that jump the user directly into a 1-on-1 private chat with a scammer promoting part-time jobs, crypto, or forex tasks.
* **Detection Criteria**:
  * Direct transition from browser/social ad within 60s into a WhatsApp 1-on-1 chat with an unsaved phone number.
* **Alert Triggered**: **DM Scam Funnel Overlay**.

#### 10. Fake Travel & Hotel Booking Scam (Cross-App Linkage)
* **Threat Mechanism**: Fake hotel, resort, or religious tour (Dham) booking listings on Google. The user is redirected to WhatsApp, where the "agent" demands advance payments via QR code.
* **Detection Criteria**:
  * Step 1: User enters travel search queries (`hotel`, `resort`, `villa`, `dhams`, `flight booking`) in a browser search bar.
  * Step 2: User opens a WhatsApp conversation within **30 minutes** (temporally tagged as `BOOKING_SCAM_RISK`).
  * Step 3: Screen displays payment requests (`qr code`, `scanner`, `upi`, `@ybl`, `@paytm`, `pay now`).
* **Alert Triggered**: **Fake Booking Warning Overlay**: *"Never pay booking advance via QR code or WhatsApp UPI."*

#### 11. Pre-Emptive Customer Care / Helpline Scam
* **Threat Mechanism**: Scammers manipulate search rankings with fake customer support numbers for banks, airlines, and courier services.
* **Detection Criteria**:
  * Browser search input contains: `customer care`, `complaint care`, `helpline`, `toll free`, `support number`.
* **Alert Triggered**: **Pre-Emptive Customer Care Alert**: Advises using only contact details found inside official banking/service apps.

#### 12. Pre-Emptive Instant Loan Trap
* **Threat Mechanism**: Predatory loan apps that charge extortionate "processing fees" and blackmail victims using phone contacts.
* **Detection Criteria**:
  * Browser search input contains queries for instant loans (`loan`, `loans`).
* **Alert Triggered**: **Pre-Emptive Loan Safety Alert**: Warns never to pay advance processing fees or download unverified loan APKs.

#### 13. Pre-Emptive Part-Time Job / Task Scam
* **Threat Mechanism**: "Like YouTube videos for money", "Work from Home daily salary", and Telegram task scams demanding registration fees.
* **Detection Criteria**:
  * Browser search input contains: `wfh jobs`, `part time jobs`, `online tasks`, `like videos for money`, `earn money online`, `data entry`.
* **Alert Triggered**: **Pre-Emptive Job Safety Alert**: Warns that any job requiring an upfront registration fee or security deposit is fraudulent.

#### 14. Pre-Emptive VIP Trading & Crypto Scam
* **Threat Mechanism**: Fake stock advisory services, binary options, and crypto doubling schemes.
* **Detection Criteria**:
  * Browser search input contains: `trading`, `crypto`, `bitcoin`, `forex`, `stock tips`, `options trading`, `binance`.
* **Alert Triggered**: **Pre-Emptive Trading Safety Alert**: Warns against joining VIP trading groups with guaranteed return claims.

---

### D. Message, Notification & Text Phishing Scams

#### 15. Electricity Bill Disconnection Scam
* **Threat**: Messages warning that electric power will be cut tonight at 9:30 PM unless an unpaid bill is cleared immediately via an attached number or link.
* **Detection Engine**: Fast deterministic parser + TFLite text classifier.
* **Alert**: Floating Scam Card & Notification.

#### 16. Bank / PAN / KYC / SIM Card Block Scam
* **Threat**: Urgent notices claiming SBI YONO, bank account, PAN card, or SIM card will be deactivated/suspended unless updated immediately via a phishing link.
* **Detection Engine**: Keyword vector heuristics + TFLite NLP classifier.
* **Alert**: Floating Scam Card & Notification.

#### 17. Fake Payment Credit Scam (Reverse-UPI Fraud)
* **Threat**: Fake SMS formatted like bank credit notifications (`"Rs. 25,000 credited to your A/C..."`) used by scammers to claim they "accidentally overpaid" and request a refund.
* **Detection Engine**: Regex-driven balance check + sender validation (flags non-bank numeric phone senders sending fake credit texts).
* **Alert**: Floating Scam Card.

#### 18. Fake Lottery / KBC / Prize Winner Scam
* **Threat**: Unsolicited SMS claiming the user has won ₹25 Lakh in KBC lottery or lucky draws.
* **Detection Engine**: TFLite NLP classifier + fast regex rule.
* **Alert**: Floating Scam Card.

#### 19. Browser Push Notification Spam / Fake Antivirus Scams
* **Threat**: Deceptive browser notifications simulating system dialogs (`"Virus detected"`, `"Phone infected"`, `"McAfee expired"`, `"Storage full"`, `"Clean now"`).
* **Detection Engine**: Browser notification listener filtering for fake infection and rogue antivirus keywords.
* **Alert**: Browser Scam Warning Overlay.

#### 20. Obfuscated Betting & Casino Funnels
* **Threat**: Illegal betting, Aviator, and slot prediction links shared in chats using spaced or leet-speak obfuscation (e.g., `b e t n o w`, `w1n b1g`).
* **Detection Engine**: Built-in De-Leetification Engine (normalizes character substitutions and whitespace) + Betting action/mechanic pairing.
* **Alert**: Scam Funnel Overlay.

---

## 3. Scam Coverage Matrix

| Scam Category | Threat Type | Primary Input / Sensor | Action / Output |
| :--- | :--- | :--- | :--- |
| **Calls & Banking** | Digital Arrest | VoIP video call >15 min + Banking app launch | Digital Arrest Overlay |
| **Calls & Banking** | OTP Theft / Vishing | Sensitive OTP arrival + Active/recent unknown call | OTP Warning Overlay |
| **Calls & Remote** | Screen-Sharing / RAT | Package install of AnyDesk/TeamViewer + Active call | RAT Watchdog Overlay |
| **Calls & Video** | Sextortion | Incoming unknown video call notification | Sextortion Warning Overlay |
| **Installation** | Trojan APK Sideload | `packageinstaller` window active + call status | Pre-Install Shield Overlay |
| **Installation** | Sideloaded Package | `ACTION_PACKAGE_ADDED` + install source info | Post-Install Alert Overlay |
| **Social Funnel** | Meta/IG Ad to Group | Rapid transition (Browser/IG $\to$ WhatsApp/TG Group) | Group Funnel Overlay |
| **Social Funnel** | Ad to Direct Message | Rapid transition (Browser/IG $\to$ WhatsApp Unsaved DM)| DM Funnel Overlay |
| **Cross-App** | Fake Travel Booking | Browser travel search + WhatsApp chat + QR request | Booking Scam Overlay |
| **Browser Intent** | Fake Customer Care | Search bar queries for customer care / helpline | Pre-Emptive Helpline Alert |
| **Browser Intent** | Predatory Loan Apps | Search bar queries for instant loans | Pre-Emptive Loan Alert |
| **Browser Intent** | Part-Time Job / Task | Search bar queries for WFH / online tasks | Pre-Emptive Job Alert |
| **Browser Intent** | VIP Trading / Crypto | Search bar queries for stock tips / crypto | Pre-Emptive Trading Alert |
| **Messages** | Electricity Bill Cut | WhatsApp / SMS message text | Scam Card Overlay |
| **Messages** | KYC / PAN / Bank Block | WhatsApp / SMS message text | Scam Card Overlay |
| **Messages** | Fake Payment Credit | Fake credit SMS from personal number | Scam Card Overlay |
| **Messages** | KBC / Lottery / Prize | WhatsApp / SMS message text | Scam Card Overlay |
| **Browser** | Fake Virus / Cleaner | Browser push notification text | Browser Spam Overlay |
| **Chat & Web** | Obfuscated Betting | WhatsApp / Browser text with leet-speak | Scam Funnel Overlay |

---

*All detections execute **100% offline** on the user's device processor without cloud egress.*

