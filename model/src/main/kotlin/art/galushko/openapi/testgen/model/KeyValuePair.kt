package art.galushko.openapi.testgen.model

/**
 * Immutable key-value pair.
 *
 * Used to preserve ordering for headers and cookies where duplicates may exist.
 *
 * @param K type of the key
 * @param V type of the value
 * @property key key component
 * @property value value component
 */
public data class KeyValuePair<out K, out V>(
    val key: K,
    val value: V
)

/**
 * Infix helper to create a [KeyValuePair] from the receiver and the given value.
 *
 * @param value associated value
 * @return a [KeyValuePair] of the receiver and [value]
 */
public infix fun <K, V> K.with(value: V): KeyValuePair<K, V> = KeyValuePair(this, value)

