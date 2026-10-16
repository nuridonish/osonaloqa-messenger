package tj.osonaloqa.chat.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.R

private data class MainTab(
    val titleRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onOpenChat: (String) -> Unit, onOpenSettings: () -> Unit) {
    val container = Dependencies.get()
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        container.chatRepository.startSync { container.sessionManager.current().accessToken }
        container.chatRepository.refreshChats()
    }

    val chats by container.chatRepository.observeChats().collectAsState(initial = emptyList())
    val unreadTotal = chats.sumOf { it.unread }

    val tabs = listOf(
        MainTab(R.string.tab_chats, Icons.Outlined.Chat, Icons.Filled.Chat),
        MainTab(R.string.tab_status, Icons.Outlined.Circle, Icons.Filled.Circle),
        MainTab(R.string.tab_communities, Icons.Outlined.Groups, Icons.Filled.Groups),
        MainTab(R.string.tab_calls, Icons.Outlined.Call, Icons.Filled.Call),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (tab == 0) "OSON ALOQA" else stringResource(tabs[tab].titleRes),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            BadgedBox(badge = {
                                if (i == 0 && unreadTotal > 0) Badge { Text(unreadTotal.toString()) }
                            }) {
                                Icon(if (tab == i) t.selectedIcon else t.icon, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(t.titleRes)) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> ChatsTab(onOpenChat = onOpenChat)
                1 -> StatusTab()
                2 -> CommunitiesTab()
                3 -> CallsTab()
            }
        }
    }
}
