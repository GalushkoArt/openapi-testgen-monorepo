package art.galushko.openapi.testgen.example.util

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
