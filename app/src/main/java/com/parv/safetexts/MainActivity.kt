// programmed by parv
package com.parv.safetexts

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.app.role.RoleManager
import android.app.AppOpsManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.parv.safetexts.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedPref = getSharedPreferences("safetexts_prefs", MODE_PRIVATE)
        val hasAccepted = sharedPref.getBoolean("has_accepted_tc", false)

        setContent {
            EdgeGuardAITheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = if (hasAccepted) "home" else "welcome") {
                    composable("welcome") { WelcomeScreen(navController, sharedPref) }
                    composable("home") { HomeScreen(navController) }
                    composable("permissions") { PermissionsScreen(navController) }
                    composable("terms") { TermsScreen(navController) }
                    composable("privacy") { PrivacyScreen(navController) }
                }
            }
        }
    }
}

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("safetexts_prefs", Context.MODE_PRIVATE) }
    
    // State for permissions
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isNotificationListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var isContactsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isOverlayPermissionGranted by remember {
        mutableStateOf(Settings.
        canDrawOverlays(context))
    }
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var isBatteryOptimized by remember { 
        mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName)) 
    }
    
    var isCallLogPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isPhoneStatePermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isUsageStatsEnabled by remember {
        mutableStateOf(hasUsageStatsPermission(context))
    }


    // Permission launchers
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isContactsPermissionGranted = isGranted
    }

    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPhoneStatePermissionGranted = isGranted
    }



    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isCallLogPermissionGranted = isGranted
    }

    // A simple way to refresh when the activity resumes
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
                isNotificationListenerEnabled = isNotificationListenerEnabled(context)
                isContactsPermissionGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
                isPhoneStatePermissionGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED

                isOverlayPermissionGranted = Settings.canDrawOverlays(context)
                isBatteryOptimized = pm.isIgnoringBatteryOptimizations(context.packageName)
                isUsageStatsEnabled = hasUsageStatsPermission(context)
                isCallLogPermissionGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Scam Message Protection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PermissionRow(
                        title = "Accessibility Access",
                        description = "Required to read messages from WhatsApp & SMS for scams.",
                        isGranted = isAccessibilityEnabled,
                        onToggle = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    PermissionRow(
                        title = "Notification Access",
                        description = "Required to scan background messages and VoIP calls for scams.",
                        isGranted = isNotificationListenerEnabled,
                        onToggle = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    PermissionRow(
                        title = "Contacts Access",
                        description = "Used to identify messages from unknown numbers.",
                        isGranted = isContactsPermissionGranted,
                        onToggle = {
                            if (!isContactsPermissionGranted) {
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            Text(
                "Scam Call Protection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PermissionRow(
                        title = "Call Log Access",
                        description = "Required to verify the incoming phone number during an OTP call scam.",
                        isGranted = isCallLogPermissionGranted,
                        onToggle = {
                            if (!isCallLogPermissionGranted) {
                                callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    PermissionRow(
                        title = "Phone State Access",
                        description = "Required to detect if you are currently on a call when an OTP arrives.",
                        isGranted = isPhoneStatePermissionGranted,
                        onToggle = {
                            if (!isPhoneStatePermissionGranted) {
                                phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            Text(
                "Screen & App Security",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PermissionRow(
                        title = "Overlay Permission",
                        description = "Required to show scam warnings over other apps.",
                        isGranted = isOverlayPermissionGranted,
                        onToggle = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                            context.startActivity(intent)
                        }
                    )
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        PermissionRow(
                            title = "Usage Access",
                            description = "Required to detect when banking apps are opened to prevent digital arrest scams.",
                            isGranted = isUsageStatsEnabled,
                            onToggle = {
                                if (!isUsageStatsEnabled) {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }

            Text(
                "System & Background",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PermissionRow(
                        title = "Battery Optimization",
                        description = "Prevent the system from killing the background scanner.",
                        isGranted = isBatteryOptimized,
                        onToggle = {
                            if (!isBatteryOptimized) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                                context.startActivity(intent)
                            } else {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )


                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Developed by EdgeGuard AI in collaboration with Indore cyber cell. AI model is trained over the scam patterns observed in the recent complaints in Indore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PermissionRow(title: String, description: String, isGranted: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val greenColor = if (isSystemInDarkTheme()) SuccessGreenDark else SuccessGreen
        Switch(
            checked = isGranted,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = greenColor
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val messages by db.messageDao().getAllMessages().collectAsStateWithLifecycle(initialValue = emptyList())
    val pm = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }

    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isNotificationListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var isContactsPermissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }
    var isPhoneStatePermissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) }
    var isOverlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isBatteryOptimized by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName)) }
    var isCallLogPermissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) }
    var isUsageStatsEnabled by remember { mutableStateOf(hasUsageStatsPermission(context)) }

    val isAllGranted = isAccessibilityEnabled && isNotificationListenerEnabled && isContactsPermissionGranted && isOverlayPermissionGranted && isBatteryOptimized && isPhoneStatePermissionGranted && isCallLogPermissionGranted && isUsageStatsEnabled

    var searchQuery by remember { mutableStateOf("") }
    val filteredMessages = remember(searchQuery, messages) {
        if (searchQuery.isBlank()) {
            messages
        } else {
            messages.filter {
                it.sender.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
                isNotificationListenerEnabled = isNotificationListenerEnabled(context)
                isContactsPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                isPhoneStatePermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                isOverlayPermissionGranted = Settings.canDrawOverlays(context)
                isBatteryOptimized = pm.isIgnoringBatteryOptimizations(context.packageName)
                isCallLogPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {}
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp)
        ) {
            // Header
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EdgeGuard AI", fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAllGranted && messages.isNotEmpty()) {
                            val scope = rememberCoroutineScope()
                            IconButton(onClick = { scope.launch { db.messageDao().deleteAll() } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete All")
                            }
                        }
                        IconButton(onClick = { navController.navigate("permissions") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
                Text(
                    "AI based scam detector", 
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (isAllGranted && (messages.isNotEmpty() || searchQuery.isNotEmpty())) {
                    Spacer(modifier = Modifier.height(16.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (searchQuery.isEmpty()) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (!isAllGranted) {
                // Blocked State
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Protection Disabled",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Protection Disabled",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "All permissions are required for protection.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { navController.navigate("permissions") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Activate Protection", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Messages List
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No messages scanned yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Unknown messages will appear here.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if (filteredMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No messages match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredMessages) { message ->
                            val scope = rememberCoroutineScope()
                            MessageCard(
                                message = message,
                                onDelete = {
                                    scope.launch { db.messageDao().deleteMessage(message) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageCard(message: MessageEntity, onDelete: () -> Unit) {
    val statusColor = when {
        message.analysisResult.contains("Scam") || message.analysisResult.contains("Alert") -> ErrorRed
        message.analysisResult == "Safe" -> SuccessGreen
        else -> MaterialTheme.colorScheme.outline
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(message.timestamp) { dateFormat.format(Date(message.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message.sender,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        message.analysisResult.uppercase(),
                        color = statusColor,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).padding(start = 8.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message.appName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(message.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text("Terms and Conditions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Welcome to EdgeGuard AI. By using our app, you agree to the following terms:\n\n" +
                "1. Acceptance of Terms\nBy accessing and using this application, you accept and agree to be bound by the terms and provision of this agreement.\n\n" +
                "2. Description of Service\nEdgeGuard AI provides local on-device scanning of incoming notifications to detect potential scams using artificial intelligence.\n\n" +
                "3. Privacy & Data\nAll message scanning is performed locally on your device. We do not transmit your private messages to any external servers.\n\n" +
                "4. Disclaimer of Warranties\nThe app is provided \"as is\". We do not guarantee that all scams will be detected, nor do we take responsibility for false positives.\n\n" +
                "5. Limitation of Liability\nEdgeGuard AI and its developers shall not be liable for any direct, indirect, incidental, or consequential damages resulting from the use or inability to use the service.",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text("Privacy Policy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your privacy is our primary concern.\n\n" +
                "On-Device Processing:\n" +
                "EdgeGuard AI was built with a strict privacy-first architecture. All machine learning inference and message analysis happens locally on your phone using TensorFlow Lite. We use our highly trained A0908i model for this purpose. Your messages never leave your device.\n\n" +
                "Permissions:\n" +
                "- Notification Access: Required strictly to read incoming messages for analysis.\n" +
                "- Contacts Access: Used locally to determine if a sender is known to you.\n" +
                "- Overlay Permission: Used to display the scam warning immediately over other apps.\n\n" +
                "Data Retention:\n" +
                "Scanned messages are stored in a local encrypted database on your device and are automatically deleted after 12 days to conserve space and maintain privacy.\n\n" +
                "Changes to Privacy Policy:\n" +
                "We reserve the right to modify this privacy policy at any time, so please review it frequently.",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp
            )
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    var accessibilityEnabled = 0
    val service = context.packageName + "/" + MessageReaderAccessibilityService::class.java.canonicalName
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            context.applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Settings.SettingNotFoundException) {
    }
    val mStringColonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    if (accessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            mStringColonSplitter.setString(settingValue)
            while (mStringColonSplitter.hasNext()) {
                val accessibilityService = mStringColonSplitter.next()
                if (accessibilityService.equals(service, ignoreCase = true)) {
                    return true
                }
            }
        }
    }
    return false
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    val packageName = context.packageName
    return enabledListeners?.contains(packageName) == true
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun WelcomeScreen(navController: NavController, sharedPref: SharedPreferences) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Security, 
                contentDescription = "Security Icon", 
                modifier = Modifier.size(80.dp), 
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("EdgeGuard AI", fontWeight = FontWeight.Bold, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Welcome to EdgeGuard AI! Our on-device AI scans incoming notifications to detect scams, protecting you entirely locally. Your messages never leave your phone.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Terms & Conditions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { navController.navigate("terms") }
                )
                Text(
                    text = "  |  ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { navController.navigate("privacy") }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    sharedPref.edit { putBoolean("has_accepted_tc", true) }
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("I Accept T&C", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


