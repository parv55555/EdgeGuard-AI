import os
import sys
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super(NumberedCanvas, self).__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            canvas.Canvas.showPage(self)
        canvas.Canvas.save(self)

    def draw_page_decorations(self, page_count):
        self.saveState()
        
        # Don't draw running header/footer on page 1 (cover)
        if self._pageNumber > 1:
            # Header
            self.setFont("Helvetica-Bold", 8)
            self.setFillColor(colors.HexColor("#0B132B"))
            self.drawString(54, 800, "EDGEGUARD AI | BEHAVIORAL THREAT DETECTION REPORT")
            self.setFont("Helvetica", 8)
            self.setFillColor(colors.HexColor("#64748B"))
            self.drawRightString(541, 800, "100% On-Device Zero-Cloud Architecture")
            
            self.setStrokeColor(colors.HexColor("#E2E8F0"))
            self.setLineWidth(0.75)
            self.line(54, 792, 541, 792)

            # Footer
            self.setStrokeColor(colors.HexColor("#E2E8F0"))
            self.setLineWidth(0.75)
            self.line(54, 45, 541, 45)

            self.setFont("Helvetica", 8)
            self.setFillColor(colors.HexColor("#64748B"))
            self.drawString(54, 32, "Confidential - EdgeGuard Security Engineering & Threat Intelligence")
            page_text = f"Page {self._pageNumber} of {page_count}"
            self.drawRightString(541, 32, page_text)
            
        self.restoreState()

def build_pdf(filename):
    doc = SimpleDocTemplate(
        filename,
        pagesize=A4,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()

    # Custom styles
    primary_color = colors.HexColor("#0B132B")
    accent_blue = colors.HexColor("#0284C7")
    danger_red = colors.HexColor("#DC2626")
    warning_orange = colors.HexColor("#D97706")
    text_dark = colors.HexColor("#1E293B")
    text_muted = colors.HexColor("#475569")
    bg_light = colors.HexColor("#F8FAFC")

    title_style = ParagraphStyle(
        'CoverTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=28,
        textColor=primary_color,
        spaceAfter=8
    )

    subtitle_style = ParagraphStyle(
        'CoverSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=16,
        textColor=accent_blue,
        spaceAfter=14
    )

    meta_style = ParagraphStyle(
        'CoverMeta',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=text_muted
    )

    h1_style = ParagraphStyle(
        'Heading1_Custom',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=15,
        leading=18,
        textColor=primary_color,
        spaceBefore=14,
        spaceAfter=6,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'Heading2_Custom',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=15,
        textColor=accent_blue,
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'Body_Custom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=text_dark,
        spaceAfter=6
    )

    bullet_style = ParagraphStyle(
        'Bullet_Custom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=text_dark,
        leftIndent=12,
        spaceAfter=3
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=11,
        textColor=colors.white
    )

    table_cell_style = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8,
        leading=11,
        textColor=text_dark
    )

    table_cell_bold = ParagraphStyle(
        'TableCellBold',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=11,
        textColor=primary_color
    )

    badge_danger = ParagraphStyle(
        'BadgeDanger',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.5,
        leading=9,
        textColor=danger_red
    )

    badge_warning = ParagraphStyle(
        'BadgeWarning',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.5,
        leading=9,
        textColor=warning_orange
    )

    badge_info = ParagraphStyle(
        'BadgeInfo',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.5,
        leading=9,
        textColor=accent_blue
    )

    story = []

    # ------------------ COVER / HEADER BANNER ------------------
    cover_data = [
        [
            Paragraph("<b>EDGEGUARD AI | ADVANCED THREAT INTELLIGENCE</b>", ParagraphStyle('TopTag', fontName='Helvetica-Bold', fontSize=8, textColor=accent_blue)),
        ],
        [
            Paragraph("Behavioral Triggers & User Behavior Analytics (UBA)", title_style),
        ],
        [
            Paragraph("A Comprehensive Technical Guide to Multi-Modal Behavioral Scanners, Temporal State Correlation, and Intercepted Frauds", subtitle_style),
        ],
        [
            HRFlowable(width="100%", thickness=1.5, color=accent_blue, spaceBefore=4, spaceAfter=8)
        ],
        [
            Paragraph("<b>Architecture:</b> 100% On-Device Neural + Behavioral Heuristic Engine<br/>"
                      "<b>Target Platform:</b> Android 8.0 - 15+ (Universal Architecture)<br/>"
                      "<b>Author:</b> EdgeGuard Security Research & Engineering<br/>"
                      "<b>Date of Report:</b> November 2026 | Version: 2.4", meta_style)
        ]
    ]

    cover_table = Table(cover_data, colWidths=[487])
    cover_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#F1F5F9")),
        ('PADDING', (0,0), (-1,-1), 14),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor("#CBD5E1")),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
    ]))
    story.append(cover_table)
    story.append(Spacer(1, 14))

    # ------------------ SECTION 1: WHAT ARE BEHAVIORAL TRIGGERS ------------------
    story.append(Paragraph("1. Executive Overview: What Are Behavioral Triggers?", h1_style))
    story.append(Paragraph(
        "Traditional mobile security applications and anti-spam filters rely predominantly on <b>static keyword matching</b> or <b>isolated natural language processing (NLP)</b>. "
        "These conventional tools inspect text messages or push notifications *only after* they arrive in an inbox. "
        "However, sophisticated cyber fraud operations (such as Digital Arrest, Screen-Sharing RAT takeovers, Social Media investment funnels, and Trojanized APKs) "
        "rarely leave an overt text trail inside traditional SMS channels. Instead, attackers exploit human psychological urgency across multiple communication protocols, "
        "initiating out-of-band VoIP calls, prompting sideloaded app installations, and coercing victims into launching banking portals.",
        body_style
    ))
    story.append(Paragraph(
        "<b>Behavioral Triggers</b> represent EdgeGuard's proactive security paradigm. Rather than merely parsing isolated text strings, "
        "EdgeGuard deploys a real-time, on-device <b>User Behavior Analytics (UBA) State Machine</b>. "
        "This engine continuously cross-references device state transitions, app switching sequences, telephony events, accessibility window context, "
        "and temporal intervals between sensitive actions. When a user's device state matches a statistically verified multi-stage fraud playbook, "
        "EdgeGuard instantly trips a high-priority warning overlay—interfering with the scam *before* financial loss occurs.",
        body_style
    ))

    # Core Principles Box
    principles_data = [
        [
            Paragraph("<b>Core Pillars of EdgeGuard's Behavioral Architecture:</b><br/>"
                      "• <b>Temporal Linkage:</b> Correlating events across time windows (e.g., search intent in browser followed by a private WhatsApp chat within 30 minutes).<br/>"
                      "• <b>Cross-App Contextual Awareness:</b> Tracking user journeys between disparate apps (e.g., ad click in Instagram jumping to a Telegram DM).<br/>"
                      "• <b>Dual-Call Telemetry State:</b> Distinguishing cellular vs. VoIP calls, audio vs. video streams, and caller identity against the local address book.<br/>"
                      "• <b>Zero-Cloud Data Sovereignty:</b> 100% of telemetry, regex scanning, and state transitions execute locally on-device with zero network egress.",
                      body_style)
        ]
    ]
    p_table = Table(principles_data, colWidths=[487])
    p_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#EFF6FF")),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor("#93C5FD")),
        ('PADDING', (0,0), (-1,-1), 10),
    ]))
    story.append(p_table)
    story.append(Spacer(1, 14))

    # ------------------ SECTION 2: HOW THE BEHAVIOR ENGINE WORKS ------------------
    story.append(Paragraph("2. Anatomy of the Behavioral Detection Engine", h1_style))
    story.append(Paragraph(
        "EdgeGuard AI uses four synchronized Android background subsystems to construct its real-time behavioral graph:",
        body_style
    ))

    subsystems = [
        ("Accessibility DOM Inspector (MessageReaderAccessibilityService)", 
         "Analyzes active window hierarchy in real-time, detecting browser download prompts, button clicks, search query intents, and package installer dialogs without keylogging or credential recording."),
        ("Notification Stream Gateway (ScamDetectionService)", 
         "Intercepts status bar notifications across cellular SMS, WhatsApp, Telegram, browsers, and the Android Download Provider to detect incoming VoIP calls, high-value OTPs, and drive-by APK downloads."),
        ("Telephony & System Broadcast Receivers (AppPhoneStateReceiver & AppInstallReceiver)", 
         "Captures cellular call state changes (IDLE, RINGING, OFFHOOK) and monitors newly installed packages to catch sideloaded APKs and remote access tools as they register on the OS."),
        ("Persistent Security State Cache (OtpManager & ChatContextDao)", 
         "Maintains thread-safe in-memory and Room DB timestamps for high-risk temporal states (last OTP time, last unknown call time, active video call duration, and search intent flags).")
    ]

    for title, desc in subsystems:
        story.append(Paragraph(f"• <b>{title}:</b> {desc}", bullet_style))

    story.append(Spacer(1, 14))

    # ------------------ SECTION 3: COMPLETE INVENTORY OF DETECTED SCAMS ------------------
    story.append(Paragraph("3. Complete Catalog of Scams Detected via Behavioral Triggers", h1_style))
    story.append(Paragraph(
        "Below is the complete inventory of cyber scams actively intercepted by EdgeGuard AI's behavioral heuristic state machine, "
        "categorized by their operational attack vector:",
        body_style
    ))

    # Master Table of all Scams
    scam_table_data = [
        [
            Paragraph("<b>Scam Category & Threat</b>", table_header_style),
            Paragraph("<b>Behavioral Trigger Condition</b>", table_header_style),
            Paragraph("<b>Sensor / Subsystem</b>", table_header_style),
            Paragraph("<b>Triggered Action</b>", table_header_style),
        ],
        # 1. Digital Arrest
        [
            Paragraph("<b>1. Digital Arrest Scam</b><br/>Fake CBI/Police video coercion extortion", table_cell_bold),
            Paragraph("VoIP video call with unknown party > 15 mins + unknown call within 24h + <b>User opens Banking/UPI app</b>", table_cell_style),
            Paragraph("Telephony Callback + UsageStatsManager + NotificationListener", table_cell_style),
            Paragraph("🚨 Emergency Digital Arrest Full-Screen Warning Card", badge_danger)
        ],
        # 2. RAT Screen Sharing
        [
            Paragraph("<b>2. RAT Screen-Sharing Takeover</b><br/>AnyDesk, TeamViewer, RustDesk fraud", table_cell_bold),
            Paragraph("Blacklisted remote support app installed while <b>on active call with unknown caller</b> (or <1h after call)", table_cell_style),
            Paragraph("AppInstallReceiver + TelephonyManager + ContactUtils", table_cell_style),
            Paragraph("🚨 Severe RAT Takeover Warning Card", badge_danger)
        ],
        # 3. OTP Snatching
        [
            Paragraph("<b>3. OTP Snatching / Social Eng.</b><br/>Bank manager / KYC impersonation", table_cell_bold),
            Paragraph("Incoming unknown call (Cellular or VoIP) arrives within <b>7 minutes</b> of receiving a bank transaction OTP", table_cell_style),
            Paragraph("AppPhoneStateReceiver + ScamDetectionService + OtpManager", table_cell_style),
            Paragraph("⚠️ OTP Snatching Warning Overlay", badge_warning)
        ],
        # 4. Sextortion
        [
            Paragraph("<b>4. Sextortion Video Call Trap</b><br/>Blackmail via unsolicited video call", table_cell_bold),
            Paragraph("Incoming WhatsApp / VoIP <b>video call</b> initiated from an unsaved phone number", table_cell_style),
            Paragraph("ScamDetectionService (Notification category & VoIP tags)", table_cell_style),
            Paragraph("⚠️ Sextortion Blackmail Warning Overlay", badge_warning)
        ],
        # 5. Sideloaded APK Watchdog
        [
            Paragraph("<b>5. Sideloaded Malware Watchdog</b><br/>Trojan APKs, e-challan, fake govt apps", table_cell_bold),
            Paragraph("Application installed from untrusted source (browser, WhatsApp, Telegram, or direct file prompt)", table_cell_style),
            Paragraph("AppInstallReceiver (PackageManager.getInstallSourceInfo)", table_cell_style),
            Paragraph("⚠️ Sideloaded App Inspection Overlay", badge_warning)
        ],
        # 6. Pre-Install Shield
        [
            Paragraph("<b>6. Pre-Install APK Gatekeeper</b><br/>User tricked into tapping Install APK", table_cell_bold),
            Paragraph("User navigates into Android PackageInstaller screen to install an unverified APK (escalated if on call)", table_cell_style),
            Paragraph("MessageReaderAccessibilityService (PackageInstaller window tree)", table_cell_style),
            Paragraph("🚨 Pre-Install Shield Modal Interception", badge_danger)
        ],
        # 7. Ad-to-DM Funnel
        [
            Paragraph("<b>7. Ad-to-DM Scam Funnel</b><br/>Meta trading ad -> private WhatsApp DM", table_cell_bold),
            Paragraph("User jumps from Chrome/Instagram/Facebook to WhatsApp/Telegram DM with unknown number in <60 seconds", table_cell_style),
            Paragraph("Accessibility Window Tracker (Temporal package jumping)", table_cell_style),
            Paragraph("⚠️ Scam Funnel DM Warning Card", badge_warning)
        ],
        # 8. Ad-to-Group Funnel
        [
            Paragraph("<b>8. Rigged Group Funnel</b><br/>Ad jumping to Telegram/WhatsApp group", table_cell_bold),
            Paragraph("User jumps from social media/browser and lands directly on 'Join Group' / 'Join Channel' screen in <60s", table_cell_style),
            Paragraph("Accessibility DOM Tracker (checkForJoinGroupUI)", table_cell_style),
            Paragraph("⚠️ Fake Group / Pump-and-Dump Warning", badge_warning)
        ],
        # 9. Fake Travel Booking
        [
            Paragraph("<b>9. Fake Hotel/Helicopter Scam</b><br/>Advance deposit scam via WhatsApp", table_cell_bold),
            Paragraph("Browser search for hotels/dham booking within 30m + User opens WhatsApp chat with unknown number asking payment", table_cell_style),
            Paragraph("Accessibility Search Intent + Temporal Chat Tagging", table_cell_style),
            Paragraph("⚠️ Fake Booking Scam Overlay", badge_warning)
        ],
        # 10. Fake Customer Care
        [
            Paragraph("<b>10. Fake Helpline Trap</b><br/>SEO poisoned customer care numbers", table_cell_bold),
            Paragraph("User types search query for bank, courier, or airline toll-free customer care numbers in web browser", table_cell_style),
            Paragraph("Accessibility Search Bar Scanner (checkForHelplineIntent)", table_cell_style),
            Paragraph("ℹ️ Pre-Emptive Customer Care Advisory", badge_info)
        ],
        # 11. Instant Loan Blackmail
        [
            Paragraph("<b>11. Instant Loan / Extortion Apps</b><br/>Predatory Chinese instant lending apps", table_cell_bold),
            Paragraph("User types search query for instant loan, no-cibil loan, 5-minute cash in web browser", table_cell_style),
            Paragraph("Accessibility Search Bar Scanner (checkForLoanIntent)", table_cell_style),
            Paragraph("ℹ️ Pre-Emptive Predatory Loan Advisory", badge_info)
        ],
        # 12. Fake Task / Part-Time Job
        [
            Paragraph("<b>12. Task Scam / Work-From-Home</b><br/>YouTube like-and-earn Telegram scam", table_cell_bold),
            Paragraph("User types search query for part-time job, telegram earning, daily task salary in web browser", table_cell_style),
            Paragraph("Accessibility Search Bar Scanner (checkForJobIntent)", table_cell_style),
            Paragraph("ℹ️ Pre-Emptive Task Scam Advisory", badge_info)
        ],
        # 13. Drive-by APK Download
        [
            Paragraph("<b>13. Drive-by APK Interceptor</b><br/>Drive-by web downloads & fake updates", table_cell_bold),
            Paragraph("Browser dialog prompted with '.apk' + download/harmful/install cues OR notification posted with '.apk'", table_cell_style),
            Paragraph("Accessibility Tree + NotificationListener + DownloadManager receiver", table_cell_style),
            Paragraph("🚨 Dangerous APK Download Interceptor", badge_danger)
        ]
    ]

    scam_table = Table(scam_table_data, colWidths=[115, 160, 112, 100])
    scam_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), primary_color),
        ('ALIGN', (0,0), (-1,-1), 'LEFT'),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E1")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#F8FAFC")]),
        ('PADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(scam_table)
    story.append(Spacer(1, 14))

    # ------------------ SECTION 4: DEEP DIVE ON FLAGSHIP BEHAVIORAL SCAMS ------------------
    story.append(Paragraph("4. Deep Dive: Key Behavioral Threat Architectures", h1_style))

    # Deep Dive 1: Digital Arrest
    da_box = [
        [Paragraph("<b>CASE STUDY 1: Digital Arrest & Law Enforcement Impersonation</b>", ParagraphStyle('H2_White', fontName='Helvetica-Bold', fontSize=10, textColor=colors.white))],
        [Paragraph(
            "<b>The Threat Vector:</b> Scammers impersonating the CBI, Narcotics Control Bureau (NCB), or state police contact victims over WhatsApp, Skype, or Google Meet video calls. "
            "They wear counterfeit police uniforms, present forged Supreme Court arrest warrants, and isolate the victim for hours under 'digital house arrest'. "
            "The victim is ordered to verify their assets by opening their banking app and RTGS-transferring funds into 'government RBI verification accounts'.<br/><br/>"
            "<b>The Behavioral Trigger State Machine:</b><br/>"
            "1. <i>VoIP Call Surveillance:</i> EdgeGuard monitors active VoIP session duration. If an incoming video call from an unsaved number exceeds <b>15 minutes</b>, the device enters `DIGITAL_ARREST_VULNERABLE` state.<br/>"
            "2. <i>Prior Call History:</i> Verifies whether an unknown phone call occurred within the preceding 24 hours.<br/>"
            "3. <i>Banking App Launch Interception:</i> As soon as the victim opens any registered banking or UPI application (e.g., SBI Yono, HDFC Mobile, PhonePe, Paytm), EdgeGuard's watchdog immediately halts access, "
            "throwing a full-screen, un-dismissible `DigitalArrestWarningCard` instructing the victim: 'Police/CBI NEVER conduct video arrest or ask for asset transfers! Hang up immediately!'",
            body_style
        )]
    ]
    t_da = Table(da_box, colWidths=[487])
    t_da.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#991B1B")),
        ('BACKGROUND', (0,1), (-1,1), colors.HexColor("#FEF2F2")),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor("#EF4444")),
        ('PADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t_da)
    story.append(Spacer(1, 10))

    # Deep Dive 2: RAT Watchdog
    rat_box = [
        [Paragraph("<b>CASE STUDY 2: Remote Access Trojan (RAT) / Screen-Sharing Takeover</b>", ParagraphStyle('H2_White', fontName='Helvetica-Bold', fontSize=10, textColor=colors.white))],
        [Paragraph(
            "<b>The Threat Vector:</b> Fraudsters call claiming to resolve an electricity bill issue, refund a faulty transaction, or update an airline ticket. "
            "They convince the victim to install AnyDesk, TeamViewer QuickSupport, RustDesk, or AirDroid from Google Play. Once installed, the attacker requests the 9-digit session code, "
            "steals screen visibility, watches the victim log in to their bank, and captures passwords and 2FA tokens.<br/><br/>"
            "<b>The Behavioral Trigger State Machine:</b><br/>"
            "1. <i>Package Registry:</i> EdgeGuard maintains an on-device registry of all commercial remote desktop packages.<br/>"
            "2. <i>Real-Time Call Telemetry:</i> When `ACTION_PACKAGE_ADDED` fires, EdgeGuard queries `TelephonyManager` and VoIP call state.<br/>"
            "3. <i>Condition Match:</i> If a remote tool is installed while the user is actively talking to an unsaved caller, OR within 1 hour after such a call ended, "
            "EdgeGuard triggers a high-severity `RatWarningCard` with audible vibration: 'DANGER: An unknown caller is attempting to view and control your screen! Do not share any 9-digit code!'",
            body_style
        )]
    ]
    t_rat = Table(rat_box, colWidths=[487])
    t_rat.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#C2410C")),
        ('BACKGROUND', (0,1), (-1,1), colors.HexColor("#FFF7ED")),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor("#F97316")),
        ('PADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t_rat)
    story.append(Spacer(1, 10))

    # Deep Dive 3: OTP Snatching
    otp_box = [
        [Paragraph("<b>CASE STUDY 3: OTP Snatching & Social Engineering Bank Fraud</b>", ParagraphStyle('H2_White', fontName='Helvetica-Bold', fontSize=10, textColor=colors.white))],
        [Paragraph(
            "<b>The Threat Vector:</b> Attackers trigger a password reset or debit transaction on a victim's account, causing the bank to dispatch a genuine OTP. "
            "Simultaneously, the attacker calls the victim claiming to be a bank fraud investigator or customer care agent verifying a 'suspicious charge' and asks them to read out the code.<br/><br/>"
            "<b>The Behavioral Trigger State Machine:</b><br/>"
            "1. <i>High-Value OTP Detection:</i> When an SMS arrives containing high-value bank/KYC cues (`bank`, `aadhar`, `kyc`, `sbi`, `hdfc`) and a numeric verification code, `OtpManager` stores the exact millisecond timestamp.<br/>"
            "2. <i>Call Correlation:</i> If an incoming cellular or WhatsApp phone call from an unsaved number rings within <b>7 minutes</b> of that OTP arrival, "
            "EdgeGuard overrides the screen with the `OtpWarningOverlay`: 'SENSITIVE BANK OTP ARRIVED RECENTLY! The incoming caller is attempting to steal your verification code. Do not disclose it under any circumstance!'",
            body_style
        )]
    ]
    t_otp = Table(otp_box, colWidths=[487])
    t_otp.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#1E3A8A")),
        ('BACKGROUND', (0,1), (-1,1), colors.HexColor("#EFF6FF")),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor("#3B82F6")),
        ('PADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t_otp)
    story.append(Spacer(1, 10))

    # Deep Dive 4: Social Media Ad-to-DM Funnels
    ad_box = [
        [Paragraph("<b>CASE STUDY 4: Social Media Ad-to-DM & Ad-to-Group Scam Funnels</b>", ParagraphStyle('H2_White', fontName='Helvetica-Bold', fontSize=10, textColor=colors.white))],
        [Paragraph(
            "<b>The Threat Vector:</b> Fraud rings run millions of targeted ads on Instagram and Facebook advertising 'Institutional Stock Recommendations', 'Zero-Risk Crypto Trading', or 'Daily Work-From-Home Task Earnings'. "
            "Clicking the ad redirects the user via an external intent jump straight into a private WhatsApp chat with an unsaved number or an invitation to join an unverified Telegram channel.<br/><br/>"
            "<b>The Behavioral Trigger State Machine:</b><br/>"
            "1. <i>App Transition Tracking:</i> EdgeGuard records timestamps when the user is active in Chrome, Instagram, or Facebook.<br/>"
            "2. <i>Cross-App Jump Gate:</i> If within <b>60 seconds</b> the user transitions into WhatsApp or Telegram, EdgeGuard evaluates the destination UI.<br/>"
            "3. <i>Condition Match:</i> If the destination is an unsaved phone number (DM jump) or a 'Join Group' dialog, the `ScamFunnelWarningCard` is immediately launched: 'You clicked an online advertisement that jumped directly into a private chat. 99% of ad-directed chats are investment or task scams!'",
            body_style
        )]
    ]
    t_ad = Table(ad_box, colWidths=[487])
    t_ad.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#4338CA")),
        ('BACKGROUND', (0,1), (-1,1), colors.HexColor("#EEF2FF")),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor("#6366F1")),
        ('PADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t_ad)
    story.append(Spacer(1, 14))

    # ------------------ SECTION 5: PRIVACY & ZERO-EGRESS GUARANTEE ------------------
    story.append(Paragraph("5. Privacy Architecture: Why Behavioral Triggers Safeguard User Privacy", h1_style))
    story.append(Paragraph(
        "A critical distinction between EdgeGuard AI and cloud-based threat detection systems is how behavioral signals are processed. "
        "Cloud-based solutions transmit user call logs, screen text, browsing history, and incoming notifications to remote servers, "
        "violating enterprise compliance frameworks and personal privacy standards.",
        body_style
    ))
    story.append(Paragraph(
        "EdgeGuard adheres to a strict <b>Zero-Cloud Egress</b> guarantee:<br/>"
        "• <b>Local Contact Address Book Lookups:</b> Phone numbers are verified solely against the device's internal `ContactsContract` database (`ContactUtils.isContactExists`). No contact hashes or phone numbers are ever sent over the network.<br/>"
        "• <b>Ephemeral In-Memory State:</b> Telemetry flags (such as `last_booking_warning_time` and `active_voip_call`) reside strictly in local `SharedPreferences` and SQLite Room tables on-device, automatically pruned after 6 days.<br/>"
        "• <b>Read-Only Event Consumption:</b> Accessibility events are inspected passively without gesture injection, automated keystroke recording, or screen recording.<br/>"
        "• <b>Independent Verification:</b> EdgeGuard has no external tracking SDKs, analytical beacon endpoints, or remote logging servers.",
        body_style
    ))
    story.append(Spacer(1, 14))

    # ------------------ CONCLUSION / SIGN-OFF ------------------
    conclusion_data = [
        [
            Paragraph("<b>Summary & Conclusion</b><br/>"
                      "Behavioral Triggers represent the future of mobile threat prevention. By anticipating the attacker's multi-step choreography—rather than waiting for a malicious text message—EdgeGuard AI provides comprehensive, zero-latency protection against the most devastating financial cyber crimes in existence today.",
                      body_style)
        ]
    ]
    c_table = Table(conclusion_data, colWidths=[487])
    c_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#F8FAFC")),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor("#0284C7")),
        ('PADDING', (0,0), (-1,-1), 10),
    ]))
    story.append(c_table)

    # Build document
    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Report generated successfully at: {filename}")

if __name__ == '__main__':
    output_path = sys.argv[1] if len(sys.argv) > 1 else "EdgeGuard_Behavioral_Triggers_and_Scams_Report.pdf"
    build_pdf(output_path)

