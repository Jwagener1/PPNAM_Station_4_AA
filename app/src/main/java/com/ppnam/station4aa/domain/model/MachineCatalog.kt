package com.ppnam.station4aa.domain.model

/**
 * Stand-in for "the handheld's machine catalogue" the contract refers to ("Scan `machineCode`
 * and resolve its corresponding `machineName` from the handheld's machine catalogue" —
 * `C:\Dev\PPNAM-Station-4\DOCS\Station4_Wastage_MQTT_Contract.md`, "Required handheld workflow"
 * step 3).
 *
 * There is no real machine-catalogue source available to this app yet — no barcode/RFID scan
 * integration and no synced directory. This is a small hardcoded placeholder list so the contract
 * fields (`machineCode`, `machineName`) have somewhere to come from; swap it for a real scan +
 * catalogue lookup (or a synced list) when that infrastructure exists. The contract notes Station
 * 4 currently trusts the scanner as authoritative for both fields, so whatever replaces this
 * MUST keep them consistent with each other.
 */
enum class MachineCatalog(val machineCode: String, val machineName: String) {
    EXTRUDER_4("EXT-04", "Extruder 4"),
    EXTRUDER_5("EXT-05", "Extruder 5"),
    MIXER_1("MIX-01", "Mixer 1"),
    PACKAGING_LINE_2("PKG-02", "Packaging Line 2"),
}
