package app.nowus.android.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.nowus.android.domain.RoutineCategory

val Forest = Color(0xFF2F6B4F)
val Sage = Color(0xFFE3EEE5)
val Page = Color(0xFFF2F5F2)
val Ink = Color(0xFF232A25)
val Muted = Color(0xFF536357)
val Night = Color(0xFF27354B)
val Sunlight = Color(0xFFF5E5B8)

fun routineCategoryColor(category:RoutineCategory):Color = when(category){
    RoutineCategory.SLEEP -> Color(0xFFDDE4F3)
    RoutineCategory.PREPARATION -> Color(0xFFF0E3D2)
    RoutineCategory.MEAL -> Color(0xFFF4E2B7)
    RoutineCategory.COMMUTE -> Color(0xFFD5EAE7)
    RoutineCategory.STUDY_WORK -> Color(0xFFDCE7F6)
    RoutineCategory.REST -> Color(0xFFE9E0F1)
    RoutineCategory.EXERCISE -> Color(0xFFF2DCD4)
    RoutineCategory.LIFE_ADMIN -> Color(0xFFDCE8D9)
    RoutineCategory.SOCIAL -> Color(0xFFF0DDE8)
    RoutineCategory.OTHER -> Color(0xFFDDE3DE)
    RoutineCategory.UNSCHEDULED -> Color(0xFFE9ECE8)
}

@Composable
fun NowUsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary=Forest, onPrimary=Color.White, primaryContainer=Sage, onPrimaryContainer=Forest,
            background=Page, onBackground=Ink, surface=Color(0xFFFAFCFA), onSurface=Ink,
            surfaceVariant=Sage, onSurfaceVariant=Muted, outline=Color(0xFF78887C),
            error=Color(0xFF9F392F)
        ),
        typography = Typography(
            headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=28.sp,lineHeight=36.sp),
            headlineSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=23.sp,lineHeight=32.sp),
            titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=20.sp,lineHeight=28.sp),
            titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=24.sp),
            bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=25.sp),
            bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp,lineHeight=22.sp),
            bodySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.sp,lineHeight=19.sp)
        ),
        content=content
    )
}
