package art.galushko.openapi.testgen.util

import art.galushko.openapi.testgen.model.KeyValuePair

/**
 * Returns a new map with the entry for the given key removed.
 *
 * @param name key to remove
 * @return a new map instance without the entry for [name]
 */
public fun <V> Map<String, V>.remove(name: String): MutableMap<String, V> = this.toMutableMap().also { it.remove(name) }

/**
 * Removes entries from the map whose keys are listed in the provided list of names.
 *
 * @param names the list of keys to be removed from the map
 * @return a new mutable map with the specified keys removed
 */
public fun <V> Map<String, V>.remove(names: List<String>): MutableMap<String, V> = this.toMutableMap().also { names.forEach { name -> it.remove(name) } }

/**
 * Returns a new map with the provided pair added, replacing any existing value for the key.
 *
 * @param pair key-value pair to add or replace
 * @return a new map instance with [pair] applied
 */
public fun <V> Map<String, V>.addOrReplace(pair: Pair<String, V>): Map<String, V> = this.toMutableMap().plus(pair)

/**
 * Adds the provided key-value pairs to the map or replaces existing entries with the same keys.
 *
 * This function creates a mutable copy of the original map, adds or replaces the entries,
 * and then returns the updated map as a new instance, leaving the original map unmodified.
 *
 * @param pairs a list of key-value pairs to add or replace in the map
 * @return a new map with the provided key-value pairs added or replaced
 */
public fun <V> Map<String, V>.addOrReplace(pairs: List<Pair<String, V>>): Map<String, V> = this.toMutableMap().plus(pairs)

/**
 * Returns a new list excluding items whose key equals the provided [key].
 *
 * @param key key to remove
 * @return list without entries for [key]
 */
public fun <T> List<KeyValuePair<String, T>>.remove(key: String, ignoringCase: Boolean = false): List<KeyValuePair<String, T>> =
    this.filter { !it.key.equals(key, ignoringCase) }

/**
 * Removes key-value pairs from the list where the keys match any of the specified keys.
 *
 * @param keys A list of keys to be removed from the original list.
 * @param ignoringCase A boolean flag indicating whether the key matching should ignore the case.
 * @return A new list with the key-value pairs removed based on the specified keys.
 */
public fun <T> List<KeyValuePair<String, T>>.remove(keys: List<String>, ignoringCase: Boolean = false): List<KeyValuePair<String, T>> {
    val keySet = keys.toSet().let { if (ignoringCase) it.map { key -> key.lowercase() } else it }
    return if (ignoringCase) {
        this.filter { !keySet.contains(it.key.lowercase()) }
    } else {
        this.filter { !keySet.contains(it.key) }
    }
}

/**
 * Returns a new list where an item with the given key is added or replaced.
 *
 * @param pair pair to add or replace by key
 * @return updated list containing the provided [pair]
 */
public fun <T : Any?> List<KeyValuePair<String, T>>.addOrReplace(pair: KeyValuePair<String, T>, ignoringCase: Boolean = false):
    MutableList<KeyValuePair<String, T>> = this.remove(pair.key, ignoringCase).toMutableList().also { it.add(pair) }

/**
 * Adds or replaces key-value pairs in the current list with key-value pairs from the provided list.
 * If a key from the provided list exists in the current list, the corresponding value is replaced.
 * If ignoringCase is true, key matching is performed case-insensitively.
 *
 * @param pairs The list of key-value pairs to be added or used to replace existing pairs in the current list.
 * @param ignoringCase A boolean flag indicating whether the key matching should ignore the case when determining replacements.
 * @return A new list containing the updated key-value pairs with the specified additions or replacements.
 */
public fun <T : Any?> List<KeyValuePair<String, T>>.addOrReplace(pairs: List<KeyValuePair<String, T>>, ignoringCase: Boolean = false):
    MutableList<KeyValuePair<String, T>> = this.remove(pairs.map { it.key }, ignoringCase).toMutableList().also { pairs.forEach { pair -> it.add(pair) } }

/**
 * Generates subsets of the input list based on specified inclusion criteria.
 *
 * @param elements The input list of elements to generate subsets from
 * @param includeCompleteSet Whether to include the complete set (all elements) in the result (default: false)
 * @param includeEmptySet Whether to include the empty set in the result (default: false)
 * @return List of all generated subsets, where each subset is represented as a list
 *
 * Example:
 * ```
 * val input = listOf(1, 2, 3)
 * val subsets = getSubsetsOfValues(input) // Returns: [[1], [2], [3], [1,2], [1,3], [2,3]]
 * ```
 *
 * Implementation details:
 * - Uses bit manipulation for efficient subset generation
 * - Each number from 1 to 2^n-1 represents a unique subset combination
 * - The i-th bit in the mask determines if the i-th element is included
 *
 * Time Complexity: O(n * 2^n) where n is the number of elements
 * Space Complexity: O(n * 2^n) for storing all subsets
 */
public fun <T> getSubsetsOfValues(elements: List<T>, includeCompleteSet: Boolean = false, includeEmptySet: Boolean = false): List<List<T>> {
    val n = elements.size
    val totalSubsets = if (includeCompleteSet) 1 shl n else (1 shl n) - 1 // 2^n
    val startMask = if (includeEmptySet) 0 else 1
    val result = mutableListOf<List<T>>()

    // Iterate through all possible subsets using bit manipulation
    for (mask in startMask until totalSubsets) {
        val subset = mutableListOf<T>()
        for (i in 0 until n) {
            // Check if the i-th bit is set in the mask
            if (mask and (1 shl i) != 0) {
                subset.add(elements[i])
            }
        }
        result.add(subset)
    }

    return result
}

/**
 * Computes the Cartesian product of multiple lists, generating all possible combinations
 * by selecting one element from each input list.
 *
 * For example, given lists [[1, 2], [3, 4]], the result would be [[1, 3], [1, 4], [2, 3], [2, 4]].
 *
 * @receiver A list of lists containing elements of type [T]
 * @return A list containing all possible combinations, where each combination is a list
 *         containing one element from each input list
 *
 * Performance characteristics:
 * - Time complexity: O(n * m) where n is the total number of output combinations and m is the number of input lists
 * - Space complexity: O(n * m) for storing all combinations
 */
public fun <T> List<List<T>>.cartesianProduct(): List<List<T>> {
    if (this.isEmpty()) return emptyList()
    return this.fold(listOf(emptyList())) { acc, list ->
        acc.flatMap { combination ->
            list.map { element ->
                combination + element
            }
        }
    }
}


