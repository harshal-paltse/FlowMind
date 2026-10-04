package com.example.flowmind.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.util.concurrent.Executors

private val TEMPLATES = listOf(
    Triple("bill",     "📸 Bill → Expenses",      "image"),
    Triple("lecture",  "🎤 Lecture → Notes",       "audio"),
    Triple("plant",    "📸 Plant → Care Plan",     "image"),
    Triple("meeting",  "🎤 Meeting → Actions",     "audio"),
    Triple("medicine", "📸 Medicine → Reminders",  "image")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveInputScreen(
    onBackClicked: () -> Unit,
    viewModel: LiveInputViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state   by viewModel.state.collectAsState()
    val amps    by viewModel.amplitudes.collectAsState()

    var selectedTemplate by remember { mutableStateOf(TEMPLATES[0]) }
    val inputMode = selectedTemplate.third  // "image" or "audio"

    // ── Permissions ──────────────────────────────────────────────────────────
    var hasCam   by remember { mutableStateOf(context.hasPerm(Manifest.permission.CAMERA)) }
    var hasAudio by remember { mutableStateOf(context.hasPerm(Manifest.permission.RECORD_AUDIO)) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
        hasCam   = map[Manifest.permission.CAMERA]       ?: hasCam
        hasAudio = map[Manifest.permission.RECORD_AUDIO] ?: hasAudio
    }
    LaunchedEffect(Unit) {
        val needed = buildList {
            if (!hasCam)   add(Manifest.permission.CAMERA)
            if (!hasAudio) add(Manifest.permission.RECORD_AUDIO)
        }
        if (needed.isNotEmpty()) permLauncher.launch(needed.toTypedArray())
    }

    // ── Gallery launcher ─────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.inferImage(it, "image", selectedTemplate.first) }
    }

    // ── CameraX capture ──────────────────────────────────────────────────────
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Input", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(pad)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Template chips ────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
                Text("Select template", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TEMPLATES.forEach { tpl ->
                        val selected = selectedTemplate.first == tpl.first
                        FilterChip(
                            selected = selected,
                            onClick  = { selectedTemplate = tpl; viewModel.reset() },
                            label    = { Text(tpl.second, fontSize = 11.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // ── Main input area ───────────────────────────────────────────────
            when {
                state is LiveInputUiState.Result -> ResultCard(
                    result = (state as LiveInputUiState.Result).text,
                    onReset = { viewModel.reset() }
                )

                state is LiveInputUiState.Processing -> ProcessingCard()

                inputMode == "image" -> ImageInputSection(
                    hasCam = hasCam,
                    onCapture = { uri -> viewModel.inferImage(uri, "image", selectedTemplate.first) },
                    onGallery = { galleryLauncher.launch("image/*") },
                    onSetCapture = { imageCapture = it },
                    capturer = imageCapture
                )

                inputMode == "audio" -> AudioInputSection(
                    hasAudio  = hasAudio,
                    isRec     = state is LiveInputUiState.Recording,
                    amplitudes = amps,
                    onStart   = { viewModel.startRecording() },
                    onStop    = { viewModel.stopRecordingAndInfer(selectedTemplate.first) }
                )

                else -> TextInputSection(
                    onSubmit = { viewModel.inferText(it, selectedTemplate.first) }
                )
            }

            // ── Error banner ─────────────────────────────────────────────────
            AnimatedVisibility(state is LiveInputUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text((state as? LiveInputUiState.Error)?.msg ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Image section (CameraX + Gallery) ────────────────────────────────────────

@Composable
private fun ImageInputSection(
    hasCam: Boolean,
    onCapture: (Uri) -> Unit,
    onGallery: () -> Unit,
    onSetCapture: (ImageCapture) -> Unit,
    capturer: ImageCapture?
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (hasCam) {
            // CameraX preview
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    AndroidView(
                        factory = { context ->
                            PreviewView(context).also { previewView ->
                                val camFuture = ProcessCameraProvider.getInstance(context)
                                camFuture.addListener({
                                    val provider = camFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    onSetCapture(capture)
                                    runCatching {
                                        provider.unbindAll()
                                        provider.bindToLifecycle(
                                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture
                                        )
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            }
                        },
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                    // Capture button overlay
                    IconButton(
                        onClick = {
                            capturer?.let { cap ->
                                val file = File(ctx.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
                                val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                                cap.takePicture(opts, Executors.newSingleThreadExecutor(),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                            onCapture(Uri.fromFile(file))
                                        }
                                        override fun onError(ex: ImageCaptureException) {}
                                    })
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Filled.CameraAlt, "Capture",
                            tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        } else {
            PermissionCard("Camera permission required") {}
        }

        OutlinedButton(
            onClick = onGallery,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Image, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pick from Gallery instead")
        }
    }
}

// ── Audio section ─────────────────────────────────────────────────────────────

@Composable
private fun AudioInputSection(
    hasAudio: Boolean,
    isRec: Boolean,
    amplitudes: List<Float>,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!hasAudio) {
            PermissionCard("Microphone permission required") {}
            return@Column
        }

        // Waveform
        val primary = MaterialTheme.colorScheme.primary
        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val barCount = amplitudes.size.coerceAtLeast(1)
                val barW = (size.width / barCount).coerceAtLeast(4f)
                amplitudes.forEachIndexed { i, amp ->
                    val barH = (amp * size.height).coerceIn(4f, size.height)
                    val x = i * barW + barW / 2
                    drawLine(
                        color  = primary,
                        start  = Offset(x, size.height / 2 - barH / 2),
                        end    = Offset(x, size.height / 2 + barH / 2),
                        strokeWidth = (barW * .6f).coerceAtLeast(3f),
                        cap    = StrokeCap.Round
                    )
                }
                if (amplitudes.isEmpty()) {
                    drawLine(primary.copy(.3f), Offset(0f, size.height / 2),
                        Offset(size.width, size.height / 2), 2f)
                }
            }
        }

        Text(
            if (isRec) "Recording… tap Stop when done" else "Tap Rec to start",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(.6f)
        )

        // Pulsing record button
        val scale by animateFloatAsState(if (isRec) 1.1f else 1f,
            infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")

        IconButton(
            onClick = { if (isRec) onStop() else onStart() },
            modifier = Modifier
                .size((72 * scale).dp)
                .background(
                    if (isRec) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    CircleShape
                )
        ) {
            Icon(
                if (isRec) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isRec) "Stop" else "Record",
                tint = Color.White, modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ── Text section ──────────────────────────────────────────────────────────────

@Composable
fun TextInputSection(onSubmit: (String) -> Unit = {}) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            label = { Text("Describe your task…") },
            shape = RoundedCornerShape(12.dp)
        )
        Button(
            onClick = { if (text.isNotBlank()) { onSubmit(text); text = "" } },
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Run")
        }
    }
}

// ── Result card ───────────────────────────────────────────────────────────────

@Composable
private fun ResultCard(result: String, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Result", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Text(result, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer,
                lineHeight = 22.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReset) { Text("New Input") }
            }
        }
    }
}

// ── Processing card ───────────────────────────────────────────────────────────

@Composable
private fun ProcessingCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            Column {
                Text("Processing…", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Routing to best model", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(.5f))
            }
        }
    }
}

// ── Permission card ───────────────────────────────────────────────────────────

@Composable
private fun PermissionCard(message: String, onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
            Button(onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Grant Permission")
            }
        }
    }
}

private fun Context.hasPerm(perm: String) =
    ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
