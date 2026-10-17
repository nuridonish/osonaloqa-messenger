package tj.osonaloqa.chat.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.R
import tj.osonaloqa.chat.data.remote.CreateStatusReq

@Composable
fun StatusTab(onOpenViewer: () -> Unit = {}) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    var statuses by remember { mutableStateOf<List<tj.osonaloqa.chat.data.remote.StatusDto>>(emptyList()) }
    var composeOpen by remember { mutableStateOf(false) }

    suspend fun load() {
        runCatching {
            container.api.statuses("Bearer ${container.sessionManager.current().accessToken}")
        }.onSuccess { statuses = it.statuses }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { load() }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { composeOpen = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) { Icon(Icons.Filled.Edit, contentDescription = null) }
                SmallFloatingActionButton(
                    onClick = { },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) { Icon(Icons.Filled.Add, contentDescription = null) }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text(
                stringResource(R.string.status_e2e_note),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 13.sp,
            )
            if (statuses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_statuses), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(statuses.size) { i ->
                        val s = statuses[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenViewer() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    (s.authorName ?: s.authorPhone ?: "?").take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(s.authorName ?: s.authorPhone ?: "", fontWeight = FontWeight.SemiBold)
                                Text(
                                    tj.osonaloqa.chat.ui.screens.main.formatTime(s.createdAt),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (composeOpen) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { composeOpen = false },
            title = { Text(stringResource(R.string.new_text_status)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.status_text)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            runCatching {
                                container.api.createStatus(
                                    "Bearer ${container.sessionManager.current().accessToken}",
                                    CreateStatusReq(body = text),
                                )
                            }
                            composeOpen = false
                            load()
                        }
                    },
                    enabled = text.isNotBlank(),
                ) { Text(stringResource(R.string.send)) }
            },
            dismissButton = {
                TextButton(onClick = { composeOpen = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
