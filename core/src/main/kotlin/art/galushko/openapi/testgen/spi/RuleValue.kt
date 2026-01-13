package art.galushko.openapi.testgen.spi

import java.util.*

/**
 * Result of applying a schema validation rule.
 *
 * Inputs: a description stack (outermost prefix first) and an invalid value.
 * Output: [buildDescription] concatenates the stack into a test case description.
 * Determinism: description order is preserved; [grow] prepends a prefix for composed rules.
 *
 * Equality compares descriptions by list contents to mirror previous Java semantics.
 */
public data class RuleValue(
    public val description: ArrayDeque<String>,
    public val value: Any,
) {

    public constructor(description: String, value: Any) : this(ArrayDeque(listOf(description)), value)

    public fun buildDescription(): String = description.joinToString(separator = "")

    public fun grow(prefix: String, newValue: Any): RuleValue {
        val newDescription = ArrayDeque(this.description)
        newDescription.addFirst(prefix)
        return RuleValue(newDescription, newValue)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RuleValue) return false
        return this.description.toList() == other.description.toList() && this.value == other.value
    }

    override fun hashCode(): Int {
        var result = description.toList().hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}


