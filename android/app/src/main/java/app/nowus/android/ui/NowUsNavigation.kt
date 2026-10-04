package app.nowus.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.nowus.android.R

@Composable
internal fun NowUsNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Triple("此刻", R.drawable.daylight_icon_now, "此刻"),
        Triple("一天", R.drawable.daylight_icon_day, "一天"),
        Triple("留话", R.drawable.daylight_icon_note, "留话"),
    )
    Surface(
        color = DaylightSurface,
        border = BorderStroke(1.dp, DaylightLine),
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, (label, icon, description) ->
                val active = index == selected
                val tint = if (active) DaylightContact else DaylightMuted
                Column(
                    Modifier.weight(1f)
                        .heightIn(min = 62.dp)
                        .clip(MaterialTheme.shapes.small)
                        .selectable(selected = active, onClick = { onSelect(index) }, role = Role.Tab)
                        .padding(horizontal = 4.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DaylightIcon(icon, null, Modifier.size(21.dp), tint)
                    Text(
                        text = label,
                        color = tint,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
