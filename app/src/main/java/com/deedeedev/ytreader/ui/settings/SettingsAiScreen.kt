package com.deedeedev.ytreader.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deedeedev.ytreader.AppContainer
import com.deedeedev.ytreader.R
import com.deedeedev.ytreader.data.AiApiKeyProfile

private enum class AiProtocolTemplate(val endpoint: String) {
    OPENAI_COMPATIBLE("https://api.openai.com/v1"),
    GEMINI("https://generativelanguage.googleapis.com/v1beta/openai/"),
    CLAUDE("https://api.anthropic.com/v1");

    val labelRes: Int
        get() = when (this) {
            OPENAI_COMPATIBLE -> R.string.settings_ai_protocol_openai
            GEMINI -> R.string.settings_ai_protocol_gemini
            CLAUDE -> R.string.settings_ai_protocol_claude
        }
}

private fun initialProtocolTemplate(endpoint: String): AiProtocolTemplate? = when {
    endpoint.contains("generativelanguage.googleapis.com", ignoreCase = true) -> AiProtocolTemplate.GEMINI
    endpoint.contains("anthropic", ignoreCase = true) -> AiProtocolTemplate.CLAUDE
    endpoint.isBlank() -> AiProtocolTemplate.OPENAI_COMPATIBLE
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAiScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            appContainer,
            appContainer.appContext,
            appContainer.userPreferencesRepository,
            appContainer.youtubeRepository,
            appContainer.videoRepository,
            appContainer.aiChatRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showModelsDialog by rememberSaveable { mutableStateOf(false) }
    var showAddProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_ai_configuration)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            ProtocolSelector(
                selectedTemplate = initialProtocolTemplate(uiState.aiEndpoint),
                onTemplateSelected = { template ->
                    viewModel.setAiEndpoint(template.endpoint)
                }
            )

            OutlinedTextField(
                value = uiState.aiEndpoint,
                onValueChange = { viewModel.setAiEndpoint(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_ai_endpoint)) },
                placeholder = { Text(stringResource(R.string.settings_ai_endpoint_placeholder)) },
                supportingText = { Text(stringResource(R.string.settings_ai_endpoint_hint)) },
                singleLine = true
            )

            ApiKeyField(
                apiKey = uiState.aiApiKey,
                onApiKeyChange = { viewModel.setAiApiKey(it) }
            )

            ApiKeyProfilesSection(
                profiles = uiState.aiApiKeyProfiles,
                activeProfileId = uiState.activeAiApiKeyProfileId,
                onActivateProfile = { viewModel.activateAiApiKeyProfile(it) },
                onDeleteProfile = { viewModel.deleteAiApiKeyProfile(it) },
                onAddProfile = { showAddProfileDialog = true }
            )

            ModelFieldWithFetch(
                model = uiState.aiModel,
                onModelChange = { viewModel.setAiModel(it) },
                isFetchingModels = uiState.isFetchingModels,
                onFetchModels = {
                    viewModel.fetchAiModels()
                    showModelsDialog = true
                }
            )

            OutlinedTextField(
                value = uiState.aiPrompt,
                onValueChange = { viewModel.setAiPrompt(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_ai_cleaning_prompt)) },
                placeholder = { Text(stringResource(R.string.settings_ai_prompt_placeholder)) },
                minLines = 6,
                maxLines = 12
            )

            ConnectionTestSection(
                connectionTest = uiState.connectionTest,
                onTestConnection = { viewModel.testAiConnection() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showModelsDialog) {
        ModelsPickerDialog(
            models = uiState.availableModels,
            isFetching = uiState.isFetchingModels,
            errorMessage = uiState.modelsFetchError,
            selectedModel = uiState.aiModel,
            onRefresh = { viewModel.fetchAiModels() },
            onSelectModel = { model ->
                viewModel.setAiModel(model)
                showModelsDialog = false
            },
            onDismiss = { showModelsDialog = false }
        )
    }

    if (showAddProfileDialog) {
        AddApiKeyProfileDialog(
            onSave = { name, key ->
                viewModel.addAiApiKeyProfile(name, key)
                showAddProfileDialog = false
            },
            onDismiss = { showAddProfileDialog = false }
        )
    }
}

@Composable
private fun ProtocolSelector(
    selectedTemplate: AiProtocolTemplate?,
    onTemplateSelected: (AiProtocolTemplate) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.settings_ai_protocol),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiProtocolTemplate.entries.forEach { template ->
                FilterChip(
                    selected = selectedTemplate == template,
                    onClick = { onTemplateSelected(template) },
                    label = {
                        Text(
                            text = stringResource(template.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    var isKeyVisible by rememberSaveable { mutableStateOf(false) }
    val showLabel = stringResource(R.string.settings_ai_api_key_show)
    val hideLabel = stringResource(R.string.settings_ai_api_key_hide)

    OutlinedTextField(
        value = apiKey,
        onValueChange = onApiKeyChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.settings_ai_api_key)) },
        placeholder = { Text(stringResource(R.string.settings_ai_api_key_placeholder)) },
        visualTransformation = if (isKeyVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                Icon(
                    imageVector = if (isKeyVisible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (isKeyVisible) hideLabel else showLabel
                )
            }
        },
        singleLine = true
    )
}

@Composable
private fun ApiKeyProfilesSection(
    profiles: List<AiApiKeyProfile>,
    activeProfileId: String?,
    onActivateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onAddProfile: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.settings_ai_profiles_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (profiles.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_ai_profiles_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            profiles.forEach { profile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = maskApiKey(profile.apiKey),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = profile.id == activeProfileId,
                        onCheckedChange = { onActivateProfile(profile.id) }
                    )
                    IconButton(onClick = { onDeleteProfile(profile.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.settings_ai_profile_delete)
                        )
                    }
                }
            }
        }
        TextButton(onClick = onAddProfile) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.height(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.settings_ai_profile_add))
        }
    }
}

private fun maskApiKey(apiKey: String): String {
    if (apiKey.isBlank()) return ""
    if (apiKey.length <= 8) return "••••••••"
    return "••••••" + apiKey.takeLast(4)
}

@Composable
private fun ModelFieldWithFetch(
    model: String,
    onModelChange: (String) -> Unit,
    isFetchingModels: Boolean,
    onFetchModels: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.settings_ai_model)) },
            placeholder = { Text(stringResource(R.string.settings_ai_model_placeholder)) },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onFetchModels,
            enabled = !isFetchingModels,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            if (isFetchingModels) {
                CircularProgressIndicator(
                    modifier = Modifier.height(16.dp).width(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.height(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.settings_ai_get_models))
        }
    }
}

@Composable
private fun ModelsPickerDialog(
    models: List<String>,
    isFetching: Boolean,
    errorMessage: String?,
    selectedModel: String,
    onRefresh: () -> Unit,
    onSelectModel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredModels = remember(models, searchQuery) {
        if (searchQuery.isBlank()) {
            models
        } else {
            models.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_ai_models_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.settings_ai_models_search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onRefresh, enabled = !isFetching) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp).width(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.settings_ai_get_models)
                                )
                            }
                        }
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    isFetching -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_ai_fetching_models),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    errorMessage != null -> {
                        Text(
                            text = stringResource(R.string.settings_ai_models_error, errorMessage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    filteredModels.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.settings_ai_models_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                        ) {
                            items(filteredModels) { model ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectModel(model) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = model,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (model == selectedModel) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = stringResource(R.string.selected),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun AddApiKeyProfileDialog(
    onSave: (name: String, key: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_ai_profile_add)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_ai_profile_name)) },
                    placeholder = { Text(stringResource(R.string.settings_ai_profile_name_placeholder)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_ai_api_key)) },
                    placeholder = { Text(stringResource(R.string.settings_ai_api_key_placeholder)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, key) },
                enabled = name.isNotBlank() && key.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ConnectionTestSection(
    connectionTest: AiConnectionTestState,
    onTestConnection: () -> Unit
) {
    Column {
        Button(
            onClick = onTestConnection,
            enabled = !connectionTest.isRunning,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (connectionTest.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp).width(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_ai_test_running))
            } else {
                Text(stringResource(R.string.settings_ai_test_connection))
            }
        }
        val latencyMs = connectionTest.latencyMs
        val errorMessage = connectionTest.errorMessage
        if (latencyMs != null) {
            Text(
                text = stringResource(R.string.settings_ai_test_success, latencyMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (errorMessage != null) {
            Text(
                text = stringResource(R.string.settings_ai_test_failed, errorMessage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
