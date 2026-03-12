package com.example.dualmind.ui.recording

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch


@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1. Setup for the Snackbar (Equivalent to ScaffoldMessenger in Flutter)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 2. Permission Handler
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val phoneGranted = permissions[Manifest.permission.READ_PHONE_STATE] == true

        if (audioGranted && phoneGranted) {
            viewModel.toggleRecording()
        } else {
            // Launch the Snackbar asynchronously
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Microphone and Phone permissions are required to record meetings.",
                    actionLabel = "OK",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    // 3. Setup the Pulse Animation for the Record Button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.isRecording) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnimation"
    )

    // Smooth color transition for the status chip
    val statusColor by animateColorAsState(
        targetValue = if (uiState.isRecording) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "statusColor"
    )

    // Wrap the screen in a Scaffold so the Snackbar knows where to appear
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- TIMER TEXT ---
            Text(
                text = viewModel.getFormattedTime(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum" // Keeps numbers fixed-width so they don't jump around
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- STATUS CHIP ---
            Surface(
                shape = CircleShape,
                color = statusColor,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = uiState.statusMessage,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = if (uiState.isRecording) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            // --- RECORD BUTTON WITH PULSE EFFECT ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Background Pulse (Only visible when recording)
                if (uiState.isRecording) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale) // Applies the animation here
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    )
                }

                // The Actual Button
                FloatingActionButton(
                    onClick = {
                        if (uiState.isRecording) {
                            viewModel.toggleRecording()
                        } else {
                            val permissionsToRequest = mutableListOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_PHONE_STATE
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        }
                    },
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    containerColor = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (uiState.isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (uiState.isRecording) "Stop Recording" else "Start Recording",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}