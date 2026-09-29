package app.nowus.android.ui

import app.nowus.android.domain.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

fun Profile.zone() = requireNotNull(Cities.byId(cityId)).zone
fun Profile.cityName() = Cities.byId(cityId)?.name.orEmpty()
fun localTime(at: Instant, profile: Profile) = at.atZone(profile.zone()).format(DateTimeFormatter.ofPattern("HH:mm"))
fun localDate(at: Instant, profile: Profile) = at.atZone(profile.zone()).format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
fun shortDateTime(at: Instant, profile: Profile) = at.atZone(profile.zone()).format(DateTimeFormatter.ofPattern("M/d HH:mm"))
fun offsetText(me: Profile, partner: Profile, now: Instant): String {
    val minutes=(partner.zone().rules.getOffset(now).totalSeconds-me.zone().rules.getOffset(now).totalSeconds)/60
    if(minutes==0) return "我们此刻在同一个时区"
    val span=listOfNotNull((abs(minutes)/60).takeIf { it>0 }?.let { "${it}小时" },(abs(minutes)%60).takeIf { it>0 }?.let { "${it}分钟" }).joinToString("")
    return "${partner.name}比我${if(minutes>0) "快" else "慢"}${span}"
}
fun sourceText(source: ActivitySource) = when(source) {
    ActivitySource.TEMPLATE -> "按通常作息 · 非实时状态"
    ActivitySource.TEMPORARY -> "主动设置 · 临时联系意愿"
    ActivitySource.UNKNOWN -> "尚未设置这段作息"
}
