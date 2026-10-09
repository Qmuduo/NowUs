package app.nowus.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nowus.android.AppViewModel
import app.nowus.android.domain.AppState
import app.nowus.android.domain.Cities
import app.nowus.android.domain.Note
import app.nowus.android.domain.Profile
import app.nowus.android.domain.Rhythm
import app.nowus.android.domain.RoutineBlock
import app.nowus.android.domain.RoutineCategory
import app.nowus.android.domain.TimeEngine
import java.time.Duration
import java.time.Instant

@Composable
internal fun DaylightHome(
    vm: AppViewModel,
    state: AppState,
    now: Instant,
    error: String?,
    saving: Boolean,
    realAccount: Boolean,
    onOpenPair: () -> Unit,
    onOpenRhythm: () -> Unit,
    onOpenNote: () -> Unit,
    onOpenWindow: (app.nowus.android.domain.Window) -> Unit,
) {
    val partner = state.partner
    val greeting = when {
        partner != null && now.atZone(state.me.zone()).toLocalDate() != now.atZone(partner.zone()).toLocalDate() -> "你这里，已经是明天。"
        partner != null -> "你的夜晚，他的早晨。"
        state.sharingPaused -> "分享暂时停在这里。"
        state.paired && state.syncStale -> "连接恢复后，再一起看此刻。"
        else -> "先过好自己的一天。"
    }
    Text(greeting, style = MaterialTheme.typography.headlineSmall, color = DaylightInk)
    Text(
        when {
            state.sharingPaused -> "你已暂停分享 · 各自生活，也彼此惦记"
            partner != null -> "${state.me.name}与${partner.name} · 各自生活，也彼此惦记"
            else -> "配对后，再把彼此的日常放在一起。"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = DaylightMuted,
    )
    ClockPair(state, now)
    if (partner != null) {
        val activePartnerStatus = state.partnerTemporary?.takeIf { now.toEpochMilli() in it.fromMillis until it.untilMillis }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(clockOffsetCaption(state.me, partner, now), style = MaterialTheme.typography.bodySmall, color = DaylightMuted)
            Text(
                when {
                    activePartnerStatus != null -> "${partner.name}主动设置 · ${if (activePartnerStatus.available) "愿意联系" else "暂不方便"}"
                    state.partnerSchedule == null -> "活动 · 对方未填写"
                    else -> "活动按通常作息"
                },
                style = MaterialTheme.typography.bodySmall,
                color = DaylightMuted,
            )
        }
    }
    val partnerNote = state.partnerNote
    if (partnerNote != null && partner != null) {
        DaylightPaperNote(
            recipient = "给 ${state.me.name}",
            message = partnerNote.text,
            signature = partner.name,
            sentTime = "${partner.cityName()} · ${shortDateTime(Instant.ofEpochMilli(partnerNote.updatedMillis), partner)}",
            relativeTime = relativeNoteTime(partnerNote.updatedMillis),
            modifier = Modifier.testTag("home-note-preview"),
            onClick = onOpenNote,
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DaylightSurface,
            border = BorderStroke(1.dp, DaylightLine),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        state.sharingPaused -> "分享已暂停，恢复后再查看对方的便签。"
                        state.paired && state.syncStale -> "对方资料暂时不可用。同步恢复后会重新显示。"
                        partner == null -> "配对后，对方的留言会留在这里。"
                        else -> "对方还没有留下留言。"
                    },
                    color = DaylightInk,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(if (partner == null && !state.paired) "邀请伴侣，开始分享彼此主动留下的话。" else "你也可以先留下一句话。", color = DaylightMuted, style = MaterialTheme.typography.bodySmall)
                if (partner == null && !state.paired) {
                    OutlinedButton(onClick = onOpenPair, modifier = Modifier.heightIn(min = 48.dp)) { Text("邀请伴侣") }
                } else if (partner != null) {
                    OutlinedButton(onClick = onOpenNote, modifier = Modifier.heightIn(min = 48.dp)) { Text("写一张便签") }
                }
            }
        }
    }
    val windows by androidx.compose.runtime.produceState<List<app.nowus.android.domain.Window>?>(null, state, now.epochSecond / 60) {
        value = null
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            partner?.let {
                app.nowus.android.domain.TimeEngine.commonWindows(
                    now, now.plusSeconds(7 * 86400), state.me, state.schedule, state.temporary,
                    it, state.partnerSchedule, state.partnerTemporary,
                )
            } ?: emptyList()
        }
    }
    ContactWindowCard(state, now, windows) { instant ->
        windows?.firstOrNull { it.start <= instant && it.end > instant }?.let(onOpenWindow) ?: onOpenRhythm()
    }
    if (state.note != null) {
        TextButton(onClick = onOpenNote, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("查看我的便签") }
    }
    if (realAccount && state.syncStale) Text("显示的是上次成功同步的资料。", color = DaylightWarning, style = MaterialTheme.typography.bodySmall)
}

private fun clockOffsetCaption(me: Profile, partner: Profile, now: Instant): String {
    val difference = (me.zone().rules.getOffset(now).totalSeconds - partner.zone().rules.getOffset(now).totalSeconds) / 60
    if (difference == 0) return "同一时区"
    val ahead = if (difference > 0) me else partner
    val minutes = kotlin.math.abs(difference)
    val hoursPart = (minutes / 60).takeIf { it > 0 }?.let { "$it 小时" }
    val minutesPart = (minutes % 60).takeIf { it > 0 }?.let { "$it 分钟" }
    return "${ahead.cityName()}快 ${listOfNotNull(hoursPart, minutesPart).joinToString(" ")}"
}

@Composable
internal fun NotePage(vm: AppViewModel, state: AppState, error: String?, saving: Boolean, realAccount: Boolean) {
    val partner = state.partner
    val partnerNote = state.partnerNote
    Text("留给彼此", style = MaterialTheme.typography.headlineSmall, color = DaylightInk)
    Text("把想说的话，轻轻放在这里。", style = MaterialTheme.typography.bodyMedium, color = DaylightMuted)
    if (partnerNote != null && partner != null) {
        DaylightPaperNote(
            recipient = "给 ${state.me.name}",
            message = partnerNote.text,
            signature = partner.name,
            sentTime = "${partner.cityName()} · ${shortDateTime(Instant.ofEpochMilli(partnerNote.updatedMillis), partner)}",
            relativeTime = relativeNoteTime(partnerNote.updatedMillis),
            modifier = Modifier.padding(top = 4.dp),
            full = true,
        )
    } else {
        Surface(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            color = DaylightSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DaylightLine),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(if (partner == null) "还没有配对伴侣" else "这里还没有新的留言", style = MaterialTheme.typography.titleMedium)
                Text(if (partner == null) "配对后，对方留下的便签会出现在这里。" else "你可以先给对方留一句话。", color = DaylightMuted)
            }
        }
    }
    HorizontalDivider(color = DaylightLine, modifier = Modifier.padding(vertical = 12.dp))
    DaylightSectionTitle("我留下的话")
    val note = state.note
    if (note != null) {
        Surface(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            color = DaylightSurface,
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, DaylightLine),
        ) {
            Column(Modifier.padding(horizontal = 17.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("写给 ${partner?.name ?: "未来的相遇"}", color = DaylightMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Text(relativeNoteTime(note.updatedMillis), color = DaylightMuted, style = MaterialTheme.typography.labelSmall)
                }
                Text(note.text, color = DaylightInk, fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 30.sp)
                Text(
                    "${state.me.cityName()} · ${shortDateTime(Instant.ofEpochMilli(note.updatedMillis), state.me)}",
                    color = DaylightMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    } else {
        Text("还没有写下便签。写下的话只会分享给已配对的伴侣。", color = DaylightMuted, modifier = Modifier.padding(vertical = 10.dp))
    }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var deleteConfirm by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(note?.revision ?: note?.updatedMillis) {
        mutableStateOf(state.noteDraft ?: note?.text.orEmpty())
    }
    OutlinedButton(
        onClick = { vm.clearError(); editorOpen = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("note-open-editor"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DaylightContact),
        border = BorderStroke(1.dp, DaylightLine),
    ) { Text(if (note == null) "写一张便签" else "编辑便签") }
    if (note != null) {
        TextButton(
            onClick = { deleteConfirm = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("note-delete"),
        ) { Text("删除便签", color = DaylightMuted) }
    }
    Text(if (realAccount) "便签仅对当前配对伴侣可见。" else "本地体验 · 留言仅保存在此设备。", style = MaterialTheme.typography.bodySmall, color = DaylightMuted)

    if (editorOpen) NoteEditorSheet(
        draft = draft,
        saving = saving,
        error = error,
        hasSavedNote = note != null,
        realAccount = realAccount,
        onDraftChange = { text ->
            val limited = limitCodePoints(text, 120)
            draft = limited
            vm.updateNoteDraft(limited)
        },
        onClose = { editorOpen = false },
        onSave = { vm.saveNote(draft) { editorOpen = false } },
    )
    if (deleteConfirm) AlertDialog(
        onDismissRequest = { deleteConfirm = false },
        title = { Text("删除这条便签？") },
        text = { Text("删除后会暫時保留撤销机会。") },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                val deleted = note ?: return@TextButton
                vm.deleteNote(deleted) { deleteConfirm = false }
            }) { Text("删除") }
        },
        dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorSheet(
    draft: String,
    saving: Boolean,
    error: String?,
    hasSavedNote: Boolean,
    realAccount: Boolean,
    onDraftChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheet, containerColor = DaylightBrandPaper) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 10.dp).padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (hasSavedNote) "改写这张便签" else "写一张便签", style = MaterialTheme.typography.titleLarge, color = DaylightPaperInk)
                TextButton(onClick = onClose, modifier = Modifier.testTag("note-editor-close")) { Text("收起") }
            }
            Text("想说的话，留在纸上。", style = MaterialTheme.typography.bodyMedium, color = DaylightPaperInk.copy(alpha = .8f))
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp).testTag("noteDraft"),
                placeholder = { Text("写下想对对方说的话…", color = DaylightPaperInk.copy(alpha = .62f)) },
                minLines = 5,
                maxLines = 8,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = DaylightPaper.copy(alpha = .55f),
                    focusedContainerColor = DaylightPaper.copy(alpha = .7f),
                    unfocusedBorderColor = DaylightPaperInk.copy(alpha = .23f),
                    focusedBorderColor = DaylightContact,
                    cursorColor = DaylightContact,
                ),
            )
            Text("${draft.codePointCount(0, draft.length)} / 120 字 · ${if (realAccount) "仅配对伴侣可见" else "仅保存在本机"}", color = DaylightPaperInk.copy(alpha = .78f), style = MaterialTheme.typography.bodySmall)
            if (error != null) ErrorText(error)
            Button(onClick = onSave, enabled = !saving && draft.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                Text(if (saving) "正在保存…" else "保存便签")
            }
        }
    }
}

@Composable
internal fun BrandAboutScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = DaylightSurface,
            border = BorderStroke(1.dp, DaylightLine),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                DaylightLogo(Modifier.fillMaxWidth().height(58.dp))
                Text("让相隔的日常，彼此可见。", style = MaterialTheme.typography.headlineSmall, color = DaylightInk)
                Text("NowUs 帮你看看两座城市的时间如何相遇，也让联系偏好与真实生活保持分开。作息只是主动填写的计划，不代表实时状态。", style = MaterialTheme.typography.bodyLarge, color = DaylightMuted)
                HorizontalDivider(color = DaylightLine)
                Text("时间按各自所在城市显示。便签和资料只会分享给当前配对的伴侣。", style = MaterialTheme.typography.bodyMedium, color = DaylightInk)
            }
        }
        DaylightMark(Modifier.height(70.dp).fillMaxWidth())
        Text("DAYLIGHT · 4.1", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium, color = DaylightMuted)
    }
}

@Composable
internal fun StatusSettings(vm: AppViewModel, state: AppState, now: Instant, saving: Boolean, error: String?, onDone: () -> Unit) {
    val current = state.temporary?.takeIf { now.toEpochMilli() in it.fromMillis until it.untilMillis }
    var choice by rememberSaveable(current?.available, current?.untilMillis) {
        mutableStateOf(when (current?.available) { true -> 0; false -> 1; null -> 2 })
    }
    var minutes by rememberSaveable(current?.untilMillis) {
        val remaining = current?.let { ((it.untilMillis - now.toEpochMilli()) / 60_000L).toInt() }
        mutableStateOf(listOf(30, 60, 180).minByOrNull { kotlin.math.abs(it - (remaining ?: 60)) } ?: 60)
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("临时状态只影响联系偏好，不会改变活动或作息。到期后自动回到通常偏好。", color = DaylightMuted, style = MaterialTheme.typography.bodyMedium)
        listOf("现在愿意联系", "暂时不方便", "跟随通常偏好").forEachIndexed { index, label ->
            FilterChip(
                selected = choice == index,
                onClick = { choice = index },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            )
        }
        if (choice != 2) {
            Text("持续时间", style = MaterialTheme.typography.titleMedium, color = DaylightInk)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 180).forEach { duration ->
                    FilterChip(
                        selected = minutes == duration,
                        onClick = { minutes = duration },
                        label = { Text(if (duration == 60) "1 小时" else if (duration == 180) "3 小时" else "30 分钟") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    )
                }
            }
        } else if (current != null) {
            Text("当前状态将在 ${shortDateTime(Instant.ofEpochMilli(current.untilMillis), state.me)} 到期。", color = DaylightMuted)
        }
        if (error != null) ErrorText(error)
        Button(
            enabled = !saving,
            onClick = {
                when (choice) {
                    0 -> vm.temporary(true, minutes, onDone)
                    1 -> vm.temporary(false, minutes, onDone)
                    else -> vm.resetTemporary(onDone)
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        ) { Text(if (saving) "正在保存…" else "保存状态") }
    }
}

@Composable
internal fun RhythmOverviewCard(rhythm: Rhythm, cityId: String, restDay: Boolean) {
    val blocks = if (rhythm.blocks.isNotEmpty()) rhythm.blocks else listOf(
        RoutineBlock("sleep-overview", "睡眠", rhythm.sleepStart, rhythm.sleepEnd, RoutineCategory.SLEEP),
        RoutineBlock("activity-overview", rhythm.activity, rhythm.activityStart, rhythm.activityEnd, RoutineCategory.STUDY_WORK),
    )
    val sleepBlock = blocks.firstOrNull { it.category == RoutineCategory.SLEEP }
    val sleepStart = TimeEngine.parseMinute(sleepBlock?.start ?: rhythm.sleepStart) ?: 0
    val sleepEnd = TimeEngine.parseMinute(sleepBlock?.end ?: rhythm.sleepEnd) ?: 0
    val sleepMinutes = (sleepEnd - sleepStart + 1440) % 1440
    Surface(color = DaylightNight, shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 17.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("${Cities.byId(cityId)?.name ?: "所选城市"} · ${if (restDay) "周六与周日" else "周一至周五"}", color = DaylightNightMuted, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 15.dp), verticalAlignment = androidx.compose.ui.Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("睡眠 ${"%.1f".format(java.util.Locale.ROOT, sleepMinutes / 60f)} 小时", color = DaylightNightInk, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 22.sp, lineHeight = 28.sp)
                Text("· ${blocks.size} 段日常安排", color = DaylightNightMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 3.dp))
            }
            androidx.compose.foundation.layout.BoxWithConstraints(
                Modifier.fillMaxWidth().height(11.dp).clip(RoundedCornerShape(4.dp)).background(DaylightNightMuted.copy(alpha = .55f)),
            ) {
                blocks.forEach { block ->
                    val start = TimeEngine.parseMinute(block.start) ?: return@forEach
                    val end = TimeEngine.parseMinute(block.end) ?: return@forEach
                    val spans = if (end > start) listOf(start to end) else listOf(start to 1440, 0 to end)
                    spans.forEach { (from, until) ->
                        if (until > from) {
                            val left = maxWidth * (from / 1440f)
                            val width = maxWidth * ((until - from) / 1440f)
                            Box(
                                Modifier.offset(x = left).width(width).fillMaxHeight()
                                    .background(routineCategoryColor(block.category)),
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "06", "12", "18", "24").forEach {
                    Text(it, color = DaylightNightMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun limitCodePoints(text: String, max: Int): String {
    val count = text.codePointCount(0, text.length)
    return if (count <= max) text else text.substring(0, text.offsetByCodePoints(0, max))
}

private fun relativeNoteTime(updatedMillis: Long): String {
    val seconds = Duration.between(Instant.ofEpochMilli(updatedMillis), Instant.now()).seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "刚刚"
        seconds < 3600 -> "${seconds / 60} 分钟前"
        seconds < 86400 -> "${seconds / 3600} 小时前"
        seconds < 86400 * 30 -> "${seconds / 86400} 天前"
        else -> "留下了一段时间"
    }
}
