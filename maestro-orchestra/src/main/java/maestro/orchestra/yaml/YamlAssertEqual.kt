package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonCreator

data class YamlAssertEqual(
    val value1: String? = null,
    val value2: String? = null,
    val optional: Boolean = false,
    val label: String? = null,
) {
    companion object {

        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun parse(condition: Any): YamlAssertEqual {
            return when (condition) { 
                is Map<*, *> -> {
                    val value1 = condition.getOrDefault("value1", null)?.toString()
                    val value2 = condition.getOrDefault("value2", null)?.toString()
                    val optional = condition.getOrDefault("optional", false) as Boolean
                    val label = condition.getOrDefault("label", null) as String?
                    YamlAssertEqual(value1, value2, optional, label)
                }
                else -> throw UnsupportedOperationException("Cannot deserialise assert equal with data type ${condition.javaClass}")
            }
        }
    }
}
