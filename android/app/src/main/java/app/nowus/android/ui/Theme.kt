package app.nowus.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.nowus.android.domain.RoutineCategory

/** Daylight v4.1 palette. Keep contact, action, day/night, and paper colors distinct. */
val DaylightBg = Color(0xFFF5F7FA)
val DaylightSurface = Color(0xFFFCFDFD)
val DaylightInk = Color(0xFF25364A)
val DaylightMuted = Color(0xFF657381)
val DaylightLine = Color(0xFFDCE3E7)
val DaylightNight = Color(0xFF2D425D)
val DaylightNightInk = Color(0xFFF0F4FB)
val DaylightNightMuted = Color(0xFFC3D0E0)
val DaylightDay = Color(0xFFF9E5C9)
val DaylightDayInk = Color(0xFF5C4836)
val DaylightContact = Color(0xFF246B65)
val DaylightContactSoft = Color(0xFFE7F0EC)
val DaylightBrandLight = Color(0xFFEFC88F)
val DaylightBrandPaper = Color(0xFFF5F1DF)
val DaylightPaper = Color(0xFFF5E8B9)
val DaylightPaperBottom = Color(0xFFEEE0AB)
val DaylightPaperInk = Color(0xFF554D36)
val DaylightPaperRule = Color(0x268C7743)
val DaylightPaperShadow = Color(0x1F68582B)
val DaylightTape = Color(0x9CAEC9C1)
val DaylightFold = Color(0xFFD9C98F)
val DaylightWarning = Color(0xFF825827)
val DaylightBoard = Color(0xFFE8EDF0)

// Existing UI call sites use these names; aliases move them onto the v4.1 tokens.
val Accent = DaylightContact
val Tint = DaylightContactSoft
val Page = DaylightBg
val Panel = DaylightSurface
val Line = DaylightLine
val Ink = DaylightInk
val Muted = DaylightMuted
val Night = DaylightNight
val OnNight = DaylightNightInk
val Day = DaylightDay
val ActionInk = DaylightNight

fun routineCategoryColor(category: RoutineCategory): Color = when (category) {
    RoutineCategory.SLEEP -> DaylightNight
    RoutineCategory.PREPARATION -> Color(0xFFE8EDF0)
    RoutineCategory.MEAL -> DaylightDay
    RoutineCategory.COMMUTE -> DaylightContactSoft
    RoutineCategory.STUDY_WORK -> Color(0xFFDCE5EC)
    RoutineCategory.REST -> DaylightSurface
    RoutineCategory.EXERCISE -> Color(0xFFF4D1A5)
    RoutineCategory.LIFE_ADMIN -> Color(0xFFE1EBE2)
    RoutineCategory.SOCIAL -> Color(0xFFF0E5D8)
    RoutineCategory.OTHER -> Color(0xFFE6EAEB)
    RoutineCategory.UNSCHEDULED -> DaylightBg
}

private val DaylightTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 37.sp,
        letterSpacing = (-0.45).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 27.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 23.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
)

@Composable
fun NowUsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = DaylightContact,
            onPrimary = DaylightSurface,
            primaryContainer = DaylightContactSoft,
            onPrimaryContainer = DaylightContact,
            secondary = DaylightNight,
            onSecondary = DaylightNightInk,
            secondaryContainer = DaylightContactSoft,
            onSecondaryContainer = DaylightInk,
            background = DaylightBg,
            onBackground = DaylightInk,
            surface = DaylightSurface,
            onSurface = DaylightInk,
            surfaceVariant = DaylightContactSoft,
            onSurfaceVariant = DaylightMuted,
            surfaceContainer = DaylightSurface,
            surfaceContainerLow = DaylightSurface,
            surfaceContainerHighest = DaylightContactSoft,
            outline = DaylightMuted,
            outlineVariant = DaylightLine,
            error = Color(0xFF9F392F),
        ),
        typography = DaylightTypography,
        content = content,
    )
}
