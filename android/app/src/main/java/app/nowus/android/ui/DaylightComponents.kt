package app.nowus.android.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nowus.android.R

@Composable
internal fun DaylightLogo(modifier: Modifier = Modifier, contentDescription: String = "NowUs") {
    Image(
        painter = painterResource(R.drawable.daylight_logo),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
    )
}
@Composable
internal fun DaylightMark(modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(
        painter = painterResource(R.drawable.ic_nowus_foreground),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

@Composable
internal fun DaylightIcon(
    @DrawableRes icon: Int,
    description: String?,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = DaylightInk,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = description,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
internal fun DaylightHeader(
    onOpenProfile: () -> Unit,
    statusLabel: String = "我的状态",
    onOpenStatus: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DaylightLogo(Modifier.width(108.dp).height(29.65.dp))
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(if (statusLabel == "我的状态") Color.Transparent else DaylightContactSoft)
                .clickable(role = Role.Button, onClickLabel = "设置我的状态", onClick = onOpenStatus)
                .heightIn(min = 44.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            DaylightIcon(R.drawable.daylight_icon_sun, null, Modifier.size(17.dp), if (statusLabel == "我的状态") DaylightMuted else DaylightContact)
            Text(statusLabel, color = if (statusLabel == "我的状态") DaylightMuted else DaylightContact, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
        IconButton(
            onClick = onOpenProfile,
            modifier = Modifier.size(48.dp).semantics { contentDescription = "我的资料与设置" },
        ) {
            DaylightIcon(R.drawable.daylight_icon_profile, null, Modifier.size(22.dp), DaylightContact)
        }
    }
}

/** A reusable v4.1 paper note: warm ruled sheet, translucent tape, lifted edge and folded corner. */
@Composable
internal fun DaylightPaperNote(
    recipient: String,
    message: String,
    signature: String,
    sentTime: String,
    relativeTime: String,
    modifier: Modifier = Modifier,
    full: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val click = if (onClick == null) Modifier else Modifier.clickable(
        role = Role.Button,
        onClickLabel = "打开完整便签",
        onClick = onClick,
    )
    Box(
        modifier
            .fillMaxWidth()
            .then(if (full) Modifier.heightIn(min = 324.dp) else Modifier)
            .padding(horizontal = if (full) 6.dp else 2.dp, vertical = if (full) 15.dp else 9.dp)
            .rotate(if (full) (-1.2f) else (-0.5f))
            .shadow(if (full) 8.dp else 5.dp, RoundedCornerShape(3.dp), clip = false)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(DaylightPaper, DaylightPaperBottom),
                    start = Offset.Zero,
                    end = Offset(900f, 1300f),
                ),
                RoundedCornerShape(3.dp),
            )
            .then(click)
            .semantics(mergeDescendants = true) { contentDescription = "$recipient：$message。$signature，$sentTime" },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val ruleGap = (if (full) 43.dp else 32.dp).toPx()
            val start = if (full) 126.dp.toPx() else 70.dp.toPx()
            var y = start
            while (y < size.height - 48.dp.toPx()) {
                drawLine(
                    color = DaylightPaperRule,
                    start = androidx.compose.ui.geometry.Offset(18.dp.toPx(), y),
                    end = androidx.compose.ui.geometry.Offset(size.width - 16.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx(),
                )
                y += ruleGap
            }
            val corner = 25.dp.toPx()
            val fold = Path().apply {
                moveTo(size.width - corner, size.height)
                lineTo(size.width, size.height - corner)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(fold, DaylightFold)
            drawPath(fold, DaylightPaperShadow, style = Stroke(width = 1.dp.toPx()))
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (full) 26.dp else 19.dp, vertical = if (full) 30.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(recipient, color = DaylightPaperInk, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text(sentTime, color = DaylightPaperInk.copy(alpha = .74f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(if (full) 34.dp else 8.dp))
            Text(
                message,
                modifier = Modifier.fillMaxWidth().then(if (full) Modifier.heightIn(min = 88.dp) else Modifier),
                color = DaylightPaperInk,
                fontFamily = FontFamily.Serif,
                fontSize = if (full) 25.sp else 21.sp,
                lineHeight = if (full) 43.sp else 32.sp,
                maxLines = if (full) Int.MAX_VALUE else 2,
                overflow = if (full) TextOverflow.Clip else TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(if (full) 38.dp else 8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(signature, color = DaylightPaperInk, fontFamily = FontFamily.Cursive, fontSize = if (full) 25.sp else 19.sp, fontWeight = FontWeight.Medium)
                    Text("♡", color = DaylightPaperInk.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(relativeTime, color = DaylightPaperInk.copy(alpha = .76f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    if (!full && onClick != null) DaylightIcon(R.drawable.daylight_icon_arrow, null, Modifier.size(15.dp), DaylightPaperInk.copy(alpha = .72f))
                }
            }
        }
        Canvas(
            Modifier.align(Alignment.TopCenter).padding(top = if (full) 1.dp else 0.dp)
                .width(if (full) 94.dp else 76.dp).height(if (full) 23.dp else 19.dp)
                .rotate(-6f),
        ) {
            val tooth = 1.3.dp.toPx()
            val teeth = 10
            val edge = Path().apply {
                moveTo(0f, tooth)
                for (i in 1..teeth) lineTo(size.width * i / teeth, if (i % 2 == 0) tooth else 0f)
                for (i in teeth downTo 0) lineTo(size.width * i / teeth, size.height - if (i % 2 == 0) tooth else 0f)
                close()
            }
            drawPath(edge, DaylightTape)
            drawLine(
                color = Color.White.copy(alpha = .32f),
                start = Offset(1.dp.toPx(), size.height * .46f),
                end = Offset(size.width - 1.dp.toPx(), size.height * .46f),
                strokeWidth = .7.dp.toPx(),
            )
        }
    }
}

@Composable
internal fun DaylightSectionTitle(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = DaylightInk)
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                modifier = Modifier.clickable(role = Role.Button, onClick = onAction).padding(10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = DaylightContact,
            )
        }
    }
}
