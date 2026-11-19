package com.emma_zara.accessbilityproject

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
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
import androidx.compose.ui.res.painterResource
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.settings.AppSettings
import com.emma_zara.accessbilityproject.settings.AppTheme
import com.emma_zara.accessbilityproject.settings.SettingsStore
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.screens.IdentifyScreen
import com.emma_zara.accessbilityproject.ui.screens.SettingsScreen
import com.emma_zara.accessbilityproject.ui.screens.WheelScreen
import com.emma_zara.accessbilityproject.ui.theme.AccessbilityProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsStore = SettingsStore(this)

        setContent {
            var settings by remember { mutableStateOf(settingsStore.load()) }

            // Make the status bar icons dark on the light theme and light on dark themes
            LaunchedEffect(settings.theme) {
                val style = if (settings.theme == AppTheme.LIGHT) {
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
                )
            }
        }
    }
}

/** The tabs at the bottom of the screen. Add new features (like the Closet) here later. */
enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    IDENTIFY("Identify", R.drawable.ic_camera),
    WHEEL("Color wheel", R.drawable.ic_palette),
    SETTINGS("Settings", R.drawable.ic_settings),
}

@Composable
fun App(settings: AppSettings, onSettingsChange: (AppSettings) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.IDENTIFY) }
    // The color currently being looked at, shared between the Identify and Color wheel tabs
    var color by remember { mutableStateOf(RgbColor(0x1E, 0x3A, 0x8A)) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { AText(t.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (tab) {
                Tab.IDENTIFY -> IdentifyScreen(
                    color = color,
                    onColorChange = { color = it },
                    onOpenInWheel = { tab = Tab.WHEEL },
                )
                Tab.WHEEL -> WheelScreen(color = color, onColorChange = { color = it })
                Tab.SETTINGS -> SettingsScreen(settings = settings, onChange = onSettingsChange)
            }
        }
    }
}
