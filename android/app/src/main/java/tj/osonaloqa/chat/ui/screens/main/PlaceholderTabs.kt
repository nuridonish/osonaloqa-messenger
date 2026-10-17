package tj.osonaloqa.chat.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tj.osonaloqa.chat.R

@Deprecated(" Replaced by CommunitiesTab.kt", ReplaceWith("CommunitiesTab"))
@Composable
fun CommunitiesTabLegacy() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Groups,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.communities_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.communities_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CallsTab(onOpenDialer: () -> Unit = {}) {
    val container = Dependencies.get()
    val recent by container.chatRepository.observeRecentCalls().collectAsState(initial = emptyList())
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (recent.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.calls_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.calls_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f)) {
                items(recent.size) { i ->
                    val c = recent[i]
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(c.name.ifBlank { c.phone }) },
                        supportingContent = { Text(c.phone) },
                        leadingContent = {
                            Icon(
                                Icons.Filled.Call,
                                contentDescription = null,
                                tint = if (c.outgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        },
                        trailingContent = { Text(tj.osonaloqa.chat.ui.screens.main.formatTime(c.time)) },
                    )
                }
            }
        }
        androidx.compose.material3.Button(
            onClick = onOpenDialer,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) { Text(stringResource(R.string.open_dialer)) }
    }
}
