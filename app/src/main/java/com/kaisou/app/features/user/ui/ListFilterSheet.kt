package com.kaisou.app.features.user.ui

// Compose animation and layouts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
// Names and their order
import java.text.Collator
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.KaisouSheet
import com.kaisou.app.core.designsystem.components.LocalSheetClose
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.title.data.ReleaseState
import com.kaisou.app.features.user.data.ListFilter
import com.kaisou.app.features.user.data.TagCategories
import com.kaisou.app.features.user.data.UserListEntry

// The sheet that narrows a list as the search of Dantotsu does: fields, genres, tags by category, cancel and apply
@Composable
fun ListFilterSheet(
    filter: ListFilter,
    entries: List<UserListEntry>,
    manga: Boolean,
    canFilterMine: Boolean,
    onApply: (ListFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val dimens = KaisouTheme.dimens
    var draft by remember { mutableStateOf(filter) }
    val sourceKeys = stringArrayResource(R.array.title_source_keys)
    val sourceNames = stringArrayResource(R.array.title_source_names)
    val formatKeys = stringArrayResource(R.array.format_keys)
    val formatNames = stringArrayResource(R.array.format_names)
    val seasonKeys = stringArrayResource(R.array.title_season_keys)
    val seasonNames = stringArrayResource(R.array.title_season_names)
    val genreKeys = stringArrayResource(R.array.genre_keys)
    val genreNames = stringArrayResource(R.array.genre_names)
    val categoryKeys = stringArrayResource(R.array.tag_category_keys)
    val categoryNames = stringArrayResource(R.array.tag_category_names)
    val tagKeys = stringArrayResource(R.array.tag_keys)
    val tagNames = stringArrayResource(R.array.tag_names)
    val categoryOf = TagCategories.of(LocalContext.current)
    val minePicks = listOf(true.toString() to stringResource(R.string.user_filter_mine_in), false.toString() to stringResource(R.string.user_filter_mine_out))

    // What each field can pick: only what the titles of the list have, in the order of its names
    val sources = remember(entries) { options(sourceKeys, sourceNames) { key -> entries.any { it.source == key } } }
    val formats = remember(entries) { options(formatKeys, formatNames) { key -> entries.any { it.formatKey == key } } }
    val seasons = remember(entries) { options(seasonKeys, seasonNames) { key -> entries.any { it.season == key } } }
    val years = remember(entries) { entries.mapNotNull { it.year }.distinct().sortedDescending() }
    val countries = remember(entries) { entries.mapNotNull { it.country }.distinct().sorted().map { it to countryName(it) } }
    val states = remember(entries) { ReleaseKeys.filter { key -> entries.any { it.airing == key } }.mapNotNull { key -> ReleaseState.of(key)?.let { key to it.label } } }
    val genreName: (String) -> String = { genre -> genreNames.getOrNull(genreKeys.indexOf(genre)) ?: genre }
    val tagName: (String) -> String = { tag -> tagNames.getOrNull(tagKeys.indexOf(tag)) ?: tag }
    val collator = remember { Collator.getInstance(Locale.getDefault()) }

    // The genres and the tags the titles have, named in the language of the app and put in the order of those names
    val genres = remember(entries) { entries.flatMap { it.genres }.distinct().map { it to genreName(it) }.sortedWith(compareBy(collator) { it.second }) }
    val tagsByCategory = remember(entries) {
        entries.flatMap { it.tags }.distinct().map { it to tagName(it) }.sortedWith(compareBy(collator) { it.second })
            .groupBy({ categoryOf[it.first] ?: TagCategories.Other }, { it })
    }
    val categories = remember(tagsByCategory) { options(categoryKeys, categoryNames) { it in tagsByCategory }.sortedWith(compareBy(collator) { it.second }) }
    var category by remember { mutableStateOf<String?>(null) }
    val allTags = remember(tagsByCategory) { tagsByCategory.values.flatten().sortedWith(compareBy(collator) { it.second }) }

    KaisouSheet(onDismiss = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        val closeSheet = LocalSheetClose.current

        // The name of the sheet and the words that take every choice back, in a header that keeps its height whether they show or not
        Row(
            modifier = Modifier.fillMaxWidth().height(UserDimens.FilterHeader).padding(horizontal = dimens.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.user_list_filter),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(visible = draft.active, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.title_list_clear),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) { draft = ListFilter() }.padding(dimens.spaceSmall)
                )
            }
        }

        // What can be chosen, which slides under the name and over the buttons
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            FieldRow {
                PickField(R.string.title_data_source, draft.source, sources) { draft = draft.copy(source = it) }
                PickField(R.string.title_data_format, draft.format, formats) { draft = draft.copy(format = it) }
            }
            FieldRow {
                PickField(R.string.title_data_status, draft.airing, states.map { (key, label) -> key to stringResource(label) }) { draft = draft.copy(airing = it) }
                PickField(R.string.user_filter_country, draft.country, countries) { draft = draft.copy(country = it) }
            }
            FieldRow {
                if (!manga) PickField(R.string.title_data_season, draft.season, seasons) { draft = draft.copy(season = it) }
                PickField(R.string.user_filter_year, draft.year?.toString(), years.map { it.toString() to it.toString() }) { draft = draft.copy(year = it?.toInt()) }
            }
            if (canFilterMine) {
                FieldRow {
                    PickField(R.string.user_filter_mine, draft.onMine?.toString(), minePicks) { draft = draft.copy(onMine = it?.toBoolean()) }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // The genres in a field as wide as the sheet, with the ones picked under it
            if (genres.isNotEmpty()) {
                FieldRow {
                    ManyField(R.string.title_data_genres, genres, draft.included, draft.excluded) { genre ->
                        val (included, excluded) = cycle(genre, draft.included, draft.excluded)
                        draft = draft.copy(included = included, excluded = excluded)
                    }
                }
                PickedPills(
                    names = (draft.included + draft.excluded).sortedWith(compareBy(collator, genreName)),
                    label = genreName,
                    excluded = draft.excluded,
                    onCycle = { genre ->
                        val (included, excluded) = cycle(genre, draft.included, draft.excluded)
                        draft = draft.copy(included = included, excluded = excluded)
                    },
                    onRemove = { genre -> draft = draft.copy(included = draft.included - genre, excluded = draft.excluded - genre) }
                )
            }

            // The tags: the field of the category, then the field of the tags of that category, and the ones picked under them
            if (categories.isNotEmpty()) {
                FieldRow {
                    PickField(R.string.user_filter_category, category, categories) { category = it }
                    ManyField(R.string.user_filter_tag, category?.let { tagsByCategory[it] } ?: allTags, draft.includedTags, draft.excludedTags) { tag ->
                        val (included, excluded) = cycle(tag, draft.includedTags, draft.excludedTags)
                        draft = draft.copy(includedTags = included, excludedTags = excluded)
                    }
                }
                PickedPills(
                    names = (draft.includedTags + draft.excludedTags).sortedWith(compareBy(collator, tagName)),
                    label = tagName,
                    excluded = draft.excludedTags,
                    onCycle = { tag ->
                        val (included, excluded) = cycle(tag, draft.includedTags, draft.excludedTags)
                        draft = draft.copy(includedTags = included, excludedTags = excluded)
                    },
                    onRemove = { tag -> draft = draft.copy(includedTags = draft.includedTags - tag, excludedTags = draft.excludedTags - tag) }
                )
            }
        }

        // Cancel closes it as it was, apply keeps what was chosen
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceSmall, bottom = dimens.spaceMedium),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            FilterButton(text = stringResource(R.string.action_cancel), filled = false, onClick = closeSheet, modifier = Modifier.weight(1f))
            FilterButton(
                text = stringResource(R.string.user_filter_apply),
                filled = true,
                onClick = {
                    onApply(draft)
                    closeSheet()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// One touch on a name has it in, the next leaves it out, the next takes it back: gives the names that are in and the ones left out
private fun cycle(name: String, included: Set<String>, excluded: Set<String>): Pair<Set<String>, Set<String>> = when (name) {
    in included -> (included - name) to (excluded + name)
    in excluded -> included to (excluded - name)
    else -> (included + name) to excluded
}

// A button of the sheet as Dantotsu draws them, square with soft corners and its name in bold: an outline, or filled for what applies
@Composable
internal fun FilterButton(text: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = MaterialTheme.shapes.large

    Box(
        modifier = modifier
            .height(UserDimens.FilterButton)
            .clip(shape)
            .then(if (filled) Modifier.background(accent) else Modifier.border(KaisouTheme.dimens.hairline * 2, MaterialTheme.colorScheme.outline, shape))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

// The release states as AniList names them, in the order the field lists them
private val ReleaseKeys = listOf("RELEASING", "FINISHED", "HIATUS", "NOT_YET_RELEASED", "CANCELLED")

// Keeps the names of the options that have a title, with the key that AniList uses for each
private fun options(keys: Array<String>, names: Array<String>, has: (String) -> Boolean): List<Pair<String, String>> =
    keys.indices.filter { has(keys[it]) }.map { keys[it] to names[it] }

// The name of a country in the language of the app, from its two letters
private fun countryName(code: String): String = Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.getDefault()).ifBlank { code }

// A line of fields that share its width
@Composable
private fun FieldRow(content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceSmall), content = content)
}

// An outlined field with its name floating on the line, that opens a menu as wide as itself and draws what is in it
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.MenuField(label: Int, shown: String, lit: Boolean, enabled: Boolean, menu: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = open && enabled, onExpandedChange = { open = it }, modifier = Modifier.weight(1f)) {
        OutlinedTextField(
            value = shown,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = stringResource(label), maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = open && enabled,
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large
        ) {
            menu { open = false }
        }
    }
}

// A line of a menu: its name, in the accent when it is the pick, and a mark at its end
@Composable
private fun MenuLine(name: String, lit: Boolean, mark: (@Composable () -> Unit)?, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
        },
        trailingIcon = mark,
        onClick = onClick
    )
}

// A field that picks one of its options, the first line of the menu taking the pick back unless it cannot be taken back
@Composable
private fun RowScope.PickField(label: Int, value: String?, options: List<Pair<String, String>>, clearable: Boolean = true, onPick: (String?) -> Unit) {
    val shown = options.firstOrNull { it.first == value }?.second ?: value ?: stringResource(R.string.user_list_all)

    MenuField(label = label, shown = shown, lit = value != null && clearable, enabled = options.isNotEmpty()) { close ->
        val pick: (String?) -> Unit = {
            close()
            onPick(it)
        }
        if (clearable) MenuLine(name = stringResource(R.string.user_list_all), lit = false, mark = null) { pick(null) }
        options.forEach { (key, name) ->
            MenuLine(
                name = name,
                lit = key == value,
                mark = if (key == value) {
                    { Icon(imageVector = KaisouIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(UserDimens.FilterArrow)) }
                } else {
                    null
                }
            ) { pick(key) }
        }
    }
}

// A field that keeps its menu open while names are picked: a touch has a name in, the next leaves it out, the next takes it back
@Composable
private fun RowScope.ManyField(label: Int, names: List<Pair<String, String>>, included: Set<String>, excluded: Set<String>, onCycle: (String) -> Unit) {
    val picked = names.count { it.first in included || it.first in excluded }
    val shown = if (picked == 0) stringResource(R.string.user_filter_pick) else pluralStringResource(R.plurals.user_filter_picked, picked, picked)

    MenuField(label = label, shown = shown, lit = picked > 0, enabled = names.isNotEmpty()) {
        names.forEach { (key, name) ->
            val mark: (@Composable () -> Unit)? = when (key) {
                in included -> {
                    { Icon(imageVector = KaisouIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(UserDimens.FilterArrow)) }
                }
                in excluded -> {
                    { Icon(imageVector = KaisouIcons.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(UserDimens.FilterArrow)) }
                }
                else -> null
            }
            MenuLine(name = name, lit = key in included, mark = mark) { onCycle(key) }
        }
    }
}

// The names that are picked, each as a pill: a touch goes to the next state, a long touch takes it away
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickedPills(names: List<String>, label: (String) -> String, excluded: Set<String>, onCycle: (String) -> Unit, onRemove: (String) -> Unit) {
    if (names.isEmpty()) return

    FlowRow(horizontalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceSmall), verticalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceSmall)) {
        names.forEach { name ->
            NamePill(text = label(name), included = name !in excluded, excluded = name in excluded, onToggle = { onCycle(name) }, onExclude = { onRemove(name) })
        }
    }
}

// A name as a pill: lit when the titles must have it, struck through with a cross when they must not
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NamePill(text: String, included: Boolean, excluded: Boolean, onToggle: () -> Unit, onExclude: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val container = when {
        included -> MaterialTheme.colorScheme.primaryContainer
        excluded -> MaterialTheme.colorScheme.errorContainer
        else -> KaisouTheme.colors.elevated
    }
    val content = when {
        included -> MaterialTheme.colorScheme.onPrimaryContainer
        excluded -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .height(UserDimens.FilterChip)
            .clip(CircleShape)
            .background(container)
            .combinedClickable(role = Role.Checkbox, onClick = onToggle, onLongClick = onExclude)
            .padding(horizontal = dimens.spaceMedium),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (included || excluded) {
            Icon(imageVector = if (included) KaisouIcons.Check else KaisouIcons.Close, contentDescription = null, tint = content, modifier = Modifier.size(UserDimens.FilterArrow))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            textDecoration = if (excluded) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
