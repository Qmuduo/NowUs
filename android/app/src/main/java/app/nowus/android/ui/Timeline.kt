package app.nowus.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.nowus.android.R
import app.nowus.android.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

private data class DayData(
    val bounds: Window,
    val me: List<Segment>,
    val partner: List<Segment>,
    val myContact: List<Window>,
    val partnerContact: List<Window>,
    val common: List<Window>,
    val state: AppState,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Timeline(
    state: AppState,
    now: Instant,
    focusInstant: Instant? = null,
    onSettings: () -> Unit = {},
) {
    val partner = state.partner.takeUnless { state.sharingPaused || state.syncStale }
    val focusKey = focusInstant?.toString()
    var dateText by rememberSaveable(focusKey) {
        mutableStateOf((focusInstant ?: now).atZone(state.me.zone()).toLocalDate().toString())
    }
    var full by rememberSaveable(focusKey) { mutableStateOf(false) }
    var detail by remember(state.me, state.schedule, partner, state.partnerSchedule, state.sharingPaused, state.syncStale) {
        mutableStateOf<Pair<Profile, Segment>?>(null)
    }
    val date = LocalDate.parse(dateText)
    val localStart = date.atStartOfDay(state.me.zone()).toInstant()
    val localEnd = date.plusDays(1).atStartOfDay(state.me.zone()).toInstant()
    val reference = focusInstant ?: now
    val referenceOnDate = reference.atZone(state.me.zone()).toLocalDate() == date
    val startMinute = if (referenceOnDate) reference.atZone(state.me.zone()).hour * 60 else now.atZone(state.me.zone()).hour * 60
    val axisStart = if (full) localStart else date.atTime(startMinute / 60, 0).atZone(state.me.zone()).toInstant()
    val axisEnd = if (full) localEnd else minOf(localEnd, axisStart.plusSeconds(4 * 3600L))
    val range = Window(axisStart, axisEnd)
    val data by produceState<DayData?>(null, state, dateText, full, axisStart, axisEnd) {
        value = null
        value = withContext(Dispatchers.Default) {
            // Keep each activity's true start/end around the displayed viewport so a
            // nearby view can clip the drawing while still labeling the full activity.
            val eventStart = range.start.minusSeconds(36 * 3600L)
            val eventEnd = range.end.plusSeconds(36 * 3600L)
            DayData(
                range,
                TimeEngine.segments(eventStart, eventEnd, state.me, state.schedule).filter { it.end > range.start && it.start < range.end },
                partner?.let { profile -> TimeEngine.segments(eventStart, eventEnd, profile, state.partnerSchedule).filter { it.end > range.start && it.start < range.end } }.orEmpty(),
                TimeEngine.contactWindows(range.start, range.end, state.me, state.schedule, state.temporary),
                partner?.let { TimeEngine.contactWindows(range.start, range.end, it, state.partnerSchedule, state.partnerTemporary) }.orEmpty(),
                partner?.let { TimeEngine.commonWindows(range.start, range.end, state.me, state.schedule, state.temporary, it, state.partnerSchedule, state.partnerTemporary) }.orEmpty(),
                state,
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Text("我们的一天", style = MaterialTheme.typography.headlineSmall, color = Ink)
            IconButton(onClick=onSettings,modifier=Modifier.size(40.dp).semantics{contentDescription="打开我的设置"}){
                Icon(painterResource(R.drawable.icon_settings),null,tint=Accent)
            }
        }
        Text("同一刻，各自的生活。", color = Muted, style = MaterialTheme.typography.bodyMedium)

        val isToday = date == now.atZone(state.me.zone()).toLocalDate()
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { dateText = date.minusDays(1).toString() }, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.icon_back), "前一天", tint = Accent)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dateLabel(date) + if(isToday) " · 今天" else "", style = MaterialTheme.typography.titleMedium, color = Ink)
                if (!isToday) {
                    TextButton(onClick = { dateText = now.atZone(state.me.zone()).toLocalDate().toString() }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 32.dp)) {
                        Text("回到今天", fontSize = 12.sp)
                    }
                }
            }
            IconButton(onClick = { dateText = date.plusDays(1).toString() }, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.icon_arrow), "后一天", tint = Accent)
            }
        }

        ScopeControl(full = full, onChange = { full = it })
        if (date != now.atZone(state.me.zone()).toLocalDate()) {
            Text("按双方通常节奏预览，不代表那一天的实际活动记录。", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        }

        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimelinePerson(state.me, range, true, Modifier.weight(1f))
            TimelinePerson(partner, range, false, Modifier.weight(1f))
        }

        val current = data?.takeIf { it.state == state && it.bounds == range }
        if (current == null) {
            Text("正在展开这段时间…", color = Muted, modifier = Modifier.padding(vertical = 24.dp))
        } else {
            Row(Modifier.fillMaxWidth().padding(top=8.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                Text("左侧刻度为${state.me.cityName()}时间",color=Muted,fontSize=10.sp)
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    Box(Modifier.width(4.dp).height(13.dp).background(Accent,CircleShape))
                    Text("愿意联系",color=Muted,fontSize=10.sp)
                }
            }
            AlignedTracks(current, state.me, partner, now, full) { profile, segment -> detail = profile to segment }
            Text("活动按通常作息排列；点按查看完整时间。\n绿色细条包含临时状态调整，不代表实时在线。",color=Muted,fontSize=10.sp,lineHeight=15.sp,modifier=Modifier.padding(top=2.dp,bottom=12.dp))
            val next = current.common.firstOrNull { it.end > now }
            if (next != null && partner != null) {
                Surface(color = Tint, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(painterResource(R.drawable.icon_link), null, tint = Accent, modifier = Modifier.size(19.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (next.start <= now) "现在，双方都愿意联系" else "下一段共同时间", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${localTime(next.start, state.me)} – ${localTime(next.end, state.me)} · ${state.me.cityName()}", color = Ink, fontSize = 13.sp)
                            Text("${localTime(next.start, partner)} – ${localTime(next.end, partner)} · ${partner.cityName()}", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            } else if (partner != null && !state.sharingPaused) {
                val unknown = state.schedule == null || state.partnerSchedule == null ||
                    state.schedule.let { !it.weekday.contactKnown && !it.rest.contactKnown } ||
                    state.partnerSchedule.let { !it.weekday.contactKnown && !it.rest.contactKnown }
                Text(if (unknown) "联系偏好尚不完整；绿色细线只标出已确认可联系的时间。" else "这段时间暂时没有共同联系窗口。", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }

    val selected = detail?.takeIf { it.first == state.me || (partner != null && it.first == partner) }
    if (selected != null) {
        val (profile, segment) = selected
        ModalBottomSheet(onDismissRequest = { detail = null }, containerColor = Panel, contentColor = Ink) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetTitle(segment.activity.label){detail=null}
                Text("${localTime(segment.start,profile)} – ${localTime(segment.end,profile)} · 当地时间", style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color=Line)
                Text("来源：${sourceText(segment.activity.source)}", color = Ink, style = MaterialTheme.typography.bodySmall)
                val schedule=if(profile==state.me)state.schedule else state.partnerSchedule
                val localDate=segment.start.atZone(profile.zone()).toLocalDate()
                val rhythm=schedule?.let{if(localDate.dayOfWeek.value>=6)it.rest else it.weekday}
                Text(
                    rhythm?.let{if(it.contactKnown)"当天联系偏好：${it.contactStart}–${it.contactEnd}" else "当天联系偏好：未知"}
                        ?: "当天联系偏好：未知",
                    color=Muted,style=MaterialTheme.typography.bodySmall,
                )
                HorizontalDivider(color=Line)
                Text("活动安排不是实时行踪；有活动，也可以选择愿意联系。",color=Muted,style=MaterialTheme.typography.bodySmall)
                Button(onClick = { detail = null }, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp).heightIn(min = 48.dp)) { Text("知道了") }
            }
        }
    }
}

@Composable
private fun ScopeControl(full: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Line, RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to "附近几小时", true to "我的当地全天").forEach { (value, label) ->
            val selected = full == value
            Surface(
                color = if (selected) Panel else Color.Transparent,
                shape = RoundedCornerShape(9.dp),
                shadowElevation = if (selected) 2.dp else 0.dp,
                modifier = Modifier.weight(1f).heightIn(min = 40.dp).clickable { onChange(value) },
            ) {
                Box(Modifier.fillMaxSize().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (selected) Ink else Muted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun TimelinePerson(profile: Profile?, range: Window, self: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(28.dp).background(if (profile == null) Line else if (profile.cityId == "beijing") Night else Daylight, CircleShape), contentAlignment = Alignment.Center) {
            Text(profile?.let { avatarInitial(it.name) } ?: "…", color = if (profile?.cityId == "beijing") OnNight else Ink, fontSize = 12.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(profile?.let{"${it.cityName()} · ${if(self)"我" else it.name}"} ?: "对方", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 3, lineHeight = 14.sp)
            Text(
                profile?.let {
                    val start = range.start.atZone(it.zone())
                    val last = range.end.minusNanos(1).atZone(it.zone())
                    val firstDate = "${start.monthValue.toString().padStart(2,'0')}/${start.dayOfMonth.toString().padStart(2,'0')}"
                    val lastDate = "${last.monthValue.toString().padStart(2,'0')}/${last.dayOfMonth.toString().padStart(2,'0')}"
                    "${if (firstDate == lastDate) firstDate else "$firstDate–$lastDate"}"
                } ?: "等待配对",
                color = Muted,
                fontSize = 10.sp,
                maxLines = 2,
                lineHeight = 13.sp,
            )
        }
    }
}

@Composable
private fun AlignedTracks(
    data: DayData,
    me: Profile,
    partner: Profile?,
    now: Instant,
    full: Boolean,
    onSegment: (Profile, Segment) -> Unit,
) {
    val totalSeconds = Duration.between(data.bounds.start, data.bounds.end).seconds.toFloat().coerceAtLeast(1f)
    val hourHeight = if (full) 96.dp else 112.dp
    val axisHeight = (hourHeight * (totalSeconds / 3600f)).coerceAtLeast(if (full) 500.dp else 160.dp)
    val tickLabel = if (LocalDensity.current.fontScale > 1.3f) 31.dp else 24.dp
    val gap = if (LocalDensity.current.fontScale > 1.4f) 6.dp else 10.dp
    val trackGap = if (LocalDensity.current.fontScale > 1.4f) 5.dp else 10.dp
    val ticks = buildList {
        var instant = data.bounds.start
        while (instant <= data.bounds.end) { add(instant); instant = instant.plusSeconds(3600) }
    }
    val repeatedHourKeys = ticks.mapIndexedNotNull { index, instant ->
        val local = instant.atZone(me.zone())
        val previous = ticks.getOrNull(index - 1)?.atZone(me.zone())
        val next = ticks.getOrNull(index + 1)?.atZone(me.zone())
        val repeated = (previous?.let { it.toLocalDate() == local.toLocalDate() && it.hour == local.hour && it.offset != local.offset } == true) ||
            (next?.let { it.toLocalDate() == local.toLocalDate() && it.hour == local.hour && it.offset != local.offset } == true)
        if (repeated) local.toLocalDate() to local.hour else null
    }.toSet()
    val axisWidth = if (repeatedHourKeys.isEmpty()) 24.dp else 48.dp
    fun y(at: Instant): Dp = axisHeight * (Duration.between(data.bounds.start, at).seconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    val nowVisible = now >= data.bounds.start && now < data.bounds.end

    BoxWithConstraints(Modifier.fillMaxWidth().height(axisHeight + 30.dp).padding(bottom = 30.dp).semantics { contentDescription = "按真实时长排列的双方时间轴" }) {
        val trackWidth = ((maxWidth - axisWidth - gap * 2 - trackGap) / 2).coerceAtLeast(48.dp)
        val leftX = axisWidth + gap
        val rightX = leftX + trackWidth + trackGap

        // Hour grid is drawn once across both columns; every Y coordinate maps to one instant.
        Canvas(Modifier.fillMaxSize()) {
            ticks.forEach { tick ->
                val position = size.height * (Duration.between(data.bounds.start, tick).seconds.toFloat() / totalSeconds)
                drawLine(Line, Offset(axisWidth.toPx(), position), Offset(size.width, position), 1.dp.toPx())
            }
        }

        ticks.forEach { tick ->
            val position = y(tick)
            val local = tick.atZone(me.zone())
            val label = local.format(java.time.format.DateTimeFormatter.ofPattern("HH")) +
                if ((local.toLocalDate() to local.hour) in repeatedHourKeys) " ${local.offset.id}" else ""
            Text(label, Modifier.offset(y = position - 9.dp).width(axisWidth), color = Muted, fontSize = 11.sp, lineHeight = 13.sp)
        }

        @Composable
        fun Track(profile: Profile?, segments: List<Segment>, contact: List<Window>, x: Dp, tag: String) {
            if (profile == null) {
                Box(Modifier.offset(x = x).width(trackWidth).height(axisHeight).clip(RoundedCornerShape(9.dp)).background(Color(0xFFEDF0F1)).padding(8.dp)) {
                    Text("作息与活动\n未知", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                return
            }
            segments.forEach { segment ->
                val visibleStart = maxOf(segment.start, data.bounds.start)
                val visibleEnd = minOf(segment.end, data.bounds.end)
                if (visibleEnd <= visibleStart) return@forEach
                val segmentTop = y(visibleStart)
                val visualHeight = axisHeight * (Duration.between(visibleStart, visibleEnd).seconds.toFloat() / totalSeconds)
                Box(
                    Modifier
                        .offset(x = x, y = segmentTop)
                        .width(trackWidth)
                        .height(visualHeight.coerceAtLeast(1.dp))
                        .semantics { contentDescription = "$tag ${segment.activity.label} · ${segment.activity.category.title} · ${shortDateTime(segment.start, profile)} 至 ${shortDateTime(segment.end, profile)} ${sourceText(segment.activity.source)}" }
                        .clickable { onSegment(profile, segment) },
                ) {
                    Box(Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(10.dp)).background(routineCategoryColor(segment.activity.category)))
                    if (visualHeight >= 46.dp) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp)) {
                            Text(segment.activity.label, color = if (segment.activity.category == RoutineCategory.SLEEP) OnNight else if (segment.activity.category in setOf(RoutineCategory.MEAL, RoutineCategory.PREPARATION, RoutineCategory.EXERCISE, RoutineCategory.SOCIAL)) DaylightInk else Ink, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                            if (visualHeight >= 65.dp) Text("${localTime(segment.start, profile)} – ${localTime(segment.end, profile)}", color = if (segment.activity.category == RoutineCategory.SLEEP) NightSub else Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp), maxLines = 1)
                        }
                    } else {
                        Text(segment.activity.label, Modifier.align(Alignment.CenterStart).padding(horizontal = 8.dp), color = Ink, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
            contact.forEach { interval ->
                val railHeight = axisHeight * (Duration.between(interval.start, interval.end).seconds.toFloat() / totalSeconds)
                Box(
                    Modifier.offset(x = x + trackWidth - 7.dp, y = y(interval.start))
                        .width(4.dp).height(railHeight.coerceAtLeast(1.dp))
                        .clip(CircleShape).background(Accent)
                        .semantics { contentDescription = "$tag 可联系时间 ${shortDateTime(interval.start, profile)} 至 ${shortDateTime(interval.end, profile)}" },
                )
            }
            if (profile == me && (data.state.schedule == null || data.state.schedule.let { !it.weekday.contactKnown && !it.rest.contactKnown })) {
                Text("联系偏好未知", Modifier.offset(x = x, y = axisHeight - 20.dp), color = Muted, fontSize = 9.sp, maxLines = 1)
            }
        }

        Track(me, data.me, data.myContact, leftX, "我")
        Track(partner, data.partner, data.partnerContact, rightX, partner?.name ?: "对方")

        if (nowVisible) {
            val lineY = y(now)
            Canvas(Modifier.offset(x = axisWidth, y = lineY).fillMaxWidth().height(18.dp)) {
                drawLine(Accent, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
                drawCircle(Accent, radius = 2.5.dp.toPx(), center = Offset.Zero)
            }
            Text("此刻", Modifier.offset(x = maxWidth - 45.dp, y = lineY - 18.dp).background(Tint, RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 1.dp), color = Accent, fontSize = 10.sp)
        }
    }

}

private fun dateLabel(date: LocalDate): String = "${date.monthValue.toString().padStart(2,'0')}/${date.dayOfMonth.toString().padStart(2,'0')}"
