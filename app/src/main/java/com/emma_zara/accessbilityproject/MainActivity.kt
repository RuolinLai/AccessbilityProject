package com.emma_zara.accessbilityproject

import com.emma_zara.accessbilityproject.ui.theme.LocalDarkTheme
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast
import androidx.compose.foundation.BorderStroke
import com.emma_zara.accessbilityproject.ui.components.LocalBottomBarSpace
import androidx.compose.runtime.CompositionLocalProvider
import com.emma_zara.accessbilityproject.ui.theme.LocalGlass
import com.emma_zara.accessbilityproject.ui.components.panelBorder
import com.emma_zara.accessbilityproject.ui.components.AppBackground
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.emma_zara.accessbilityproject.closet.BackgroundRemover
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.SharedItem
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.settings.AppSettings
import com.emma_zara.accessbilityproject.settings.AppTheme
import com.emma_zara.accessbilityproject.settings.SettingsStore
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.screens.IdentifyScreen
import com.emma_zara.accessbilityproject.ui.screens.PickedPhoto
import com.emma_zara.accessbilityproject.ui.screens.RecommendScreen
import com.emma_zara.accessbilityproject.weather.WeatherRepository
import com.emma_zara.accessbilityproject.ui.screens.SettingsScreen
import com.emma_zara.accessbilityproject.ui.screens.WheelScreen
import com.emma_zara.accessbilityproject.ui.screens.closet.ClosetTab
import com.emma_zara.accessbilityproject.ui.theme.AccessbilityProjectTheme
import com.emma_zara.accessbilityproject.ui.theme.LocalMotion

class MainActivity : ComponentActivity() {
    /** A product link or picture shared from another app, waiting to be opened in the shopping check. */
    private var shared by mutableStateOf<SharedItem?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsStore = SettingsStore(this)
        // Only on the first start: after the screen rotates it was already handled
        if (savedInstanceState == null) shared = SharedItem.from(intent)

        setContent {
            var settings by remember { mutableStateOf(settingsStore.load()) }

            // Make the status bar icons dark on the light theme and light on dark themes
            LaunchedEffect(settings.theme) {
                val style = if (settings.theme == AppTheme.LIGHT || settings.theme == AppTheme.GLASS) {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            AccessbilityProjectTheme(settings) {
                App(
                    settings = settings,
                    onSettingsChange = {
                        settings = it
                        settingsStore.save(it)
                    },
                    shared = shared,
                    onSharedHandled = { shared = null },
                )
            }
        }
    }

    // The app is already open and something new is shared to it
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SharedItem.from(intent)?.let { shared = it }
    }
}

/** The tabs at the bottom of the screen. Add new features (like the Closet) here later. */
enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    IDENTIFY("Identify", R.drawable.ic_camera),
    CLOSET("Closet", R.drawable.ic_closet),
    OUTFITS("Outfits", R.drawable.ic_outfit),
    WHEEL("Color wheel", R.drawable.ic_palette),
    SETTINGS("Settings", R.drawable.ic_settings),
}

@Composable
fun App(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    shared: SharedItem? = null,
    onSharedHandled: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(Tab.IDENTIFY) }
    // Something shared from another app goes to the shopping check in the Closet tab
    LaunchedEffect(shared) { if (shared != null) tab = Tab.CLOSET }
    // The color currently being looked at, shared between the Identify and Color wheel tabs
    var color by remember { mutableStateOf(RgbColor(0x1E, 0x3A, 0x8A)) }
    // The camera starts off and only runs after the user turns it on (kept when switching tabs)
    var cameraOn by rememberSaveable { mutableStateOf(false) }
    // A photo chosen from the gallery on the Identify tab (kept when switching tabs)
    var identifyPhoto by remember { mutableStateOf<PickedPhoto?>(null) }
    // The digital closet, saved on the phone
    val context = LocalContext.current
    val closet = remember { ClosetRepository(context.applicationContext) }
    // Start downloading the "background remover" right away, so it's ready by the time it's needed
    LaunchedEffect(Unit) { BackgroundRemover.prepare(context) }
    // Today's weather for outfit recommendations
    val weather = remember { WeatherRepository(context.applicationContext) }
    val motion = LocalMotion.current

    val glass = LocalGlass.current
    // In the glass theme the chosen tab is a bright glass bubble inside the glass bar
    val itemColors = if (glass) {
        NavigationBarItemDefaults.colors(
            indicatorColor = Color(0x2E2E3248),   // a soft slate bubble behind the chosen tab
            selectedIconColor = MaterialTheme.colorScheme.onSurface,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        NavigationBarItemDefaults.colors()
    }
    val tabItems: @Composable RowScope.() -> Unit = {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        colors = itemColors,
                        icon = {
                            // The chosen tab's icon does a little bounce
                            val scale by animateFloatAsState(
                                if (tab == t) 1.2f else 1f,
                                animationSpec = if (motion) spring(dampingRatio = 0.4f, stiffness = 500f) else snap(),
                                label = "tabIcon",
                            )
                            Icon(
                                painterResource(t.icon),
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                            )
                        },
                        label = { AText(t.label, maxLines = 1) },
                    )
                }
    }

    AppBackground {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,   // the background is drawn by AppBackground
        // With a see-through container, text needs to be told its color (or it would be black on every theme)
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            // A long rounded bar floating above the bottom of the screen, in every theme.
            // Only the glass theme's bar is see-through; the others are solid in the theme's color.
            val highContrast = LocalHighContrast.current
            val dark = LocalDarkTheme.current
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    // Glass: mostly white so the tab names stay easy to read over the page scrolling behind it
                    color = if (glass) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surfaceContainer,
                    // A soft shadow lifts the solid bar off the page. Not for glass (a shadow shows through
                    // see-through glass as a gray box), dark (it can't be seen) or high contrast (it has a border).
                    shadowElevation = if (glass || dark || highContrast) 0.dp else 6.dp,
                    border = panelBorder() ?: if (dark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = tabItems,
                    )
                }
            }
        },
    ) { innerPadding ->
        // Pages go on under the floating tab bar (the space beside and below it shows the page),
        // and each page leaves room at its end so nothing stays hidden behind the bar
        val bottomSpace = innerPadding.calculateBottomPadding()
        val pagePadding = Modifier.padding(top = innerPadding.calculateTopPadding())
        CompositionLocalProvider(LocalBottomBarSpace provides bottomSpace) {
        // Screens fade and rise in when you switch tabs
        AnimatedContent(
            targetState = tab,
            modifier = pagePadding,
            transitionSpec = {
                if (motion) {
                    (fadeIn(tween(250, delayMillis = 60)) + slideInVertically(tween(300)) { it / 20 }) togetherWith
                        fadeOut(tween(120))
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "tabs",
        ) { shown ->
            when (shown) {
                Tab.IDENTIFY -> IdentifyScreen(
                    color = color,
                    onColorChange = { color = it },
                    onOpenInWheel = { tab = Tab.WHEEL },
                    cameraOn = cameraOn,
                    onCameraOnChange = { cameraOn = it },
                    photo = identifyPhoto,
                    onPhotoChange = { identifyPhoto = it },
                )
                Tab.CLOSET -> ClosetTab(closet, incoming = shared, onIncomingHandled = onSharedHandled)
                Tab.OUTFITS -> RecommendScreen(closet, weather, onGoToCloset = { tab = Tab.CLOSET })
                Tab.WHEEL -> WheelScreen(color = color, onColorChange = { color = it })
                Tab.SETTINGS -> SettingsScreen(settings = settings, onChange = onSettingsChange)
            }
        }
        }
    }
    }
}
