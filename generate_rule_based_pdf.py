# -*- coding: utf-8 -*-
import sys
import os
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
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
            super(NumberedCanvas, self).showPage()
        super(NumberedCanvas, self).save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#718096"))
        
        # Header (pages 2+)
        if self._pageNumber > 1:
            self.drawString(54, 750, "EdgeGuard AI — Rule-Based Behavioral Scam Detection & Alert Reference")
            self.setStrokeColor(colors.HexColor("#E2E8F0"))
            self.setLineWidth(0.5)
            self.line(54, 744, 558, 744)
        
        # Footer
        footer_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(558, 36, footer_text)
        self.drawString(54, 36, "CONFIDENTIAL & PROPRIETARY — EDGEGUARD AI ARCHITECTURE")
        self.setStrokeColor(colors.HexColor("#E2E8F0"))
        self.setLineWidth(0.5)
        self.line(54, 46, 558, 46)
        
        self.restoreState()

def build_pdf(filename):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()

    # Custom typography
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=22,
        leading=26,
        textColor=colors.HexColor("#1A202C")
    )
    subtitle_style = ParagraphStyle(
        'DocSub',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=11,
        leading=15,
        textColor=colors.HexColor("#4A5568")
    )
    h1_style = ParagraphStyle(
        'Heading1_Custom',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=18,
        textColor=colors.HexColor("#0D47A1"),
        spaceBefore=14,
        spaceAfter=6
    )
    h2_style = ParagraphStyle(
        'Heading2_Custom',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=15,
        textColor=colors.HexColor("#C62828"),
        spaceBefore=10,
        spaceAfter=4
    )
    body_style = ParagraphStyle(
        'Body_Custom',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#2D3748")
    )
    body_bold = ParagraphStyle(
        'Body_Bold',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#1A202C")
    )
    card_title_style = ParagraphStyle(
        'CardTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=10,
        leading=13,
        textColor=colors.HexColor("#B71C1C"),
        alignment=1 # Center
    )
    card_body_style = ParagraphStyle(
        'CardBody',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=12,
        textColor=colors.HexColor("#212121"),
        alignment=1 # Center
    )
    card_btn_style = ParagraphStyle(
        'CardBtn',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=10,
        textColor=colors.white,
        alignment=1
    )
    table_cell_style = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor("#1A202C")
    )
    table_hdr_style = ParagraphStyle(
        'TableHdr',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=11,
        textColor=colors.white
    )

    story = []

    # Title Banner
    story.append(Paragraph("EdgeGuard AI: Rule-Based Behavioral Detection", title_style))
    story.append(Paragraph("<b>Complete Threat Diagnosis & Alert Reference Manual</b>", subtitle_style))
    story.append(Spacer(1, 4))
    story.append(Paragraph("<b>Engine:</b> 100% On-Device Deterministic Rule Engine &nbsp;|&nbsp; <b>Author:</b> Parv Khandelwal &nbsp;|&nbsp; <b>Release:</b> Production v2.0", subtitle_style))
    story.append(Spacer(1, 10))
    story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor("#0D47A1"), spaceBefore=0, spaceAfter=12))

    # Executive Summary Paragraph
    exec_text = (
        "EdgeGuard AI uses a <b>deterministic, zero-cloud rule-based engine</b> to protect users from social engineering, "
        "digital extortion, unverified application installations, and financial theft. Rather than intercepting private chat text, "
        "EdgeGuard analyzes <b>telephony states (ringing, active call), VoIP category flags, cross-app temporal transitions, "
        "and accessibility window events</b>. Below is the complete diagnostic specification and the exact UI warning card "
        "rendered for all 15 behavioral fraud triggers."
    )
    story.append(Paragraph(exec_text, body_style))
    story.append(Spacer(1, 12))

    # Helper function to create an Alert Card representation in PDF
    def make_alert_box(title, message, btn_text, bg_hex, stroke_hex, title_hex=None, btn_hex=None):
        t_color = title_hex or stroke_hex
        b_color = btn_hex or stroke_hex
        
        title_p = Paragraph(f"<b>{title}</b>", ParagraphStyle('T', parent=card_title_style, textColor=colors.HexColor(t_color)))
        msg_p = Paragraph(message.replace("\n\n", "<br/><br/>"), card_body_style)
        btn_p = Paragraph(f"<b>[{btn_text}]</b>", ParagraphStyle('B', parent=card_btn_style, textColor=colors.HexColor(b_color)))
        
        box_data = [
            [title_p],
            [Spacer(1, 4)],
            [msg_p],
            [Spacer(1, 6)],
            [btn_p]
        ]
        
        box_table = Table(box_data, colWidths=[490])
        box_table.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), colors.HexColor(bg_hex)),
            ('BOX', (0,0), (-1,-1), 1.5, colors.HexColor(stroke_hex)),
            ('TOPPADDING', (0,0), (-1,-1), 6),
            ('BOTTOMPADDING', (0,0), (-1,-1), 6),
            ('LEFTPADDING', (0,0), (-1,-1), 12),
            ('RIGHTPADDING', (0,0), (-1,-1), 12),
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
        ]))
        return box_table

    # Scams definition list
    scams = [
        {
            "id": "1",
            "name": "Pre-Install Malicious APK Gatekeeper",
            "component": "MessageReaderAccessibilityService.kt",
            "profile": "Victims are tricked into tapping 'Install' on malicious Android packages (.apk) sent over messaging apps or downloaded from unverified websites.",
            "diag": "<b>1. Package Match:</b> Active foreground package matches <code>packageinstaller</code> (excluding system <code>android</code> and <code>myfiles</code>).<br/>"
                    "<b>2. UI State:</b> Accessibility window state change detects installation action nodes (clickable text containing 'install', 'update').<br/>"
                    "<b>3. Live Call Correlation:</b> Evaluates <code>ContactUtils.isUserOnCall()</code> to identify if an active call is manipulating the user in real time.<br/>"
                    "<b>4. Timing:</b> Throttled to 3,000ms debounce. Can be temporarily bypassed for 60s if user selects 'Install Anyway'.",
            "card_title": "🛑 UNVERIFIED APP WARNING  (or  🚨 SECURITY ALERT 🚨  if on live call)",
            "card_msg": "You are trying to install an unverified app.<br/><br/>BEWARE: If someone claiming to be a customer service agent, government officer, or bank/loan person asks you to download this over a call or video call, they might hack your phone & bank account. If they ask you to install it, they are just scammers.",
            "card_btn": "CANCEL INSTALL   |   INSTALL ANYWAY",
            "bg": "#FFF3E0", "stroke": "#E65100"
        },
        {
            "id": "2",
            "name": "Post-Install Sideloaded Malware Watchdog",
            "component": "AppInstallReceiver.kt",
            "profile": "Silent or tricked installation of Trojanized apps outside official app stores that attempt to hijack SMS and accessibility permissions.",
            "diag": "<b>1. System Broadcast:</b> Intercepts <code>Intent.ACTION_PACKAGE_ADDED</code>.<br/>"
                    "<b>2. Installer Audit:</b> Queries <code>PackageManager.getInstallSourceInfo()</code>.<br/>"
                    "<b>3. Safe Store Whitelist:</b> Verifies against Google Play, Samsung Galaxy Store, Xiaomi GetApps, Oppo Market, Vivo AppStore, Huawei AppGallery.<br/>"
                    "<b>4. Sideload Trigger:</b> Fires immediately if installer is untrusted (browser, messaging app, file manager, or null ADB).",
            "card_title": "🚨 CRITICAL THREAT INSTALLED",
            "card_msg": "A highly dangerous app was just installed on your phone from an untrusted source ([maliciousPackageName]).<br/><br/>This app could be secretly reading your SMS and stealing money from your bank accounts. UNINSTALL IT IMMEDIATELY!",
            "card_btn": "I WILL UNINSTALL IT NOW",
            "bg": "#FFF0F0", "stroke": "#B71C1C"
        },
        {
            "id": "3",
            "name": "Remote Access Tool (RAT) Screen-Sharing Takeover",
            "component": "AppInstallReceiver.kt",
            "profile": "Scammers instruct victims to install remote desktop tools (AnyDesk, TeamViewer, RustDesk) to view bank OTPs and take control of the phone.",
            "diag": "<b>1. Blacklist Package Identification:</b> AnyDesk, TeamViewer QuickSupport, TeamViewer Host, RustDesk, Apowersoft Mirror, Microsoft Remote Desktop, Chrome Remote Desktop, AirDroid, RealVNC.<br/>"
                    "<b>2. Telephony Correlation:</b> Active telecom call (<code>CALL_STATE_OFFHOOK</code>) or VoIP call active with an <b>unsaved contact</b>.<br/>"
                    "<b>3. Temporal Window:</b> Also triggers if an unknown call ended within the past <b>1 hour (3,600,000ms)</b>.",
            "card_title": "🚨 SECURITY ALERT 🚨  (or  ⚠️ REMOTE ACCESS WARNING)",
            "card_msg": "If the caller has asked you to download this app, it is better not to install it as it might hack your device & bank account.<br/><br/>Even if you are on a call with someone claiming to be a government officer, a bank person, or a loan giver, DO NOT install this app! If they ask you to install it, they are just scammers.",
            "card_btn": "I UNDERSTAND THE RISK",
            "bg": "#FFEBEE", "stroke": "#D50000"
        },
        {
            "id": "4",
            "name": "Digital Arrest Video Call Coercion",
            "component": "ScamDetectionService.kt & MessageReaderAccessibilityService.kt",
            "profile": "Impostors pose as Police/CBI on prolonged video calls, intimidating victims into opening bank or UPI apps to transfer funds for 'court verification'.",
            "diag": "<b>1. Continuous Video Call Duration:</b> Unknown VoIP video call (WhatsApp, Telegram, Zoom, Meet) sustained for <b>&gt; 15 minutes (900,000ms)</b>.<br/>"
                    "<b>2. High-Risk Flag:</b> Sets <code>digitalArrestRiskTime</code> in persistent preferences.<br/>"
                    "<b>3. Financial Transition:</b> User opens any recognized Indian Banking or UPI app (SBI, HDFC, ICICI, PhonePe, GPay, Paytm) <b>during the video call</b> or within <b>2 hours (7,200,000ms)</b> post-call.",
            "card_title": "🚨 DIGITAL ARREST SCAM WARNING 🚨",
            "card_msg": "You are on a long video call following an unknown call, and just opened a banking app.<br/><br/>Police/CBI will NEVER arrest you over a video call. This is a scam.<br/><br/>DO NOT TRANSFER MONEY.",
            "card_btn": "I UNDERSTAND",
            "bg": "#FFEBEE", "stroke": "#D50000"
        },
        {
            "id": "5",
            "name": "OTP Snatching & Call Interception",
            "component": "AppPhoneStateReceiver.kt & ScamDetectionService.kt",
            "profile": "Scammers trigger bank debits or password resets, then ring the victim immediately posing as fraud prevention to solicit the SMS verification code.",
            "diag": "<b>1. Sensitive OTP Detection:</b> High-value bank/KYC SMS code arrives (Bank, Aadhar, PAN, UIDAI, SBI, HDFC, ICICI, Axis, PNB).<br/>"
                    "<b>2. Temporal Rapid Call:</b> An unsaved caller dials the device over cellular (ringing/offhook) or WhatsApp within <b>7 minutes (420,000ms)</b>.<br/>"
                    "<b>3. Reverse Sequence:</b> An OTP arrives while user is on an active unknown call or within 1 hour post-call.",
            "card_title": "⚠️ OTP SECURITY ALERT",
            "card_msg": "You recently received an OTP.<br/><br/>Do NOT share it with anyone on this call or message. Bank officials will NEVER ask for your OTP.",
            "card_btn": "I UNDERSTAND",
            "bg": "#FFF8E1", "stroke": "#FF8F00"
        },
        {
            "id": "6",
            "name": "Sextortion Honeytrap Video Call",
            "component": "ScamDetectionService.kt",
            "profile": "Unsolicited incoming video calls from unknown numbers where attackers display compromising clips, record the victim's face, and initiate blackmail.",
            "diag": "<b>1. Notification Intercept:</b> Detects incoming VoIP call category with video flag (<code>isVideo == true</code>).<br/>"
                    "<b>2. Contact Verification:</b> Caller number or ID is confirmed <b>completely unsaved</b> in local contacts (<code>!ContactUtils.isContactExists()</code>).<br/>"
                    "<b>3. Immediate Interception:</b> Triggers before the call is answered to prevent camera access.",
            "card_title": "⚠️ VIDEO CALL ALERT",
            "card_msg": "Scammers use video calls to record your face for blackmail and extortion.<br/><br/>If you see a naked girl or a police officer on call, IMMEDIATELY cut the call & don't pay anything.<br/><br/>DO NOT SHOW YOUR FACE.",
            "card_btn": "DISMISS & HANG UP",
            "bg": "#FFEBEE", "stroke": "#D50000"
        },
        {
            "id": "7",
            "name": "Social Media Ad-to-Group Rigged Funnel",
            "component": "MessageReaderAccessibilityService.kt",
            "profile": "Deceptive Facebook/Instagram/browser ads direct users into rigged Telegram or WhatsApp groups promoting fake trading, betting, or task scams.",
            "diag": "<b>1. Cross-App Transition:</b> Foreground package jumps from browser or social feed to WhatsApp/Telegram in <b>&lt; 60 seconds (60,000ms)</b>.<br/>"
                    "<b>2. Node Content Inspection:</b> Detects group invite cues: 'join group', 'join channel', 'join chat', 'request to join', 'join now'.",
            "card_title": "⚠️ SCAM FUNNEL DETECTED",
            "card_msg": "Do not join groups for trading, forex, money tasks, betting. It is possibly a scam group designed to steal your funds.",
            "card_btn": "I UNDERSTAND",
            "bg": "#FFF0F0", "stroke": "#D32F2F"
        },
        {
            "id": "8",
            "name": "Social Media Ad-to-DM Private Funnel",
            "component": "MessageReaderAccessibilityService.kt",
            "profile": "Web ads route users straight to private 1-on-1 chats with scammers to bypass public group scrutiny and orchestrate advance-fee fraud.",
            "diag": "<b>1. Rapid Ad Transition:</b> Browser/feed jump into a messaging conversation within 60 seconds.<br/>"
                    "<b>2. Unverified Contact:</b> Chat title is not in contacts and <b>not a verified WhatsApp Business entity</b>.<br/>"
                    "<b>3. Structure:</b> Target entity is an unsaved raw numeric phone number (7+ digits).",
            "card_title": "⚠️ SCAM FUNNEL DETECTED",
            "card_msg": "Do not interact with this person for trading, forex, money tasks, or betting. It is possibly a scammer designed to steal your funds.",
            "card_btn": "I UNDERSTAND",
            "bg": "#FFF0F0", "stroke": "#D32F2F"
        },
        {
            "id": "9",
            "name": "Pre-Emptive Travel Booking Scam Search",
            "component": "MessageReaderAccessibilityService.kt",
            "profile": "Fraudulent listings for pilgrimage helicopter tickets (Kedarnath), luxury resorts, and flights lure victims with fake advance payment demands.",
            "diag": "<b>1. Browser Keyword Extraction:</b> Searches for 'hotel booking', 'resort booking', 'helicopter ticket', 'kedarnath booking', 'advance deposit'.<br/>"
                    "<b>2. Rate Limit:</b> Warns once per 60 minutes.<br/>"
                    "<b>3. Temporal Linkage Seed:</b> Stores <code>booking_intent_timestamp</code> to track chat follow-ups.",
            "card_title": "✈️ TRAVEL SAFETY ALERT",
            "card_msg": "You are searching for travel bookings (hotels, flights, dhams, buses).<br/><br/>Never pay travel agents or booking services an advance over WhatsApp using QR codes or UPI.<br/><br/>Do bookings only from official & genuine websites.<br/><br/>Scammers create fake listings to steal your money.",
            "card_btn": "I UNDERSTAND",
            "bg": "#E3F2FD", "stroke": "#1976D2", "title_hex": "#1565C0"
        },
        {
            "id": "10",
            "name": "Temporal Fake Booking Payment Interceptor",
            "component": "MessageReaderAccessibilityService.kt",
            "profile": "Following an online hotel or travel search, a scammer contacts the victim on WhatsApp demanding an immediate UPI QR code payment.",
            "diag": "<b>1. Temporal Link:</b> User conducted a travel search within the past <b>30 minutes (1,800,000ms)</b>.<br/>"
                    "<b>2. Chat Interaction:</b> User is chatting with an <b>unsaved contact</b>.<br/>"
                    "<b>3. Payment Cue:</b> Node inspection detects payment solicitations ('advance', 'qr code', 'upi', 'pay here', 'booking amount', 'deposit').",
            "card_title": "⚠️ BOOKING SCAM WARNING",
            "card_msg": "You are discussing a booking or service with '[chatTitle]' and they have requested payment.<br/><br/>Scammers frequently create fake listings and websites to steal advance payments via QR codes.<br/><br/>NEVER scan a QR code for an unverified booking or service.",
            "card_btn": "I UNDERSTAND",
            "bg": "#FFF3E0", "stroke": "#F57C00", "title_hex": "#E65100"
        },
        {
            "id": "11",
            "name": "Pre-Emptive Predatory Loan App Search",
            "component": "MessageReaderAccessibilityService.kt",
            "profile": "Victims seeking instant cash are lured into downloading predatory 7-day loan apps that harvest phone contacts and extort families.",
            "diag": "<b>1. Browser Search Terms:</b> 'instant loan', 'loan without cibil', 'urgent cash loan', 'fast loan approval', 'loan apk download'.<br/>"
                    "<b>2. Rate Limit:</b> Warns at most once per 60 minutes.",
            "card_title": "💰 LOAN SAFETY ALERT",
            "card_msg": "You are searching for loans online.<br/><br/>⚠️ NEVER download unauthorized loan apps sent via WhatsApp or SMS.<br/><br/>⚠️ NEVER pay an 'advance fee', 'processing fee', or 'GST' to get a loan approved.<br/><br/>Scammers use these tricks to steal money and hijack your phone contacts.",
            "card_btn": "I UNDERSTAND",
            "bg": "#FFF3E0", "stroke": "#F57C00", "title_hex": "#E65100"
        },
        {
            "id": "12",
            "name": "Pre-Emptive Fake Customer Care / Helpline Trap",
            "component": "MessageReaderAccessibilityService.kt",
            "profile": "Scammers post fake customer care numbers online for banks, couriers, and airlines to trick callers into screen sharing or OTP disclosure.",
            "diag": "<b>1. Browser Search Terms:</b> 'customer care number', 'helpline number', 'toll free number', 'refund complaint', 'bank customer service'.<br/>"
                    "<b>2. Rate Limit:</b> Warns at most once per 60 minutes.",
            "card_title": "📞 CUSTOMER CARE ALERT",
            "card_msg": "You are searching for customer care or helplines.<br/><br/>Be very careful of numbers found on Google search results or 3rd party websites.<br/><br/>Scammers post fake customer care numbers online to steal your money by asking you to download screen sharing apps or share OTPs.<br/><br/>Only use contact details found inside the official app.",
            "card_btn": "I UNDERSTAND",
            "bg": "#FFF3E0", "stroke": "#F57C00", "title_hex": "#E65100"
        },
        {
            "id": "13",
            "name": "Pre-Emptive Work-From-Home / Task Scam Search",
            "component": "MessageReaderAccessibilityService.kt",
            "profile": "Fraudulent job offers promising daily income for liking YouTube videos or rating hotels, requiring victims to deposit funds for 'merchant tasks'.",
            "diag": "<b>1. Browser Search Terms:</b> 'online part time job', 'work from home daily payment', 'youtube like job', 'review rating job', 'task earning'.<br/>"
                    "<b>2. Rate Limit:</b> Warns at most once per 60 minutes.",
            "card_title": "💼 JOB SAFETY ALERT",
            "card_msg": "You are searching for online jobs or tasks.<br/><br/>⚠️ NEVER pay a 'registration fee', 'security deposit', or any sort of fee for a job. If asked for registration money etc, then it's a scam.",
            "card_btn": "I UNDERSTAND",
            "bg": "#E3F2FD", "stroke": "#1976D2", "title_hex": "#1976D2"
        },
        {
            "id": "14",
            "name": "Pre-Emptive VIP Trading & Crypto Search",
            "component": "MessageReaderAccessibilityService.kt",
            "profile": "Victims are lured into unregulated crypto schemes and fake stock tips channels with falsified screenshots of high profits.",
            "diag": "<b>1. Browser Search Terms:</b> 'vip trading group', 'guaranteed crypto returns', 'forex trading signals', 'share market tips telegram'.<br/>"
                    "<b>2. Rate Limit:</b> Warns at most once per 60 minutes.",
            "card_title": "📈 TRADING SAFETY ALERT",
            "card_msg": "You are searching for trading or cryptocurrency.<br/><br/>⚠️ NEVER join 'VIP Trading Groups' on Telegram or WhatsApp for stock tips.<br/><br/>⚠️ Beware of platforms promising guaranteed high returns or risk-free profits. If it sounds too good to be true, it's a scam designed to steal your investments.",
            "card_btn": "I UNDERSTAND",
            "bg": "#E8F5E9", "stroke": "#2E7D32", "title_hex": "#2E7D32"
        },
        {
            "id": "15",
            "name": "In-Browser & Notification APK Download Shield",
            "component": "MessageReaderAccessibilityService.kt & ScamDetectionService.kt",
            "profile": "Phishing websites, spoofed links, and drive-by downloads attempting to push Trojanized APKs to the device.",
            "diag": "<b>1. User Click Interceptor (TYPE_VIEW_CLICKED):</b> Triggers specifically when user taps an APK link, 'Download APK' button, or confirms download.<br/>"
                    "<b>2. Post-Click Browser Dialog:</b> Strictly verifies browser security prompts ('file might be harmful' + 'download anyway') for genuine dialogs; immune on page load.<br/>"
                    "<b>3. DownloadManager & Notification:</b> Intercepts active .apk downloads and completions.",
            "card_title": "🚨 DANGEROUS APK DETECTED 🚨",
            "card_msg": "Source: [Browser Name / Download Manager]<br/><br/>An unverified APK is being downloaded or received. Fraudulent APKs often hide Remote Access Trojans (RATs) that steal OTPs, intercept SMS, and drain bank accounts.<br/><br/>🛑 DO NOT install or open this APK! Delete the file immediately to keep your phone secure.",
            "card_btn": "DISMISS   |   NOT A SCAM",
            "bg": "#FFF0F0", "stroke": "#FF5252"
        }
    ]

    for item in scams:
        scam_elements = []
        scam_elements.append(Paragraph(f"<b>{item['id']}. {item['name']}</b>", h1_style))
        scam_elements.append(Paragraph(f"<b>Source Component:</b> <code>{item['component']}</code>", subtitle_style))
        scam_elements.append(Spacer(1, 4))
        
        scam_elements.append(Paragraph("<b>Threat Modus Operandi:</b>", body_bold))
        scam_elements.append(Paragraph(item['profile'], body_style))
        scam_elements.append(Spacer(1, 4))
        
        scam_elements.append(Paragraph("<b>Diagnostic Rules & Behavioral Telemetry:</b>", body_bold))
        scam_elements.append(Paragraph(item['diag'], body_style))
        scam_elements.append(Spacer(1, 6))
        
        scam_elements.append(Paragraph("<b>Exact User-Facing Overlay Card:</b>", body_bold))
        box = make_alert_box(
            title=item['card_title'],
            message=item['card_msg'],
            btn_text=item['card_btn'],
            bg_hex=item['bg'],
            stroke_hex=item['stroke'],
            title_hex=item.get('title_hex'),
            btn_hex=item.get('btn_hex')
        )
        scam_elements.append(box)
        scam_elements.append(Spacer(1, 14))
        
        story.append(KeepTogether(scam_elements))

    # Page Break for the Summary Table
    story.append(PageBreak())
    story.append(Paragraph("<b>Comprehensive Behavioral Matrix Summary</b>", h1_style))
    story.append(Spacer(1, 6))

    # Table of all 15 scams
    table_data = [
        [
            Paragraph("<b>#</b>", table_hdr_style),
            Paragraph("<b>Scam Threat</b>", table_hdr_style),
            Paragraph("<b>Primary Trigger Condition</b>", table_hdr_style),
            Paragraph("<b>Key Temporal Bounds</b>", table_hdr_style),
            Paragraph("<b>Alert Title</b>", table_hdr_style)
        ]
    ]

    summary_rows = [
        ("1", "Pre-Install APK Shield", "PackageInstaller active + install button tap", "Real-time + 3s throttle", "🛑 UNVERIFIED APP WARNING"),
        ("2", "Post-Install Watchdog", "Untrusted app installed outside Play Store", "Immediate post-install", "🚨 CRITICAL THREAT INSTALLED"),
        ("3", "RAT Screen Sharing", "Blacklisted remote app + active/recent call", "Active call or < 1 hr", "🚨 SECURITY ALERT 🚨"),
        ("4", "Digital Arrest", "Long video call + Banking app opened", "> 15 min call, < 2 hr bank", "🚨 DIGITAL ARREST SCAM WARNING"),
        ("5", "OTP Snatching", "Sensitive OTP + rapid unknown call", "Call within 7 min of OTP", "⚠️ OTP SECURITY ALERT"),
        ("6", "Sextortion Trap", "Incoming unknown video call", "Ringing phase (< 5s)", "⚠️ VIDEO CALL ALERT"),
        ("7", "Ad-to-Group Funnel", "Browser to messaging + Join Group UI", "< 60s transition window", "⚠️ SCAM FUNNEL DETECTED"),
        ("8", "Ad-to-DM Funnel", "Browser to messaging + Unknown 1-on-1", "< 60s transition window", "⚠️ SCAM FUNNEL DETECTED"),
        ("9", "Travel Booking Search", "Browser query for travel/hotels/dhams", "1 hour rate limit", "✈️ TRAVEL SAFETY ALERT"),
        ("10", "Fake Booking Payment", "Travel search + Unsaved chat payment", "< 30 min from search", "⚠️ BOOKING SCAM WARNING"),
        ("11", "Predatory Loan Search", "Browser query for instant loans", "1 hour rate limit", "💰 LOAN SAFETY ALERT"),
        ("12", "Fake Helpline Search", "Browser query for customer care/helpline", "1 hour rate limit", "📞 CUSTOMER CARE ALERT"),
        ("13", "Task Scam Search", "Browser query for work-from-home jobs", "1 hour rate limit", "💼 JOB SAFETY ALERT"),
        ("14", "Trading Group Search", "Browser query for VIP tips/crypto", "1 hour rate limit", "📈 TRADING SAFETY ALERT"),
        ("15", "APK Download Shield", "Browser UI .apk prompt or DownloadManager", "Real-time download", "🚨 DANGEROUS APK DETECTED 🚨")
    ]

    for row in summary_rows:
        table_data.append([
            Paragraph(row[0], table_cell_style),
            Paragraph(f"<b>{row[1]}</b>", table_cell_style),
            Paragraph(row[2], table_cell_style),
            Paragraph(row[3], table_cell_style),
            Paragraph(row[4], table_cell_style),
        ])

    summary_table = Table(table_data, colWidths=[20, 115, 155, 95, 115])
    summary_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#0D47A1")),
        ('ALIGN', (0,0), (-1,-1), 'LEFT'),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E0")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.HexColor("#FFFFFF"), colors.HexColor("#F7FAFC")]),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ('LEFTPADDING', (0,0), (-1,-1), 5),
        ('RIGHTPADDING', (0,0), (-1,-1), 5),
    ]))
    story.append(summary_table)

    # Build document
    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Generated PDF successfully at: {filename}")

if __name__ == "__main__":
    out_path = r"C:\Users\Parv Khandelwal\Downloads\EdgeGuard_Rule_Based_Behavioral_Detection_Reference.pdf"
    if len(sys.argv) > 1:
        out_path = sys.argv[1]
    build_pdf(out_path)

