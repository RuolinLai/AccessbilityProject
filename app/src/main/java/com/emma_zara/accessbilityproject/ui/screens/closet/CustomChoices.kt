package com.emma_zara.accessbilityproject.ui.screens.closet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.CustomPattern
import com.emma_zara.accessbilityproject.closet.CustomType
import com.emma_zara.accessbilityproject.closet.Pattern
import com.emma_zara.accessbilityproject.closet.role
import com.emma_zara.accessbilityproject.ui.components.AText

/** A chip with a check mark when chosen, so it doesn't depend on seeing colors. */
@Composable
private fun ChoiceChip(label: String, selected: Boolean, description: String? = null, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { AText(label) },
        leadingIcon = if (selected) {
            { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        modifier = if (description != null) Modifier.semantics { stateDescription = description } else Modifier,
    )
}

/** The "+ Your own ..." chip. */
@Composable
private fun AddOwnChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { AText(label) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_add), null, Modifier.size(FilterChipDefaults.IconSize)) },
    )
}

/**
 * Choose the type of item: one of the built-in categories, or one of your own (like "Jacket").
 * [typeName] is the chosen custom type, or null for a built-in one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypeChips(repo: ClosetRepository, category: Category?, typeName: String?, onSelect: (Category, String?) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var managing by remember { mutableStateOf(false) }

    // If the chosen custom type was just removed, fall back to what it worked like
    LaunchedEffect(repo.customTypes) {
        if (typeName != null && repo.customTypes.none { it.name == typeName } && category != null) onSelect(category, null)
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Category.entries.forEach { c ->
            ChoiceChip(c.label, selected = category == c && typeName == null) { onSelect(c, null) }
        }
        repo.customTypes.forEach { t ->
            ChoiceChip(t.name, selected = typeName == t.name, description = "Your own type, works like ${t.worksLike.role}") {
                onSelect(t.worksLike, t.name)
            }
        }
        AddOwnChip("Your own type") { adding = true }
    }
    if (repo.customTypes.isNotEmpty()) {
        TextButton(onClick = { managing = true }) { AText("Remove your own types") }
    }

    if (adding) {
        NewTypeDialog(repo, onDismiss = { adding = false }) {
            adding = false
            onSelect(it.worksLike, it.name)
        }
    }
    if (managing) {
        RemoveDialog(
            title = "Your own types",
            note = "Clothes of a removed type stay in your closet as what it worked like.",
            names = repo.customTypes.map { it.name to "Works like ${it.worksLike.role}" },
            onRemove = { name -> repo.customTypes.firstOrNull { it.name == name }?.let(repo::deleteCustomType) },
            onDismiss = { managing = false },
        )
    }
}

/**
 * Choose the pattern: one of the built-in ones, or one of your own (like "Floral").
 * [patternName] is the chosen custom pattern, or null for a built-in one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatternChips(repo: ClosetRepository, pattern: Pattern, patternName: String?, onSelect: (Pattern, String?) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var managing by remember { mutableStateOf(false) }

    LaunchedEffect(repo.customPatterns) {
        if (patternName != null && repo.customPatterns.none { it.name == patternName }) onSelect(pattern, null)
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Pattern.entries.forEach { p ->
            ChoiceChip(p.label, selected = pattern == p && patternName == null, description = p.description) { onSelect(p, null) }
        }
        repo.customPatterns.forEach { p ->
            ChoiceChip(p.name, selected = patternName == p.name, description = busyText(p.busy)) {
                onSelect(p.asPattern, p.name)
            }
        }
        AddOwnChip("Your own pattern") { adding = true }
    }
    if (repo.customPatterns.isNotEmpty()) {
        TextButton(onClick = { managing = true }) { AText("Remove your own patterns") }
    }

    if (adding) {
        NewPatternDialog(repo, onDismiss = { adding = false }) {
            adding = false
            onSelect(it.asPattern, it.name)
        }
    }
    if (managing) {
        RemoveDialog(
            title = "Your own patterns",
            note = "Clothes with a removed pattern become \"Print\" (if it was busy) or \"Plain\".",
            names = repo.customPatterns.map { it.name to busyText(it.busy) },
            onRemove = { name -> repo.customPatterns.firstOrNull { it.name == name }?.let(repo::deleteCustomPattern) },
            onDismiss = { managing = false },
        )
    }
}

private fun busyText(busy: Boolean) =
    if (busy) "Busy: wear it with plain pieces" else "Calm: goes with everything, like a plain color"

/** One option in a list of radio buttons. The whole row is tappable. */
@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        AText(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NameField(name: String, onChange: (String) -> Unit, hint: String, problem: String?) {
    val showProblem = name.isNotBlank() && problem != null
    OutlinedTextField(
        value = name,
        onValueChange = onChange,
        label = { AText(hint) },
        singleLine = true,
        isError = showProblem,
        supportingText = if (showProblem) ({ AText(problem!!) }) else null,
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NewTypeDialog(repo: ClosetRepository, onDismiss: () -> Unit, onCreated: (CustomType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var worksLike by remember { mutableStateOf<Category?>(null) }
    val problem = repo.problemWithTypeName(name)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AText("Your own type") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NameField(name, { name = it }, "Name, like Jacket or Hat", problem)
                AText(
                    "Where does it go in an outfit? This helps the app suggest what to wear it with.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Column(Modifier.selectableGroup()) {
                    Category.entries.forEach { c ->
                        RadioRow("It's ${c.role}", selected = worksLike == c) { worksLike = c }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = problem == null && worksLike != null,
                onClick = {
                    val type = CustomType(name.trim(), worksLike!!)
                    repo.addCustomType(type)
                    onCreated(type)
                },
            ) { AText("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AText("Cancel") } },
    )
}

@Composable
private fun NewPatternDialog(repo: ClosetRepository, onDismiss: () -> Unit, onCreated: (CustomPattern) -> Unit) {
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(true) }
    val problem = repo.problemWithPatternName(name)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AText("Your own pattern") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NameField(name, { name = it }, "Name, like Floral or Lace", problem)
                AText(
                    "Is it busy? Busy patterns look best with plain pieces, so the app won't pair two of them.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Column(Modifier.selectableGroup()) {
                    RadioRow(busyText(true), selected = busy) { busy = true }
                    RadioRow(busyText(false), selected = !busy) { busy = false }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = problem == null,
                onClick = {
                    val pattern = CustomPattern(name.trim(), busy)
                    repo.addCustomPattern(pattern)
                    onCreated(pattern)
                },
            ) { AText("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AText("Cancel") } },
    )
}

/** A list of the user's own types or patterns, each with a remove button. */
@Composable
private fun RemoveDialog(
    title: String,
    note: String,
    names: List<Pair<String, String>>,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AText(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AText(note, style = MaterialTheme.typography.bodyMedium)
                if (names.isEmpty()) AText("None left.", style = MaterialTheme.typography.bodyLarge)
                names.forEach { (name, detail) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            AText(name, style = MaterialTheme.typography.titleMedium)
                            AText(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onRemove(name) }, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = "Remove $name")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { AText("Done") } },
    )
}
