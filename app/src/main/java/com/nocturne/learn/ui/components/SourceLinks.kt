package com.nocturne.learn.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.learn.data.search.WebSearchSource
import com.nocturne.learn.ui.design.RowDot
import com.nocturne.learn.ui.theme.NocturneColors

/** 搜索结果只露出标题、站点和链接；点开才离开应用。 */
@Composable
fun SourceLinks(
    sources: List<WebSearchSource>,
    modifier: Modifier = Modifier,
) {
    if (sources.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RowDot(NocturneColors.Moon.copy(alpha = 0.75f), diameter = 4.dp)
            Text(
                text = "  参考来源",
                style = MaterialTheme.typography.labelSmall,
                color = NocturneColors.TextTertiary,
            )
        }
        Spacer(Modifier.height(4.dp))
        sources.take(3).forEach { source ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        runCatching { uriHandler.openUri(source.url) }
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = source.title.ifBlank { source.site },
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.Moon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = source.site.ifBlank { source.url },
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturneColors.TextTertiary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
