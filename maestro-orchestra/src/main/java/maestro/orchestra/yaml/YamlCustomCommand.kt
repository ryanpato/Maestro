package maestro.orchestra.yaml

// Represents a custom command that is not a built-in Maestro command.
data class YamlCustomCommand(
    val commandName: String,
    val params: Map<String, Any> = emptyMap(),
    val label: String? = null,
    val optional: Boolean = false,
)
