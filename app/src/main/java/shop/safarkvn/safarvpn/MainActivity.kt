package shop.safarkvn.safarvpn

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import shop.safarkvn.safarvpn.ui.ProfilesTab
import shop.safarkvn.safarvpn.ui.LogsTab
import shop.safarkvn.safarvpn.ui.SettingsTab
import shop.safarkvn.safarvpn.ui.DeployTab
import shop.safarkvn.safarvpn.ui.ExceptionsTab
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // VPN permission dialog finished
    }

    private val batteryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        checkAndRequestVpn()
    }

    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        checkAndRequestBattery()
    }

    companion object {
        var activeActivities = 0
        var isForeground: Boolean
            get() = activeActivities > 0
            set(value) {}

        // Статическая ссылка на текущую Activity
        var currentActivity: MainActivity? = null

        // URI файла .qwdtt, ожидающего импорта
        val pendingFileUri = mutableStateOf<android.net.Uri?>(null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                pendingFileUri.value = uri
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activeActivities++
        currentActivity = this
        ManlCaptchaWebViewManager.checkAndShowPendingCaptcha(this)
        VkAuthWebViewManager.checkAndShowPendingAuth(this)
    }

    override fun onStop() {
        super.onStop()
        activeActivities--
        if (currentActivity == this) {
            currentActivity = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestNotifications()

        handleIncomingIntent(intent)

        setContent {
            val settingsStore = remember { SettingsStore(this) }
            val themeMode by settingsStore.themeMode.collectAsStateWithLifecycle(initialValue = "system")
            val isDynamicColor by settingsStore.isDynamicColor.collectAsStateWithLifecycle(initialValue = false)
            val themePalette by settingsStore.themePalette.collectAsStateWithLifecycle(initialValue = "indigo")
            val scope = rememberCoroutineScope()

            WDTTTheme(themeMode = themeMode, dynamicColor = isDynamicColor, themePalette = themePalette) {
                MainScreen(
                    settingsStore = settingsStore,
                    themeMode = themeMode,
                    onThemeChange = { mode ->
                        scope.launch {
                            settingsStore.saveThemeMode(mode)
                        }
                    },
                    isDynamicColor = isDynamicColor,
                    onDynamicColorChange = { enabled ->
                        scope.launch { settingsStore.saveDynamicColor(enabled) }
                    },
                    currentPalette = themePalette,
                    onPaletteChange = { palette ->
                        scope.launch { settingsStore.saveThemePalette(palette) }
                    }
                )
            }
        }
    }

    private fun checkAndRequestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkAndRequestBattery()
            }
        } else {
            checkAndRequestBattery()
        }
    }

    private fun checkAndRequestBattery() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                batteryLauncher.launch(intent)
            } catch (e: Exception) {
                checkAndRequestVpn()
            }
        } else {
            checkAndRequestVpn()
        }
    }

    private fun checkAndRequestVpn() {
        try {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnLauncher.launch(vpnIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// ═══ Навигация ═══

private enum class MainTab {
    TUNNEL,
    DEPLOY,
    PROFILES,
    EXCEPTIONS,
    LOGS
}

private data class NavItem(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val allNavItems = listOf(
    NavItem(MainTab.TUNNEL, "Туннель", Icons.Filled.VpnKey, Icons.Outlined.VpnKey),
    NavItem(MainTab.DEPLOY, "Деплой", Icons.Filled.Cloud, Icons.Outlined.Cloud),
    NavItem(MainTab.PROFILES, "Профили", Icons.Filled.FolderOpen, Icons.Outlined.Folder),
    NavItem(MainTab.EXCEPTIONS, "Обход", Icons.Filled.FilterList, Icons.Outlined.FilterList),
    NavItem(MainTab.LOGS, "Логи", Icons.Filled.Terminal, Icons.Outlined.Terminal),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settingsStore: SettingsStore,
    themeMode: String = "system",
    onThemeChange: (String) -> Unit = {},
    isDynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
    currentPalette: String = "indigo",
    onPaletteChange: (String) -> Unit = {}
) {
    val unreadErrors by TunnelManager.unreadErrorCount.collectAsStateWithLifecycle()
    val tunnelRunning by TunnelManager.running.collectAsStateWithLifecycle()
    val showBlockerWarning by TunnelManager.showBlockerWarning.collectAsStateWithLifecycle()
    val hasSeenWelcomeDialog by settingsStore.hasSeenWelcomeDialog.collectAsStateWithLifecycle(initialValue = true)
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.TUNNEL.name) }
    var adminModeEnabled by rememberSaveable { mutableStateOf(false) }
    val navItems = remember(adminModeEnabled) {
        if (adminModeEnabled) allNavItems else allNavItems.filterNot { it.tab == MainTab.DEPLOY }
    }
    val selectedTab = runCatching { MainTab.valueOf(selectedTabName) }.getOrDefault(MainTab.TUNNEL)
    val selectedTabIndex = navItems.indexOfFirst { it.tab == selectedTab }.takeIf { it >= 0 } ?: 0
    var dragTargetIndex by remember { mutableIntStateOf(-1) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val safeBottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + 96.dp

    LaunchedEffect(selectedTab) {
        if (selectedTab == MainTab.LOGS) TunnelManager.clearUnreadErrors()
    }

    LaunchedEffect(adminModeEnabled, selectedTab) {
        if (!adminModeEnabled && selectedTab == MainTab.DEPLOY) {
            selectedTabName = MainTab.TUNNEL.name
        }
    }

    val pendingFileUri = MainActivity.pendingFileUri.value
    LaunchedEffect(pendingFileUri) {
        if (pendingFileUri != null) {
            selectedTabName = MainTab.PROFILES.name
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackdrop(modifier = Modifier.matchParentSize())

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            containerColor = Color.Transparent,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .pointerInput(selectedTabIndex, navItems.size) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalDrag = 0f
                                dragTargetIndex = -1
                                dragProgress = 0f
                            },
                            onDragCancel = {
                                dragTargetIndex = -1
                                dragProgress = 0f
                            },
                            onDragEnd = {
                                if (dragTargetIndex in navItems.indices && dragProgress >= 0.5f) {
                                    val targetTab = navItems[dragTargetIndex].tab
                                    selectedTabName = targetTab.name
                                    if (targetTab == MainTab.LOGS) TunnelManager.clearUnreadErrors()
                                }
                                dragTargetIndex = -1
                                dragProgress = 0f
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                            if (abs(totalDrag) < 12f) {
                                dragTargetIndex = -1
                                dragProgress = 0f
                                return@detectHorizontalDragGestures
                            }

                            val candidate = if (totalDrag < 0f) selectedTabIndex + 1 else selectedTabIndex - 1
                            if (candidate !in navItems.indices) {
                                dragTargetIndex = -1
                                dragProgress = 0f
                                return@detectHorizontalDragGestures
                            }

                            dragTargetIndex = candidate
                            dragProgress = (abs(totalDrag) / 180f).coerceIn(0f, 1f)
                        }
                    }
            ) {
                androidx.compose.animation.Crossfade(
                    targetState = selectedTab,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = navOverlayReserve),
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        MainTab.TUNNEL -> SettingsTab(
                            themeMode = themeMode,
                            onThemeChange = onThemeChange,
                            isDynamicColor = isDynamicColor,
                            onDynamicColorChange = onDynamicColorChange,
                            currentPalette = currentPalette,
                            onPaletteChange = onPaletteChange,
                            onNavigateToLogs = { selectedTabName = MainTab.LOGS.name },
                            adminModeEnabled = adminModeEnabled,
                            onAdminModeChange = { adminModeEnabled = it }
                        )
                        MainTab.DEPLOY -> DeployTab()
                        MainTab.PROFILES -> ProfilesTab(
                            onProfileApplied = { selectedTabName = MainTab.TUNNEL.name },
                            importFileUri = MainActivity.pendingFileUri.value,
                            onImportHandled = { MainActivity.pendingFileUri.value = null }
                        )
                        MainTab.EXCEPTIONS -> ExceptionsTab()
                        MainTab.LOGS -> LogsTab()
                    }
                }

                ProxyNavigationBar(
                    navItems = navItems,
                    selectedTab = selectedTabIndex,
                    dragTargetIndex = dragTargetIndex,
                    dragProgress = dragProgress,
                    unreadErrors = unreadErrors,
                    tunnelRunning = tunnelRunning,
                    onTabSelected = { index ->
                        val targetTab = navItems.getOrNull(index)?.tab ?: return@ProxyNavigationBar
                        if (selectedTab != targetTab) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            selectedTabName = targetTab.name
                            if (targetTab == MainTab.LOGS) TunnelManager.clearUnreadErrors()
                        }
                        dragTargetIndex = -1
                        dragProgress = 0f
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

    }

    if (!hasSeenWelcomeDialog) {
        AlertDialog(
            onDismissRequest = { 
                scope.launch { settingsStore.saveHasSeenWelcomeDialog(true) }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Готовые профили",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Подписка покупается и продлевается в Telegram-боте SafarVPN:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/safarvpn_bot"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("@safarvpn_bot", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Обновления приложения приходят через Telegram-бот @safarvpn_bot.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Скопируйте JSON-ссылку подписки из бота и импортируйте ее на вкладке «Профили». Эта памятка также доступна в настройках.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { scope.launch { settingsStore.saveHasSeenWelcomeDialog(true) } }
                ) {
                    Text("Понятно")
                }
            }
        )
    }

    if (showBlockerWarning) {
        var dontShowAgain by rememberSaveable { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { TunnelManager.showBlockerWarning.value = false },
            title = { Text("Внимание", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Не используйте приложение, если белые списки не включены, так как это негативно влияет на способ обхода.")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dontShowAgain = !dontShowAgain }
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Больше не показывать", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        TunnelManager.showBlockerWarning.value = false
                        scope.launch {
                            settingsStore.saveHideBlockerWarning(dontShowAgain)
                        }
                        context.startService(Intent(context, TunnelService::class.java).apply {
                            action = "START_FORCED"
                        })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Всё равно подключиться", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { TunnelManager.showBlockerWarning.value = false }) {
                    Text("Отмена")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ProxyNavigationBar(
    navItems: List<NavItem>,
    selectedTab: Int,
    dragTargetIndex: Int,
    dragProgress: Float,
    unreadErrors: Int,
    tunnelRunning: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.22f
    val selectedColor = colors.primary
    val unselectedColor = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val shellColor = if (isDark) {
        colors.surface.copy(alpha = 0.78f)
    } else {
        lerp(colors.surface, colors.surfaceVariant, 0.48f).copy(alpha = 0.95f)
    }
    val shellBorder = if (isDark) {
        colors.outlineVariant.copy(alpha = 0.42f)
    } else {
        colors.outline.copy(alpha = 0.16f)
    }
    val indicatorColor = if (isDark) {
        colors.primaryContainer.copy(alpha = 0.84f)
    } else {
        lerp(colors.primaryContainer, colors.surface, 0.18f).copy(alpha = 0.97f)
    }
    val indicatorIndex = remember { Animatable(selectedTab.toFloat()) }
    val dragVisualIndex = indicatorIndex.value

    LaunchedEffect(selectedTab) {
        if (dragTargetIndex !in navItems.indices) {
            indicatorIndex.animateTo(
                targetValue = selectedTab.toFloat(),
                animationSpec = tween(
                    durationMillis = 720,
                    easing = CubicBezierEasing(0.2f, 0.9f, 0.24f, 1f)
                )
            )
        }
    }

    LaunchedEffect(selectedTab, dragTargetIndex, dragProgress) {
        if (dragTargetIndex in navItems.indices) {
            val target = selectedTab.toFloat() + (dragTargetIndex - selectedTab) * dragProgress
            indicatorIndex.snapTo(target)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val trackPadding = 8.dp
        val itemWidth = (maxWidth - trackPadding * 2) / navItems.size
        val indicatorOffset = trackPadding + itemWidth * dragVisualIndex

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = shellColor,
            border = BorderStroke(1.dp, shellBorder),
            tonalElevation = 0.dp,
            shadowElevation = if (isDark) 10.dp else 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = indicatorColor,
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
                        .padding(vertical = 6.dp)
                        .width(itemWidth)
                        .fillMaxHeight()
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = trackPadding, vertical = 6.dp)
                ) {
                    navItems.forEachIndexed { index, item ->
                        val emphasis = (1f - abs(index - dragVisualIndex)).coerceIn(0f, 1f)
                        val iconColor = lerp(unselectedColor, selectedColor, emphasis)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onTabSelected(index) },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = if (emphasis > 0.55f) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp),
                                    tint = iconColor
                                )
                                if (item.tab == MainTab.LOGS && unreadErrors > 0) {
                                    Badge(
                                        containerColor = if (tunnelRunning) colors.primary else WDTTColors.warning,
                                        contentColor = colors.onPrimary,
                                        modifier = Modifier.offset(x = 12.dp, y = (-8).dp)
                                    ) {
                                        Text("$unreadErrors")
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.1.sp
                                ),
                                fontWeight = if (emphasis > 0.55f) FontWeight.SemiBold else FontWeight.Normal,
                                color = iconColor.copy(alpha = if (emphasis > 0.4f) 1f else 0.5f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.22f
    val baseBrush = remember(colors.background, colors.surface, colors.surfaceVariant) {
        Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    lerp(colors.background, colors.surface, 0.18f),
                    colors.background,
                    lerp(colors.surfaceVariant, colors.background, 0.72f)
                )
            } else {
                listOf(
                    lerp(colors.background, colors.surface, 0.78f),
                    colors.background,
                    lerp(colors.surfaceVariant, colors.background, 0.30f)
                )
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBrush)
    )
}
