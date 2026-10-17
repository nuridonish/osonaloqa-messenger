package tj.osonaloqa.chat.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.R

@Composable
fun CommunitiesTab(onOpenChat: (String) -> Unit = {}, onNewGroup: () -> Unit = {}) {
    val container = Dependencies.get()
    val chats by container.chatRepository.observeChats().collectAsState(initial = emptyList())
    val groups = chats.filter { it.type != "private" }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewGroup,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            ) { Text(stringResource(R.string.menu_new_group)) }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (groups.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(16.dp))
                        Text(stringResource(R.string.communities_title), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.communities_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(groups.size, key = { groups[it].id }) { i ->
                        val g = groups[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChat(g.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    g.lastMessagePreview.ifBlank { stringResource(R.string.no_messages) },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            if (g.unread > 0) {
                                androidx.compose.material3.Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(g.unread.toString(), color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
