package com.sri.aham.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sri.aham.assistant.viewmodel.ModelState

@Composable
fun ModelSetupScreen(
    modelState: ModelState,
    downloadProgress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    errorMessage: String?,
    hfToken: String,
    onHfTokenChanged: (String) -> Unit,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Aham Assistant",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(8.dp))

            when (modelState) {
                ModelState.NOT_READY -> {
                    Text(
                        text = "Runs fully offline using Gemma 4 E2B (~2.6 GB). Downloaded once and stored on-device.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    HfTokenField(token = hfToken, onTokenChanged = onHfTokenChanged)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Get a free token at huggingface.co/settings/tokens\nand accept the Gemma license on the model page.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hfToken.isNotBlank(),
                    ) {
                        Text("Download Model")
                    }
                }

                ModelState.DOWNLOADING -> {
                    val downloadedMB = downloadedBytes / (1024f * 1024f)
                    val totalMB = totalBytes / (1024f * 1024f)
                    val statusText = when {
                        downloadProgress > 0f && totalBytes > 0 ->
                            "Downloading… ${(downloadProgress * 100).toInt()}%  (%.0f / %.0f MB)".format(downloadedMB, totalMB)
                        downloadedBytes > 0 ->
                            "Downloading… %.0f MB".format(downloadedMB)
                        else -> "Starting download…"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    if (downloadProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                ModelState.LOADING -> {
                    Text(
                        text = "Loading model into memory…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }

                ModelState.ERROR -> {
                    Text(
                        text = errorMessage ?: "An unknown error occurred.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(16.dp))
                    HfTokenField(token = hfToken, onTokenChanged = onHfTokenChanged)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
                    }
                }

                ModelState.READY -> { /* handled by parent — chat UI shown instead */ }
            }
        }
    }
}

@Composable
private fun HfTokenField(token: String, onTokenChanged: (String) -> Unit) {
    var showToken by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = token,
        onValueChange = onTokenChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("HuggingFace Token") },
        placeholder = { Text("hf_…") },
        singleLine = true,
        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showToken = !showToken }) {
                Icon(
                    imageVector = if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (showToken) "Hide token" else "Show token",
                )
            }
        },
    )
}
