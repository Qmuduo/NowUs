package app.nowus.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nowus.android.AppViewModel
import app.nowus.android.R
import app.nowus.android.domain.AppState
import app.nowus.android.domain.Profile
import app.nowus.android.domain.Rhythm
import app.nowus.android.domain.RoutineBlock
import app.nowus.android.domain.RoutineCategory
import app.nowus.android.domain.RoutineTemplate
import app.nowus.android.domain.RoutineTemplates
import app.nowus.android.domain.Schedule
import app.nowus.android.domain.TimeEngine
import kotlinx.serialization.json.Json
import java.util.Locale

@Composable
internal fun DaylightProfileSettings(
    vm: AppViewModel,
    state: AppState,
    error: String?,
    saving: Boolean,
    demo: Boolean,
    realAccount: Boolean,
    onOpenRhythm: () -> Unit,
    onOpenPair: () -> Unit,
    onOpenAbout: () -> Unit,
    onLogout: () -> Unit,
    onExitDemo: () -> Unit,
) {
    var editProfile by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(state.me.name) { mutableStateOf(state.me.name) }
    var city by rememberSaveable(state.me.cityId) { mutableStateOf(state.me.cityId) }
    var confirmUnpair by rememberSaveable { mutableStateOf(false) }
    val imported by vm.localImport.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(Modifier.size(51.dp).background(DaylightNight, CircleShape), contentAlignment = Alignment.Center) {
                Text(state.me.name.firstOrNull()?.toString().orEmpty(), color = DaylightNightInk, style = MaterialTheme.typography.titleLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(state.me.name, style = MaterialTheme.typography.titleMedium, color = DaylightInk)
                Text("${state.me.cityName()} · ${state.me.zone().id}", style = MaterialTheme.typography.bodySmall, color = DaylightMuted)
            }
        }

        Text("我的日常", color = DaylightMuted, style = MaterialTheme.typography.bodySmall, letterSpacing = 1.sp)
        Column {
            ProfileSettingRow(
                icon = R.drawable.daylight_icon_day,
                title = "我的节奏",
                detail = "工作日与休息日的通常安排",
                onClick = onOpenRhythm,
                testTag = "settings-rhythm",
            )
            ProfileSettingRow(
                icon = R.drawable.daylight_icon_profile,
                title = "昵称与城市",
                detail = "手动选择，不获取实时位置",
                onClick = { vm.clearError(); editProfile = true },
                testTag = "settings-profile",
            )
        }

        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("我们之间", style = MaterialTheme.typography.titleMedium, color = DaylightInk)
            Text(state.partner?.let { "${state.me.name} · ${it.name}" } ?: "尚未配对", style = MaterialTheme.typography.bodySmall, color = DaylightMuted)
        }
        Column {
            ProfileSettingRow(
                icon = R.drawable.daylight_icon_shield,
                title = when {
                    !state.paired -> "尚未配对"
                    state.sharingEnabled -> "正在分享日常"
                    else -> "分享已暂停"
                },
                detail = "作息、联系偏好和当前留言",
                onClick = { vm.setSharing(!state.sharingEnabled) },
                enabled = state.paired && !saving,
                testTag = "settings-sharing",
                switchState = state.paired && state.sharingEnabled,
            )
            ProfileSettingRow(
                icon = R.drawable.daylight_icon_link,
                title = "邀请与配对流程",
                detail = if (state.partner == null) "创建邀请或输入伴侣邀请码" else "查看或管理当前配对",
                onClick = onOpenPair,
                testTag = "settings-pair",
            )
            ProfileSettingRow(
                icon = R.drawable.daylight_icon_pause,
                title = "解除配对",
                detail = "自己的节奏会保留",
                onClick = { confirmUnpair = true },
                enabled = state.paired && !saving,
                testTag = "settings-unpair",
            )
        }

        ProfileSettingRow(
            icon = R.drawable.daylight_icon_now,
            title = "应用图标与 Logo",
            detail = "DAYLIGHT · 4.1",
            onClick = onOpenAbout,
            testTag = "settings-about",
        )

        if (realAccount) {
            DaylightSectionTitle("账号")
            Text(
                "最后成功同步：${state.lastSyncMillis?.let { app.nowus.android.ui.shortDateTime(java.time.Instant.ofEpochMilli(it), state.me) } ?: "尚未成功"}",
                style = MaterialTheme.typography.bodySmall,
                color = DaylightMuted,
            )
            OutlinedButton(enabled = !saving, onClick = vm::inspectLocalImport, modifier = Modifier.fillMaxWidth()) {
                Text("检查可导入的本机资料")
            }
            OutlinedButton(enabled = !saving, onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("退出登录")
            }
        } else if (demo) {
            Text("你的生活由你决定。这里不记录在线时长，也不推测实时行踪。", color = DaylightMuted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onExitDemo) { Text("返回登录") }
        }
        if (error != null) ErrorText(error)
    }

    if (editProfile) EditorDialog("昵称与城市", { editProfile = false }) {
        ProfileFields(name, { name = it }, city, { city = it })
        if (error != null) ErrorText(error)
        Button(enabled = !saving, onClick = { vm.saveProfile(Profile(name.trim(), city)) { editProfile = false } }) {
            Text("保存资料")
        }
    }
    if (confirmUnpair) AlertDialog(
        onDismissRequest = { confirmUnpair = false },
        title = { Text("解除配对？") },
        text = { Text("双方将立即失去对彼此资料的访问。你自己的资料会保留；之后可重新邀请配对。") },
        confirmButton = { TextButton(enabled = !saving, onClick = { vm.unpair { confirmUnpair = false } }) { Text("解除配对") } },
        dismissButton = { TextButton(onClick = { confirmUnpair = false }) { Text("取消") } },
    )
    if (imported != null) AlertDialog(
        onDismissRequest = vm::cancelLocalImport,
        title = { Text("确认导入本人资料？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("昵称：${imported!!.profile.name} · 城市：${imported!!.profile.cityName()}")
                Text("工作日和休息日作息：${if (imported!!.schedule == null) "未填写" else "将上传"}")
                Text("当前留言：${if (imported!!.note == null) "未填写" else "将上传"}")
                Text("只上传以上本人资料；不上传模拟伴侣、演示邀请或固定演示时间。")
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = vm::importLocalData) { Text("确认并上传") } },
        dismissButton = { TextButton(onClick = vm::cancelLocalImport) { Text("取消") } },
    )
}

@Composable
private fun ProfileSettingRow(
    icon: Int,
    title: String,
    detail: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String,
    switchState: Boolean? = null,
) {
    Column {
        val rowModifier = Modifier.fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(
                if (switchState != null) Modifier.toggleable(
                    value = switchState,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = { onClick() },
                ).semantics {
                    contentDescription = "向对方分享日常"
                    stateDescription = if (switchState) "已开启" else "已关闭"
                } else Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            )
            .testTag(testTag)
            .padding(vertical = 9.dp)
        Row(rowModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DaylightIcon(icon, null, Modifier.size(19.dp), DaylightMuted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = DaylightInk)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = DaylightMuted, maxLines = 2)
            }
            if (switchState != null) {
                Box(
                    Modifier.size(width = 37.dp, height = 22.dp).clip(CircleShape)
                        .background(if (switchState) DaylightContact else DaylightMuted),
                    contentAlignment = if (switchState) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(Modifier.padding(3.dp).size(16.dp).background(DaylightSurface, CircleShape))
                }
            } else {
                DaylightIcon(R.drawable.daylight_icon_arrow, null, Modifier.size(16.dp), DaylightMuted)
            }
        }
        HorizontalDivider(color = DaylightLine)
    }
}

@Composable
internal fun DaylightRhythmScreen(
    vm: AppViewModel,
    state: AppState,
    error: String?,
    saving: Boolean,
    realAccount: Boolean,
) {
    var draft by rememberSaveable(state.schedule) {
        mutableStateOf(Json.encodeToString(state.schedule ?: defaultSchedule(state.me.cityId)))
    }
    var restDay by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<RoutineBlock?>(null) }
    var editingNew by remember { mutableStateOf(false) }
    var editingContact by rememberSaveable { mutableStateOf(false) }
    var showTemplates by rememberSaveable { mutableStateOf(false) }
    var pendingTemplate by remember { mutableStateOf<RoutineTemplate?>(null) }

    val schedule = Json.decodeFromString<Schedule>(draft)
    val rhythm = if (restDay) schedule.rest else schedule.weekday
    val dayKey = if (restDay) "rest" else "weekday"
    val blocks = rhythm.blocks.ifEmpty {
        listOf(
            RoutineBlock("sleep", "睡眠", rhythm.sleepStart, rhythm.sleepEnd, RoutineCategory.SLEEP),
            RoutineBlock("morning-activity", rhythm.activity, rhythm.activityStart, rhythm.activityEnd, RoutineCategory.STUDY_WORK),
        )
    }
    fun updateRhythm(next: Rhythm) {
        val nextSchedule = if (restDay) schedule.copy(rest = next) else schedule.copy(weekday = next)
        draft = Json.encodeToString(nextSchedule)
        saved = false
    }
    fun updateCurrentBlocks(next: List<RoutineBlock>) = updateRhythm(updateBlocks(rhythm, next))
    val addSlot = if (blocks.size < 20) firstAvailableRoutineSlot(blocks) else null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("安排好平常的一天，就不用每天填写。", color = DaylightMuted, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !restDay, onClick = { restDay = false }, label = { Text("工作日") })
            FilterChip(selected = restDay, onClick = { restDay = true }, label = { Text("休息日") })
        }
        RhythmOverviewCard(rhythm, state.me.cityId, restDay)
        DaylightSectionTitle(
            "通常的一天",
            addSlot?.let { "＋ 添加时段" },
            { if (blocks.size < 20) {
                addSlot?.let { (start, end) ->
                    val nextNumber = (blocks.mapNotNull { it.id.substringAfter("custom-", "").toIntOrNull() }.maxOrNull() ?: 0) + 1
                    editingNew = true
                    editingBlock = RoutineBlock("custom-$nextNumber", "新时段", start, end, RoutineCategory.OTHER)
                }
            } },
        )
        Column {
        if (blocks.isEmpty()) {
            Text("还没有安排时段。", color = DaylightMuted)
        } else {
            blocks.forEach { block ->
                RhythmRoutineRow(block, dayKey) {
                    editingNew = false
                    editingBlock = block
                }
            }
        }
        }
        Surface(
            color = DaylightContactSoft,
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("愿意联系的时间", color = DaylightInk, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { editingContact = true }, modifier = Modifier.heightIn(min = 44.dp)) { Text("编辑") }
                }
                Text(
                    if (rhythm.contactKnown) "${rhythm.contactStart} – ${rhythm.contactEnd}" else "联系偏好尚未设置",
                    color = DaylightInk,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                )
                Text("这是联系偏好，不是随时回复的承诺。", color = DaylightMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = { showTemplates = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
            Text("使用学生或上班族参考模板")
        }
        Text("临时有事？在「此刻」调整状态，到期自动恢复。", color = DaylightMuted, style = MaterialTheme.typography.bodySmall)
        if (error != null) ErrorText(error)
        Button(
            enabled = !saving,
            onClick = { saved = false; vm.saveSchedule(schedule) { saved = true } },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(if (saving) "保存中…" else "保存我的节奏")
        }
        if (saved) Text(if (realAccount) "已保存并同步" else "已保存到本机", color = DaylightContact)
        if (realAccount && state.schedule == null) Text("目前展示的是可编辑建议；保存后才会同步给账号。", color = DaylightMuted, style = MaterialTheme.typography.bodySmall)
    }

    editingBlock?.let { selected ->
        var label by rememberSaveable(selected.id) { mutableStateOf(selected.label) }
        var category by rememberSaveable(selected.id) { mutableStateOf(selected.category) }
        var start by rememberSaveable(selected.id) { mutableStateOf(selected.start) }
        var end by rememberSaveable(selected.id) { mutableStateOf(selected.end) }
        EditorDialog(if (editingNew) "添加时段" else "编辑时段", { editingBlock = null }) {
            OutlinedTextField(label, { label = it }, label = { Text("时段名称") }, modifier = Modifier.fillMaxWidth().testTag("rhythm-$dayKey-${selected.id}-label"), singleLine = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("类别", color = DaylightMuted)
                RoutineCategoryPicker(category, "rhythm-$dayKey-${selected.id}-category") { category = it }
            }
            TimePair(if (selected.id == "sleep") "睡眠" else label, "rhythm-$dayKey-${selected.id}", start, end, { start = it }, { end = it })
            if (error != null) ErrorText(error)
            if (selected.id != "sleep" && !editingNew) TextButton(onClick = {
                updateCurrentBlocks(blocks.filterNot { it.id == selected.id })
                editingBlock = null
            }) { Text("删除时段") }
            Button(enabled = label.isNotBlank(), onClick = {
                val updated = selected.copy(label = label.trim(), category = category, start = start, end = end)
                val next = if (editingNew) blocks + updated else blocks.map { if (it.id == selected.id) updated else it }
                updateCurrentBlocks(next)
                editingBlock = null
            }, modifier = Modifier.fillMaxWidth()) { Text("保存时段") }
        }
    }

    if (editingContact) {
        var known by rememberSaveable(dayKey) { mutableStateOf(rhythm.contactKnown) }
        var start by rememberSaveable(dayKey) { mutableStateOf(rhythm.contactStart) }
        var end by rememberSaveable(dayKey) { mutableStateOf(rhythm.contactEnd) }
        EditorDialog("愿意联系的时间", { editingContact = false }) {
            Text("活动安排与联系偏好相互独立。", color = DaylightMuted)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (known) "我愿意联系" else "暂不设置联系时间")
                Switch(known, onCheckedChange = { known = it })
            }
            if (known) TimePair("联系", "rhythm-$dayKey-contact", start, end, { start = it }, { end = it })
            Button(onClick = { updateRhythm(rhythm.copy(contactKnown = known, contactStart = start, contactEnd = end)); editingContact = false }) {
                Text("保存联系偏好")
            }
        }
    }
    if (showTemplates) AlertDialog(
        onDismissRequest = { showTemplates = false },
        title = { Text("选择参考作息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("模板会替换工作日和休息日两张作息表，之后仍可逐项调整。", color = DaylightMuted)
                RoutineTemplate.entries.forEach { template ->
                    OutlinedButton(onClick = { pendingTemplate = template; showTemplates = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("${template.title}模板")
                    }
                }
            }
        },
        confirmButton = {},
    )
    pendingTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingTemplate = null },
            title = { Text("应用${template.title}模板？") },
            text = { Text("这会替换尚未保存的工作日和休息日时段。应用后仍可逐项修改。") },
            confirmButton = {
                TextButton(onClick = {
                    draft = Json.encodeToString(RoutineTemplates.forCity(state.me.cityId, template))
                    saved = false
                    pendingTemplate = null
                }) { Text("应用模板") }
            },
            dismissButton = { TextButton(onClick = { pendingTemplate = null }) { Text("继续编辑") } },
        )
    }
}

@Composable
private fun RhythmRoutineRow(block: RoutineBlock, dayKey: String, onClick: () -> Unit) {
    val isOvernight = (TimeEngine.parseMinute(block.start) ?: 0) > (TimeEngine.parseMinute(block.end) ?: 0)
    val glyph = when (block.category) {
        RoutineCategory.SLEEP -> R.drawable.daylight_icon_moon
        RoutineCategory.MEAL -> R.drawable.daylight_icon_cup
        RoutineCategory.STUDY_WORK -> R.drawable.daylight_icon_work
        RoutineCategory.COMMUTE -> R.drawable.daylight_icon_arrow
        RoutineCategory.PREPARATION, RoutineCategory.EXERCISE -> R.drawable.daylight_icon_sun
        RoutineCategory.REST -> R.drawable.daylight_icon_moon
        RoutineCategory.LIFE_ADMIN, RoutineCategory.SOCIAL, RoutineCategory.OTHER, RoutineCategory.UNSCHEDULED -> R.drawable.daylight_icon_clock
    }
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag("rhythm-$dayKey-${block.id}-row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(Modifier.size(39.dp).background(DaylightSurface, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                DaylightIcon(glyph, null, Modifier.size(19.dp), DaylightMuted)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(block.label, color = DaylightInk, style = MaterialTheme.typography.titleSmall)
                Text("${block.start} – ${block.end}${if (isOvernight) " · 次日" else ""}", color = DaylightMuted, style = MaterialTheme.typography.bodySmall)
            }
            DaylightIcon(R.drawable.daylight_icon_arrow, null, Modifier.size(16.dp), DaylightMuted)
        }
        HorizontalDivider(color = DaylightLine)
    }
}

private fun firstAvailableRoutineSlot(blocks: List<RoutineBlock>): Pair<String, String>? {
    val occupied = BooleanArray(24 * 60)
    blocks.forEach { block ->
        val start = TimeEngine.parseMinute(block.start) ?: return@forEach
        val end = TimeEngine.parseMinute(block.end) ?: return@forEach
        fun mark(from: Int, until: Int) {
            for (minute in from.coerceIn(0, occupied.size) until until.coerceIn(0, occupied.size)) occupied[minute] = true
        }
        if (end > start) mark(start, end) else {
            mark(start, occupied.size)
            mark(0, end)
        }
    }
    for (start in 0..(occupied.size - 30) step 15) {
        if ((start until start + 30).none { occupied[it] }) {
            val end = start + 30
            return String.format(Locale.ROOT, "%02d:%02d", start / 60, start % 60) to
                String.format(Locale.ROOT, "%02d:%02d", end / 60, end % 60)
        }
    }
    return null
}
