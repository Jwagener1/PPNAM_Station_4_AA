package com.ppnam.station4aa.ui.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppnam.station4aa.data.mqtt.MqttConnectionManager
import com.ppnam.station4aa.data.mqtt.MqttConnectionState
import com.ppnam.station4aa.data.mqtt.WasteCollectionPublisher
import com.ppnam.station4aa.data.session.OperatorSession
import com.ppnam.station4aa.data.session.OperatorSessionHolder
import com.ppnam.station4aa.data.settings.SettingsRepository
import com.ppnam.station4aa.domain.model.MachineCatalog
import com.ppnam.station4aa.domain.model.WasteCollectionEvent
import com.ppnam.station4aa.domain.model.WasteTypeCatalog
import com.ppnam.station4aa.domain.usecase.AuthUseCase
import com.ppnam.station4aa.ui.components.ConnectionStatus
import com.ppnam.station4aa.ui.components.connectionStatusFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the one screen implementing
 * `C:\Dev\PPNAM-Station-4\DOCS\Station4_Wastage_MQTT_Contract.md`'s "Required handheld workflow".
 */
class WasteGatheringViewModel(
    private val settingsRepository: SettingsRepository,
    private val connectionManager: MqttConnectionManager,
    private val publisher: WasteCollectionPublisher,
    private val sessionHolder: OperatorSessionHolder,
    private val authUseCase: AuthUseCase,
) : ViewModel() {

    val connectionStatus: StateFlow<ConnectionStatus> = connectionStatusFlow(
        connectionManager.connectionState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionStatus.Offline)

    /** Durably queued events awaiting PUBACK — surfaced so the operator can see unsynced work
     * exists, per the contract's reconciliation-visibility requirement. */
    val pendingCount: StateFlow<Int> = publisher.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val session: StateFlow<OperatorSession?> = sessionHolder.session

    /**
     * "Obtain the existing wastage-operator value supplied by the handheld" (workflow step 1) —
     * now the logged-in operator's own identity, not a free-text field. Prefers the display name
     * (closer to what the contract's "or display label" allowance calls for); falls back to the
     * operator ID if the server didn't supply one. SessionWatcher guarantees this screen is never
     * reached without a session, so a null here would be a bug elsewhere, not a normal state.
     */
    val collectedBy: StateFlow<String> = session
        .map { it?.operatorName?.ifBlank { it.operatorId } ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    // Cleared after every submit (workflow step 2: "clear any machine-operator value left from a
    // prior transaction").
    private val _machineOperatorUserId = MutableStateFlow("")
    val machineOperatorUserId: StateFlow<String> = _machineOperatorUserId.asStateFlow()

    private val _lastQueuedMessage = MutableStateFlow<String?>(null)
    val lastQueuedMessage: StateFlow<String?> = _lastQueuedMessage.asStateFlow()

    init {
        viewModelScope.launch { connectionManager.connect(settingsRepository.current()) }
        // Flush anything durably queued while offline as soon as the broker link comes back —
        // the contract requires retrying with the exact original payload, which retryPending()
        // does by re-reading the immutable rows rather than re-deriving anything.
        viewModelScope.launch {
            connectionManager.connectionState
                .filter { it == MqttConnectionState.CONNECTED }
                .collect { publisher.retryPending() }
        }
    }

    fun onMachineOperatorUserIdChanged(value: String) {
        _machineOperatorUserId.value = value
    }

    /**
     * Workflow steps 8–10: generate messageId/collectionId/collectedAtUtc only now, for this
     * completed transaction; durably queue before the first publish attempt (via
     * [WasteCollectionPublisher.submit]); only then clear the machine-operator field. Callers
     * (the confirmation dialog) are expected to have already checked
     * [com.ppnam.station4aa.domain.validation.WasteCollectionValidator] against the current
     * [collectedBy]/[machineOperatorUserId] — this does not re-gate on it, matching the durable
     * write, not validation, being this method's one job.
     */
    fun submit(machine: MachineCatalog, wasteType: WasteTypeCatalog) {
        val event = WasteCollectionEvent.create(
            machineCode = machine.machineCode,
            machineName = machine.machineName,
            wasteTypeCode = wasteType.code,
            collectedBy = collectedBy.value,
            machineOperatorUserId = _machineOperatorUserId.value,
        )
        viewModelScope.launch {
            publisher.submit(event)
            _machineOperatorUserId.value = ""
            // Acceptance criterion 20: a PUBACK (or even just a durable local write) is never
            // presented as Station 4 business acceptance — "Queued", not "Submitted"/"Accepted".
            _lastQueuedMessage.value = "Queued ${event.collectionId} for delivery"
        }
    }

    fun dismissLastQueuedMessage() {
        _lastQueuedMessage.value = null
    }

    /** SessionWatcher (mounted at the nav-graph root) handles the actual navigation back to
     * Login once [sessionHolder]'s session goes null — this just triggers that. */
    fun logout() {
        viewModelScope.launch { authUseCase.logout() }
    }
}
