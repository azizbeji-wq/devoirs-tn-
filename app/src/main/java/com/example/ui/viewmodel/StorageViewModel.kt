package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.download.DevoirDownloadProgress
import com.example.data.download.DownloadPack
import com.example.data.download.NetworkConnectionKind
import com.example.data.download.PdfDownloadCoordinator
import com.example.data.download.PdfStorageManager
import com.example.data.model.DevoirItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Cible d'une demande de téléchargement (devoir individuel ou pack complet). */
sealed interface DownloadTarget {
    val label: String
    val estimatedSizeMb: Double

    data class SingleDevoir(val devoir: DevoirItem) : DownloadTarget {
        override val label: String = devoir.title
        override val estimatedSizeMb: Double = devoir.sizeMb
    }

    data class Pack(val pack: DownloadPack, val displayTitle: String) : DownloadTarget {
        override val label: String = displayTitle
        override val estimatedSizeMb: Double = pack.remainingSizeMb
    }
}

/** Dialogue de confirmation réseau avant de lancer un téléchargement. */
sealed interface PendingDownloadConfirmation {
    val target: DownloadTarget

    /** L'élève est sur données mobiles alors que le mode Wi-Fi uniquement est actif. */
    data class WifiOnlyBlocked(
        override val target: DownloadTarget
    ) : PendingDownloadConfirmation

    /** Le téléchargement dépasse 20 Mo en données mobiles : avertissement explicite requis. */
    data class LargeMobileWarning(
        override val target: DownloadTarget,
        val sizeMb: Double
    ) : PendingDownloadConfirmation
}

/**
 * ViewModel dédié aux téléchargements hors ligne (WorkManager), aux packs
 * et à l'écran « Stockage » (espace occupé, suppression, règle Wi-Fi & seuil 20 Mo).
 */
class StorageViewModel(
    private val coordinator: PdfDownloadCoordinator,
    private val networkKindProvider: () -> NetworkConnectionKind = { coordinator.getActiveNetworkKind() }
) : ViewModel() {

    val downloadStates: StateFlow<Map<String, DevoirDownloadProgress>> =
        coordinator.observeDownloadStates()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    private val _totalUsedBytes = MutableStateFlow(coordinator.getTotalUsedBytes())
    val totalUsedBytes: StateFlow<Long> = _totalUsedBytes.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingDownloadConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingDownloadConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun refreshDiskUsage() {
        _totalUsedBytes.value = coordinator.getTotalUsedBytes()
    }

    fun hasResumablePart(devoirId: String): Boolean = coordinator.hasResumablePart(devoirId)

    /**
     * Demande le téléchargement (ou la reprise) d'un devoir unique.
     * Vérifie la politique Wi-Fi par défaut ainsi que le seuil d'avertissement de 20 Mo en données mobiles.
     */
    fun requestDownloadDevoir(devoir: DevoirItem, allowMobileData: Boolean) {
        val target = DownloadTarget.SingleDevoir(devoir)
        evaluateAndStartOrPrompt(target, allowMobileData)
    }

    /**
     * Demande le téléchargement en un clic d'un pack complet (ex. Trimestre 1 — 9ème année).
     */
    fun requestDownloadPack(
        pack: DownloadPack,
        displayTitle: String,
        allowMobileData: Boolean
    ) {
        if (pack.missingDevoirs.isEmpty()) return
        val target = DownloadTarget.Pack(pack, displayTitle)
        evaluateAndStartOrPrompt(target, allowMobileData)
    }

    private fun evaluateAndStartOrPrompt(
        target: DownloadTarget,
        allowMobileData: Boolean
    ) {
        val networkKind = networkKindProvider()
        val exceeds20Mb = target.estimatedSizeMb > PdfStorageManager.MOBILE_WARNING_THRESHOLD_MB

        when {
            // Sur données mobiles avec option Wi-Fi uniquement : proposer d'attendre le Wi-Fi ou d'autoriser
            networkKind == NetworkConnectionKind.MOBILE_METERED && !allowMobileData -> {
                _pendingConfirmation.value = PendingDownloadConfirmation.WifiOnlyBlocked(target)
            }

            // Sur données mobiles autorisées ET fichier/pack > 20 Mo : avertissement obligatoire
            networkKind == NetworkConnectionKind.MOBILE_METERED && allowMobileData && exceeds20Mb -> {
                _pendingConfirmation.value = PendingDownloadConfirmation.LargeMobileWarning(
                    target = target,
                    sizeMb = target.estimatedSizeMb
                )
            }

            else -> {
                executeTargetDownload(target, allowMobileData)
            }
        }
    }

    /** Confirme le téléchargement en données mobiles (après avertissement > 20 Mo ou activation mobile). */
    fun confirmMobileDataDownload(
        alsoEnableMobileDataPreference: Boolean,
        onEnableMobileData: () -> Unit
    ) {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        if (alsoEnableMobileDataPreference) {
            onEnableMobileData()
        }
        // Si on vient de débloquer les données mobiles pour un gros téléchargement (> 20 Mo),
        // afficher l'avertissement > 20 Mo avant de lancer effectivement.
        if (pending is PendingDownloadConfirmation.WifiOnlyBlocked &&
            pending.target.estimatedSizeMb > PdfStorageManager.MOBILE_WARNING_THRESHOLD_MB
        ) {
            _pendingConfirmation.value = PendingDownloadConfirmation.LargeMobileWarning(
                target = pending.target,
                sizeMb = pending.target.estimatedSizeMb
            )
            return
        }
        executeTargetDownload(pending.target, allowMobileData = true)
    }

    /** Met le téléchargement en file d'attente WorkManager (démarrera dès le retour du Wi-Fi). */
    fun queueForWifiOnly() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        executeTargetDownload(pending.target, allowMobileData = false)
    }

    fun dismissPendingConfirmation() {
        _pendingConfirmation.value = null
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun cancelDevoirDownload(devoirId: String) {
        coordinator.cancelDevoirDownload(devoirId)
        refreshDiskUsage()
    }

    fun cancelPackDownload(packId: String) {
        coordinator.cancelPackDownload(packId)
        refreshDiskUsage()
    }

    fun deleteDevoirDownload(devoirId: String) {
        viewModelScope.launch {
            coordinator.deleteDevoirDownload(devoirId)
            refreshDiskUsage()
        }
    }

    fun deletePackDownloads(pack: DownloadPack) {
        viewModelScope.launch {
            coordinator.deletePackDownloads(pack)
            refreshDiskUsage()
        }
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            coordinator.deleteAllDownloads()
            refreshDiskUsage()
        }
    }

    private fun executeTargetDownload(target: DownloadTarget, allowMobileData: Boolean) {
        when (target) {
            is DownloadTarget.SingleDevoir -> {
                coordinator.enqueueDevoirDownload(
                    devoir = target.devoir,
                    allowMobileData = allowMobileData
                )
            }

            is DownloadTarget.Pack -> {
                coordinator.enqueuePackDownload(
                    pack = target.pack,
                    allowMobileData = allowMobileData
                )
            }
        }
        refreshDiskUsage()
    }

    class Factory(
        private val coordinator: PdfDownloadCoordinator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StorageViewModel(coordinator) as T
        }
    }
}
