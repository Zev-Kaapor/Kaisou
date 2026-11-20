package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Pictures
import coil3.compose.AsyncImage
// Java text and time
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.KaisouSheet
import com.kaisou.app.core.designsystem.components.KaisouSwitch
import com.kaisou.app.core.designsystem.components.LocalSheetClose
import com.kaisou.app.core.designsystem.components.OutlineIconButton
import com.kaisou.app.core.designsystem.components.PrimaryButton
import com.kaisou.app.core.designsystem.components.SheetCloseButton
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouMotion
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.title.data.ListDraft
import com.kaisou.app.features.title.data.ListOptions
import com.kaisou.app.features.title.data.ListStatus
import com.kaisou.app.features.title.data.ReleaseState
import com.kaisou.app.features.title.data.ScoreFormat
import com.kaisou.app.features.title.data.TitleUi
import com.kaisou.app.features.title.ui.TitleDimens

// The editor of the list, as AniList has it, small and with the weight of the rest of the app: the title with its cover on top, the status
// in pills with their icons, three in each line, the progress and the score written over a line, with a bar under the progress that fills in
// the violet, the two days side by side, and under More what is not asked every time. Putting the progress at the end makes the title done,
// and making it done fills the progress and the days. A title that is not on the list starts as the one being watched or read, or the
// planned one when it is not out
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListEditorSheet(title: TitleUi, options: ListOptions?, onSave: (ListDraft) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val entry = title.entry
    // What the account says: how the score is given and the lists of the user. Until it is known the score is from 0 to 10
    val account = options ?: ListOptions()
    val format = account.scoreFormat
    val totalUnits = (if (title.manga) title.chapters else title.episodes)?.takeIf { it > 0 }
    val totalVolumes = title.volumes?.takeIf { it > 0 }
    var status by rememberSaveable {
        mutableStateOf(entry?.status ?: if (title.releaseState == ReleaseState.NotYetReleased) ListStatus.Planning else ListStatus.Current)
    }
    var progress by rememberSaveable { mutableStateOf(entry?.progress ?: 0) }
    var volumes by rememberSaveable { mutableStateOf(entry?.progressVolumes ?: 0) }
    var score by rememberSaveable { mutableStateOf(entry?.score?.takeIf { it > 0f }?.let(::scoreText).orEmpty()) }
    var lists by rememberSaveable { mutableStateOf(entry?.customLists.orEmpty()) }
    var advanced by rememberSaveable {
        mutableStateOf(account.advancedScoring.map { name -> entry?.advancedScores?.get(name)?.takeIf { it > 0f }?.let(::scoreText).orEmpty() })
    }
    var started by rememberSaveable { mutableStateOf(entry?.startedAt?.toEpochDay()) }
    var ended by rememberSaveable { mutableStateOf(entry?.completedAt?.toEpochDay()) }
    var repeat by rememberSaveable { mutableStateOf(entry?.repeat ?: 0) }
    var notes by rememberSaveable { mutableStateOf(entry?.notes.orEmpty()) }
    var isPrivate by rememberSaveable { mutableStateOf(entry?.isPrivate ?: false) }
    var hidden by rememberSaveable { mutableStateOf(entry?.hidden ?: false) }
    var moreOpen by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    var moreFull by remember { mutableIntStateOf(0) }
    val moreHeight by animateIntAsState(
        targetValue = if (moreOpen) moreFull else 0,
        animationSpec = tween(KaisouMotion.ExpandMillis, easing = FastOutSlowInEasing),
        label = "listMore"
    )
    // What was there before the title was made done, to put back when the user changes their mind
    var backup by remember { mutableStateOf<Backup?>(null) }
    val names = stringArrayResource(if (title.manga) R.array.title_list_names_manga else R.array.title_list_names_anime)

    // Makes the title done: all of it is done and it ended today, and it started today when there was no day for it
    fun complete() {
        backup = Backup(progress, volumes, started, ended)
        totalUnits?.let { progress = it }
        totalVolumes?.let { volumes = it }
        val today = LocalDate.now().toEpochDay()
        if (started == null) started = today
        ended = today
    }

    fun choose(option: ListStatus) {
        if (option == ListStatus.Completed && status != ListStatus.Completed) {
            complete()
        } else if (option != ListStatus.Completed && status == ListStatus.Completed) {
            backup?.let {
                progress = it.progress
                volumes = it.volumes
                started = it.started
                ended = it.ended
            }
            backup = null
        }
        status = option
    }

    fun move(step: Int) {
        val next = (progress + step).coerceIn(0, totalUnits ?: MaxUnits)
        progress = next
        // The first one makes a planned title one that is being watched, and the last one makes it done
        if (step > 0 && status == ListStatus.Planning) status = ListStatus.Current
        if (step > 0 && totalUnits != null && next == totalUnits && status != ListStatus.Completed) {
            status = ListStatus.Completed
            complete()
        }
    }

    KaisouSheet(onDismiss = onDismiss) {
        val close = LocalSheetClose.current

        Column(
            modifier = Modifier
                .imePadding()
                .scrollIndicator(scroll)
                .verticalScroll(scroll)
                .padding(horizontal = dimens.screenPadding)
                .padding(bottom = dimens.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            // The title with its cover, so it is clear what is being marked
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = title.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(TitleDimens.ListCover)
                        .clip(MaterialTheme.shapes.small)
                        .background(KaisouTheme.colors.skeleton)
                )
                Text(
                    text = title.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                SheetCloseButton()
            }
                        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                ListStatus.entries.chunked(StatusesPerLine).forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                        line.forEach { option ->
                            StatusPill(
                                icon = listStatusIcon(option, title.manga),
                                text = names[option.ordinal],
                                selected = option == status,
                                onClick = { choose(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            // The progress is the big thing: the number between one less and one more, and the bar under it filling as far as it goes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(KaisouTheme.colors.elevated)
                    .padding(dimens.spaceMedium),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(if (title.manga) R.string.title_list_chapters else R.string.title_list_progress),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    StepButton(symbol = MinusSymbol, onClick = { move(-1) })
                    NumberInput(
                        value = progress,
                        max = totalUnits ?: MaxUnits,
                        onValueChange = { progress = it },
                        suffix = "/ ${totalUnits ?: UnknownTotal}",
                        big = true
                    )
                    StepButton(symbol = PlusSymbol, onClick = { move(1) })
                }
                ProgressBar(fraction = if (totalUnits != null) progress / totalUnits.toFloat() else null)
                if (title.manga) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.title_list_volumes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        StepButton(symbol = MinusSymbol, onClick = { volumes = (volumes - 1).coerceAtLeast(0) })
                        NumberInput(value = volumes, max = totalVolumes ?: MaxUnits, onValueChange = { volumes = it }, suffix = "/ ${totalVolumes ?: UnknownTotal}", big = false)
                        StepButton(symbol = PlusSymbol, onClick = { volumes = (volumes + 1).coerceAtMost(totalVolumes ?: MaxUnits) })
                    }
                }
            }
            // The score in the way the user chose on AniList: a number to write, five stars or three faces
            ScoreRow(
                format = format,
                label = stringResource(R.string.title_list_score),
                value = score,
                onValueChange = { score = it }
            )
            // The two days side by side
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                DayPill(label = stringResource(R.string.title_list_started), ask = stringResource(R.string.title_list_started_ask), day = started, onChange = { started = it }, modifier = Modifier.weight(1f))
                DayPill(label = stringResource(R.string.title_list_ended), ask = stringResource(R.string.title_list_ended_ask), day = ended, onChange = { ended = it }, modifier = Modifier.weight(1f))
            }
            // What is not asked every time stays under a line that opens
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null, role = Role.Button) { moreOpen = !moreOpen }
                    .padding(vertical = dimens.spaceExtraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.title_list_other),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (moreOpen) KaisouIcons.Close else KaisouIcons.Plus,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(TitleDimens.ListIcon)
                )
            }
            // The box has the height of what is inside it while it is open and none while it is closed, and the height is what moves, so the
            // end of the movement is the place it was going to
            Box(modifier = Modifier.fillMaxWidth().height(with(density) { moreHeight.toDp() }).clipToBounds()) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .onSizeChanged { moreFull = it.height }
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(KaisouTheme.colors.elevated)
                        .padding(dimens.spaceMedium),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(if (title.manga) R.string.title_list_repeat_manga else R.string.title_list_repeat_anime),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        StepButton(symbol = MinusSymbol, onClick = { repeat = (repeat - 1).coerceAtLeast(0) })
                        NumberInput(value = repeat, max = MaxRepeat, onValueChange = { repeat = it }, suffix = null, big = false)
                        StepButton(symbol = PlusSymbol, onClick = { repeat = (repeat + 1).coerceAtMost(MaxRepeat) })
                    }
                    if (account.advancedScoring.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.title_list_advanced),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        account.advancedScoring.forEachIndexed { index, name ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                                Text(
                                    text = advancedLabel(name),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                WriteBox(
                                    value = advanced.getOrElse(index) { "" },
                                    onValueChange = { text ->
                                        if (isScoreText(format, text)) advanced = advanced.toMutableList().also { it[index] = text }
                                    },
                                    placeholder = "0",
                                    keyboardType = if (format == ScoreFormat.Point10Decimal) KeyboardType.Decimal else KeyboardType.Number,
                                    width = TitleDimens.ListScoreBox
                                )
                            }
                        }
                    }
                    if (account.customLists.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.title_list_custom),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        account.customLists.forEach { name ->
                            SwitchLine(name, name in lists) { on -> lists = if (on) lists + name else lists - name }
                        }
                    }
                    SwitchLine(stringResource(R.string.title_list_private), isPrivate) { isPrivate = it }
                    SwitchLine(stringResource(R.string.title_list_hidden), hidden) { hidden = it }
                    BasicTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        minLines = NotesMinLines,
                        maxLines = NotesMaxLines,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(KaisouTheme.colors.skeleton)
                            .padding(dimens.spaceMedium),
                        decorationBox = { field ->
                            Box {
                                if (notes.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.title_list_notes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                field()
                            }
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium), verticalAlignment = Alignment.CenterVertically) {
                if (entry != null) {
                    OutlineIconButton(
                        icon = KaisouIcons.Trash,
                        contentDescription = stringResource(R.string.title_list_delete),
                        onClick = {
                            onRemove()
                            close()
                        }
                    )
                }
                PrimaryButton(
                    text = stringResource(R.string.title_list_save),
                    onClick = {
                        onSave(
                            ListDraft(
                                status = status,
                                progress = progress,
                                progressVolumes = volumes,
                                score = score.replace(',', '.').toFloatOrNull() ?: 0f,
                                customLists = lists.takeIf { account.customLists.isNotEmpty() },
                                advancedScores = advanced.map { it.replace(',', '.').toFloatOrNull() ?: 0f }.takeIf { account.advancedScoring.isNotEmpty() },
                                repeat = repeat,
                                notes = notes.trim(),
                                isPrivate = isPrivate,
                                hidden = hidden,
                                startedAt = started?.let(LocalDate::ofEpochDay),
                                completedAt = ended?.let(LocalDate::ofEpochDay)
                            )
                        )
                        close()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// What the title had before it was made done
private class Backup(val progress: Int, val volumes: Int, val started: Long?, val ended: Long?)

// A status with its icon: filled in the violet of the buttons when it is the one chosen, quiet when it is not
@Composable
private fun StatusPill(icon: ImageVector, text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KaisouTheme.colors
    val dimens = KaisouTheme.dimens
    val content = if (selected) colors.onAccentFill else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) Brush.horizontalGradient(listOf(colors.buttonStart, colors.buttonEnd)) else SolidColor(colors.elevated))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(TitleDimens.ListIcon))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = content, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

// The bar of the progress: a track that is filled in the violet as far as the fraction goes, which moves smoothly when it changes, with how
// much it is in percent at its end. Without a total there is nothing to fill
@Composable
private fun ProgressBar(fraction: Float?) {
    val colors = KaisouTheme.colors
    val dimens = KaisouTheme.dimens
    val filled by animateFloatAsState(
        targetValue = (fraction ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(KaisouMotion.ExpandMillis, easing = FastOutSlowInEasing),
        label = "listProgress"
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
        Box(modifier = Modifier.weight(1f).height(TitleDimens.ListBar).clip(CircleShape).background(colors.skeleton)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(filled)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(colors.buttonStart, colors.buttonEnd)))
            )
        }
        if (fraction != null) {
            Text(
                text = "${(filled * PercentScale).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(TitleDimens.ListPercentWidth),
                textAlign = TextAlign.End
            )
        }
    }
}

// A number that is written over a line, with how many there are in all after it when there is a total, big when it is the one that matters
@Composable
private fun NumberInput(value: Int, max: Int, onValueChange: (Int) -> Unit, suffix: String?, big: Boolean) {
    val dimens = KaisouTheme.dimens

    Row(modifier = Modifier.padding(horizontal = dimens.spaceSmall), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
        WriteBox(
            value = if (value == 0) "" else value.toString(),
            onValueChange = { text -> onValueChange((text.filter(Char::isDigit).take(MaxDigits).toIntOrNull() ?: 0).coerceAtMost(max)) },
            placeholder = "0",
            keyboardType = KeyboardType.Number,
            width = if (big) TitleDimens.ListNumberBig else TitleDimens.ListNumber,
            big = big
        )
        if (suffix != null) {
            Text(text = suffix, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// A place to write: the number in the middle over a line, which turns violet while it is being written
@Composable
private fun WriteBox(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType, width: androidx.compose.ui.unit.Dp, big: Boolean = false) {
    val dimens = KaisouTheme.dimens
    val style = if (big) MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal) else MaterialTheme.typography.bodyLarge

    var focused by remember { mutableStateOf(false) }
    val line = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style.copy(color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.width(width).onFocusChanged { focused = it.isFocused },
        decorationBox = { field ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) Text(text = placeholder, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    field()
                }
                Box(modifier = Modifier.padding(top = dimens.spaceExtraSmall).fillMaxWidth().height(TitleDimens.ListLine).background(line))
            }
        }
    )
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TitleDimens.StepButton)
            .clip(CircleShape)
            .background(KaisouTheme.colors.skeleton)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

// A pill with a day or the name of the day when there is none, that opens the calendar, and a cross that takes the day away
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPill(label: String, ask: String, day: Long?, onChange: (Long?) -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    var picking by remember { mutableStateOf(false) }
    val format = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()) }

    Row(
        modifier = modifier
            .height(TitleDimens.ListPill)
            .clip(CircleShape)
            .background(KaisouTheme.colors.elevated)
            .clickable(role = Role.Button) { picking = true }
            .padding(horizontal = dimens.spaceMedium),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = KaisouIcons.Calendar,
            contentDescription = null,
            tint = if (day != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(TitleDimens.ListIcon)
        )
        Text(
            text = day?.let { LocalDate.ofEpochDay(it).format(format) } ?: label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (day != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (day != null) {
            Icon(
                imageVector = KaisouIcons.Close,
                contentDescription = stringResource(R.string.title_list_clear),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(TitleDimens.ListIcon)
                    .clickable(role = Role.Button) { onChange(null) }
            )
        }
    }
    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (day ?: LocalDate.now().toEpochDay()) * MillisPerDay,
            yearRange = FirstYear..LocalDate.now().year
        )
        val colors = KaisouTheme.colors
        val primary = MaterialTheme.colorScheme.primary
        DatePickerDialog(
            onDismissRequest = { picking = false },
            shape = MaterialTheme.shapes.extraLarge,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) }
                    picking = false
                }) { Text(text = stringResource(android.R.string.ok), style = MaterialTheme.typography.bodyMedium) }
            },
            dismissButton = {
                Row {
                    // A day is most often today, so it is one tap away
                    TextButton(onClick = {
                        onChange(LocalDate.now().toEpochDay())
                        picking = false
                    }) { Text(text = stringResource(R.string.title_list_today), style = MaterialTheme.typography.bodyMedium) }
                    TextButton(onClick = { picking = false }) {
                        Text(text = stringResource(android.R.string.cancel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        ) {
            DatePicker(
                state = state,
                showModeToggle = false,
                title = {
                    Text(
                        text = ask,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = dimens.spaceLarge, end = dimens.spaceLarge, top = dimens.spaceLarge)
                    )
                },
                headline = {
                    Text(
                        text = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(format) } ?: EmptyValue,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
                        color = primary,
                        modifier = Modifier.padding(start = dimens.spaceLarge, end = dimens.spaceLarge, bottom = dimens.spaceMedium)
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    selectedDayContainerColor = primary,
                    selectedDayContentColor = colors.onAccentFill,
                    todayContentColor = primary,
                    todayDateBorderColor = primary,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    dayContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationContentColor = MaterialTheme.colorScheme.onBackground,
                    subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    yearContentColor = MaterialTheme.colorScheme.onBackground,
                    currentYearContentColor = primary,
                    selectedYearContainerColor = primary,
                    selectedYearContentColor = colors.onAccentFill
                )
            )
        }
    }
}

// A line with a name and a switch
@Composable
private fun SwitchLine(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        KaisouSwitch(checked = checked, onCheckedChange = onChange)
    }
}

// The highest score of each way to give it
private fun scoreMax(format: ScoreFormat): Float =
    when (format) {
        ScoreFormat.Point100 -> 100f
        ScoreFormat.Point5 -> 5f
        ScoreFormat.Point3 -> 3f
        else -> MaxScore
    }

// The text of a score being written: nothing, or a number up to the highest of the way to give it, with one decimal at most and with a dot
// or a comma only when the way allows it
private fun isScoreText(format: ScoreFormat, text: String): Boolean {
    if (text.isEmpty()) return true
    val pattern = if (format == ScoreFormat.Point10Decimal) Regex("""\d{1,2}([.,]\d?)?""") else Regex("""\d{1,3}""")
    if (!pattern.matches(text)) return false
    return (text.replace(',', '.').toFloatOrNull() ?: return false) <= scoreMax(format)
}

// The score one step up or down by the way to give it, a few points at a time on the scale of a hundred and half a point with decimals,
// and nothing when it goes down to zero
private fun stepScore(format: ScoreFormat, text: String, direction: Int): String {
    val step = when (format) {
        ScoreFormat.Point100 -> ScorePointsStep
        ScoreFormat.Point10Decimal -> ScoreDecimalStep
        else -> 1f
    }
    val next = ((text.replace(',', '.').toFloatOrNull() ?: 0f) + direction * step).coerceIn(0f, scoreMax(format))
    val rounded = Math.round(next * 10f) / 10f
    return if (rounded <= 0f) "" else scoreText(rounded)
}

// The score as a row of its own: a number written over a line, five stars or three faces, by the way the user chose to give it
@Composable
private fun ScoreRow(format: ScoreFormat, label: String, value: String, onValueChange: (String) -> Unit) {
    val dimens = KaisouTheme.dimens
    val chosen = value.replace(',', '.').toFloatOrNull()?.toInt() ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(KaisouTheme.colors.elevated)
            .padding(dimens.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        Icon(imageVector = KaisouIcons.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.ListIcon))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        when (format) {
            // Five stars: a tap on one gives its number, and a tap on the one that is chosen takes the score away
            ScoreFormat.Point5 -> repeat(StarCount) { index ->
                val number = index + 1
                Icon(
                    imageVector = KaisouIcons.Star,
                    contentDescription = null,
                    tint = if (number <= chosen) KaisouTheme.colors.medalGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FaintAlpha),
                    modifier = Modifier
                        .size(TitleDimens.ListScoreIcon)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onValueChange(if (number == chosen) "" else number.toString()) }
                )
            }
            // Three faces, the sad one, the neutral one and the happy one
            ScoreFormat.Point3 -> listOf(KaisouIcons.Frown, KaisouIcons.Meh, KaisouIcons.Smile).forEachIndexed { index, face ->
                val number = index + 1
                Icon(
                    imageVector = face,
                    contentDescription = null,
                    tint = if (number == chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FaintAlpha),
                    modifier = Modifier
                        .size(TitleDimens.ListScoreIcon)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onValueChange(if (number == chosen) "" else number.toString()) }
                )
            }
            // A number: the same buttons, number and sizes as the progress, so both rows go together
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(symbol = MinusSymbol, onClick = { onValueChange(stepScore(format, value, -1)) })
                Row(
                    modifier = Modifier.padding(horizontal = dimens.spaceSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                ) {
                    WriteBox(
                        value = value,
                        onValueChange = { text -> if (isScoreText(format, text)) onValueChange(text) },
                        placeholder = "0",
                        keyboardType = if (format == ScoreFormat.Point10Decimal) KeyboardType.Decimal else KeyboardType.Number,
                        width = TitleDimens.ListNumberBig,
                        big = true
                    )
                    Text(
                        text = stringResource(if (format == ScoreFormat.Point100) R.string.title_list_score_scale_100 else R.string.title_list_score_scale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StepButton(symbol = PlusSymbol, onClick = { onValueChange(stepScore(format, value, 1)) })
            }
        }
    }
}

// The name of a part of the detailed score, in the language of the app when it is one of the five that AniList starts with
@Composable
private fun advancedLabel(name: String): String {
    val keys = stringArrayResource(R.array.title_advanced_keys)
    val names = stringArrayResource(R.array.title_advanced_names)
    return names.getOrNull(keys.indexOf(name)) ?: name
}

private fun scoreText(score: Float): String =
    if (score % 1f == 0f) score.toInt().toString() else String.format(Locale.getDefault(), "%.1f", score)

private const val MinusSymbol = "−"
private const val PlusSymbol = "+"
private const val EmptyValue = "—"
private const val MaxUnits = 5000
private const val FirstYear = 1900
private const val MaxRepeat = 99
private const val MaxDigits = 4
private const val MaxScore = 10f
private const val ScorePointsStep = 5f
private const val ScoreDecimalStep = 0.5f
// What the total says when it is not known
private const val UnknownTotal = "??"
private const val StarCount = 5
private const val FaintAlpha = 0.35f
private const val StatusesPerLine = 3
private const val PercentScale = 100
private const val MillisPerDay = 86_400_000L
private const val NotesMinLines = 2
private const val NotesMaxLines = 5
