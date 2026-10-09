package app.nowus.android.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.nowus.android.domain.RoutineCategory

val Accent = Color(0xFF246B65)
val Tint = Color(0xFFE7F0EC)
val Page = Color(0xFFF5F7FA)
val Panel = Color(0xFFFCFDFD)
val Line = Color(0xFFDCE3E7)
val Ink = Color(0xFF25364A)
val Muted = Color(0xFF657381)
val Night = Color(0xFF2D425D)
val OnNight = Color(0xFFF0F4FB)
val NightSub = Color(0xFFC3D0E0)
val Daylight = Color(0xFFF9E5C9)
val DaylightInk = Color(0xFF5C4836)
val Peach = Color(0xFFF4D1A5)
val Paper = Color(0xFFF5E8B9)
val PaperBottom = Color(0xFFEEE0AB)
val PaperInk = Color(0xFF554D36)
val PaperRule = Color(0x268C7743)
val PaperTape = Color(0x9CAEC9C1)
val PaperTapeEdge = Color(0x75F0F6E9)
val PaperFold = Color(0xFFD9C98F)

// Keep the retained v4.1 component sources compiling while the native screens
// use the shared tokens above.
val DaylightSurface = Panel
val DaylightMuted = Muted
val DaylightLine = Line
val DaylightNight = Night
val DaylightNightInk = OnNight
val DaylightNightMuted = NightSub
val DaylightContact = Accent
val DaylightContactSoft = Tint
val DaylightBrandPaper = Color(0xFFF5F1DF)
val DaylightPaper = Paper
val DaylightPaperBottom = PaperBottom
val DaylightPaperInk = PaperInk
val DaylightPaperRule = PaperRule
val DaylightPaperShadow = Color(0x1F68582B)
val DaylightTape = PaperTape
val DaylightFold = PaperFold
val DaylightWarning = Color(0xFF825827)

fun routineCategoryColor(category:RoutineCategory):Color = when(category){
    RoutineCategory.SLEEP -> Night
    RoutineCategory.PREPARATION -> Daylight
    RoutineCategory.MEAL -> Daylight
    RoutineCategory.COMMUTE -> Tint
    RoutineCategory.STUDY_WORK -> Panel
    RoutineCategory.REST -> Tint
    RoutineCategory.EXERCISE -> Peach
    RoutineCategory.LIFE_ADMIN -> Panel
    RoutineCategory.SOCIAL -> Daylight
    RoutineCategory.OTHER -> Panel
    RoutineCategory.UNSCHEDULED -> Color(0xFFEDF0F1)
}

@Composable
fun NowUsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary=Accent, onPrimary=Color.White, primaryContainer=Tint, onPrimaryContainer=Accent,
            background=Page, onBackground=Ink, surface=Panel, onSurface=Ink,
            surfaceVariant=Tint, onSurfaceVariant=Muted, outline=Muted, outlineVariant=Line,
            secondary=Accent, onSecondary=Color.White, secondaryContainer=Tint, onSecondaryContainer=Ink,
            surfaceContainer=Panel, surfaceContainerLow=Panel, surfaceContainerHighest=Tint,
            error=Color(0xFF9F392F)
        ),
        typography = Typography(
            headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=27.sp,lineHeight=38.sp),
            headlineSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=22.sp,lineHeight=32.sp),
            titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=20.sp,lineHeight=28.sp),
            titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=14.sp,lineHeight=22.sp),
            bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=27.sp),
            bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp,lineHeight=24.sp),
            bodySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.sp,lineHeight=20.sp)
        ),
        content=content
    )
}
