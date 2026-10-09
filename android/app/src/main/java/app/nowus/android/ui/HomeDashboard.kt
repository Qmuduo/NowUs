package app.nowus.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nowus.android.domain.*
import java.time.Instant
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Matched pair of local sky panels. Times and schedule labels always come from account state. */
@Composable
internal fun ClockPair(state: AppState, now: Instant, modifier: Modifier = Modifier) {
    val partner = state.partner.takeUnless { state.sharingPaused || state.syncStale }
    BoxWithConstraints(modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))) {
        val stack = partner != null && (LocalDensity.current.fontScale > 1.3f || maxWidth < 300.dp)
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                ClockFace(state.me, state.schedule, now, true, Modifier.fillMaxWidth().heightIn(min = 160.dp).height(170.dp))
                ClockFace(partner, state.partnerSchedule, now, false, Modifier.fillMaxWidth().heightIn(min = 160.dp).height(170.dp))
            }
        } else {
            Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                ClockFace(state.me, state.schedule, now, true, Modifier.weight(1f).fillMaxHeight())
                if (partner != null) ClockFace(partner, state.partnerSchedule, now, false, Modifier.weight(1f).fillMaxHeight())
                else SoloSkyPlaceholder(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ClockFace(profile: Profile, schedule: Schedule?, now: Instant, self: Boolean, modifier: Modifier = Modifier) {
    val local = now.atZone(profile.zone())
    val daylight = local.hour in 6..17
    val ink = if (daylight) DaylightInk else OnNight
    val secondary = if (daylight) DaylightInk.copy(alpha = .76f) else NightSub
    val activity = TimeEngine.activityAt(profile, schedule, null, now)
    Box(
        modifier
            .background(if (daylight) Daylight else Night)
            .testTag(if (self) "clock-self" else "clock-partner")
            .padding(horizontal = 15.dp, vertical = 14.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) { drawSky(daylight) }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(profile.cityName(), color = ink, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (self) "我 · ${profile.name}" else profile.name, color = secondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                local.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)),
                color = ink,
                fontSize = 36.sp,
                lineHeight = 43.sp,
                letterSpacing = (-1).sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(local.format(DateTimeFormatter.ofPattern("M/d EEE", Locale.CHINA)), color = secondary, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    activity.source == ActivitySource.UNKNOWN -> "作息待补充"
                    else -> activity.label
                },
                color = ink,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SoloSkyPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(Tint).padding(15.dp), contentAlignment = Alignment.Center) {
        Text("这里，为另一个人留着。\n配对后显示对方的当地时间", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
    }
}

private fun DrawScope.drawSky(daylight: Boolean) {
    if (daylight) {
        drawCircle(Peach.copy(alpha = .75f), radius = 18.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width - 10.dp.toPx(), 53.dp.toPx()))
    } else {
        drawCircle(OnNight.copy(alpha = .85f), radius = 13.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width - 25.dp.toPx(), 66.dp.toPx()))
        drawCircle(Night, radius = 12.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width - 30.dp.toPx(), 61.dp.toPx()))
    }
    drawOval(
        color = (if (daylight) DaylightInk else OnNight).copy(alpha = .05f),
        topLeft = androidx.compose.ui.geometry.Offset(-40.dp.toPx(), size.height - 72.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(230.dp.toPx(), 115.dp.toPx()),
    )
    drawOval(
        color = (if (daylight) DaylightInk else OnNight).copy(alpha = .05f),
        topLeft = androidx.compose.ui.geometry.Offset(40.dp.toPx(), size.height - 30.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(240.dp.toPx(), 110.dp.toPx()),
    )
}

@Composable
internal fun ContactWindowCard(state: AppState, now: Instant, windows: List<Window>?, onOpenTimeline: (Instant) -> Unit = {}) {
    val partner = state.partner.takeUnless { state.sharingPaused || state.syncStale }
    val next = windows?.firstOrNull { it.end > now }
    var detailsOpen by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Surface(color = Tint, shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(app.nowus.android.R.drawable.icon_link), null, tint = Accent, modifier = Modifier.size(17.dp))
                Text("${if (next?.start?.let { it <= now } == true) "现在" else "下一段共同时间"} · 双方愿意联系", color = Accent, fontSize = 11.sp)
            }
            if (windows == null) Text("正在寻找共同时间…", color = Muted)
            else if (next == null || partner == null) {
                val unknown = partner == null || state.schedule == null || state.partnerSchedule == null ||
                    state.schedule.let { !it.weekday.contactKnown && !it.rest.contactKnown } ||
                    state.partnerSchedule.let { !it.weekday.contactKnown && !it.rest.contactKnown }
                Text(if (unknown) "联系偏好尚不完整，暂未找到共同时间" else "未来七天暂无共同联系窗口", color = Ink, fontSize = 16.sp, lineHeight = 23.sp)
                Text("双方可以调整各自愿意联系的时段。作息不是实时在线状态。", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
            } else {
                val start = maxOf(next.start, now)
                Text("有 ${Duration.between(start, next.end).toMinutes()} 分钟，可以慢慢聊", color = Ink, fontSize = 19.sp, lineHeight = 25.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ContactTime(state.me, start, next.end, self = true, Modifier.weight(1f))
                    ContactTime(partner, start, next.end, self = false, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("按双方联系偏好估计，尚未约定", color = Muted, fontSize = 10.sp)
                    androidx.compose.material3.TextButton(onClick = { detailsOpen = true }, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.heightIn(min = 40.dp)) {
                        Text("查看这段时间  →", color = Accent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
    if (detailsOpen) AlertDialog(
        onDismissRequest = { detailsOpen = false },
        title = { Text("共同联系时间") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (next != null && partner != null) {
                ContactTime(state.me, maxOf(next.start, now), next.end, true)
                ContactTime(partner, maxOf(next.start, now), next.end, false)
            }
            Text("来自双方通常联系偏好与尚未到期的主动设置。时间是建议，请先和对方确认。", color = Muted)
        } },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                detailsOpen = false
                next?.let { onOpenTimeline(it.start) }
            }) { Text("放到一天里看看") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { detailsOpen = false }) { Text("关闭") } },
    )
}

@Composable
private fun ContactTime(profile: Profile, start: Instant, end: Instant, self: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 2.dp)) {
        Text("${if (self) "你" else profile.name} · ${profile.cityName()}", color = Muted, fontSize = 10.sp, maxLines = 1)
        Text("${localTime(start, profile)} – ${localTime(end, profile)}", color = Ink, fontSize = 18.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}
