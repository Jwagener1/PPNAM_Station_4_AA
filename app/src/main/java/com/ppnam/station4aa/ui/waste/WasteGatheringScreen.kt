package com.ppnam.station4aa.ui.waste

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ppnam.station4aa.domain.model.MachineCatalog
import com.ppnam.station4aa.domain.model.WasteTypeCatalog
import com.ppnam.station4aa.domain.validation.WasteCollectionValidator
import com.ppnam.station4aa.ui.components.AppScaffold
import com.ppnam.station4aa.ui.theme.AmberPrimary
import com.ppnam.station4aa.ui.theme.GraphiteBorder
import com.ppnam.station4aa.ui.theme.GraphiteSurface
import com.ppnam.station4aa.ui.theme.TextMuted
import com.ppnam.station4aa.ui.theme.TextPrimary
import com.ppnam.station4aa.ui.theme.WarningOrange

@Composable
fun WasteGatheringScreen(
    onSettings: () -> Unit,
    viewModel: WasteGatheringViewModel,
) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val session by viewModel.session.collectAsState()
    val collectedBy by viewModel.collectedBy.collectAsState()
    val machineOperatorUserId by viewModel.machineOperatorUserId.collectAsState()
    val lastQueuedMessage by viewModel.lastQueuedMessage.collectAsState()

    var machine by remember { mutableStateOf(MachineCatalog.EXTRUDER_4) }
    var wasteType by remember { mutableStateOf(WasteTypeCatalog.GENERAL) }
    var showConfirm by remember { mutableStateOf(false) }

    // collectedBy comes from the logged-in session (see WasteGatheringViewModel), so this is a
    // defensive check for a malformed session, not something an operator can normally trigger.
    val collectedByError = WasteCollectionValidator.validateCollectedBy(collectedBy)
    val machineOperatorError = WasteCollectionValidator.validateMachineOperatorUserId(machineOperatorUserId)
    val canSubmit = collectedByError == null && machineOperatorError == null

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm waste collection", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConfirmRow("Machine", machine.machineName)
                    ConfirmRow("Waste type", wasteType.display)
                    ConfirmRow("Wastage operator", collectedBy)
                    ConfirmRow("Machine operator ID", machineOperatorUserId)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.submit(machine, wasteType)
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
            containerColor = GraphiteSurface
        )
    }

    AppScaffold(
        title = "Waste Gathering",
        status = connectionStatus,
        onSettings = onSettings,
        operatorName = session?.operatorName?.ifBlank { session?.operatorId },
        operatorRole = session?.role,
        onLogout = viewModel::logout,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (pendingCount > 0) {
                Text(
                    "$pendingCount collection${if (pendingCount == 1) "" else "s"} queued, awaiting delivery",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningOrange,
                )
            }
            lastQueuedMessage?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }

            EnumDropdownSelector(
                label = "Machine",
                options = MachineCatalog.entries,
                selected = machine,
                display = { "${it.machineName} (${it.machineCode})" },
                onSelected = { machine = it },
            )
            EnumDropdownSelector(
                label = "Waste Type",
                options = WasteTypeCatalog.entries,
                selected = wasteType,
                display = { it.display },
                onSelected = { wasteType = it },
            )

            IdentityField(
                label = "Machine Operator ID",
                value = machineOperatorUserId,
                onValueChange = viewModel::onMachineOperatorUserIdChanged,
                errorMessage = machineOperatorError,
            )

            Button(
                onClick = { showConfirm = true },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Submit")
            }
        }
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
}

@Composable
private fun IdentityField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    errorMessage: String?,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = errorMessage != null && value.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AmberPrimary,
                focusedLabelColor = AmberPrimary,
                cursorColor = AmberPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorMessage != null && value.isNotEmpty()) {
            Text(errorMessage, style = MaterialTheme.typography.labelSmall, color = WarningOrange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    display: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        border = BorderStroke(1.dp, GraphiteBorder),
    ) {
        Box(Modifier.padding(12.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                TextField(
                    value = display(selected),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(label, color = TextMuted) },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(display(option)) },
                            onClick = {
                                onSelected(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
