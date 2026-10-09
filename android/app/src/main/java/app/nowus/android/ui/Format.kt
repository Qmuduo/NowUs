package app.nowus.android.ui

import app.nowus.android.domain.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Profile.zone() = requireNotNull(Cities.byId(cityId)).zone
fun Profile.cityName() = Cities.byId(cityId)?.name.orEmpty()
fun localTime(at: Instant, profile: Profile) = at.atZone(profile.zone()).format(DateTimeFormatter.ofPattern("HH:mm"))
fun localDate(at: Instant, profile: Profile) = at.atZone(profile.zone()).format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
fun shortDateTime(at: Instant, profile: Profile) = at.atZone(profile.zone()).format(DateTimeFormatter.ofPattern("M/d HH:mm"))
fun offsetText(me: Profile, partner: Profile, now: Instant): String {
    val mine = now.atZone(me.zone()).offset.totalSeconds
    val theirs = now.atZone(partner.zone()).offset.totalSeconds
    val difference = kotlin.math.abs(theirs - mine)
    if (difference == 0) return "两地此刻时差为零"
    val hours = difference / 3600
    val minutes = difference % 3600 / 60
    val seconds = difference % 60
    val amount = listOfNotNull(
        hours.takeIf { it > 0 }?.let { "$it 小时" },
        minutes.takeIf { it > 0 }?.let { "$it 分钟" },
        seconds.takeIf { it > 0 }?.let { "$it 秒" },
    ).joinToString(" ")
    val leading = if (mine >= theirs) me else partner
    return "${leading.cityName()}快 $amount"
}
fun sourceText(source: ActivitySource) = when(source) {
    ActivitySource.TEMPLATE -> "按通常作息"
    ActivitySource.TEMPORARY -> "主动设置 · 临时联系意愿"
    ActivitySource.UNKNOWN -> "尚未设置这段作息"
}
