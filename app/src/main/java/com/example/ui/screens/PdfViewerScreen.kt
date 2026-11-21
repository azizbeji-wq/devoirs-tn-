package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.data.ai.PdfPageImageScaler
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.adaptiveCrimson
import com.example.ui.components.adaptiveEmerald
import com.example.ui.pdf.PdfDocumentController
import com.example.ui.pdf.PdfLoadError
import com.example.ui.viewmodel.ExamModePhase
import com.example.ui.viewmodel.ExamModeUiState
import com.example.ui.viewmodel.ExamTimerState

/** Type de document ouvert dans le lecteur PDF. */
enum class PdfDocumentKind {
    SUJET,
    CORRIGE
}

/** État interne d'ouverture d'un document PDF. */
private sealed interface PdfDocumentUiState {
    data object Loading : PdfDocumentUiState
    data class Ready(val controller: PdfDocumentController) : PdfDocumentUiState
    data class Error(val error: PdfLoadError) : PdfDocumentUiState
}

/**
 * Lecteur PDF natif utilisant [android.graphics.pdf.PdfRenderer] :
 * - Défilement vertical fluide page par page,
 * - Zoom par pincement (pinch-to-zoom) et double-tap + boutons +/- accessibles,
 * - Indicateur « Page X / Y » flottant,
 * - Rendu asynchrone en arrière-plan avec cache LRU de bitmaps,
 * - Barre flottante du chronomètre toujours visible par-dessus le PDF,
 * - Gestion propre des erreurs (fichier manquant, PDF corrompu, mémoire insuffisante),
 * - Libération stricte des ressources à la fermeture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    documentTitle: String,
    pdfAssetPath: String,
    documentKind: PdfDocumentKind,
    timerState: ExamTimerState,
    examModeState: ExamModeUiState,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onFinishExamMode: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onExplainCurrentPage: ((pageNumber: Int, documentKind: PdfDocumentKind, pageBitmap: Bitmap) -> Unit)? = null
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCapturingPageForAi by remember { mutableStateOf(false) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var documentState by remember(pdfAssetPath, reloadNonce) {
        mutableStateOf<PdfDocumentUiState>(PdfDocumentUiState.Loading)
    }

    // Ouverture asynchrone du document PDF sur Dispatchers.IO
    LaunchedEffect(pdfAssetPath, reloadNonce) {
        documentState = PdfDocumentUiState.Loading
        val result = PdfDocumentController.openFromAsset(context, pdfAssetPath)
        documentState = result.fold(
            onSuccess = { PdfDocumentUiState.Ready(it) },
            onFailure = { err ->
                val typed = err as? PdfLoadError ?: PdfLoadError.Unexpected(err.message)
                PdfDocumentUiState.Error(typed)
            }
        )
    }

    // Libération garantie du PdfRenderer et du ParcelFileDescriptor dès que l'écran se ferme ou change
    val readyController = (documentState as? PdfDocumentUiState.Ready)?.controller
    DisposableEffect(readyController) {
        onDispose {
            readyController?.close()
        }
    }

    // État de zoom (pincement + double-tap + boutons +/-) et de déplacement quand zoomé
    var targetScale by remember(pdfAssetPath) { mutableFloatStateOf(1f) }
    var panOffset by remember(pdfAssetPath) { mutableStateOf(Offset.Zero) }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 180),
        label = "pdf_zoom_scale"
    )

    val lazyListState = rememberLazyListState()
    val emerald = adaptiveEmerald()

    val isExamInProgress = examModeState.phase == ExamModePhase.IN_PROGRESS
    val badgeText = stringResource(
        if (documentKind == PdfDocumentKind.CORRIGE) R.string.pdf_badge_corrige else R.string.pdf_badge_subject
    )
    val badgeColor = if (documentKind == PdfDocumentKind.CORRIGE) {
        emerald
    } else {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdf_viewer_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = documentTitle,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pdf_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_cd)
                        )
                    }
                },
                actions = {
                    if (readyController != null) {
                        IconButton(
                            onClick = {
                                targetScale = (targetScale - 0.5f).coerceAtLeast(1f)
                                if (targetScale <= 1.02f) panOffset = Offset.Zero
                            },
                            enabled = targetScale > 1.02f,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pdf_zoom_out_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ZoomOut,
                                contentDescription = stringResource(R.string.cd_pdf_zoom_out)
                            )
                        }
                        IconButton(
                            onClick = {
                                targetScale = (targetScale + 0.5f).coerceAtMost(3.5f)
                            },
                            enabled = targetScale < 3.45f,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pdf_zoom_in_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ZoomIn,
                                contentDescription = stringResource(R.string.cd_pdf_zoom_in)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        ) {
            val density = LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            // Largeur de rendu nette (x1.5 pour la netteté lors du zoom, plafonnée dans le contrôleur)
            val renderWidthPx = (maxWidthPx * 1.4f).toInt()

            when (val state = documentState) {
                is PdfDocumentUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.pdf_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is PdfDocumentUiState.Error -> {
                    PdfErrorCard(
                        error = state.error,
                        onRetry = { reloadNonce++ },
                        onBack = onBack,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp)
                    )
                }

                is PdfDocumentUiState.Ready -> {
                    val controller = state.controller
                    val currentPage by remember(controller.pageCount) {
                        derivedStateOf {
                            (lazyListState.firstVisibleItemIndex + 1).coerceIn(1, controller.pageCount)
                        }
                    }

                    // Zone de lecture zoomable (pincement + double-tap + défilement vertical)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (targetScale > 1.15f) {
                                            targetScale = 1f
                                            panOffset = Offset.Zero
                                        } else {
                                            targetScale = 2.2f
                                            val centerX = maxWidthPx / 2f
                                            val centerY = maxHeightPx / 2f
                                            val maxPanX = (maxWidthPx * (targetScale - 1f)) / 2f
                                            val maxPanY = (maxHeightPx * (targetScale - 1f)) / 2f
                                            panOffset = Offset(
                                                x = ((centerX - tapOffset.x) * 0.8f).coerceIn(-maxPanX, maxPanX),
                                                y = ((centerY - tapOffset.y) * 0.4f).coerceIn(-maxPanY, maxPanY)
                                            )
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val pressedPointers = event.changes.count { it.pressed }
                                        val zoomDelta = event.calculateZoom()
                                        val panDelta = event.calculatePan()

                                        // Pincement à 2 doigts (zoom libre 1x -> 3.5x)
                                        if (pressedPointers >= 2 && zoomDelta != 1f) {
                                            val newScale = (targetScale * zoomDelta).coerceIn(1f, 3.5f)
                                            targetScale = newScale
                                            val maxPanX = (maxWidthPx * (newScale - 1f)) / 2f
                                            val maxPanY = (maxHeightPx * (newScale - 1f)) / 2f
                                            panOffset = if (newScale <= 1.02f) {
                                                Offset.Zero
                                            } else {
                                                Offset(
                                                    x = (panOffset.x + panDelta.x).coerceIn(-maxPanX, maxPanX),
                                                    y = (panOffset.y + panDelta.y).coerceIn(-maxPanY, maxPanY)
                                                )
                                            }
                                            event.changes.forEach { it.consume() }
                                        } else if (targetScale > 1.05f && panDelta != Offset.Zero) {
                                            // Déplacement horizontal / pan lorsqu'une page est zoomée
                                            val maxPanX = (maxWidthPx * (targetScale - 1f)) / 2f
                                            val maxPanY = (maxHeightPx * (targetScale - 1f)) / 2f
                                            val nextX = (panOffset.x + panDelta.x).coerceIn(-maxPanX, maxPanX)
                                            val nextY = (panOffset.y + panDelta.y).coerceIn(-maxPanY, maxPanY)
                                            panOffset = Offset(nextX, nextY)
                                            if (kotlin.math.abs(panDelta.x) > kotlin.math.abs(panDelta.y)) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                }
                                .testTag("pdf_pages_list"),
                            // Espace haut pour la barre flottante du chronomètre et bas pour l'indicateur de page
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 74.dp,
                                bottom = 76.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                count = controller.pageCount,
                                key = { index -> "${pdfAssetPath}_page_$index" }
                            ) { pageIndex ->
                                PdfPageCard(
                                    controller = controller,
                                    pageIndex = pageIndex,
                                    renderWidthPx = renderWidthPx
                                )
                            }
                        }
                    }

                    // Barre flottante basse : « Page X / Y » + zoom reset + bouton IA « Explique cette page »
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.90f),
                            shadowElevation = 6.dp,
                            modifier = Modifier.testTag("pdf_page_indicator")
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.pdf_page_indicator,
                                    currentPage,
                                    controller.pageCount
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }

                        if (targetScale > 1.05f) {
                            Surface(
                                onClick = {
                                    targetScale = 1f
                                    panOffset = Offset.Zero
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("pdf_zoom_reset_btn")
                            ) {
                                Text(
                                    text = "${(animatedScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }

                        // Bouton « Explique cette page » (masqué pendant un Mode Examen verrouillé)
                        if (onExplainCurrentPage != null && !isExamInProgress) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    if (!isCapturingPageForAi) {
                                        isCapturingPageForAi = true
                                        coroutineScope.launch {
                                            try {
                                                val pageIndex = (currentPage - 1).coerceIn(0, controller.pageCount - 1)
                                                val result = controller.renderPage(
                                                    pageIndex = pageIndex,
                                                    targetWidthPx = PdfPageImageScaler.DEFAULT_MAX_DIMENSION_PX
                                                )
                                                result.onSuccess { bmp ->
                                                    if (!bmp.isRecycled) {
                                                        onExplainCurrentPage(currentPage, documentKind, bmp)
                                                    }
                                                }
                                            } finally {
                                                isCapturingPageForAi = false
                                            }
                                        }
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("pdf_explain_page_btn"),
                                icon = {
                                    if (isCapturingPageForAi) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = stringResource(R.string.cd_pdf_explain_page)
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        text = stringResource(R.string.pdf_explain_page_btn),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Barre flottante du chronomètre toujours visible par-dessus le lecteur PDF
            FloatingPdfTimerBar(
                timerState = timerState,
                examModeState = examModeState,
                isExamInProgress = isExamInProgress,
                onToggleTimer = onToggleTimer,
                onResetTimer = onResetTimer,
                onFinishExamMode = onFinishExamMode,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * Affiche une page PDF rendue en arrière-plan sous forme de feuille papier blanche.
 */
@Composable
private fun PdfPageCard(
    controller: PdfDocumentController,
    pageIndex: Int,
    renderWidthPx: Int
) {
    var pageBitmap by remember(controller, pageIndex) { mutableStateOf<Bitmap?>(null) }
    var pageError by remember(controller, pageIndex) { mutableStateOf<PdfLoadError?>(null) }
    var retryCounter by remember(controller, pageIndex) { mutableIntStateOf(0) }

    LaunchedEffect(controller, pageIndex, renderWidthPx, retryCounter) {
        pageError = null
        val result = controller.renderPage(pageIndex, renderWidthPx)
        result.fold(
            onSuccess = { bmp ->
                pageBitmap = bmp
                pageError = null
            },
            onFailure = { err ->
                pageBitmap = null
                pageError = err as? PdfLoadError ?: PdfLoadError.Unexpected(err.message)
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_page_$pageIndex"),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        val bmp = pageBitmap
        when {
            bmp != null && !bmp.isRecycled -> {
                val ratio = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_pdf_page_image, pageIndex + 1),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                )
            }

            pageError != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (pageError is PdfLoadError.OutOfMemory) {
                            Icons.Filled.Memory
                        } else {
                            Icons.Filled.BrokenImage
                        },
                        contentDescription = stringResource(R.string.cd_pdf_error_icon),
                        tint = adaptiveCrimson(),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (pageError is PdfLoadError.OutOfMemory) {
                            stringResource(R.string.pdf_error_oom_title)
                        } else {
                            stringResource(R.string.pdf_error_corrupted_title)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF1A1C1E),
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = { retryCounter++ },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(stringResource(R.string.pdf_retry_btn))
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.707f), // Format A4 par défaut pendant le chargement
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text(
                            text = stringResource(R.string.pdf_page_loading, pageIndex + 1),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF5A6270)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Barre flottante du chronomètre affichée par-dessus le lecteur PDF.
 * Affiche le décompte du Mode Examen s'il est actif, sinon le chronomètre libre (Lecture/Pause/Reset).
 */
@Composable
private fun FloatingPdfTimerBar(
    timerState: ExamTimerState,
    examModeState: ExamModeUiState,
    isExamInProgress: Boolean,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onFinishExamMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedSeconds = if (isExamInProgress) {
        examModeState.remainingSeconds
    } else {
        timerState.remainingSeconds
    }
    val hours = displayedSeconds / 3600
    val minutes = (displayedSeconds % 3600) / 60
    val seconds = displayedSeconds % 60
    val formattedTime = "%02d:%02d:%02d".format(hours, minutes, seconds)

    val crimson = adaptiveCrimson()
    val containerColor = if (isExamInProgress) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    }
    val borderColor = if (isExamInProgress) {
        crimson.copy(alpha = 0.5f)
    } else if (timerState.isRunning) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_floating_timer_bar"),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = stringResource(R.string.cd_floating_timer_icon),
                    tint = if (isExamInProgress) crimson else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    if (isExamInProgress) {
                        Text(
                            text = stringResource(R.string.pdf_floating_exam_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = crimson
                        )
                    }
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isExamInProgress) crimson else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("pdf_floating_timer_text")
                    )
                }
            }

            if (isExamInProgress) {
                FilledTonalButton(
                    onClick = onFinishExamMode,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pdf_floating_finish_exam_btn"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.pdf_floating_finish_btn),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = onToggleTimer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pdf_floating_timer_toggle_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (timerState.isRunning) R.string.timer_pause else R.string.timer_start
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    IconButton(
                        onClick = onResetTimer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pdf_floating_timer_reset_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.timer_reset),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Carte d'erreur claire et non bloquante (fichier manquant, PDF corrompu, mémoire insuffisante).
 */
@Composable
private fun PdfErrorCard(
    error: PdfLoadError,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crimson = adaptiveCrimson()
    val icon = when (error) {
        is PdfLoadError.FileNotFound -> Icons.Filled.Description
        is PdfLoadError.CorruptedPdf -> Icons.Filled.BrokenImage
        is PdfLoadError.OutOfMemory -> Icons.Filled.Memory
        is PdfLoadError.Unexpected -> Icons.Filled.ErrorOutline
    }
    val title = when (error) {
        is PdfLoadError.FileNotFound -> stringResource(R.string.pdf_error_missing_title)
        is PdfLoadError.CorruptedPdf -> stringResource(R.string.pdf_error_corrupted_title)
        is PdfLoadError.OutOfMemory -> stringResource(R.string.pdf_error_oom_title)
        is PdfLoadError.Unexpected -> stringResource(R.string.pdf_error_generic_title)
    }
    val body = when (error) {
        is PdfLoadError.FileNotFound -> stringResource(R.string.pdf_error_missing_body, error.path)
        is PdfLoadError.CorruptedPdf -> stringResource(R.string.pdf_error_corrupted_body)
        is PdfLoadError.OutOfMemory -> stringResource(R.string.pdf_error_oom_body)
        is PdfLoadError.Unexpected -> stringResource(R.string.pdf_error_generic_body)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_error_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, crimson.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(crimson.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.cd_pdf_error_icon),
                    tint = crimson,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(stringResource(R.string.back_button_cd))
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pdf_error_retry_btn")
                ) {
                    Text(stringResource(R.string.pdf_retry_btn))
                }
            }
        }
    }
}
