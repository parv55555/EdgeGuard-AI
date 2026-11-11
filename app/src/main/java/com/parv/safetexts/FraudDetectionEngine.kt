// programmed by parv
package com.parv.safetexts

import java.net.IDN
import java.util.Locale

data class ScanResult(
    val score: Int,
    val reasons: List<String>,
    val fraudCategory: String = "GENERIC" // "GOVERNMENT", "BANK", "ECOMMERCE", "GENERIC"
)

object FraudDetectionEngine {

    // ========================================================================
    // 1. PROTECTED OFFICIAL BRANDS & MASTER INDIAN BANK REGISTRY
    // ========================================================================
    val PROTECTED_BRANDS = listOf(
        // E-Commerce, Tech, Quick-Commerce, Travel & Delivery Giants
        "amazon.in" to "amazon",
        "amazon.com" to "amazon",
        "flipkart.com" to "flipkart",
        "ekartlogistics.com" to "ekart",
        "myntra.com" to "myntra",
        "meesho.com" to "meesho",
        "ajio.com" to "ajio",
        "nykaa.com" to "nykaa",
        "tatacliq.com" to "tatacliq",
        "snapdeal.com" to "snapdeal",
        "croma.com" to "croma",
        "reliancedigital.in" to "reliancedigital",
        "jiomart.com" to "jiomart",
        "bigbasket.com" to "bigbasket",
        "zeptonow.com" to "zepto",
        "blinkit.com" to "blinkit",
        "bookmyshow.com" to "bookmyshow",
        "makemytrip.com" to "makemytrip",
        "goibibo.com" to "goibibo",
        "google.com" to "google",
        "google.co.in" to "google",
        "apple.com" to "apple",
        "microsoft.com" to "microsoft",
        "swiggy.com" to "swiggy",
        "zomato.com" to "zomato",
        "jio.com" to "jio",
        "airtel.in" to "airtel",
        "phonepe.com" to "phonepe",
        "paytm.com" to "paytm",
        "paypal.com" to "paypal",
        "netflix.com" to "netflix",
        "whatsapp.com" to "whatsapp",
        "instagram.com" to "instagram",
        "facebook.com" to "facebook",
        "bluedart.com" to "bluedart",
        "delhivery.com" to "delhivery",
        "dtdc.in" to "dtdc",
        "fedex.com" to "fedex",
        "bajajfinserv.in" to "bajajfinserv",

        // Statutory & Sovereign Exceptions
        "irctc.co.in" to "irctc",
        "rbi.org.in" to "rbi",
        "licindia.in" to "lic",
        "epfindia.gov.in" to "epfo",
        "indiapost.gov.in" to "indiapost",

        // All 12 Public Sector Banks in India
        "sbi.co.in" to "sbi",
        "onlinesbi.sbi" to "sbi",
        "sbi.bank" to "sbi",
        "pnbindia.in" to "pnb",
        "netpnb.com" to "pnb",
        "bankofbaroda.in" to "bankofbaroda",
        "bobworld.com" to "bobworld",
        "canarabank.com" to "canarabank",
        "unionbankofindia.co.in" to "unionbank",
        "bankofindia.co.in" to "bankofindia",
        "indianbank.in" to "indianbank",
        "centralbankofindia.co.in" to "centralbank",
        "iob.in" to "iob",
        "ucobank.com" to "ucobank",
        "bankofmaharashtra.in" to "bankofmaharashtra",
        "punjabandsindbank.co.in" to "punjabandsind",

        // Major Private Sector Banks in India
        "hdfcbank.com" to "hdfcbank",
        "icicibank.com" to "icicibank",
        "axisbank.com" to "axisbank",
        "kotak.com" to "kotak",
        "indusind.com" to "indusind",
        "yesbank.in" to "yesbank",
        "idfcfirstbank.com" to "idfcfirst",
        "federalbank.co.in" to "federalbank",
        "southindianbank.com" to "southindianbank",
        "rblbank.com" to "rblbank",
        "bandhanbank.com" to "bandhanbank",
        "idbibank.in" to "idbibank",
        "jkbank.com" to "jkbank",
        "kvb.co.in" to "kvb",
        "cityunionbank.com" to "cityunionbank",
        "sc.com" to "standardchartered",
        "citibank.co.in" to "citibank",
        "hsbc.co.in" to "hsbc",

        // Payments Banks
        "paytmbank.com" to "paytmbank",
        "ippbonline.com" to "ippb",
        "finobank.com" to "finobank",
        "jiopaymentsbank.com" to "jiopayments",

        // Small Finance Banks
        "aubank.in" to "aubank",
        "equitasbank.com" to "equitas",
        "ujjivansfb.in" to "ujjivan",
        "capitalbank.co.in" to "capitalbank",
        "janabank.com" to "janabank"
    )

    // Official sibling/shortlink/subsidiary domains belonging to verified protected brands
    private val OFFICIAL_COMPANION_DOMAINS = setOf(
        "amazon.co.uk", "amazon.de", "amazon.ae", "amazon.ca", "amazon.co.jp", "amazon.fr",
        "amazon.es", "amazon.it", "amazon.com.au", "amazon.sg", "amazon.nl", "amazon.se",
        "amazon.pl", "amazon.com.tr", "amazon.com.mx", "amazon.com.br", "amazon.sa", "amazon.eg",
        "amazonaws.com", "amzn.to", "amzn.in", "primevideo.com",
        "fkrt.it", "fkrt.cc", "shopsy.in",
        "gmail.com", "googlemail.com", "googleusercontent.com", "googleapis.com", "gstatic.com",
        "android.com", "g.co", "goo.gl", "youtu.be",
        "icloud.com", "mzstatic.com", "apple.co",
        "microsoftonline.com", "live.com", "outlook.com", "office.com", "office365.com",
        "sharepoint.com", "azure.com", "github.com", "msn.com", "xbox.com", "visualstudio.com", "bing.com",
        "wa.me", "whatsapp.net", "fb.com", "fb.me", "threads.net", "meta.com",
        "paypal.me",
        "sbi.co.in", "sbicard.com", "sbilife.co.in", "hdfcsec.com", "hdfclife.com",
        "icicidirect.com", "kotaksecurities.com", "axisdirect.in"
    )

    // Common generic words or infra labels that should never match fuzzy Levenshtein typosquatting
    private val GENERIC_INFRA_WORDS = setOf(
        "app", "apps", "pay", "art", "cart", "shop", "mail", "web", "com", "org", "net",
        "gov", "edu", "docs", "help", "apply", "maple", "ample", "blog", "news", "club",
        "live", "card", "bank", "game", "play", "book", "show", "trip", "make", "fast",
        "best", "plus", "line", "star"
    )

    // Highly distinctive trademarked brand strings safe for embedded substring matching in any host label
    private val DISTINCT_SUBSTRING_BRANDS = setOf(
        "flipkart", "myntra", "meesho", "phonepe", "paytm", "paypal", "swiggy", "zomato",
        "nykaa", "tatacliq", "snapdeal", "jiomart", "bigbasket", "blinkit", "bookmyshow",
        "makemytrip", "goibibo", "whatsapp", "instagram", "facebook", "netflix", "irctc",
        "indiapost", "bluedart", "delhivery", "bajajfinserv", "hdfcbank", "icicibank",
        "axisbank", "canarabank", "bankofbaroda", "bobworld", "yesbank", "indusind",
        "idbibank", "bandhanbank", "federalbank", "kotak", "airtel", "microsoft"
    )

    // Commercial & phishing affixes used to spot compound spoofing (e.g. "amazonsale", "applesupport", "sbikyc")
    private val BRAND_LURE_AFFIXES = listOf(
        "sale", "sales", "offer", "offers", "deal", "deals", "shop", "shopping", "store",
        "mart", "mall", "kart", "cart", "login", "signin", "verify", "verification",
        "kyc", "update", "support", "care", "help", "helpdesk", "refund", "reward",
        "rewards", "claim", "win", "lucky", "promo", "bigbillion", "festival", "official",
        "india", "pay", "card", "app", "apk", "online", "portal", "web", "account",
        "secure", "my", "get", "free", "gift", "bank", "net", "direct", "club", "vip"
    )

    // Bank keywords mapped to their authorized official domains
    private val BANK_NAME_TO_AUTHORIZED_DOMAINS = mapOf(
        "sbi" to listOf("sbi.co.in", "onlinesbi.sbi", "sbi.bank"),
        "yono" to listOf("sbi.co.in", "onlinesbi.sbi", "sbi.bank"),
        "state bank of india" to listOf("sbi.co.in", "onlinesbi.sbi", "sbi.bank"),
        "hdfc" to listOf("hdfcbank.com"),
        "icici" to listOf("icicibank.com"),
        "axis bank" to listOf("axisbank.com"),
        "kotak" to listOf("kotak.com"),
        "punjab national bank" to listOf("pnbindia.in", "netpnb.com"),
        "pnb" to listOf("pnbindia.in", "netpnb.com"),
        "bank of baroda" to listOf("bankofbaroda.in", "bobworld.com"),
        "bob world" to listOf("bankofbaroda.in", "bobworld.com"),
        "canara bank" to listOf("canarabank.com"),
        "union bank of india" to listOf("unionbankofindia.co.in"),
        "bank of india" to listOf("bankofindia.co.in"),
        "indian bank" to listOf("indianbank.in"),
        "central bank of india" to listOf("centralbankofindia.co.in"),
        "indian overseas bank" to listOf("iob.in"),
        "uco bank" to listOf("ucobank.com"),
        "bank of maharashtra" to listOf("bankofmaharashtra.in"),
        "punjab and sind" to listOf("punjabandsindbank.co.in"),
        "idfc first" to listOf("idfcfirstbank.com"),
        "yes bank" to listOf("yesbank.in"),
        "indusind" to listOf("indusind.com"),
        "federal bank" to listOf("federalbank.co.in"),
        "south indian bank" to listOf("southindianbank.com"),
        "rbl bank" to listOf("rblbank.com"),
        "bandhan bank" to listOf("bandhanbank.com"),
        "idbi bank" to listOf("idbibank.in"),
        "paytm bank" to listOf("paytmbank.com", "paytm.com"),
        "india post payment" to listOf("ippbonline.com"),
        "ippb" to listOf("ippbonline.com"),
        "airtel payment" to listOf("airtel.in"),
        "au small finance" to listOf("aubank.in"),
        "equitas" to listOf("equitasbank.com")
    )

    // ========================================================================
    // 2. VERIFIED SEARCH ENGINES, PLATFORMS & GOVT UTILITY AGGREGATORS
    // ========================================================================
    val VERIFIED_SEARCH_ENGINES = setOf(
        "google.com", "google.co.in", "google.in", "google.co.uk", "google.ca",
        "google.com.au", "google.de", "google.fr", "google.co.jp",
        "bing.com", "duckduckgo.com", "yahoo.com", "search.yahoo.com",
        "ecosia.org", "startpage.com", "brave.com", "search.brave.com"
    )

    val VERIFIED_TRUSTED_PLATFORMS = setOf(
        "youtube.com", "wikipedia.org", "reddit.com", "quora.com",
        "facebook.com", "instagram.com", "twitter.com", "x.com", "linkedin.com",
        "pinterest.com", "indiatimes.com", "ndtv.com", "gadgets360.com",
        "gsmarena.com", "91mobiles.com", "mysmartprice.com", "cashkaro.com"
    )

    val VERIFIED_GOVT_UTILITY_AGGREGATORS = setOf(
        "cleartax.in", "quicko.com", "myparkplus.com", "cardekho.com",
        "carinfo.app", "paisabazaar.com", "bankbazaar.com", "acko.com",
        "policybazaar.com", "digit.com", "godigit.com"
    )

    // ========================================================================
    // 3. SOVEREIGN GOVERNMENT IDENTITY & SCHEME KEYWORDS
    // ========================================================================
    private val GOVT_IDENTITY_KEYWORDS = listOf(
        "government of india", "govt of india", "bharat sarkar", "satyameva jayate",
        "ministry of", "department of", "directorate of", "national portal of india",
        "parivahan", "echallan", "e-challan", "vahan", "sarathi", "traffic police challan", "morth",
        "pm kisan", "pmkisan", "pradhan mantri", "pm awas", "ayushman bharat", "ladli behna",
        "subhadra yojana", "kisan samman", "ration card", "e-shram", "eshram",
        "uidai", "myaadhaar", "aadhaar update", "income tax department", "income tax return", "tax refund",
        "passport seva", "epfo", "epf balance", "e-courts", "high court of", "supreme court of india",
        "sarkari result", "sarkari exam", "sarkari naukri", "admit card download"
    )

    private val GOVT_FORM_INTERACTION_KEYWORDS = listOf(
        "vehicle number", "chassis", "pay challan", "registration fee", "enter aadhaar",
        "aadhaar number", "pan number", "enter otp", "download admit card", "application fee",
        "click here to pay", "submit", "apply now", "view challan", "claim refund"
    )

    // ========================================================================
    // 4. BANKING CREDENTIAL HARVESTING & PANIC / KYC PHISHING KEYWORDS
    // ========================================================================
    private val BANK_LOGIN_CREDENTIAL_KEYWORDS = listOf(
        "net banking", "internet banking", "mobile banking", "customer id", "user id",
        "cif number", "crn number", "login password", "transaction password", "profile password",
        "mpin", "atm pin", "debit card pin", "card number", "cvv", "expiry date"
    )

    private val BANK_PANIC_KYC_KEYWORDS = listOf(
        "update kyc", "pan card blocked", "account suspended", "account blocked",
        "sim blocked", "deactivation warning", "complete verification", "link pan with bank",
        "reward points expire", "redeem points", "electricity disconnected"
    )

    // ========================================================================
    // 5. HIGH-RISK BURNER TLDS & E-COMMERCE SCAM SIGNALS
    // ========================================================================
    private val HIGH_RISK_TLDS = setOf(
        "shop", "top", "xyz", "site", "online", "club", "buzz", "store", "live", "space", "click"
    )

    private val NON_WEB_SCHEMES = setOf(
        "mailto", "tel", "sms", "smsto", "mms", "javascript", "about",
        "chrome", "chrome-extension", "file", "intent", "data", "blob",
        "android-app", "market", "geo", "upi", "paytmmp", "phonepe", "tez"
    )

    private val NON_WEBSITE_FILE_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv",
        "jpg", "jpeg", "png", "gif", "webp", "svg", "mp3", "mp4", "wav",
        "avi", "mkv", "apk", "json", "xml", "yaml", "yml"
    )

    private val ECOMMERCE_KEYWORDS = listOf(
        "add to cart", "buy now", "checkout", "shop now",
        "order now", "cash on delivery", "claim now", "claim offer",
        "claim prize", "spin and win", "scratch and win",
        "flash sale", "loot deal", "big billion", "great indian festival"
    )

    private val SHOPPING_HOST_WORDS = listOf(
        "shop", "shopping", "store", "mart", "mall", "kart", "deal", "deals",
        "offer", "offers", "sale", "bazaar", "discount", "bazar"
    )

    private val COMMERCE_IMPERSONATION_BRAND_ROOTS = setOf(
        "flipkart", "amazon", "myntra", "meesho", "ajio", "nykaa", "tatacliq",
        "snapdeal", "croma", "jiomart", "bigbasket", "blinkit", "zepto",
        "bookmyshow", "makemytrip", "goibibo", "swiggy", "zomato", "ekart"
    )

    private val BANK_BRAND_ROOTS = setOf(
        "sbi", "pnb", "hdfcbank", "icicibank", "axisbank", "kotak",
        "canarabank", "bobworld", "bankofbaroda", "unionbank", "bankofindia",
        "indianbank", "centralbank", "iob", "ucobank", "bankofmaharashtra",
        "punjabandsind", "indusind", "yesbank", "idfcfirst", "federalbank",
        "southindianbank", "rblbank", "bandhanbank", "idbibank", "jkbank",
        "kvb", "cityunionbank", "standardchartered", "citibank", "hsbc",
        "paytmbank", "ippb", "finobank", "jiopayments", "aubank",
        "equitas", "ujjivan", "capitalbank", "janabank", "bajajfinserv"
    )

    private val PATH_IMPERSONATION_BRAND_ROOTS = COMMERCE_IMPERSONATION_BRAND_ROOTS +
            BANK_BRAND_ROOTS + setOf("paytm", "phonepe", "paypal", "irctc", "indiapost", "bluedart", "delhivery", "rbi", "lic", "epfo")

    private val EXTREME_DISCOUNT_REGEX = Regex("\\b(9[5-9]|100)%\\s*(off|discount|cashback)\\b", RegexOption.IGNORE_CASE)
    private val HIGH_DISCOUNT_REGEX = Regex("\\b(8\\d|9[0-4])%\\s*(off|discount)\\b", RegexOption.IGNORE_CASE)

    private val IMPLAUSIBLE_DISCOUNTS_AND_SCARCITY = listOf(
        "only 2 left", "flash sale ends", "clearance sale", "hurry up", "loot deal"
    )

    private val UNOFFICIAL_SUPPORT_EMAILS = listOf(
        "@gmail.com", "@yahoo.com", "@outlook.com"
    )

    private val BRAND_TO_URL_RULES = mapOf(
        "flipkart" to listOf("flipkart.com", "fkrt.it", "fkrt.cc", "ekartlogistics.com"),
        "big billion" to listOf("flipkart.com"),
        "amazon" to listOf("amazon.in", "amazon.com", "amzn.to", "amzn.in"),
        "great indian festival" to listOf("amazon.in", "amazon.com"),
        "myntra" to listOf("myntra.com"),
        "meesho" to listOf("meesho.com"),
        "ajio" to listOf("ajio.com"),
        "nykaa" to listOf("nykaa.com"),
        "snapdeal" to listOf("snapdeal.com"),
        "tatacliq" to listOf("tatacliq.com"),
        "croma" to listOf("croma.com"),
        "reliancedigital" to listOf("reliancedigital.in"),
        "jiomart" to listOf("jiomart.com"),
        "bigbasket" to listOf("bigbasket.com"),
        "blinkit" to listOf("blinkit.com"),
        "zepto" to listOf("zeptonow.com"),
        "bookmyshow" to listOf("bookmyshow.com"),
        "makemytrip" to listOf("makemytrip.com"),
        "goibibo" to listOf("goibibo.com"),
        "swiggy" to listOf("swiggy.com"),
        "zomato" to listOf("zomato.com"),
        "paytm" to listOf("paytm.com", "paytmbank.com"),
        "phonepe" to listOf("phonepe.com"),
        "paypal" to listOf("paypal.com", "paypal.me"),
        "bluedart" to listOf("bluedart.com"),
        "delhivery" to listOf("delhivery.com"),
        "fedex" to listOf("fedex.com"),
        "bajajfinserv" to listOf("bajajfinserv.in")
    )

    private val BRAND_TEXT_REGEXES = BRAND_TO_URL_RULES.keys.associateWith { key ->
        Regex("\\b${Regex.escape(key)}\\b")
    }

    private val SERP_ENGINE_HOST_REGEX = Regex(
        "^(?:[a-z0-9-]+\\.)?(?:google|bing|duckduckgo|yahoo)\\.(?:com|co\\.[a-z]{2}|com\\.[a-z]{2}|[a-z]{2})$"
    )

    private val TOKEN_SPLIT_REGEX = Regex("[\\s,()<>{}\\[\\]\"']+")

    private val USERINFO_TLD_SUFFIXES = setOf(
        "com", "in", "org", "net", "edu", "gov", "co", "io", "uk", "xyz", "shop", "top", "online", "site"
    )

    private data class ParsedUrl(
        val rawToken: String,
        val domain: String,
        val pathAndQuery: String,
        val spoofedPrefixes: List<String>,
        val hasUserInfoMask: Boolean
    )

    /**
     * Checks if a domain is a legally authentic Indian Government portal.
     */
    fun isAuthenticGovernmentDomain(domain: String): Boolean {
        if (domain.endsWith(".gov.in") || domain.endsWith(".nic.in") || domain.endsWith(".mil.in")) {
            return true
        }
        val statutoryExceptions = setOf(
            "irctc.co.in", "rbi.org.in", "licindia.in", "epfindia.gov.in"
        )
        return statutoryExceptions.any { domain == it || domain.endsWith(".$it") }
    }

    /**
     * 100% on-device heuristic and mathematical scoring engine.
     * Evaluates active browser URL and rendered screen text nodes.
     */
    fun analyze(rawUrl: String, screenText: String): ScanResult {
        val parsed = parseBestUrlFromInput(rawUrl) ?: return ScanResult(0, emptyList())
        val cleanDomain = parsed.domain
        if (cleanDomain.isBlank()) {
            return ScanResult(0, emptyList())
        }

        var score = 0
        val reasons = mutableListOf<String>()
        var detectedCategory = "GENERIC"
        val lowerText = screenText.lowercase(Locale.ROOT)
        val tld = cleanDomain.substringAfterLast('.', "").lowercase(Locale.ROOT)

        // ====================================================================
        // WHITELIST CHECK 1: Exact official brand, sibling & bank whitelist check
        // ====================================================================
        val isAuthenticBrand = PROTECTED_BRANDS.any { (officialDomain, _) ->
            cleanDomain == officialDomain || cleanDomain.endsWith(".$officialDomain")
        } || OFFICIAL_COMPANION_DOMAINS.any { companion ->
            cleanDomain == companion || cleanDomain.endsWith(".$companion")
        }
        if (isAuthenticBrand) {
            return ScanResult(0, listOf("Verified authentic domain ($cleanDomain)"))
        }

        // ====================================================================
        // WHITELIST CHECK 2: Legitimate Indian Government Domain
        // ====================================================================
        if (isAuthenticGovernmentDomain(cleanDomain)) {
            return ScanResult(0, listOf("Verified authentic government domain ($cleanDomain)"))
        }

        // ====================================================================
        // WHITELIST CHECK 3: Verified Search Engines & SERP Results Pages
        // ====================================================================
        if (isSearchEngineOrSerp(cleanDomain, rawUrl)) {
            return ScanResult(0, listOf("Verified search engine / SERP ($cleanDomain)"))
        }

        // ====================================================================
        // WHITELIST CHECK 4: Verified Trusted Platforms & Govt Utility Portals
        // ====================================================================
        val isTrustedPlatform = VERIFIED_TRUSTED_PLATFORMS.any { tp ->
            cleanDomain == tp || cleanDomain.endsWith(".$tp")
        } || VERIFIED_GOVT_UTILITY_AGGREGATORS.any { util ->
            cleanDomain == util || cleanDomain.endsWith(".$util")
        }
        if (isTrustedPlatform) {
            return ScanResult(0, listOf("Verified trusted platform ($cleanDomain)"))
        }

        // ====================================================================
        // CHECK 0: URL UserInfo Masking ('@' Basic-Auth Phishing Trick)
        // ====================================================================
        if (parsed.hasUserInfoMask) {
            score += 75
            reasons.add("Deceptive '@' credential-masking URL hiding real destination host '$cleanDomain'.")
        }

        // ====================================================================
        // CHECK 1: Subdomain & TLD Deception Trap (e.g., echallan.gov.in.pay-scam.com or sbi.nic.xy)
        // ====================================================================
        if (cleanDomain.contains(".gov.in.") || cleanDomain.contains(".nic.in.") ||
            (cleanDomain.contains(".nic.") && !cleanDomain.endsWith(".nic.in")) ||
            (cleanDomain.contains(".gov.") && !cleanDomain.endsWith(".gov.in"))
        ) {
            score += 95
            reasons.add("Critical: Subdomain / TLD spoofing detected. Domain '$cleanDomain' disguises an unauthorized host using a counterfeit government (.gov / .nic) label.")
            detectedCategory = "GOVERNMENT"
        }

        // ====================================================================
        // CHECK 2: Sovereign Government Portal Spoofing
        // ====================================================================
        val govtDomainKeywords = listOf("parivahan", "echallan", "pmkisan", "uidai", "sarkariresult", "incometax", "epfindia", "digilocker", "vahan", "challan")
        val allUrlCheckText = (cleanDomain + " " + parsed.spoofedPrefixes.joinToString(" ")).lowercase(Locale.ROOT)
        val domainGovtSlug = govtDomainKeywords.firstOrNull { allUrlCheckText.contains(it) }
        val matchedGovtKeyword = GOVT_IDENTITY_KEYWORDS.firstOrNull { lowerText.contains(it) } ?: domainGovtSlug

        if ((domainGovtSlug != null || matchedGovtKeyword != null) && detectedCategory != "GOVERNMENT") {
            val hasInteraction = GOVT_FORM_INTERACTION_KEYWORDS.any { lowerText.contains(it) }
            val hasBurnerTld = HIGH_RISK_TLDS.contains(tld)

            if (domainGovtSlug != null || hasInteraction || hasBurnerTld) {
                score += 85
                val reasonKeyword = domainGovtSlug ?: matchedGovtKeyword ?: "Government Service"
                reasons.add("Counterfeit government portal: Domain or page claims official Government status ('$reasonKeyword') on unauthorized non-government host '$cleanDomain'. Official government portals strictly end with .gov.in or .nic.in.")
                detectedCategory = "GOVERNMENT"
            }
        }

        // ====================================================================
        // CHECK 3: Master Bank Registry Impersonation
        // ====================================================================
        val domainLabels = cleanDomain.split('.', '-') +
                parsed.spoofedPrefixes.flatMap { it.split('.', '-', '_', ':') }.filter { it.isNotBlank() }
        for ((bankKeyword, authorizedDomains) in BANK_NAME_TO_AUTHORIZED_DOMAINS) {
            val matchesInDomain = if (bankKeyword.length <= 4) {
                domainLabels.contains(bankKeyword) ||
                    allUrlCheckText.contains("online$bankKeyword") ||
                    allUrlCheckText.contains("${bankKeyword}bank") ||
                    allUrlCheckText.contains("${bankKeyword}net") ||
                    allUrlCheckText.contains("${bankKeyword}kyc")
            } else {
                allUrlCheckText.contains(bankKeyword) || domainLabels.contains(bankKeyword)
            }
            val matchesInText = lowerText.contains(bankKeyword)

            if (matchesInDomain || matchesInText) {
                val isAuthorized = authorizedDomains.any { auth ->
                    cleanDomain == auth || cleanDomain.endsWith(".$auth")
                }
                if (!isAuthorized) {
                    val hasBankingActions = BANK_LOGIN_CREDENTIAL_KEYWORDS.any { lowerText.contains(it) } ||
                            BANK_PANIC_KYC_KEYWORDS.any { lowerText.contains(it) } ||
                            HIGH_RISK_TLDS.contains(tld) ||
                            matchesInDomain
                    if (hasBankingActions) {
                        score += 90
                        reasons.add("Bank impersonation: Unauthorized host '$cleanDomain' simulates registered banking portal '$bankKeyword'.")
                        detectedCategory = "BANK"
                        break
                    }
                }
            }
        }

        // ====================================================================
        // CHECK 4: Zero-Day Universal Banking Phishing Trap (For ANY Unlisted Bank)
        // ====================================================================
        val hasCredentialHarvesting = BANK_LOGIN_CREDENTIAL_KEYWORDS.any { lowerText.contains(it) }
        val hasPanicPretext = BANK_PANIC_KYC_KEYWORDS.any { lowerText.contains(it) }
        if (hasCredentialHarvesting && hasPanicPretext && detectedCategory != "BANK") {
            score += 90
            reasons.add("Critical: Urgent KYC / NetBanking credential harvesting form detected on unauthorized host '$cleanDomain'.")
            detectedCategory = "BANK"
        }

        // ====================================================================
        // CHECK 5: Punycode / Homograph Spoof Check
        // ====================================================================
        try {
            val asciiDomain = IDN.toASCII(cleanDomain)
            if (asciiDomain.startsWith("xn--", ignoreCase = true) ||
                asciiDomain.contains("xn--", ignoreCase = true) ||
                cleanDomain.startsWith("xn--", ignoreCase = true) ||
                cleanDomain.contains("xn--", ignoreCase = true)
            ) {
                score += 60
                reasons.add("Punycode / Homograph spoofing detected ($asciiDomain)")
            }
        } catch (_: Exception) {
            // Ignore malformed domain encoding exceptions
        }

        // ====================================================================
        // CHECK 6: Multi-Layer Brand Hijacking, Prefix/Concatenated Spoofing & Typosquatting
        // ====================================================================
        val nonTldHostLabels = cleanDomain.split('.').dropLast(1).filter { it.length >= 2 }
        val hyphenHostTokens = nonTldHostLabels.flatMap { it.split('-', '_') }.filter { it.length >= 3 }
        val spoofPrefixTokens = parsed.spoofedPrefixes
            .flatMap { it.split('.', '-', '_', ':') }
            .filter { it.length >= 3 }
        val allHostCandidates = (nonTldHostLabels + hyphenHostTokens + spoofPrefixTokens).distinct()

        fun categoryForBrand(brandRoot: String): String = when (brandRoot) {
            in BANK_BRAND_ROOTS -> "BANK"
            in COMMERCE_IMPERSONATION_BRAND_ROOTS -> "ECOMMERCE"
            else -> detectedCategory
        }

        var hostBrandMatched = false

        // 6a. Exact token match across ALL brands first (subdomains, hyphen segments, colon/'@' prefixes)
        for ((officialDomain, brandRoot) in PROTECTED_BRANDS) {
            if (cleanDomain != officialDomain && !cleanDomain.endsWith(".$officialDomain") && allHostCandidates.contains(brandRoot)) {
                val isAppleNaturalPhrase = brandRoot == "apple" &&
                        !nonTldHostLabels.contains("apple") &&
                        !spoofPrefixTokens.contains("apple") &&
                        !HIGH_RISK_TLDS.contains(tld) &&
                        !allHostCandidates.any { it in BRAND_LURE_AFFIXES || it in setOf("iphone", "ipad", "macbook", "icloud", "ios") }
                if (!isAppleNaturalPhrase) {
                    score += 65
                    reasons.add("Brand hijacking: Registered brand '$brandRoot' embedded in unauthorized host '$cleanDomain'")
                    detectedCategory = categoryForBrand(brandRoot)
                    hostBrandMatched = true
                    break
                }
            }
        }

        // 6b. Compound / Concatenated Brand Spoofing inside domain labels (e.g., flipkartsale.xyz, priyaflipkart.xyz, amazonsale.shop)
        if (!hostBrandMatched) {
            for ((officialDomain, brandRoot) in PROTECTED_BRANDS) {
                if (cleanDomain != officialDomain && !cleanDomain.endsWith(".$officialDomain")) {
                    val compoundMatch = allHostCandidates.firstOrNull { cand ->
                        matchesEmbeddedBrand(cand, brandRoot)
                    }
                    if (compoundMatch != null) {
                        score += 65
                        reasons.add("Brand impersonation: Registered brand '$brandRoot' embedded in unauthorized domain label '$compoundMatch' ('$cleanDomain')")
                        detectedCategory = categoryForBrand(brandRoot)
                        hostBrandMatched = true
                        break
                    }
                }
            }
        }

        // 6c. Typosquatting across non-generic host labels & prefixes
        if (!hostBrandMatched) {
            for ((officialDomain, brandRoot) in PROTECTED_BRANDS) {
                if (cleanDomain != officialDomain && !cleanDomain.endsWith(".$officialDomain")) {
                    var typoMatched = false
                    for (cand in allHostCandidates) {
                        if (cand.length < 3 || cand in GENERIC_INFRA_WORDS) continue
                        val maxAllowedDistance = when {
                            brandRoot.length <= 4 -> 0
                            brandRoot.length <= 6 -> if (cand.length >= 4) 1 else 0
                            else -> if (cand.length >= 5) 2 else 1
                        }
                        if (maxAllowedDistance == 0) continue
                        val distance = levenshtein(cand, brandRoot)
                        val lengthDiff = kotlin.math.abs(cand.length - brandRoot.length)
                        if (distance in 1..maxAllowedDistance && lengthDiff <= maxAllowedDistance) {
                            score += 55
                            reasons.add("Typosquatting of '$officialDomain' detected (edit distance: $distance on '$cand')")
                            detectedCategory = categoryForBrand(brandRoot)
                            typoMatched = true
                            hostBrandMatched = true
                            break
                        }
                    }
                    if (typoMatched) break
                }
            }
        }

        // 6d. URL Path Brand Impersonation (e.g., priyashopping.xyz/flipkart or priyashopping.xyz/flipkart-sale)
        if (!hostBrandMatched && parsed.pathAndQuery.isNotBlank()) {
            val pathPart = parsed.pathAndQuery.substringBefore('?').substringBefore('#')
            val pathTokens = pathPart.split('/', '-', '_', '.').filter { it.length >= 3 }
            for ((officialDomain, brandRoot) in PROTECTED_BRANDS) {
                if (brandRoot in PATH_IMPERSONATION_BRAND_ROOTS &&
                    cleanDomain != officialDomain && !cleanDomain.endsWith(".$officialDomain")
                ) {
                    val pathMatch = pathTokens.firstOrNull { token ->
                        token == brandRoot || matchesEmbeddedBrand(token, brandRoot)
                    }
                    if (pathMatch != null) {
                        score += if (HIGH_RISK_TLDS.contains(tld)) 45 else 35
                        reasons.add("Path brand impersonation: Official brand '$brandRoot' used in URL path '$pathPart' on unauthorized host '$cleanDomain'")
                        if (detectedCategory == "GENERIC") {
                            detectedCategory = when (brandRoot) {
                                in BANK_BRAND_ROOTS -> "BANK"
                                in COMMERCE_IMPERSONATION_BRAND_ROOTS -> "ECOMMERCE"
                                else -> "GENERIC"
                            }
                        }
                        break
                    }
                }
            }
        }

        // ====================================================================
        // CHECK 7: High-Risk Burner TLD Check
        // ====================================================================
        if (tld.isNotBlank() && HIGH_RISK_TLDS.contains(tld)) {
            score += 25
            reasons.add("High-risk burner TLD commonly associated with scam stores (.$tld)")
        }

        // ====================================================================
        // CHECK 8: Fake Shopping, Brand-Text Mismatch & E-Commerce Heuristics
        // ====================================================================
        val isEcommerce = ECOMMERCE_KEYWORDS.any { lowerText.contains(it) }
        val hasShoppingHostWord = SHOPPING_HOST_WORDS.any { cleanDomain.contains(it) }
        val hasPriceOrCurrency = lowerText.contains("₹") || lowerText.contains("rs.") || lowerText.contains("rs ") || lowerText.contains("inr")
        val hasExtremeDiscount = EXTREME_DISCOUNT_REGEX.containsMatchIn(lowerText)
        val hasHighDiscount = HIGH_DISCOUNT_REGEX.containsMatchIn(lowerText)
        val hasUnofficialEmail = UNOFFICIAL_SUPPORT_EMAILS.any { lowerText.contains(it) }
        val hasDirectUpi = lowerText.contains("upi://pay") || lowerText.contains("@upi") || lowerText.contains("@okaxis")

        // 8a. Brand-to-URL Mismatch in screen/message text (triggers even without "add to cart", e.g., "Flipkart: priyashopping.xyz")
        var brandMismatchAdded = false
        val hasTextCommerceSignal = isEcommerce ||
                hasExtremeDiscount ||
                hasHighDiscount ||
                hasPriceOrCurrency ||
                hasUnofficialEmail ||
                hasDirectUpi
        for ((brandKeyword, authorizedDomains) in BRAND_TO_URL_RULES) {
            val matchedBrandInText = BRAND_TEXT_REGEXES[brandKeyword]?.containsMatchIn(lowerText) == true
            if (matchedBrandInText) {
                val isAuthorized = authorizedDomains.any { auth ->
                    cleanDomain == auth || cleanDomain.endsWith(".$auth")
                }
                if (!isAuthorized) {
                    val hasImpersonationSyntax = lowerText.contains("$brandKeyword:") ||
                            lowerText.contains("$brandKeyword :") ||
                            lowerText.contains("$brandKeyword sale") ||
                            lowerText.contains("$brandKeyword offer") ||
                            lowerText.contains("$brandKeyword deal") ||
                            lowerText.contains("$brandKeyword gift") ||
                            lowerText.contains("$brandKeyword prize") ||
                            lowerText.contains("$brandKeyword official") ||
                            lowerText.contains("$brandKeyword login") ||
                            lowerText.contains("$brandKeyword reward") ||
                            lowerText.contains("$brandKeyword support")

                    if (isEcommerce || hasShoppingHostWord || hasImpersonationSyntax || (HIGH_RISK_TLDS.contains(tld) && hasTextCommerceSignal)) {
                        val mismatchScore = if (hasTextCommerceSignal || hasShoppingHostWord || hasImpersonationSyntax) 55 else 30
                        score += mismatchScore
                        reasons.add("Brand mismatch: Content references '$brandKeyword' on unauthorized host '$cleanDomain'")
                        if (detectedCategory == "GENERIC") detectedCategory = "ECOMMERCE"
                        brandMismatchAdded = true
                        break
                    }
                }
            }
        }

        // 8b. Additional E-Commerce Scam Signals (Extreme discounts, Gmail support, UPI traps)
        val hasCommerceContext = isEcommerce || hasShoppingHostWord || brandMismatchAdded ||
                ((HIGH_RISK_TLDS.contains(tld) || hasPriceOrCurrency) && (hasExtremeDiscount || hasDirectUpi))
        if (hasCommerceContext) {
            if (detectedCategory == "GENERIC") detectedCategory = "ECOMMERCE"

            if (hasExtremeDiscount) {
                score += 50
                reasons.add("Extreme implausible discount (95%-99% off) detected, typical of counterfeit scam stores")
            } else if (isEcommerce && (hasHighDiscount || IMPLAUSIBLE_DISCOUNTS_AND_SCARCITY.any { lowerText.contains(it) })) {
                score += 30
                reasons.add("Implausible discount or artificial urgency keywords detected")
            }

            if (isEcommerce && hasUnofficialEmail) {
                score += 25
                reasons.add("E-commerce site lists free public email (@gmail/@yahoo/@outlook) for support")
            }

            if (hasDirectUpi) {
                score += 40
                reasons.add("Direct unverified UPI payment link detected on unauthorized shopping portal")
            }
        }

        return ScanResult(
            score = score.coerceAtMost(100),
            reasons = reasons,
            fraudCategory = detectedCategory
        )
    }

    /**
     * Checks if a candidate domain/path label embeds a protected brand name cleanly without false positives.
     */
    private fun matchesEmbeddedBrand(cand: String, brandRoot: String): Boolean {
        if (cand == brandRoot || cand.length <= brandRoot.length) return false
        if (!cand.contains(brandRoot)) return false

        // Unmistakable trademarked brand strings: any occurrence inside an unverified domain is impersonation
        if (brandRoot in DISTINCT_SUBSTRING_BRANDS) {
            return true
        }

        // Special guard for "amazon": avoid geographic words like "amazonia" / "amazonas" / "amazonian"
        if (brandRoot == "amazon") {
            return cand != "amazonia" && cand != "amazonas" && !cand.contains("amazonian")
        }

        // Short / generic-word brands ("apple", "google", "sbi", "jio", etc.): match only with phishing/commerce affixes
        return BRAND_LURE_AFFIXES.any { affix ->
            cand == "$brandRoot$affix" ||
                cand == "$affix$brandRoot" ||
                cand.startsWith("$brandRoot$affix") ||
                cand.endsWith("$affix$brandRoot")
        }
    }

    /**
     * Identifies verified search engine homepages or Search Engine Results Pages (SERP)
     * to avoid triggering false alarms on aggregated search results and sponsored shopping links.
     */
    fun isSearchEngineOrSerp(cleanDomain: String, rawUrl: String): Boolean {
        if (cleanDomain.isBlank()) return false
        val isEngine = VERIFIED_SEARCH_ENGINES.any { se ->
            cleanDomain == se || cleanDomain.endsWith(".$se")
        }
        if (isEngine) return true

        val lowerUrl = rawUrl.lowercase(Locale.ROOT)
        if ((lowerUrl.contains("/search?") || lowerUrl.contains("/search/") ||
                lowerUrl.contains("/search") || lowerUrl.contains("?q=") || lowerUrl.contains("&q=")) &&
            SERP_ENGINE_HOST_REGEX.matches(cleanDomain)
        ) {
            return true
        }
        return false
    }

    /**
     * Checks if the given text represents a fully formed, navigated website URL
     * rather than an in-progress search query or arbitrary typed text.
     */
    fun isLikelyNavigatedUrl(text: String): Boolean {
        return parseSingleUrlToken(text) != null
    }

    /**
     * Extracts a valid URL from raw text or talkback descriptions (e.g., "Search or type URL, amaz0n-sale.shop").
     * Returns null if no valid navigated URL is present.
     */
    fun findValidUrlInText(rawText: String?): String? {
        return parseBestUrlFromInput(rawText)?.rawToken
    }

    /**
     * Extracts all distinct candidate URL tokens from a message or block of text in appearance order.
     */
    fun findAllValidUrlsInText(rawText: String?): List<String> {
        if (rawText.isNullOrBlank()) return emptyList()
        val trimmed = rawText.trim()
        val results = LinkedHashSet<String>()

        parseSingleUrlToken(trimmed)?.let { results.add(it.rawToken) }

        val tokens = trimmed.split(TOKEN_SPLIT_REGEX)
        for (token in tokens) {
            val cleanToken = token.trim().trim('\'', '"', '(', ')', '<', '>', '/', '.', ',', ';', ':', '!', '?')
            if (cleanToken.isNotEmpty()) {
                parseSingleUrlToken(cleanToken)?.let { results.add(it.rawToken) }
            }
        }
        return results.toList()
    }

    /**
     * Scans all URLs present in a message and returns the highest-scoring URL and its ScanResult.
     * Prevents attackers from bypassing detection by placing a legitimate URL before a spoofed URL.
     */
    fun findHighestRiskUrlInText(rawText: String?): Pair<String, ScanResult>? {
        if (rawText.isNullOrBlank()) return null
        val urls = findAllValidUrlsInText(rawText)
        var bestPair: Pair<String, ScanResult>? = null
        for (url in urls) {
            val scan = analyze(url, rawText)
            if (bestPair == null || scan.score > bestPair.second.score) {
                bestPair = url to scan
            }
        }
        return bestPair
    }

    /**
     * Extracts clean lowercase hostname, removing protocol, userinfo/spoof prefix, port, query, and www prefix.
     * Returns empty string if the input is not a valid website domain.
     */
    fun extractDomain(url: String): String {
        return parseBestUrlFromInput(url)?.domain ?: ""
    }

    private fun parseBestUrlFromInput(rawText: String?): ParsedUrl? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()

        // Direct single-URL match
        val directParsed = parseSingleUrlToken(trimmed)
        if (directParsed != null) {
            return directParsed
        }

        // Multi-word accessibility string or message token split
        val tokens = trimmed.split(TOKEN_SPLIT_REGEX)
        for (token in tokens) {
            val cleanToken = token.trim().trim('\'', '"', '(', ')', '<', '>', '/', '.', ',', ';', ':', '!', '?')
            val parsed = parseSingleUrlToken(cleanToken)
            if (parsed != null) {
                return parsed
            }
        }
        return null
    }

    /**
     * Parses a single URL candidate token, handling standard URLs, HTTP Basic-Auth '@' masks,
     * and colon-spoofed domain prefixes like "flipkart:priyashopping.xyz".
     */
    private fun parseSingleUrlToken(rawInput: String): ParsedUrl? {
        val clean = rawInput.trim().trimEnd('.', ',', ';', ':', '!', '?')
        if (clean.length < 4 || clean.contains(' ') || clean.contains('\n') || clean.contains('\r')) {
            return null
        }

        val lower = clean.lowercase(Locale.ROOT)
        val spoofedPrefixes = mutableListOf<String>()
        var hasWebScheme = false
        var hasUserInfoMask = false
        var remainder = lower

        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            hasWebScheme = true
            remainder = lower.substringAfter("://")
        } else if (lower.contains("://")) {
            val rawScheme = lower.substringBefore("://")
            if (rawScheme.contains(':')) {
                val lastColonScheme = rawScheme.substringAfterLast(':')
                val spoofPrefix = rawScheme.substringBeforeLast(':')
                if ((lastColonScheme == "http" || lastColonScheme == "https") && !spoofPrefix.contains('/')) {
                    if (spoofPrefix in NON_WEB_SCHEMES) {
                        return null
                    }
                    if (spoofPrefix.isNotEmpty()) {
                        spoofedPrefixes.add(spoofPrefix)
                    }
                    hasWebScheme = true
                    remainder = lower.substringAfter("://")
                } else {
                    return null
                }
            } else if (rawScheme.isNotEmpty() && rawScheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }) {
                if (rawScheme in NON_WEB_SCHEMES || (rawScheme != "http" && rawScheme != "https")) {
                    return null
                }
                hasWebScheme = true
                remainder = lower.substringAfter("://")
            }
        } else {
            val authorityCandidate = lower.substringBefore('/').substringBefore('?').substringBefore('#')
            if (authorityCandidate.contains(':') && !authorityCandidate.contains('@')) {
                val beforeColon = authorityCandidate.substringBefore(':')
                val afterColonFull = lower.substringAfter(':')
                if (beforeColon in NON_WEB_SCHEMES) {
                    return null
                }
                val afterColonAuthority = afterColonFull.substringBefore('/').substringBefore('?').substringBefore('#')
                val isNumericPort = afterColonAuthority.isNotEmpty() && afterColonAuthority.all { it.isDigit() }
                if (isNumericPort) {
                    if (!beforeColon.contains('.')) {
                        // e.g. "10:30" or "localhost:8080"
                        return null
                    }
                    remainder = lower
                } else if (afterColonAuthority.contains('.')) {
                    // Colon-spoofed prefix, e.g. "flipkart:priyashopping.xyz"
                    if (beforeColon.isNotEmpty()) {
                        spoofedPrefixes.add(beforeColon)
                    }
                    remainder = afterColonFull
                } else {
                    return null
                }
            }
        }

        val authority = remainder.substringBefore('/').substringBefore('?').substringBefore('#')
        val pathAndQuery = remainder.removePrefix(authority)
        var hostPortPart = authority

        // Handle colon-spoofed prefix even after http:// (e.g. "https://flipkart:priyashopping.xyz")
        if (!authority.contains('@') && authority.contains(':')) {
            val left = authority.substringBefore(':')
            val right = authority.substringAfter(':')
            val rightIsNumericPort = right.isEmpty() || right.all { it.isDigit() }
            if (!rightIsNumericPort && right.contains('.')) {
                if (left.isNotEmpty()) spoofedPrefixes.add(left)
                hostPortPart = right
            }
        }

        // Handle '@' credential masking (e.g. "https://flipkart.com@priyashopping.xyz" or "flipkart:sale@priyashopping.xyz")
        if (authority.contains('@')) {
            val userInfo = authority.substringBeforeLast('@')
            val afterAt = authority.substringAfterLast('@')
            val userInfoTrailingTld = userInfo.substringAfterLast('.', "")
            val looksLikeDomainInUserInfo = userInfo.contains('.') && userInfoTrailingTld in USERINFO_TLD_SUFFIXES
            val looksLikePlainEmail = !hasWebScheme &&
                    !clean.contains('/') &&
                    !clean.contains('?') &&
                    !clean.contains('#') &&
                    !userInfo.contains(':') &&
                    !looksLikeDomainInUserInfo
            if (looksLikePlainEmail) {
                return null
            }
            hasUserInfoMask = true
            if (userInfo.isNotEmpty()) {
                spoofedPrefixes.add(userInfo)
            }
            hostPortPart = afterAt
        }

        if (hostPortPart.startsWith('[')) {
            return null // Reject IPv6 literals
        }

        val rawHost = if (hostPortPart.contains(':')) {
            val possiblePort = hostPortPart.substringAfter(':')
            if (possiblePort.all { it.isDigit() }) {
                hostPortPart.substringBefore(':')
            } else {
                return null
            }
        } else {
            hostPortPart
        }

        val normalizedHost = rawHost.trim().trimEnd('.', '/', ':').removePrefix("www.")
        if (!normalizedHost.contains('.') || normalizedHost.startsWith('.') || normalizedHost.endsWith('.') || normalizedHost.contains("..")) {
            return null
        }

        val labels = normalizedHost.split('.')
        if (labels.any { label -> label.isEmpty() || label.startsWith('-') || label.endsWith('-') || !label.all { ch -> ch.isLetterOrDigit() || ch == '-' } }) {
            return null
        }

        val tld = labels.last()
        if (tld.length < 2 || !tld.all { it.isLetter() }) {
            return null
        }

        if (!hasWebScheme && !clean.contains('/') && !hasUserInfoMask && spoofedPrefixes.isEmpty() && tld in NON_WEBSITE_FILE_EXTENSIONS) {
            return null
        }

        return ParsedUrl(
            rawToken = clean,
            domain = normalizedHost,
            pathAndQuery = pathAndQuery,
            spoofedPrefixes = spoofedPrefixes,
            hasUserInfoMask = hasUserInfoMask
        )
    }

    /**
     * Memory-efficient O(min(M, N)) Levenshtein Distance implementation.
     */
    fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val m = lhs.length
        val n = rhs.length
        if (m == 0) return n
        if (n == 0) return m

        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)

        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,       // insertion
                    prev[j] + 1,           // deletion
                    prev[j - 1] + cost     // substitution
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[n]
    }
}
