package tj.osonaloqa.chat.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tj.osonaloqa.chat.Dependencies
import tj.osonaloqa.chat.R
import tj.osonaloqa.chat.ui.countries.Country
import tj.osonaloqa.chat.ui.countries.Countries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneScreen(onBack: () -> Unit, onCodeSent: (String) -> Unit) {
    val container = Dependencies.get()
    val scope = rememberCoroutineScope()
    var country by remember { mutableStateOf(Countries.find("TJ")!!) }
    var national by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.enter_phone_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.enter_phone_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickerOpen = true }) {
                    Text("${country.flag} ${country.dial}")
                }
                OutlinedTextField(
                    value = national,
                    onValueChange = { national = it.filter { c -> c.isDigit() || c == ' ' } },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.phone_number)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                )
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        val e164 = national.replace(" ", "").let { n ->
                            if (n.startsWith("+")) n else country.dial + n
                        }
                        container.authRepository.requestOtp(e164)
                            .onSuccess {
                                loading = false
                                onCodeSent(e164)
                            }
                            .onFailure { e ->
                                loading = false
                                error = e.message ?: "error"
                            }
                    }
                },
                enabled = !loading && national.filter { it.isDigit() }.length >= 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Text(stringResource(R.string.send_code), fontSize = 16.sp)
            }
        }
    }

    if (pickerOpen) {
        ModalBottomSheet(onDismissRequest = { pickerOpen = false }) {
            CountryPicker(
                onPick = { c ->
                    country = c
                    pickerOpen = false
                },
            )
        }
    }
}

@Composable
fun CountryPicker(onPick: (Country) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = Countries.all.filter {
        query.isBlank() || it.nameTj.contains(query, true) || it.iso.contains(query, true) || it.dial.contains(query)
    }
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = { Text(stringResource(R.string.search_country)) },
            singleLine = true,
        )
        androidx.compose.foundation.lazy.LazyColumn(Modifier.height(420.dp)) {
            items(filtered.size) { i ->
                val c = filtered[i]
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(c.nameTj) },
                    leadingContent = { Text(c.flag, fontSize = 24.sp) },
                    trailingContent = { Text(c.dial, color = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(c) },
                )
            }
        }
    }
}
