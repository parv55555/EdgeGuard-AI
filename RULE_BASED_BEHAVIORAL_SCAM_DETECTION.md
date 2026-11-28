# EdgeGuard AI: Rule-Based Behavioral Threat Detection & Alert Reference Manual

**Author:** Parv Khandelwal  
**System:** EdgeGuard AI (SafeTexts)  
**Engine Architecture:** Zero-Cloud, Pure Rule-Based Behavioral Telemetry Engine  
**Version:** Production Release v2.0  

---

## Executive Summary

EdgeGuard AI’s behavioral threat detection engine operates **100% on-device** without recording audio, logging keystrokes, or violating user privacy. Rather than inspecting private chat bodies, EdgeGuard monitors **structural device state transitions, cross-app temporal linkages, accessibility UI cues, and telephony state changes**.

This document outlines all **15 behavioral threat vectors** detected by the rule-based engine, specifying:
1. **Scam Profile & Attack Vector**: The adversary's exploitation choreography.
2. **Diagnostic Rules & Behavioral Telemetry**: Exact trigger conditions, cross-app timings, accessibility node matching, and telephony parameters.
3. **Exact User-Facing Alert**: UI card style, warning title, advisory body, and interactive buttons.

---

## 1. Pre-Install Malicious APK Gatekeeper

### Scam Profile
Scammers trick victims into sideloading unverified Android packages (`.apk`) sent over WhatsApp, Telegram, or downloaded from phishing web redirects disguised as government portals, lottery forms, or banking KYC updates.

### Diagnostic Logic & Rules
- **Component:** `MessageReaderAccessibilityService.kt`
- **Trigger Conditions:**
  1. Package name matches `packageinstaller` (excluding system `android` and native `myfiles`).
  2. Accessibility window events (`TYPE_WINDOW_STATE_CHANGED` or `TYPE_WINDOW_CONTENT_CHANGED`).
  3. Recursive node inspection detects exact install actions:
     - Clickable button with text `"install"` or `"update"` (case-insensitive).
     - UI keywords: `"install"`, `"package"`, `"unknown sources"`, `"allow from this source"`.
  4. Call telemetry correlation: Evaluates `ContactUtils.isUserOnCall(context)` to verify if the victim is currently being coerced over an active cellular or VoIP call.
  5. Debounce: Minimum 3,000ms threshold between warnings.
  6. Temporary Bypass Guard: Suppressed if the user explicitly clicked "Install Anyway" within the past 60 seconds (`allowNextInstallUntilTime`).

### User-Facing Alert Displayed
- **Card Builder:** `AlertCardBuilder.buildPreInstallCard(context, isSevere, onDismiss)`
- **UI Styling:**
  - Standard: Warm Amber Background (`#FFF3E0`), Deep Amber Stroke (`#E65100`), Elevation: `40f`.
  - Severe (Active Call): Crimson Red Background (`#FFEBEE`), Severe Red Stroke (`#D50000`), Elevation: `40f`.
- **Alert Title:**
  - Normal: `⚠️ UNVERIFIED APP WARNING`
  - On Call: `🚨 SECURITY ALERT 🚨`
- **Warning Message:**
  - Normal:  
    > *"You are trying to install an unverified app.*  
    >  
    > *BEWARE: If someone claiming to be a customer service/care agent, a government officer, or a banking/loan person asks you to download this over a call or video call, they might hack your phone & bank account. If they ask you to install it, they are just scammers."*
  - On Call:  
    > *"If the caller has asked you to download this app, it is better not to install it as it might hack your device & bank account.*  
    >  
    > *Even if you are on a call with someone claiming to be a government officer, a bank person, or a loan giver, DO NOT install this app! If they ask you to install it, they are just scammers."*
- **Action Buttons:**
  - `[CANCEL INSTALL]` (Red/Amber solid button — dismisses prompt).
  - `[INSTALL ANYWAY]` (Outlined button — unlocks install for 60 seconds).

---

## 2. Post-Install Sideloaded Malware Watchdog

### Scam Profile
If a malicious APK bypasses installation dialogs (e.g. via background installer or pre-granted permissions), the malware immediately attempts to register background accessibility or notification listeners to intercept SMS OTPs.

### Diagnostic Logic & Rules
- **Component:** `AppInstallReceiver.kt`
- **Trigger Conditions:**
  1. System broadcast `Intent.ACTION_PACKAGE_ADDED`.
  2. Extracts package name and queries `PackageManager.getInstallSourceInfo(packageName)`.
  3. Installer verification against trusted store package names:
     - `com.android.vending` (Google Play Store)
     - `com.sec.android.app.samsungapps` (Samsung Galaxy Store)
     - `com.xiaomi.mipicks` (Xiaomi GetApps)
     - `com.heytap.market` / `com.oppo.market` (OPPO App Market)
     - `com.bbk.appstore` (Vivo V-Appstore)
     - `com.huawei.appmarket` (Huawei AppGallery)
  4. Triggers if installer is untrusted (e.g. `null`, `packageinstaller`, `browser`, `chrome`, `whatsapp`, `telegram`).

### User-Facing Alert Displayed
- **Card Builder:** `AlertCardBuilder.buildPostInstallCard(context, maliciousPkg, onDismiss)`
- **UI Styling:** Crimson Wash Background (`#FFF0F0`), Deep Burgundy Stroke (`#B71C1C`, 8px), Elevation: `32f`.
- **Alert Title:** `🚨 CRITICAL THREAT INSTALLED`
- **Warning Message:**  
  > *"A highly dangerous app was just installed on your phone from an untrusted source ([maliciousPackageName]).*  
  >  
  > *This app could be secretly reading your SMS and stealing money from your bank accounts. UNINSTALL IT IMMEDIATELY!"*
- **Action Button:** `[I WILL UNINSTALL IT NOW]` (Dark Red solid button).

---

## 3. Remote Access Tool (RAT) Screen-Sharing Takeover

### Scam Profile
Impostors pretending to be bank managers, KYC officers, or courier customer support instruct victims to install screen-sharing software (AnyDesk, TeamViewer, RustDesk) so they can remotely view OTPs and account balances.

### Diagnostic Logic & Rules
- **Component:** `AppInstallReceiver.kt`
- **Trigger Conditions:**
  1. Broadcast `ACTION_PACKAGE_ADDED` detects installation of a blacklisted RAT tool:
     - `com.anydesk.anydeskandroid` (AnyDesk)
     - `com.teamviewer.quicksupport.market` (TeamViewer QuickSupport)
     - `com.teamviewer.host.market` (TeamViewer Host)
     - `com.carriez.rustdesk` (RustDesk)
     - `com.apowersoft.mirror` (Apowersoft Mirror)
     - `com.microsoft.rdc.androidx` (Microsoft Remote Desktop)
     - `com.google.chromeremotedesktop` (Chrome Remote Desktop)
     - `com.sand.airdroid` (AirDroid)
     - `com.realvnc.viewer.android` (RealVNC)
  2. Telephony State Evaluation:
     - Checks active cellular off-hook state (`TelephonyManager.CALL_STATE_OFFHOOK`) or active VoIP call (`OtpManager.getActiveVoipCallNumber()`).
     - Checks if caller is unsaved in contacts (`!ContactUtils.isContactExists()`).
     - Checks if an unknown call occurred in the past 1 hour (`timeSinceUnknownCall <= 3,600,000ms`).

### User-Facing Alert Displayed
- **Card Builder:** `AlertCardBuilder.buildRatCard(context, isSevere, onDismiss)`
- **UI Styling:**
  - On Active Unknown Call: High-Risk Crimson (`#FFEBEE`), Blood Red Stroke (`#D50000`, 6px), Elevation: `40f`.
  - Non-Call Installation: Caution Amber (`#FFF3E0`), Deep Amber Stroke (`#E65100`, 6px), Elevation: `40f`.
- **Alert Title:**
  - Severe: `🚨 SECURITY ALERT 🚨`
  - Non-Severe: `⚠️ REMOTE ACCESS WARNING`
- **Warning Message:**
  - Severe:  
    > *"If the caller has asked you to download this app, it is better not to install it as it might hack your device & bank account.*  
    >  
    > *Even if you are on a call with someone claiming to be a government officer, a bank person, or a loan giver, DO NOT install this app! If they ask you to install it, they are just scammers."*
  - Non-Severe:  
    > *"You just installed a screen-sharing app.*  
    >  
    > *BEWARE: If someone claiming to be a customer service/care agent, a government officer, or a banking/loan person asks you to download this over a call or video call, they might hack your phone & bank account. If they ask you to install it, they are just scammers."*
- **Action Button:** `[I UNDERSTAND THE RISK]` / `[GOT IT]`.

---

## 4. Digital Arrest Video Call Coercion

### Scam Profile
Scammers pose as Police, CBI, NCB, or Telecom Enforcement officers, keeping victims isolated on a video call (claiming "digital house arrest") for hours while compelling them to open netbanking or UPI apps to transfer "security collateral."

### Diagnostic Logic & Rules
- **Components:** `ScamDetectionService.kt` & `MessageReaderAccessibilityService.kt`
- **Trigger Conditions (Two-Stage Temporal Lock):**
  - **Stage 1 (Coercive Video Call Duration):**
    - Notification listener tracks continuous unknown VoIP video call (`WhatsApp`, `Telegram`, `Zoom`, `Google Meet`).
    - Duration threshold: `timeSinceVideoStart > 15 minutes` (900,000ms).
    - Preceded by or involving an unsaved contact within 24 hours.
    - Sets flag: `OtpManager.setDigitalArrestRiskTime(context)`.
  - **Stage 2 (Banking App Launch):**
    - User opens a recognized Indian banking/UPI application (SBI, HDFC, ICICI, Axis, PNB, BOB, Kotak, PhonePe, Google Pay, Paytm, CRED).
    - Condition: App is launched **during the active video call** OR **within 2 hours (7,200,000ms)** of the digital arrest risk window.

### User-Facing Alert Displayed
- **Card Builder:** `AlertCardBuilder.buildDigitalArrestCard(context, onDismiss)`
- **UI Styling:** Emergency Red Background (`#FFEBEE`), Heavy Warning Stroke (`#D50000`, 8px), Elevation: `48f`.
- **Alert Title:** `🚨 DIGITAL ARREST\nSCAM WARNING 🚨`
- **Warning Message:**  
  > *"You are on a long video call following an unknown call, and just opened a banking app.*  
  >  
  > *Police/CBI will NEVER arrest you over a video call. This is a scam.*  
  >  
  > *DO NOT TRANSFER MONEY."*
- **Action Button:** `[I UNDERSTAND]` (Solid Crimson, padding 32px).

---

## 5. OTP Theft & Snatching Pattern

### Scam Profile
Attacker initiates an unauthorized banking transaction, then immediately calls the victim posing as a fraud agent or bank security officer asking for the "verification code" to "cancel the fraud transaction."

### Diagnostic Logic & Rules
- **Components:** `AppPhoneStateReceiver.kt` & `ScamDetectionService.kt`
- **Trigger Conditions:**
  - **Pattern A (Call following OTP):**
    1. Incoming SMS contains high-value banking/KYC keywords: `bank`, `aadhar`, `pan`, `uidai`, `kyc`, `sbi`, `hdfc`, `icici`, `axis`, `pnb`, `bob` alongside OTP tokens.
    2. Telephony state switches to `CALL_STATE_RINGING` or `CALL_STATE_OFFHOOK` from an **unknown number**, OR an incoming VoIP call arrives.
    3. Temporal Window: Call occurs within **7 minutes (420,000ms)** of the OTP receipt (`currentTime - lastOtpTime <= 7 * 60 * 1000`).
  - **Pattern B (OTP during/after Call):**
    1. Device is currently in an active unknown call, OR an unknown call finished within the past 1 hour.
    2. A sensitive banking OTP arrives while the call state is active or fresh.

### User-Facing Alert Displayed
- **Card Builder:** `AlertCardBuilder.buildOtpCard(context, onDismiss)`
- **UI Styling:** Amber Alert Background (`#FFF8E1`), Deep Orange Stroke (`#FF8F00`, 4px), Elevation: `32f`.
- **Alert Title:** `⚠️ OTP SECURITY ALERT`
- **Warning Message:**  
  > *"You recently received an OTP.*  
  >  
  > *Do NOT share it with anyone on this call or message. Bank officials will NEVER ask for your OTP."*
- **Action Button:** `[I UNDERSTAND]` (Solid Orange).

---

## 6. Sextortion Honeytrap Video Call Trap

### Scam Profile
Scammers place unsolicited video calls from unsaved numbers. The moment the victim answers, the caller plays pre-recorded compromising footage, records the victim's face, and demands immediate ransom payments.

### Diagnostic Logic & Rules
- **Component:** `ScamDetectionService.kt`
- **Trigger Conditions:**
  1. Notification listener intercepts an incoming VoIP call notification (`isCallCategory` or text contains `"call"`).
  2. Notification text contains `"video call"` or originates from video conferencing packages.
  3. Caller ID is verified against device contacts: entity is completely **unsaved** (`!ContactUtils.isContactExists()`).
  4. Triggers immediately on ringing/connection before the camera can be weaponized.

### User-Facing Alert Displayed
- **Card Builder:** `AlertCardBuilder.buildSextortionCard(context, onDismiss)`
- **UI Styling:** Critical Crimson (`#FFEBEE`), Heavy Stroke (`#D50000`, 6px), Elevation: `40f`.
- **Alert Title:** `⚠️ VIDEO CALL ALERT`
- **Warning Message:**  
  > *"Scammers use video calls to record your face for blackmail and extortion.*  
  >  
  > *If you see a naked girl or a police officer on call, IMMEDIATELY cut the call & don't pay anything.*  
  >  
  > *DO NOT SHOW YOUR FACE."*
- **Action Button:** `[DISMISS & HANG UP]` (Bold Red button).

---

## 7. Social Media Ad-to-Group Rigged Funnel

### Scam Profile
Deceptive advertisements on Facebook, Instagram, or mobile browsers (e.g. "Work From Home Daily ₹5,000", "VIP Stock Tips", "Color Trading") immediately open WhatsApp or Telegram with a pre-configured group invitation link.

### Diagnostic Logic & Rules
- **Component:** `MessageReaderAccessibilityService.kt`
- **Trigger Conditions:**
  1. Foreground package changes from a browser (`Chrome`, `Samsung Internet`, `Firefox`, `Brave`, etc.) or ad container (`Facebook`, `Instagram`) to WhatsApp or Telegram.
  2. Temporal window: Transition occurs in **< 60 seconds (60,000ms)** (`currentTime - lastBrowserJumpTime < 60000L`).
  3. Accessibility node tree inspects screen:
     - Detects buttons or text: `"join group"`, `"join channel"`, `"join chat"`, `"request to join"`, `"join now"`, `"view community"`.
     - Detects Telegram/WhatsApp group invitation landing screens.

### User-Facing Alert Displayed
- **Card Builder:** `AlertCardBuilder.buildScamFunnelCard(context, isDM = false, onDismiss)`
- **UI Styling:** Soft Rose Background (`#FFF0F0`), Crimson Stroke (`#D32F2F`, 8px), Elevation: `32f`.
- **Alert Title:** `⚠️ SCAM FUNNEL DETECTED`
- **Warning Message:**  
  > *"Do not join groups for trading, forex, money tasks, betting. It is possibly a scam group designed to steal your funds."*
- **Action Button:** `[I UNDERSTAND]`.

---

## 8. Social Media Ad-to-DM Private Funnel

### Scam Profile
Advertisements direct victims straight into a private 1-on-1 WhatsApp or Telegram conversation with a scammer pretending to be a recruiter, broker, or merchant, bypassing group moderation.

### Diagnostic Logic & Rules
- **Component:** `MessageReaderAccessibilityService.kt`
- **Trigger Conditions:**
  1. Rapid jump from web browser / social feed to WhatsApp / Telegram within 60 seconds.
  2. Accessibility window identifies active chat screen:
     - Extracts chat title via `extractBestEffortChatTitle()`.
  3. Strict identity validation:
     - Target number/title is **NOT in user's contacts** (`!ContactUtils.isContactExists()`).
     - Target is **NOT a verified WhatsApp Business account** (`!ContactUtils.isWhatsAppCompanyAccount()`).
     - Chat displays a raw phone number format (7+ digits).

### User-Facing Alert Displayed
- **Card Builder:** `AlertCardBuilder.buildScamFunnelCard(context, isDM = true, onDismiss)`
- **UI Styling:** Soft Rose Background (`#FFF0F0`), Crimson Stroke (`#D32F2F`, 8px), Elevation: `32f`.
- **Alert Title:** `⚠️ SCAM FUNNEL DETECTED`
- **Warning Message:**  
  > *"Do not interact with this person for trading, forex, money tasks, or betting. It is possibly a scammer designed to steal your funds."*
- **Action Button:** `[I UNDERSTAND]`.

---

## 9. Pre-Emptive Travel Booking Scam Search

### Scam Profile
Victims searching for Kedarnath helicopter tickets, luxury resort packages, or pilgrimage tours click sponsored ads that mimic legitimate booking sites to collect non-refundable advances.

### Diagnostic Logic & Rules
- **Component:** `MessageReaderAccessibilityService.kt`
- **Trigger Conditions:**
  1. Active package is a recognized web browser.
  2. Accessibility node text extraction inspects search input:
     - Keywords: `"hotel booking"`, `"resort booking"`, `"helicopter ticket"`, `"kedarnath booking"`, `"dham booking"`, `"advance deposit"`, `"travel package"`.
  3. Frequency limit: Warns at most once per 60 minutes (`last_booking_warning_time`).
  4. Stores `booking_intent_timestamp` in shared preferences to activate Stage 2 chat monitoring.

### User-Facing Alert Displayed
- **Card Builder:** `OverlayUtils.showPreEmptiveBookingAlert(context)` -> `AlertCardBuilder.buildSimpleAlertCard(...)`
- **UI Styling:** Ice Blue Background (`#E3F2FD`), Deep Blue Stroke (`#1976D2`, 4px), Title Color: `#1565C0`.
- **Alert Title:** `✈️ TRAVEL SAFETY ALERT`
- **Warning Message:**  
  > *"You are searching for travel bookings (hotels, flights, dhams, buses).*  
  >  
  > *Never pay travel agents or booking services an advance over WhatsApp using QR codes or UPI.*  
  >  
  > *Do bookings only from official & genuine websites.*  
  >  
  > *Scammers create fake listings to steal your money."*
- **Action Button:** `[I UNDERSTAND]`.

---

## 10. Temporal Fake Booking Payment Interceptor (Chat-Linked)

### Scam Profile
After a victim searches for hotel or flight bookings online, scammers route them to a private WhatsApp chat and share a UPI QR code demanding immediate payment to "lock the room."

### Diagnostic Logic & Rules
- **Component:** `MessageReaderAccessibilityService.kt`
- **Trigger Conditions:**
  1. Temporal Linkage: User engaged in travel/hotel booking browser searches within the last **30 minutes** (`currentTime - bookingIntentTimestamp <= 30 * 60 * 1000`).
  2. User opens WhatsApp or Telegram with an **unsaved contact**.
  3. Accessibility text scanner detects financial urgency keywords: `"advance"`, `"qr code"`, `"upi"`, `"pay here"`, `"booking deposit"`, `"scan to pay"`, `"token amount"`.

### User-Facing Alert Displayed
- **Card Builder:** `OverlayUtils.showFakeBookingWarningOverlay(context, chatTitle)`
- **UI Styling:** Amber Warning Background (`#FFF3E0`), Bright Amber Stroke (`#F57C00`, 4px), Title Color: `#E65100`.
- **Alert Title:** `⚠️ BOOKING SCAM WARNING`
- **Warning Message:**  
  > *"You are discussing a booking or service with '[chatTitle]' and they have requested payment.*  
  >  
  > *Scammers frequently create fake listings and websites to steal advance payments via QR codes.*  
  >  
  > *NEVER scan a QR code for an unverified booking or service."*
- **Action Button:** `[I UNDERSTAND]`.

---

## 11. Pre-Emptive Predatory Loan App Search

### Scam Profile
Victims seeking urgent credit search for "instant loans without CIBIL." Scammers serve shady APK links that request contacts and storage access, subsequently extorting victims with doctored photos.

### Diagnostic Logic & Rules
- **Component:** `MessageReaderAccessibilityService.kt`
- **Trigger Conditions:**
  1. Active package is a browser.
  2. Search input matches loan queries: `"instant loan"`, `"loan without cibil"`, `"urgent cash loan"`, `"fast loan approval"`, `"loan apk download"`, `"7 day loan"`.
  3. Throttled to once per 60 minutes.

### User-Facing Alert Displayed
- **Card Builder:** `OverlayUtils.showPreEmptiveLoanAlert(context)`
- **UI Styling:** Warning Orange Background (`#FFF3E0`), Amber Stroke (`#F57C00`, 4px), Title Color: `#E65100`.
- **Alert Title:** `💰 LOAN SAFETY ALERT`
- **Warning Message:**  
  > *"You are searching for loans online.*  
  >  
  > *⚠️ NEVER download unauthorized loan apps sent via WhatsApp or SMS.*  
  >  
  > *⚠️ NEVER pay an 'advance fee', 'processing fee', or 'GST' to get a loan approved.*  
  >  
  > *Scammers use these tricks to steal money and hijack your phone contacts."*
- **Action Button:** `[I UNDERSTAND]`.

---

## 12. Pre-Emptive Fake Customer Care / Helpline Trap

### Scam Profile
Scammers purchase Google Search ads or create fake blogs with fraudulent "toll-free helpline numbers" for banks, airlines, or parcel tracking (e.g. BlueDart, DTDC, SBI, Amazon refund).

### Diagnostic Logic & Rules
- **Component:** `MessageReaderAccessibilityService.kt`
- **Trigger Conditions:**
  1. Active package is a browser.
  2. Search query matches customer care terms: `"customer care number"`, `"helpline number"`, `"toll free number"`, `"refund complaint"`, `"order cancellation number"`, `"bank support number"`.
  3. Throttled to once per 60 minutes.

### User-Facing Alert Displayed
- **Card Builder:** `OverlayUtils.showPreEmptiveHelplineAlert(context)`
- **UI Styling:** Warning Orange Background (`#FFF3E0`), Amber Stroke (`#F57C00`, 4px), Title Color: `#E65100`.
- **Alert Title:** `📞 CUSTOMER CARE ALERT`
- **Warning Message:**  
  > *"You are searching for customer care or helplines.*  
  >  
  > *Be very careful of numbers found on Google search results or 3rd party websites.*  
  >  
  > *Scammers post fake customer care numbers online to steal your money by asking you to download screen sharing apps or share OTPs.*  
  >  
  > *Only use contact details found inside the official app."*
- **Action Button:** `[I UNDERSTAND]`.

---

## 13. Pre-Emptive Work-From-Home / Task Scam Search

### Scam Profile
Victims looking for freelance or side income search for "daily task jobs" or "YouTube like earn." Scammers lure them into Telegram task groups where victims are tricked into "prepaid merchant tasks."

### Diagnostic Logic & Rules
- **Component:** `MessageReaderAccessibilityService.kt`
- **Trigger Conditions:**
  1. Active package is a browser.
  2. Search query contains: `"online part time job"`, `"work from home daily payment"`, `"youtube like job"`, `"hotel review job"`, `"task earning app"`.
  3. Throttled to once per 60 minutes.

### User-Facing Alert Displayed
- **Card Builder:** `OverlayUtils.showPreEmptiveJobAlert(context)`
- **UI Styling:** Sky Blue Background (`#E3F2FD`), Blue Stroke (`#1976D2`, 4px), Title Color: `#1976D2`.
- **Alert Title:** `💼 JOB SAFETY ALERT`
- **Warning Message:**  
  > *"You are searching for online jobs or tasks.*  
  >  
  > *⚠️ NEVER pay a 'registration fee', 'security deposit', or any sort of fee for a job. If asked for registration money etc, then it's a scam."*
- **Action Button:** `[I UNDERSTAND]`.

---

## 14. Pre-Emptive VIP Trading & Crypto Search

### Scam Profile
Scammers advertise fake cryptocurrency exchanges, foreign exchange (Forex) arbitrage, and fraudulent equity trading tips channels that show manipulated portfolio dashboards.

### Diagnostic Logic & Rules
- **Component:** `MessageReaderAccessibilityService.kt`
- **Trigger Conditions:**
  1. Active package is a browser.
  2. Search query matches: `"vip trading group"`, `"guaranteed crypto returns"`, `"forex trading signals"`, `"share market tips telegram"`, `"crypto doubling"`.
  3. Throttled to once per 60 minutes.

### User-Facing Alert Displayed
- **Card Builder:** `OverlayUtils.showPreEmptiveTradingAlert(context)`
- **UI Styling:** Soft Mint Green Background (`#E8F5E9`), Forest Green Stroke (`#2E7D32`, 4px), Title Color: `#2E7D32`.
- **Alert Title:** `📈 TRADING SAFETY ALERT`
- **Warning Message:**  
  > *"You are searching for trading or cryptocurrency.*  
  >  
  > *⚠️ NEVER join 'VIP Trading Groups' on Telegram or WhatsApp for stock tips.*  
  >  
  > *⚠️ Beware of platforms promising guaranteed high returns or risk-free profits. If it sounds too good to be true, it's a scam designed to steal your investments."*
- **Action Button:** `[I UNDERSTAND]`.

---

## 15. In-Browser & System Notification APK Download Shield

### Scam Profile
Phishing websites trigger automatic "drive-by" APK downloads or deceive users with fake prompts claiming "Your phone has a virus! Download update.apk."

### Diagnostic Logic & Rules
- **Components:** `MessageReaderAccessibilityService.kt` & `ScamDetectionService.kt`
- **Execution Timing:** **Triggers ONLY when the user clicks to download, NOT passively when visiting the webpage.**
- **Trigger Pathways (3-Way Precise Interception):**
  - **Path 1 (User Click Interceptor — `TYPE_VIEW_CLICKED`):**
    - Intercepts user taps on links/buttons: `event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED`.
    - Node traversal inspects the clicked view:
      1. Direct `.apk` filename match (`Regex("""(?i)\b[\w\-\.\+]+\.apk\b""")`).
      2. Clickable action text combining `"download"` / `"install"` / `"get"` with `"apk"` (e.g. "Download APK", "Install APK").
      3. Confirmation button clicks: Tapping "Download anyway" / "Download" on browser confirmation bottom sheets.
      4. Parent anchor link inspection (`<a href="...apk">`).
  - **Path 2 (Browser Native Confirmation Dialog — Post-Click):**
    - Scans specifically for native browser security bottom sheets that appear strictly *after* clicking a download link:
      - Chrome / Edge / Samsung Internet: Requires strict prompt phrases (`"file might be harmful"` + `"download anyway"`, OR `"this type of file may harm"`) alongside an `.apk` filename.
      - **Immunity:** Normal web pages mentioning `.apk` or displaying un-clicked download buttons are never triggered on page load.
  - **Path 3 (Download Manager & Notification Gateway):**
    - Broadcast `DownloadManager.ACTION_DOWNLOAD_COMPLETE` for completed `.apk` downloads.
    - Notification stream gateway intercepting active download progress or completion notifications for `.apk` files.

### User-Facing Alert Displayed
- **Card Builder:** `ScamAnalyzer.getInstance().showScamWarningOverlay(alert, isApk = true)` -> `AlertCardBuilder.buildScamCard(...)`
- **UI Styling:** Urgent Rose Background (`#FFF0F0`), Crimson Alert Stroke (`#FF5252`, 6px), Elevation: `32f`.
- **Alert Title:** `🚨 DANGEROUS APK DETECTED 🚨`
- **Sender / Source Display:** `Source: [Browser Name / Download Manager]`
- **Warning Description:**  
  > *"An unverified APK is being downloaded or received. Fraudulent APKs often hide Remote Access Trojans (RATs) that steal OTPs, intercept SMS, and drain bank accounts."*
- **Critical Directive:**  
  > *"🛑 DO NOT install or open this APK! Delete the file immediately to keep your phone secure."*
- **Action Buttons:**
  - `[DISMISS]` (Solid Red button — explicit user dismissal required).
  - `[NOT A SCAM]` (Solid Green button — logs feedback and updates database).
- **Persistent Anti-Dismissal Shield:**
  - **Zero Accidental Dismissals:** Background dim touches are absorbed without dismissing the card. The card stays permanently on screen until the user deliberately taps **[DISMISS]** or **[NOT A SCAM]**.
  - **Permission Prompt Layering:** When the browser requests system storage or download permissions, the overlay is configured with `FLAG_NOT_FOCUSABLE` and `FLAG_LAYOUT_IN_SCREEN`, preventing focus dropping or premature dismissal.
  - **Touch Leakage Protection:** The full-screen overlay absorbs stray touches so that taps cannot accidentally grant browser download permissions or start drive-by downloads in the background while the warning is active.

---

## 16. User-Directed Sender Whitelist & Trust Shield

### Feature Profile
Victims and regular users frequently communicate with individuals whose phone numbers are not saved in Android Contacts (e.g., local merchants, new acquaintances, delivery coordinators, freelancers). When a false positive scam alert is triggered for an unsaved contact, users need the ability to permanently trust the sender so that the app completely stops scanning their incoming messages.

### Diagnostic Logic & Rules
- **Component:** `AlertCardBuilder.kt`, `ContactUtils.kt`, `MessageReaderAccessibilityService.kt`, `ScamDetectionService.kt`, `ScamAnalyzer.kt`
- **Trigger Conditions:**
  1. Any message scam alert card (`buildScamCard`) displays an interactive `[🛡️ TRUST SENDER (DON'T SCAN AGAIN)]` action button.
  2. Clicking this button immediately normalizes and writes the sender identity to `SharedPreferences` (`trusted_senders` set):
     - Normalized international format (e.g., `+919876543210`).
     - Pure numeric digits (e.g., `919876543210`).
     - National 10-digit format (e.g., `9876543210`).
     - Raw string display title (e.g., `+91 98765 43210` or alphanumeric name).
  3. **Universal Exemption Routing:**
     - **In-App Message Reader (`MessageReaderAccessibilityService`):** Checks `ContactUtils.isTrustedSender(context, currentChatTitle)`. If trusted, the chat message container is skipped entirely without reading a single message.
     - **Notification Scanner (`ScamDetectionService`):** Background notifications matching trusted senders are immediately skipped.
     - **Analysis Engine (`ScamAnalyzer`):** Drops message evaluation upfront for any trusted sender.
     - **Contact Lookup Bridge (`ContactUtils.isContactExists`):** Automatically returns `true` for any trusted sender, seamlessly whitelisting telephony and cross-app telemetry checks.
  4. **Management & Revocation:** Users can view active trusted senders count and clear the whitelist at any time under EdgeGuard Settings (`PermissionsScreen`).

### User-Facing Alert Displayed
- **Button Injected:** `[🛡️ TRUST SENDER (DON'T SCAN AGAIN)]` (Deep Trust Blue `#1976D2`, full-width card layout).
- **Confirmation Toast:**
  > *"Trusted '+91 98765 43210'. Messages from this number will no longer be scanned."*

---

## Matrix: Complete Rule-Based Behavioral Trigger Reference

| # | Threat Vector | Primary Source File | Trigger Conditions & Time Rules | Alert Title | Primary Call to Action |
|---|---------------|---------------------|----------------------------------|-------------|------------------------|
| **1** | Pre-Install APK Shield | `MessageReaderAccessibilityService.kt` | PackageInstaller active + Install button clicked + Live call check | `🛑 UNVERIFIED APP WARNING` / `🚨 SECURITY ALERT 🚨` | `CANCEL INSTALL` / `INSTALL ANYWAY` |
| **2** | Post-Install Watchdog | `AppInstallReceiver.kt` | `ACTION_PACKAGE_ADDED` from untrusted installer | `🚨 CRITICAL THREAT INSTALLED` | `I WILL UNINSTALL IT NOW` |
| **3** | RAT Screen Sharing | `AppInstallReceiver.kt` | Blacklisted RAT installed + Active/recent unknown call (<1h) | `🚨 SECURITY ALERT 🚨` / `⚠️ REMOTE ACCESS WARNING` | `I UNDERSTAND THE RISK` |
| **4** | Digital Arrest | `ScamDetectionService.kt` & `MessageReaderAccessibilityService.kt` | Unknown VoIP video call >15m + Banking app opened within 2h | `🚨 DIGITAL ARREST SCAM WARNING 🚨` | `I UNDERSTAND` |
| **5** | OTP Snatching | `AppPhoneStateReceiver.kt` & `ScamDetectionService.kt` | Sensitive OTP received + Unknown call rings within 7 mins | `⚠️ OTP SECURITY ALERT` | `I UNDERSTAND` |
| **6** | Sextortion Trap | `ScamDetectionService.kt` | Incoming unknown VoIP video call ringing | `⚠️ VIDEO CALL ALERT` | `DISMISS & HANG UP` |
| **7** | Ad-to-Group Funnel | `MessageReaderAccessibilityService.kt` | Browser to WhatsApp/Telegram <60s + Join Group UI | `⚠️ SCAM FUNNEL DETECTED` | `I UNDERSTAND` |
| **8** | Ad-to-DM Funnel | `MessageReaderAccessibilityService.kt` | Browser to WhatsApp/Telegram <60s + Unknown 1-on-1 chat | `⚠️ SCAM FUNNEL DETECTED` | `I UNDERSTAND` |
| **9** | Travel Booking Search | `MessageReaderAccessibilityService.kt` | Browser search for travel/hotels/dhams (1h throttle) | `✈️ TRAVEL SAFETY ALERT` | `I UNDERSTAND` |
| **10** | Fake Booking Chat | `MessageReaderAccessibilityService.kt` | Travel search in past 30m + Unsaved chat + Payment cue | `⚠️ BOOKING SCAM WARNING` | `I UNDERSTAND` |
| **11** | Predatory Loan Search | `MessageReaderAccessibilityService.kt` | Browser search for instant loans/7-day credit | `💰 LOAN SAFETY ALERT` | `I UNDERSTAND` |
| **12** | Fake Helpline Search | `MessageReaderAccessibilityService.kt` | Browser search for customer care / toll-free numbers | `📞 CUSTOMER CARE ALERT` | `I UNDERSTAND` |
| **13** | Task Scam Search | `MessageReaderAccessibilityService.kt` | Browser search for work-from-home daily payout | `💼 JOB SAFETY ALERT` | `I UNDERSTAND` |
| **14** | Trading Group Search | `MessageReaderAccessibilityService.kt` | Browser search for VIP stock tips / crypto returns | `📈 TRADING SAFETY ALERT` | `I UNDERSTAND` |
| **15** | APK Download Shield | `MessageReaderAccessibilityService.kt` & `ScamDetectionService.kt` | Browser UI `.apk` prompt OR `DownloadManager` complete | `🚨 DANGEROUS APK DETECTED 🚨` | `DISMISS` / `NOT A SCAM` |
| **16** | Trusted Sender Whitelist | `AlertCardBuilder.kt` & `ContactUtils.kt` | User taps "Trust Sender" on scam alert overlay | `🛡️ TRUST SENDER` | Whitelists contact permanently |

---
*Generated by Antigravity for EdgeGuard AI codebase.*

