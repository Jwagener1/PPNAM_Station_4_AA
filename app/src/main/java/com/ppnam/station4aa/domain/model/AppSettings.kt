package com.ppnam.station4aa.domain.model

/**
 * Device configuration.
 *
 * Broker host/port/websocket/TLS defaults match the deployment default stated in the normative
 * contract (`C:\Dev\PPNAM-Station-4\DOCS\Station4_Wastage_MQTT_Contract.md` — "Broker |
 * Deployment-configured; the current Station 4 default is `ppnam-mqtt:1883`"), not Station 2's
 * broker — the two stations are separate deployments. All of it remains operator-editable in
 * Settings because the contract calls the broker "deployment-configured".
 *
 * [deviceId] doubles as this handheld's MQTT client identifier (see MqttClientFactory) — the
 * contract requires "a unique, stable client ID" per publisher.
 *
 * Broker credentials have no defaults deliberately: a default here is an APK constant shipped to
 * every device. [com.ppnam.station4aa.data.security.SecureCredentialStore] holds the password
 * encrypted under an Android Keystore key; this field carries it in memory only, between being
 * read out of that store and being handed to the MQTT client.
 */
data class AppSettings(
    val deviceId: String = "station4_handheld_1",
    val mqttHost: String = "ppnam-mqtt",
    val mqttPort: Int = 1883,
    val mqttUseWebSocket: Boolean = false,
    val mqttUseTls: Boolean = false,
    val mqttUsername: String = "",
    val mqttPassword: String = "",
) {
    /** True once this handheld has been provisioned with its own broker credential. */
    val hasBrokerCredential: Boolean
        get() = mqttUsername.isNotBlank() && mqttPassword.isNotBlank()
}
