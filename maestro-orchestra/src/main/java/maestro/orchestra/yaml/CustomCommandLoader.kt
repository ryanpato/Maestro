package maestro.orchestra.yaml

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

// Loads custom command definitions from the workspace's commands directory.
object CustomCommandLoader {

    // Find workspace
    fun findWorkspaceRoot(flowPath: Path): Path? {
        var current = flowPath.toAbsolutePath().parent
        while (current != null) {
            if (current.resolve("config.yaml").exists()) return current
            current = current.parent
        }
        return null
    }

    // Find commands from workspace
    fun getCommandsDirectory(workspaceRoot: Path): Path? {
        val commandsDir = workspaceRoot.resolve("commands")
        return if (commandsDir.exists() && commandsDir.isDirectory()) commandsDir else null
    }

    // Find command from commands
    fun findCustomCommand(flowPath: Path, commandName: String): Path? {
        val workspaceRoot = findWorkspaceRoot(flowPath) ?: return null
        val commandsDir = getCommandsDirectory(workspaceRoot) ?: return null
        return commandsDir.resolve("$commandName.yaml").takeIf { it.exists() }
    }
}
