package com.emma_zara.accessbilityproject.ui.screens

import com.emma_zara.accessbilityproject.ui.components.LocalBottomBarSpace
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.Occasion
import com.emma_zara.accessbilityproject.closet.fitReasons
import com.emma_zara.accessbilityproject.closet.recommendOutfits
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.ExpandableCard
import com.emma_zara.accessbilityproject.ui.components.ScreenTitle
import com.emma_zara.accessbilityproject.ui.components.SectionCard
import com.emma_zara.accessbilityproject.ui.components.popIn
import com.emma_zara.accessbilityproject.ui.components.rememberSpeaker
import com.emma_zara.accessbilityproject.ui.screens.closet.OutfitCard
import com.emma_zara.accessbilityproject.ui.screens.closet.SuggestFromChips
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast
import com.emma_zara.accessbilityproject.ui.theme.LocalInvertedColors
import com.emma_zara.accessbilityproject.weather.Place
import com.emma_zara.accessbilityproject.weather.Weather
import com.emma_zara.accessbilityproject.weather.WeatherRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** An icon for each occasion. Screen readers skip it and read the label instead. */
private fun Occasion?.icon() = when (this) {
    null -> R.drawable.ic_occasion_any
    Occasion.SCHOOL -> R.drawable.ic_occasion_school
    Occasion.FORMAL -> R.drawable.ic_occasion_formal
    Occasion.SPORT -> R.drawable.ic_occasion_sport
    Occasion.HOME -> R.drawable.ic_occasion_home
}

/** An icon for the weather. */
private fun Weather.icon() = when {
    code >= 95 -> R.drawable.ic_weather_storm
    snowy -> R.drawable.ic_weather_snow
    rainy -> R.drawable.ic_weather_rain
    code in 45..48 -> R.drawable.ic_weather_fog
    code == 3 -> R.drawable.ic_weather_cloud
    code == 2 -> R.drawable.ic_weather_partly
    !isDay -> R.drawable.ic_weather_moon
    else -> R.drawable.ic_weather_sun
}

/**
 * Outfit recommendations for where you're going (school, formal, sport) and today's weather.
 * The weather comes from Open-Meteo for a place the user chooses (their location or a city).
 */
@Composable
fun RecommendScreen(closet: ClosetRepository, weatherRepo: WeatherRepository, onGoToCloset: () -> Unit) {
    var occasion by rememberSaveable { mutableStateOf<Occasion?>(null) }
    var useWeather by rememberSaveable { mutableStateOf(true) }
    val place = weatherRepo.place

    // Get the weather when the screen opens or the place changes (it's kept for 15 minutes)
    LaunchedEffect(place) { weatherRepo.refresh() }

    val weather = if (useWeather) weatherRepo.weather else null
    val outfits = remember(closet.items, closet.suggestFrom, occasion, weather) {
        recommendOutfits(closet.items, closet.suggestFrom, occasion, weather)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalBottomBarSpace.current)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Outfits for today")

        WeatherCard(weatherRepo, useWeather) { useWeather = it }

        OccasionPicker(occasion) { occasion = it }

        if (closet.items.isEmpty()) {
            SectionCard(title = "Your closet is empty") {
                AText("Add some clothes to your closet first, then come back for outfit ideas.", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onGoToCloset, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { AText("Go to my closet") }
            }
            return@Column
        }

        // Heading for the results, with the categories to choose from folded away
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            AText(
                when (outfits.size) {
                    0 -> "No ideas yet"
                    1 -> "1 idea for you"
                    else -> "${outfits.size} ideas for you"
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        ExpandableCard(
            title = "Categories",
            summary = if (closet.suggestFrom.size == 7) "Using everything in your closet" else
                closet.suggestFrom.sortedBy { it.ordinal }.joinToString { it.label }.ifEmpty { "None chosen" },
        ) { SuggestFromChips(closet) }

        if (closet.items.none { it.occasions.isNotEmpty() || it.warmth != null }) {
            TipBanner(
                "Tip: open a piece in your closet, then \"Edit details\" to say when to wear it (like School or Cold days). " +
                    "The ideas get better.",
                action = "Go to my closet",
                onAction = onGoToCloset,
            )
        }

        if (outfits.isEmpty()) {
            SectionCard {
                AText(
                    buildString {
                        append("There aren't enough pieces that go together")
                        if (occasion != null) append(" for ${occasion!!.label.lowercase()}")
                        if (weather != null) append(" in this weather")
                        append(". An outfit needs a top with pants or a skirt, or a dress, in colors that match. ")
                        append("Try \"Anywhere\", turn off the weather, or mark more clothes for this occasion.")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            outfits.forEachIndexed { i, outfit ->
                OutfitCard(
                    closet,
                    outfit,
                    title = "Outfit ${i + 1}",
                    modifier = Modifier.popIn(i),
                    extraReasons = outfit.fitReasons(occasion, weather),
                )
            }
        }
        Spacer(Modifier.size(8.dp))
    }
}

/** Four big tiles: Anywhere, School, Formal, Sport. Only one can be chosen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OccasionPicker(selected: Occasion?, onSelect: (Occasion?) -> Unit) {
    val highContrast = LocalHighContrast.current
    // Three tiles per row (two with big text, so the words still fit)
    val perRow = if (MaterialTheme.typography.labelLarge.fontSize.value > 18f) 2 else 3
    val choices = listOf<Occasion?>(null) + Occasion.entries
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AText("Where are you going?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = perRow,
            modifier = Modifier.fillMaxWidth().selectableGroup(),
        ) {
            choices.forEach { o ->
                val on = o == selected
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = when {
                        on -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        highContrast -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                        else -> null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .selectable(selected = on, role = Role.RadioButton) { onSelect(o) },
                ) {
                    Column(
                        Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painterResource(o.icon()),
                            contentDescription = null,
                            tint = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.size(4.dp))
                        AText(
                            o?.label ?: "Anywhere",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
            // Fill the last row with empty space so every tile is the same size
            val left = (perRow - choices.size % perRow) % perRow
            repeat(left) { Spacer(Modifier.weight(1f)) }
        }
        selected?.let {
            AText(
                it.tip,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** A small highlighted note with one button. */
@Composable
private fun TipBanner(text: String, action: String, onAction: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = if (LocalHighContrast.current) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            AText(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) { AText(action) }
        }
    }
}

/** Today's weather and what to wear for it, or a way to choose the place to get the weather for. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherCard(repo: WeatherRepository, useWeather: Boolean, onUseWeatherChange: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val speaker = rememberSpeaker()
    var choosing by remember { mutableStateOf(false) }
    val place = repo.place

    // The card uses the opposite theme so it stands out: dark on the light theme, light on the dark theme.
    // Everything inside (text, icons, buttons, the switch) follows automatically.
    MaterialTheme(colorScheme = LocalInvertedColors.current) {
    SectionCard {
        // Title with the on/off switch
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .toggleable(value = useWeather, role = Role.Switch, onValueChange = onUseWeatherChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                AText("Today's weather", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                if (!useWeather) {
                    AText("Off: outfits are for any weather", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = useWeather, onCheckedChange = null)
        }
        if (!useWeather) return@SectionCard

        if (place == null || choosing) {
            PlaceChooser(repo, canCancel = place != null) { choosing = false }
            return@SectionCard
        }

        val weather = repo.weather
        when {
            repo.loading && weather == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                AText("Getting the weather for ${place.name}…", style = MaterialTheme.typography.bodyLarge)
            }
            repo.error != null && weather == null -> {
                AText(repo.error ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { scope.launch { repo.refresh(force = true) } }) { AText("Try again") }
            }
            weather != null -> {
                val tips = weather.tips()
                // Big temperature, like a weather app
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                ) {
                    Icon(
                        painterResource(weather.icon()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        AText(
                            "${weather.temperature.roundToInt()}°C",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        AText(
                            buildString {
                                append(weather.description)
                                val f = weather.feelsLike.roundToInt()
                                if (f != weather.temperature.roundToInt()) append(" · feels like $f°")
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        AText(place.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // What to wear, as a list on a soft background
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tips.forEach { tip ->
                            // "Cold: wear warm pieces..." -> bold "Cold", then the advice
                            val label = tip.substringBefore(":", "")
                            val advice = tip.substringAfter(": ").replaceFirstChar { it.uppercase() }
                            Column {
                                if (label.isNotEmpty()) {
                                    AText(label, style = MaterialTheme.typography.titleSmall)
                                }
                                AText(advice, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    TextButton(onClick = { speaker.speak("${place.name}. ${weather.summary()} ${tips.joinToString(" ")}") }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(painterResource(R.drawable.ic_volume), contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        AText("Read aloud")
                    }
                    TextButton(onClick = { scope.launch { repo.refresh(force = true) } }, modifier = Modifier.heightIn(min = 48.dp)) { AText("Refresh") }
                    TextButton(onClick = { choosing = true }, modifier = Modifier.heightIn(min = 48.dp)) { AText("Change place") }
                }
                AText("Weather from Open-Meteo.com", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    }
}

/** Use the phone's location, or search for a city by name. */
@Composable
private fun PlaceChooser(repo: WeatherRepository, canCancel: Boolean, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Place>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }

    fun choose(p: Place) {
        repo.choosePlace(p)
        onDone()
    }

    fun locate() {
        busy = true
        problem = null
        scope.launch {
            val p = repo.currentLocation()
            busy = false
            if (p == null) {
                problem = "Couldn't find your location. Turn on Location in your phone's settings, or search for your city."
                searching = true
            } else {
                choose(p)
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            locate()
        } else {
            problem = "Without the location permission, search for your city instead."
            searching = true
        }
    }

    fun search() {
        if (query.isBlank()) return
        busy = true
        problem = null
        scope.launch {
            try {
                results = repo.searchPlaces(query)
                if (results.isNullOrEmpty()) problem = "No places called \"${query.trim()}\". Check the spelling."
            } catch (e: Exception) {
                problem = "Couldn't search. Check the internet connection and try again."
            }
            busy = false
        }
    }

    AText("Get outfits that suit today's weather where you are.", style = MaterialTheme.typography.bodyLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { if (repo.hasLocationPermission()) locate() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            enabled = !busy,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        ) { AText("Use my location", textAlign = TextAlign.Center) }
        OutlinedButton(
            onClick = { searching = !searching },
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        ) { AText("Search a city", textAlign = TextAlign.Center) }
    }
    AText(
        "Only a rough location (about 1 km) is sent to Open-Meteo, a free weather service.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (searching) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { AText("City") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = ::search, enabled = !busy && query.isNotBlank(), modifier = Modifier.heightIn(min = 56.dp)) {
                AText("Search")
            }
        }
    }

    if (busy) CircularProgressIndicator(Modifier.size(24.dp))
    problem?.let {
        AText(
            it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    results?.takeIf { it.isNotEmpty() && searching }?.let { list ->
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column {
                list.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AText(
                        p.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable(role = Role.Button, onClickLabel = "Use this place") { choose(p) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
    if (canCancel) TextButton(onClick = onDone) { AText("Cancel") }
}
