package app.nowus.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.nowus.android.R

@Composable
internal fun NowUsNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("此刻", R.drawable.icon_now, "查看双方此刻"),
        Triple("一天", R.drawable.icon_day, "查看我们的一天"),
        Triple("留话", R.drawable.icon_note, "查看留话"),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .navigationBarsPadding()
            .selectableGroup(),
    ) {
        items.forEachIndexed { index, (label, icon, description) ->
            val active = index == selected
            val ink = if (active) Accent else Muted
            Column(
                Modifier
                    .weight(1f)
                    .selectable(active, onClick = { onSelect(index) }, role = Role.Tab)
                    .heightIn(min = 42.dp)
                    .padding(vertical = 0.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = description,
                    tint = ink,
                    modifier = Modifier
                        .size(28.dp)
                        .background(if (active) Tint else Panel, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                )
                Text(
                    label,
                    color = ink,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                )
            }
        }
    }
}
