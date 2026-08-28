package maestro.orchestra.workspace

import maestro.orchestra.WorkspaceConfig
import maestro.orchestra.error.ValidationError
import maestro.orchestra.yaml.CustomActionCatalog
import maestro.orchestra.yaml.CustomActionDefinition
import maestro.orchestra.yaml.builtInCommandNames
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.streams.toList

object CustomActionDiscovery {

    private val defaultPatterns = listOf("actions/*.yaml", "actions/*.yml")

    /**
     * Action names come from matching filenames. The conventional actions directory is
     * intentionally non-recursive; nested layouts must opt in with an explicit `actions` glob.
     */
    fun discover(
        workspaceRoots: Set<Path>,
        configuredPatterns: WorkspaceConfig.StringList?,
    ): CustomActionCatalog = discover(workspaceRoots, configuredPatterns, failOnNoMatches = true)

    /**
     * A detached flow does not carry its workspace root. Prefer the config directory,
     * then walk up from the flow until the first root containing matching actions.
     */
    fun discoverForSingleFlow(
        flow: Path,
        config: Path,
        configuredPatterns: WorkspaceConfig.StringList?,
    ): CustomActionCatalog {
        if (configuredPatterns?.isEmpty() == true) return CustomActionCatalog.EMPTY

        val candidates = buildList {
            add(config.toAbsolutePath().normalize().parent)
            var current: Path? = flow.toAbsolutePath().normalize().parent
            while (current != null) {
                add(current)
                current = current.parent
            }
        }.distinct()

        candidates.forEach { root ->
            val catalog = discover(setOf(root), configuredPatterns, failOnNoMatches = false)
            if (catalog.files.isNotEmpty()) return catalog
        }

        if (configuredPatterns != null) throw noPatternMatches(configuredPatterns)
        return CustomActionCatalog.EMPTY
    }

    private fun discover(
        workspaceRoots: Set<Path>,
        configuredPatterns: WorkspaceConfig.StringList?,
        failOnNoMatches: Boolean,
    ): CustomActionCatalog {
        val patterns = configuredPatterns ?: defaultPatterns
        if (patterns.isEmpty()) return CustomActionCatalog.EMPTY

        val matches = workspaceRoots
            .flatMap { root -> matchingFiles(root, patterns).map { root to it } }
            .distinctBy { (_, path) -> path.toAbsolutePath().normalize() }
            .sortedBy { (_, path) -> path.toString() }

        if (failOnNoMatches && configuredPatterns != null && matches.isEmpty()) {
            throw noPatternMatches(configuredPatterns)
        }

        val definitions = matches.map { (root, match) ->
            val absoluteRoot = root.toAbsolutePath().normalize()
            val absoluteMatch = match.toAbsolutePath().normalize()
            CustomActionDefinition(
                name = match.nameWithoutExtension,
                path = match,
                sourceDescription = absoluteRoot.relativize(absoluteMatch).pathString,
            )
        }

        definitions.groupBy { it.name }.filterValues { it.size > 1 }.entries.firstOrNull()?.let { (name, duplicates) ->
            throw ValidationError(
                "Custom action name '$name' is defined by multiple files:\n" +
                    duplicates.joinToString("\n") { "- ${it.path}" }
            )
        }

        definitions.firstOrNull { it.name in builtInCommandNames }?.let { definition ->
            throw ValidationError(
                "Custom action '${definition.name}' conflicts with a built-in Maestro command: ${definition.path}"
            )
        }

        return CustomActionCatalog.of(definitions)
    }

    private fun matchingFiles(root: Path, patterns: List<String>): List<Path> {
        return patterns.flatMap { pattern -> matchingFiles(root, pattern) }
    }

    private fun matchingFiles(root: Path, pattern: String): List<Path> {
        val absolutePattern = root.resolve(pattern).normalize().pathString
        val matcher = root.fileSystem.getPathMatcher("glob:${escapeSlashesForWindows(absolutePattern)}")
        val walkRoot = globWalkRoot(root, pattern)
        if (!walkRoot.exists()) return emptyList()

        return Files.walk(walkRoot).use { paths ->
            paths
                .filter { it.isRegularFile() && it.extension.lowercase() in setOf("yaml", "yml") }
                .filter(matcher::matches)
                .toList()
        }
    }

    private fun globWalkRoot(root: Path, pattern: String): Path {
        val firstGlob = pattern.indexOfFirst { it in "*?{[" }
        val staticPrefix = if (firstGlob == -1) pattern else pattern.substring(0, firstGlob)
        val prefixPath = root.resolve(staticPrefix).normalize()
        return if (firstGlob != -1 && (staticPrefix.endsWith('/') || staticPrefix.endsWith('\\'))) {
            prefixPath
        } else {
            prefixPath.parent ?: root
        }
    }

    private fun escapeSlashesForWindows(path: String): String = path.replace("\\", "\\\\")

    private fun noPatternMatches(patterns: List<String>) = ValidationError(
        "Custom action pattern(s) did not match any YAML files:\n${patterns.joinToString("\n") { "- $it" }}"
    )
}
