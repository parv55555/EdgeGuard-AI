# EdgeGuard AI: Test Dataset & Sample Threat Cases

This directory contains standardized scam samples, social engineering vectors, and legitimate control messages used to test and benchmark the **EdgeGuard AI** on-device scam detection pipeline.

---

## 📁 File Structure

| File | Description |
| :--- | :--- |
| [`scam_test_cases.json`](./scam_test_cases.json) | Full structured test suite in JSON with metadata, indicators, and threat levels |
| [`scam_test_cases.csv`](./scam_test_cases.csv) | Tabular CSV version for automated evaluation, benchmarking, and bulk parsing |

---

## 🎯 Threat Taxonomy & Test Categories

The sample suite covers the top social engineering vectors observed in modern cyber fraud campaigns:

### 1. Electricity & Utility Bill Disconnection (`ELECTRICITY_DISCONNECTION`)
* **Vector:** Urgency regarding imminent power cut (often specifying "tonight at 9:30 PM") and providing a personal phone number rather than an official utility portal.
* **Sample:** `TC-001`
* **Trigger Tokens:** `electricity`, `bijli`, `power`, `disconnect`, `unpaid`, `9:30`, `tonight`

### 2. Banking, PAN & KYC Suspension (`BANK_KYC_SUSPENSION`)
* **Vector:** False claim that bank accounts, cards, or services are frozen/blocked due to missing KYC or expired PAN cards, with malicious phishing links.
* **Sample:** `TC-002`
* **Trigger Tokens:** `kyc`, `pan card`, `pancard`, `aadhaar`, `account`, `debit card`, `blocked`, `suspend`, `update immediately`

### 3. Fake Lottery & Prize Money (`FAKE_LOTTERY_KBC`)
* **Vector:** Claims of winning millions in KBC, lottery, or customer lucky draws demanding an advance processing fee or WhatsApp communication.
* **Sample:** `TC-003`
* **Trigger Tokens:** `kbc`, `lottery`, `congratulations`, `prize`, `winner`, `lakh`, `crore`, `won`, `claim`

### 4. Part-Time Work from Home / Task Fraud (`WORK_FROM_HOME_TASK`)
* **Vector:** Unrealistic daily income promises (Rs 3,000–8,000/day) for liking YouTube videos or completing small prepaid Telegram tasks.
* **Sample:** `TC-004`
* **Trigger Tokens:** `part time`, `daily income`, `youtube`, `like and subscribe`, `telegram`, `per day`, `earn`

### 5. Malicious APK & Traffic E-Challan (`MALICIOUS_APK_ECHALLAN`)
* **Vector:** Traffic police or government impersonation attaching or linking directly to a disguised `.apk` trojan (e.g. `echallan.apk`).
* **Sample:** `TC-005`
* **Trigger Tokens:** `.apk`, `challan`, `echallan`, `traffic`, `court`, `fine`

### 6. Digital Arrest & Law Enforcement Extortion (`DIGITAL_ARREST_LAW_ENFORCEMENT`)
* **Vector:** Severe psychological intimidation by impersonating CBI, Cyber Crime, or Narcotics Bureau, threatening digital arrest or asset freeze.
* **Sample:** `TC-006`
* **Trigger Tokens:** `cbi`, `narcotics`, `cyber crime`, `police`, `digital arrest`, `warrant`, `arrest`, `illegal`, `court`

### 7. India Post / Courier Package Delivery Failure (`POSTAL_DELIVERY_FAILURE`)
* **Vector:** Smishing message claiming package delivery failed due to address error, forcing users to click a credential harvesting portal.
* **Sample:** `TC-007`
* **Trigger Tokens:** `india post`, `courier`, `parcel`, `delivery failed`, `address update`, `reschedule`

### 8. Remote Access & OTP Harvesting (`REMOTE_ACCESS_OTP_HARVESTING`)
* **Vector:** Tricking victims into downloading screen-sharing software (AnyDesk, TeamViewer, RustDesk) or relaying OTP verification codes.
* **Sample:** `TC-008`
* **Trigger Tokens:** `anydesk`, `teamviewer`, `quicksupport`, `rustdesk`, `otp`, `share`, `forward`

### 9. Pre-Approved Loan & Fake Refund (`PRE_APPROVED_LOAN_REFUND`)
* **Vector:** Unsolicited loan or tax cashback approval requiring opening an unsecured link.
* **Sample:** `TC-009`
* **Trigger Tokens:** `loan`, `pre-approved`, `credited`, `refund`, `cashback`, `bit.ly`

### 10. SIM Card Biometric Deactivation (`SIM_CARD_DEACTIVATION`)
* **Vector:** Impersonating telecom operators claiming SIM deactivation within hours unless verified.
* **Sample:** `TC-010`
* **Trigger Tokens:** `sim card`, `sim block`, `deactivate`, `biometric`, `expire`

---

## 🛡️ False Positive Control Samples

To ensure the model and rule engine do not trigger disruptive false alarms during day-to-day smartphone use, the dataset includes control test cases:

* `TC-CONTROL-001`: Legitimate bank debit SMS (e.g. UPI transaction statements).
* `TC-CONTROL-002`: Standard two-factor authentication (OTP) receipt from Google.
* `TC-CONTROL-003`: Casual, personal SMS or WhatsApp message from a family member.
* `TC-CONTROL-004`: Legitimate work calendar / meeting sync invitation.

---

## 🧪 Automated Testing

Automated test cases verifying these samples are integrated into the Android test suite:
```bash
./gradlew test
```
See [`app/src/test/java/com/parv/safetexts/ScamDetectionUnitTest.kt`](../app/src/test/java/com/parv/safetexts/ScamDetectionUnitTest.kt) for the test implementation.
