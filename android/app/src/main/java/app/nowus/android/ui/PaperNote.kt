package app.nowus.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nowus.android.domain.Note
import app.nowus.android.domain.Profile
import java.time.Instant
import kotlin.math.max

/** The same lined, taped paper language is used for the home preview and the full note. */
@Composable
internal fun PaperNote(
    note: Note,
    recipient: Profile,
    sender: Profile,
    now: Instant,
    full: Boolean,
    empty: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val outerRotation = if (full) -1.3f else -.6f
    val horizontal = if (full) 26.dp else 19.dp
    val top = if (full) 30.dp else 17.dp
    val bottom = if (full) 26.dp else 12.dp
    val lineHeight = if (full) 43.sp else 32.sp
    val messageSize = if (full) 25.sp else 21.sp
    val time = Instant.ofEpochMilli(note.updatedMillis)
    val elapsedMinutes = max(0, (now.toEpochMilli() - note.updatedMillis) / 60_000)
    val relative = when {
        elapsedMinutes < 60 -> "$elapsedMinutes 分钟前"
        elapsedMinutes < 24 * 60 -> "${elapsedMinutes / 60} 小时前"
        else -> "${elapsedMinutes / (24 * 60)} 天前"
    }
    Box(
        modifier
            .fillMaxWidth()
            .padding(top = if (full) 34.dp else 8.dp, bottom = if (full) 12.dp else 8.dp)
            .heightIn(min = if (full) 324.dp else 0.dp)
            .rotate(outerRotation)
            .shadow(7.dp, RoundedCornerShape(2.dp), clip = false)
            .drawBehind {
                // Slightly offset second sheet under the top page, as in .paper-note:before.
                drawRoundRect(
                    PaperBottom,
                    topLeft = Offset(3.dp.toPx(), 5.dp.toPx()),
                    size = Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
                drawRoundRect(
                    Brush.linearGradient(listOf(Paper, PaperBottom), start = Offset.Zero, end = Offset(size.width, size.height)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
                // Fine ruled lines continue behind wrapped text and scale with the paper height.
                val step = lineHeight.toPx()
                var y = top.toPx() + 11.dp.toPx() + step
                while (y < size.height - bottom.toPx() - 12.dp.toPx()) {
                    drawLine(PaperRule, Offset(horizontal.toPx(), y), Offset(size.width - horizontal.toPx(), y), 1.dp.toPx())
                    y += step
                }
                // Small folded lower-right corner.
                val fold = if (full) 24.dp.toPx() else 19.dp.toPx()
                val foldPath = Path().apply {
                    moveTo(size.width - fold, size.height)
                    lineTo(size.width, size.height - fold)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(foldPath, PaperFold)
                drawPath(foldPath, PaperFold.copy(alpha = .5f), style = Stroke(width = 1.dp.toPx()))
            }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = horizontal, end = horizontal, top = top, bottom = bottom),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TO ${recipient.name}", color = PaperInk.copy(alpha = .82f), fontSize = if (full) 11.sp else 10.sp, letterSpacing = .6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${sender.cityName()} · ${localClock(time, sender)}", color = PaperInk.copy(alpha = .82f), fontSize = if (full) 11.sp else 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(if (full) 30.dp else 6.dp))
            Text(
                if (empty) "对方还没有留下便签。" else note.text,
                color = if (empty) PaperInk.copy(alpha = .7f) else PaperInk,
                fontFamily = FontFamily.Serif,
                fontSize = if (empty) messageSize * .76f else messageSize,
                lineHeight = lineHeight,
                letterSpacing = .2.sp,
                modifier = Modifier.fillMaxWidth().heightIn(min = if (full) 88.dp else 32.dp),
            )
            Spacer(Modifier.height(if (full) 30.dp else 6.dp))
            Row(Modifier.fillMaxWidth().padding(end = if (full) 4.dp else 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (empty || sender.name.isBlank()) "" else "${sender.name}  ♡", color = PaperInk, fontFamily = FontFamily.Serif, fontSize = if (full) 25.sp else 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (empty) "" else relative, color = PaperInk.copy(alpha = .8f), fontSize = if (full) 11.sp else 10.sp)
            }
        }
        PaperTape(full, Modifier.align(Alignment.TopCenter).offset(y = if (full) (-13).dp else (-11).dp))
    }
}

@Composable
private fun PaperTape(full: Boolean, modifier: Modifier = Modifier) {
    val width = if (full) 96.dp else 77.dp
    val height = if (full) 29.dp else 24.dp
    Box(
        modifier
            .width(width)
            .height(height)
            .rotate(if (full) -7f else -5f)
            .drawBehind {
                val w = size.width
                val h = size.height
                val tape = Path().apply {
                    moveTo(0f, h * .05f)
                    val topFractions = listOf(.08f, .15f, .24f, .33f, .42f, .54f, .66f, .77f, .88f, 1f)
                    topFractions.forEachIndexed { i, fraction -> lineTo(w * fraction, h * if (i % 2 == 0) 0f else .05f) }
                    lineTo(w, h)
                    listOf(.87f, .76f, .65f, .54f, .43f, .32f, .21f, .10f, 0f).forEachIndexed { i, fraction ->
                        lineTo(w * fraction, h * if (i % 2 == 0) .95f else 1f)
                    }
                    close()
                }
                drawPath(tape, PaperTape)
                drawLine(PaperTapeEdge, Offset(.5.dp.toPx(), h * .08f), Offset(.5.dp.toPx(), h * .92f), 1.dp.toPx())
                drawLine(PaperTapeEdge, Offset(w - .5.dp.toPx(), h * .08f), Offset(w - .5.dp.toPx(), h * .92f), 1.dp.toPx())
            }
    )
}

private fun localClock(instant: Instant, profile: Profile): String =
    instant.atZone(profile.zone()).toLocalTime().let { "%02d:%02d".format(it.hour, it.minute) }

@Composable
internal fun OwnNoteSurface(note: Note, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Panel,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("我留下的", color = Muted, fontSize = 12.sp)
            Text(note.text, color = Ink, fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 32.sp)
        }
    }
}
