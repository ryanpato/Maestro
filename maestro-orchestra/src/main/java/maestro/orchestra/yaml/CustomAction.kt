package maestro.orchestra.yaml

import java.nio.file.Path

// name is the command users write in YAML.
// path is the actual file Maestro reads.
// sourceDescription is the human-readable path used in reporting and dependency handling.
// 
// Example:
// CustomActionDefinition(
//     name = "login",
//     path = Path.of("/workspace/actions/login.yaml"),
//     sourceDescription = "actions/login.yaml",
// )
data class CustomActionDefinition(
    val name: String,
    val path: Path,
    val sourceDescription: String,
)

// Provides a small read-only interface for getting action info
// 
// catalog["login"] // Find one action
// catalog.names()  // Get all action names
// catalog.files    // Get all action file paths
class CustomActionCatalog private constructor(
    // key is the name
    // "login"  -> CustomActionDefinition(...)
    // "logout" -> CustomActionDefinition(...)
    private val definitions: Map<String, CustomActionDefinition>,
) {

    // Get a list of paths i.e CustomActionDefinition.path
    val files: Set<Path>
        get() = definitions.values.mapTo(linkedSetOf()) { it.path }

    // Get an action
    operator fun get(name: String): CustomActionDefinition? = definitions[name]

    // all possible action names
    fun names(): Set<String> = definitions.keys

    // 
    companion object {
        val EMPTY = CustomActionCatalog(emptyMap())

        fun of(definitions: Collection<CustomActionDefinition>): CustomActionCatalog {
            // use definition.name as its map key.
            return CustomActionCatalog(definitions.associateBy { it.name })
        }
    }
}

// The parsable action form yaml file
data class YamlCustomAction(
    val name: String,
    val params: Map<String, Any?> = emptyMap(),
)
