package app.nowus.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nowus.android.R
import app.nowus.android.domain.*
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Paired home uses account data only; simulated fixtures are opt-in elsewhere. */
@Composable internal fun ClockPair(state: AppState, now: Instant) {
    val partner = state.partner
    Surface(shape = RoundedCornerShape(19.dp), color = Color.Transparent) {
        BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))) {
            val horizontal = partner != null && maxWidth >= 230.dp && LocalDensity.current.fontScale <= 1.15f
            if (horizontal) Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ClockFace(state.me, state.schedule, now, true, Modifier.weight(1f).fillMaxHeight())
                ClockFace(partner, state.partnerSchedule, now, false, Modifier.weight(1f).fillMaxHeight())
            } else Column {
                ClockFace(state.me, state.schedule, now, true)
                if (partner != null) ClockFace(partner, state.partnerSchedule, now, false)
            }
        }
    }
}

@Composable private fun ClockFace(profile: Profile, schedule: Schedule?, now: Instant, self: Boolean, modifier: Modifier = Modifier) {
    val local = now.atZone(profile.zone())
    val night = local.hour < 6 || local.hour >= 18
    val ink = if (night) OnNight else Ink
    val secondary = if (night) Color(0xFFBCC7D4) else Muted
    val activity = TimeEngine.activityAt(profile, schedule, null, now)
    val fontScale = LocalDensity.current.fontScale
    val screenWidth = LocalConfiguration.current.screenWidthDp
    Box(
        modifier.fillMaxWidth().height(
            when {
                fontScale > 1.15f -> 252.dp
                screenWidth <= 350 -> 160.dp
                else -> 170.dp
            },
        ).clip(RoundedCornerShape(2.dp))
            .background(if (night) Night else Day).testTag(if (self) "clock-self" else "clock-partner"),
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val orbRadius = if (night) 13.dp.toPx() else 18.dp.toPx()
            val orbCenter = Offset(
                if (night) size.width - 26.dp.toPx() else size.width + 10.dp.toPx(),
                66.dp.toPx(),
            )
            if (night) {
                drawCircle(DaylightNightInk.copy(alpha = .87f), orbRadius, orbCenter)
                drawCircle(Night, orbRadius, Offset(orbCenter.x - 7.dp.toPx(), orbCenter.y - 4.dp.toPx()))
            } else drawCircle(DaylightBrandLight.copy(alpha = .85f), orbRadius, orbCenter)
            val hill = Path().apply {
                moveTo(0f, size.height - 20.dp.toPx())
                cubicTo(size.width * .28f, size.height - 64.dp.toPx(), size.width * .52f, size.height - 7.dp.toPx(), size.width, size.height - 43.dp.toPx())
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(hill, ink.copy(alpha = .045f))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(profile.cityName(), color = ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (self) "我" else profile.name, color = secondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Text(localTime(now, profile), color = ink, fontSize = if (screenWidth <= 350) 33.sp else 36.sp, lineHeight = 42.sp, letterSpacing = (-1.5).sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(local.format(DateTimeFormatter.ofPattern("M/d · EEE", Locale.CHINA)), color = secondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Text(
            when { activity.source == ActivitySource.UNKNOWN -> "作息待填写"; activity.category == RoutineCategory.UNSCHEDULED -> "此段未安排"; else -> activity.label },
            color = secondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 15.dp),
        )
    }
}

@Composable internal fun ContactWindowCard(
    state: AppState,
    now: Instant,
    windows: List<Window>?,
    onViewWindow: ((Window) -> Unit)? = null,
    onOpenRhythm: (() -> Unit)? = null,
) {
    val partner = state.partner
    val next = windows?.firstOrNull { it.end > now }
    var detailsOpen by rememberSaveable { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(17.dp), color = DaylightContactSoft) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (windows == null) Text("正在寻找共同时间…", color = Muted)
            else if (next == null || partner == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DaylightIcon(R.drawable.daylight_icon_clock, null, Modifier.size(15.dp), DaylightContact)
                    Text(if (state.sharingPaused) "分享已暂停" else "未来七天", color = DaylightContact, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                if (state.sharingPaused) {
                    Text("按照自己的节奏来", color = DaylightInk, style = MaterialTheme.typography.titleLarge)
                    Text("恢复分享后，再查看共同联系时间。", color = Muted, style = MaterialTheme.typography.bodySmall)
                    return@Column
                }
                if (state.paired && state.syncStale) {
                    Text("等待彼此的节奏", color = DaylightInk, style = MaterialTheme.typography.titleLarge)
                    Text("联网并同步后再查看共同联系时间。", color = Muted, style = MaterialTheme.typography.bodySmall)
                    return@Column
                }
                val unknown = partner == null || state.schedule == null || state.partnerSchedule == null ||
                    state.schedule.let { !it.weekday.contactKnown && !it.rest.contactKnown } ||
                    state.partnerSchedule.let { !it.weekday.contactKnown && !it.rest.contactKnown }
                Text(if (unknown) "等待彼此的节奏" else "暂时没有重合的联系时间", color = DaylightInk, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(3.dp))
                Text(if (unknown) "对方还没有填写作息，暂时不能估计共同时间。" else "可以调整自己的联系偏好，也可以先留一句话。", color = Muted, style = MaterialTheme.typography.bodySmall)
                if (!unknown && onOpenRhythm != null) {
                    TextButton(onClick = onOpenRhythm, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 44.dp)) {
                        Text("调整我的联系偏好", color = DaylightContact)
                        DaylightIcon(R.drawable.daylight_icon_arrow, null, Modifier.size(16.dp), DaylightContact)
                    }
                }
            } else {
                val start = maxOf(next.start, now)
                val minutes = Duration.between(start, next.end).toMinutes().coerceAtLeast(0)
                val until = Duration.between(now, next.start).toMinutes().coerceAtLeast(0)
                val isToday = start.atZone(state.me.zone()).toLocalDate() == now.atZone(state.me.zone()).toLocalDate()
                val heading = when {
                    next.start <= now -> "现在 · 双方愿意联系"
                    isToday -> if (until < 60) "$until 分钟后" else "${until / 60} 小时后"
                    else -> "${start.atZone(state.me.zone()).format(DateTimeFormatter.ofPattern("M/d"))} · 下一段共同时间"
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DaylightIcon(R.drawable.daylight_icon_link, null, Modifier.size(15.dp), DaylightContact)
                    Text(heading, color = DaylightContact, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "有 $minutes 分钟，可以慢慢聊",
                    color = DaylightInk,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    WindowColumn(state.me, start, next.end, now, true, Modifier.weight(1f))
                Spacer(Modifier.width(1.dp).height(43.dp).background(DaylightLine))
                    WindowColumn(partner, start, next.end, now, false, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("按联系偏好估计，尚未约定", color = Muted, style = MaterialTheme.typography.labelSmall)
                    TextButton(
                        onClick = { detailsOpen = true },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.heightIn(min = 40.dp).semantics { contentDescription = "查看共同时间详情" },
                    ) { DaylightIcon(R.drawable.daylight_icon_arrow, null, Modifier.size(17.dp), DaylightContact) }
                }
            }
        }
    }
    if (detailsOpen) AlertDialog(
        onDismissRequest = { detailsOpen = false },
        title = { Text("共同联系时间") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (next != null && partner != null) {
                WindowLine(partner, maxOf(next.start, now), next.end, false)
                WindowLine(state.me, maxOf(next.start, now), next.end, true)
            } else Text("这段时间已结束，请查看下一次窗口。")
            Text("来自通常联系偏好与双方尚未到期的主动设置。作息不是实时在线状态，请先和对方确认。")
        } },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (next != null && onViewWindow != null) TextButton(onClick = { detailsOpen = false; onViewWindow(next) }) { Text("在一天中查看") }
                TextButton(onClick = { detailsOpen = false }) { Text("知道了") }
            }
        },
    )
}

@Composable private fun WindowColumn(profile: Profile, start: Instant, end: Instant, now: Instant, self: Boolean, modifier: Modifier = Modifier) {
    val zone = profile.zone()
    val startDate = start.atZone(zone).toLocalDate()
    val endDate = end.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    val dateLabel = if (startDate != today) " · ${startDate.format(DateTimeFormatter.ofPattern("M/d"))}" else ""
    val endLabel = if (startDate == endDate) localTime(end, profile) else "${endDate.format(DateTimeFormatter.ofPattern("M/d"))} ${localTime(end, profile)}"
    Column(modifier) {
        Text("${if (self) "你" else profile.name} · ${profile.cityName()}$dateLabel", color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${localTime(start, profile)}–$endLabel",
            color = DaylightInk,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable private fun WindowLine(profile: Profile, start: Instant, end: Instant, self: Boolean) {
    val endLabel = if (start.atZone(profile.zone()).toLocalDate() == end.atZone(profile.zone()).toLocalDate()) localTime(end, profile) else shortDateTime(end, profile)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val sameDate=start.atZone(profile.zone()).toLocalDate()==end.atZone(profile.zone()).toLocalDate()
        if(maxWidth>=248.dp&&LocalDensity.current.fontScale<=1.15f&&sameDate)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("${localTime(start,profile)} – $endLabel",style=MaterialTheme.typography.titleMedium,color=Ink)
            Text("${start.atZone(profile.zone()).format(DateTimeFormatter.ofPattern("M/d"))} ${profile.cityName()}${if(self)" · 我" else ""}",color=Muted,style=MaterialTheme.typography.bodySmall)
        }else Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${if (self) "我" else profile.name} · ${profile.cityName()}", color = Muted, style = MaterialTheme.typography.bodySmall)
            Text("${shortDateTime(start, profile)} – $endLabel", style = MaterialTheme.typography.titleMedium, color = Ink)
        }
    }
}
