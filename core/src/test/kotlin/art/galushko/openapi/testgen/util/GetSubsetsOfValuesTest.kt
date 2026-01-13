package art.galushko.openapi.testgen.util

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@Epic("Test utils validation")
@Feature("Validation of getSubsetsOfValues function")
@Suppress("NoMultipleSpaces")
class GetSubsetsOfValuesTest {

    @Nested
    @Story("Parameter combinations tests")
    @DisplayName("Parameter combinations tests")
    inner class ParameterCombinationsTests {

        @Test
        @DisplayName("Default behavior: exclude empty and complete sets")
        fun testDefaultBehavior() {
            val input = listOf("a", "b", "c")
            val result = getSubsetsOfValues(input)

            assertEquals(6, result.size, "Should have 2^3 - 2 = 6 subsets")
            assertFalse(result.any { it.isEmpty() }, "Should not contain empty set")
            assertFalse(result.any { it.toSet() == input.toSet() }, "Should not contain complete set")
        }

        @Test
        @DisplayName("includeCompleteSet = true: include complete set, exclude empty")
        fun testIncludeCompleteSet() {
            val input = listOf("a", "b", "c")
            val result = getSubsetsOfValues(input, includeCompleteSet = true)

            assertEquals(7, result.size, "Should have 2^3 - 1 = 7 subsets")
            assertFalse(result.any { it.isEmpty() }, "Should not contain empty set")
            assertTrue(result.any { it.toSet() == input.toSet() }, "Should contain complete set")
        }

        @Test
        @DisplayName("includeEmptySet = true: include empty set, exclude complete")
        fun testIncludeEmptySet() {
            val input = listOf("a", "b", "c")
            val result = getSubsetsOfValues(input, includeEmptySet = true)

            assertEquals(7, result.size, "Should have 2^3 - 1 = 7 subsets")
            assertTrue(result.any { it.isEmpty() }, "Should contain empty set")
            assertFalse(result.any { it.toSet() == input.toSet() }, "Should not contain complete set")
        }

        @Test
        @DisplayName("Both true: include both empty and complete sets")
        fun testBothParameters() {
            val input = listOf("a", "b", "c")
            val result = getSubsetsOfValues(input, includeCompleteSet = true, includeEmptySet = true)

            assertEquals(8, result.size, "Should have 2^3 = 8 subsets")
            assertTrue(result.any { it.isEmpty() }, "Should contain empty set")
            assertTrue(result.any { it.toSet() == input.toSet() }, "Should contain complete set")
        }

        @ParameterizedTest
        @CsvSource(
            "false, false, 6",
            "true, false, 7",
            "false, true, 7",
            "true, true, 8"
        )
        @DisplayName("Verify all parameter combinations return correct count")
        fun testAllCombinations(includeAll: Boolean, excludeAll: Boolean, expectedSize: Int) {
            val input = listOf("a", "b", "c")
            val result = getSubsetsOfValues(input, includeAll, excludeAll)
            assertEquals(expectedSize, result.size)
        }
    }

    @Nested
    @Story("Basic functionality tests")
    @DisplayName("Basic functionality tests")
    inner class BasicTests {

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        @DisplayName("Should return proper subsets for [a, b, c]")
        fun testThreeElements(includeCompleteSet: Boolean) {
            val input = listOf("a", "b", "c")
            val result = getSubsetsOfValues(input, includeCompleteSet)

            if (includeCompleteSet) {
                assertEquals(7, result.size, "Should have 2^3 - 1 = 7 subsets (excluding empty)")
                assertTrue(result.any { it.toSet() == input.toSet() }, "Should contain complete set")
            } else {
                assertEquals(6, result.size, "Should have 2^3 - 2 = 6 subsets (excluding empty and complete)")
                assertFalse(result.any { it.toSet() == input.toSet() }, "Should not contain complete set")
            }

            // Verify common subsets are present in both cases
            val commonExpected = listOf(
                setOf("a"),
                setOf("b"),
                setOf("c"),
                setOf("a", "b"),
                setOf("a", "c"),
                setOf("b", "c"),
            )
            assertThat(result.map { it.toSet() }).containsAll(commonExpected)

            // Verify an empty set is NOT included
            assertFalse(result.any { it.isEmpty() })
        }

        @Test
        @DisplayName("Should return empty list for single element (default params)")
        fun testSingleElementDefault() {
            val input = listOf("a")
            val result = getSubsetsOfValues(input)

            assertTrue(result.isEmpty(), "No proper subsets exist (excluding empty and complete)")
        }

        @Test
        @DisplayName("Should return single element when includeCompleteSet = true")
        fun testSingleElementIncludeAll() {
            val input = listOf("a")
            val result = getSubsetsOfValues(input, includeCompleteSet = true)

            assertEquals(1, result.size)
            assertEquals(listOf("a"), result[0])
        }

        @Test
        @DisplayName("Should return empty set when includeEmptySet = true")
        fun testSingleElementExcludeAll() {
            val input = listOf("a")
            val result = getSubsetsOfValues(input, includeEmptySet = true)

            assertEquals(1, result.size)
            assertTrue(result[0].isEmpty())
        }

        @Test
        @DisplayName("Should return both empty and complete for single element when both params true")
        fun testSingleElementBothParams() {
            val input = listOf("a")
            val result = getSubsetsOfValues(input, includeCompleteSet = true, includeEmptySet = true)

            assertEquals(2, result.size)
            assertTrue(result.any { it.isEmpty() })
            assertTrue(result.any { it == listOf("a") })
        }

        @Test
        @DisplayName("Should return empty list for empty input (default)")
        fun testEmptyInputDefault() {
            val input = emptyList<String>()
            val result = getSubsetsOfValues(input)

            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("Should return empty list when includeEmptySet = true on empty input")
        fun testEmptyInputExcludeAll() {
            val input = emptyList<String>()
            val result = getSubsetsOfValues(input, includeEmptySet = true)

            assertEquals(0, result.size)
        }

        @Test
        @DisplayName("Should work with integers")
        fun testWithIntegers() {
            val input = listOf(1, 2)
            val result = getSubsetsOfValues(input)

            assertEquals(2, result.size)

            val expected = listOf(
                setOf(1),
                setOf(2)
            )

            assertThat(result.map { it.toSet() }).containsAll(expected)
        }

        @Test
        @DisplayName("Should preserve element order in subsets")
        fun testElementOrder() {
            val input = listOf("a", "b", "c")
            val result = getSubsetsOfValues(input)

            // Find the subset containing both "a" and "b"
            val subsetAB = result.first { it.containsAll(listOf("a", "b")) && it.size == 2 }

            assertEquals(listOf("a", "b"), subsetAB)
        }
    }

    @Nested
    @Story("Empty set specific tests")
    @DisplayName("Empty set specific tests")
    inner class EmptySetTests {

        @Test
        @DisplayName("Empty set should be first when includeEmptySet = true")
        fun testEmptySetFirst() {
            val input = listOf("a", "b")
            val result = getSubsetsOfValues(input, includeEmptySet = true)

            assertEquals(emptyList<String>(), result[0], "Empty set should be first (mask 0)")
        }

        @Test
        @DisplayName("Empty set should not affect other subsets")
        fun testEmptySetDoesNotAffectOthers() {
            val input = listOf("a", "b")
            val resultWithEmpty = getSubsetsOfValues(input, includeEmptySet = true)
            val resultWithoutEmpty = getSubsetsOfValues(input)

            val withoutEmptyFromFirst = resultWithEmpty.filter { it.isNotEmpty() }
            assertEquals(resultWithoutEmpty, withoutEmptyFromFirst)
        }

        @Test
        @DisplayName("Should handle empty set with includeCompleteSet and includeEmptySet")
        fun testEmptySetWithBothParams() {
            val input = listOf("a", "b")
            val result = getSubsetsOfValues(input, includeCompleteSet = true, includeEmptySet = true)

            assertEquals(4, result.size) // 2^2 = 4: {}, {a}, {b}, {a,b}
            assertEquals(emptyList<String>(), result[0])
            assertEquals(listOf("a", "b"), result.last())
        }
    }

    @Nested
    @Story("Edge cases")
    @DisplayName("Edge cases")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Should handle duplicate elements")
        fun testDuplicateElements() {
            val input = listOf("a", "a", "b")
            val result = getSubsetsOfValues(input)

            assertEquals(6, result.size)
            // Each subset is a valid combination, duplicates are preserved
            assertTrue(result.contains(listOf("a", "a")))
        }

        @Test
        @DisplayName("Should work with larger sets")
        fun testLargerSet() {
            val input = listOf(1, 2, 3, 4)
            val result = getSubsetsOfValues(input)

            assertEquals(14, result.size, "Should have 2^4 - 2 = 14 subsets")
            assertFalse(result.any { it.size == 4 }, "Should not contain the complete set")
            assertFalse(result.any { it.isEmpty() }, "Should not contain empty set")
        }

        @Test
        @DisplayName("Should work with larger sets and all parameters")
        fun testLargerSetAllParams() {
            val input = listOf(1, 2, 3, 4)
            val result = getSubsetsOfValues(input, includeCompleteSet = true, includeEmptySet = true)

            assertEquals(16, result.size, "Should have 2^4 = 16 subsets")
            assertTrue(result.any { it.size == 4 }, "Should contain the complete set")
            assertTrue(result.any { it.isEmpty() }, "Should contain empty set")
        }

        @Test
        @DisplayName("Should work with null elements")
        fun testNullElements() {
            val input = listOf("a", null, "b")
            val result = getSubsetsOfValues(input)

            assertEquals(6, result.size)
            assertTrue(result.any { it.contains(null) })
        }

        @Test
        @DisplayName("Should handle list with all nulls")
        fun testAllNulls() {
            val input = listOf<Any?>(null, null)
            val result = getSubsetsOfValues(input, includeCompleteSet = true)

            assertEquals(3, result.size)
            assertTrue(result.all { subset -> subset.all { it == null } })
        }

        @Test
        @DisplayName("Should work with complex objects")
        fun testComplexObjects() {
            data class Person(val name: String, val age: Int)
            val input = listOf(Person("Alice", 30), Person("Bob", 25))
            val result = getSubsetsOfValues(input)

            assertEquals(2, result.size)
            assertTrue(result.any { it.size == 1 && it[0].name == "Alice" })
            assertTrue(result.any { it.size == 1 && it[0].name == "Bob" })
        }
    }

    @Nested
    @Story("Property-based tests")
    @DisplayName("Property-based tests")
    inner class PropertyTests {

        @ParameterizedTest
        @CsvSource(
            "0, false, false, 0",  // empty input, no subsets
            "1, false, false, 0",  // single element, no proper subsets
            "1, true, false, 1",   // single element with includeAll
            "1, false, true, 1",   // single element with excludeAll
            "1, true, true, 2",    // single element with both
            "2, false, false, 2",  // two elements
            "2, true, false, 3",   // two elements with includeAll
            "2, false, true, 3",   // two elements with excludeAll
            "2, true, true, 4",    // two elements with both
            "3, false, false, 6",
            "3, true, false, 7",
            "3, false, true, 7",
            "3, true, true, 8",
            "4, false, false, 14",
            "4, true, false, 15",
            "4, false, true, 15",
            "4, true, true, 16"
        )
        @DisplayName("Number of subsets should match expected formula")
        fun testSubsetCountFormula(n: Int, includeAll: Boolean, excludeAll: Boolean, expected: Int) {
            val input = (1..n).toList()
            val result = getSubsetsOfValues(input, includeAll, excludeAll)

            assertEquals(expected, result.size, "Failed for n=$n, includeAll=$includeAll, excludeAll=$excludeAll")
        }

        @Test
        @DisplayName("All subsets should be unique (when no duplicate input elements)")
        fun testUniqueness() {
            val input = listOf("a", "b", "c", "d")
            val result = getSubsetsOfValues(input, includeCompleteSet = true, includeEmptySet = true)

            val uniqueSubsets = result.map { it.toSet() }.toSet()
            assertEquals(result.size, uniqueSubsets.size, "All subsets should be unique")
        }

        @Test
        @DisplayName("Every element should appear in exactly 2^(n-1) subsets")
        fun testElementFrequency() {
            val input = listOf("a", "b", "c")
            val result = getSubsetsOfValues(input, includeCompleteSet = true, includeEmptySet = true)

            input.forEach { element ->
                val count = result.count { subset -> subset.contains(element) }
                assertEquals(4, count, "Element $element should appear in 2^(3-1) = 4 subsets")
            }
        }

        @Test
        @DisplayName("Subsets should cover all possible sizes")
        fun testSubsetSizes() {
            val input = listOf(1, 2, 3)
            val result = getSubsetsOfValues(input, includeCompleteSet = true, includeEmptySet = true)

            val sizeDistribution = result.groupingBy { it.size }.eachCount()

            assertEquals(1, sizeDistribution[0], "Should have 1 subset of size 0")
            assertEquals(3, sizeDistribution[1], "Should have 3 subsets of size 1")
            assertEquals(3, sizeDistribution[2], "Should have 3 subsets of size 2")
            assertEquals(1, sizeDistribution[3], "Should have 1 subset of size 3")
        }

        @Test
        @DisplayName("Default parameters should exclude both empty and complete")
        fun testDefaultExclusions() {
            val input = listOf("a", "b", "c", "d")
            val result = getSubsetsOfValues(input)

            assertFalse(result.any { it.isEmpty() }, "Empty set should be excluded by default")
            assertFalse(result.any { it.size == input.size }, "Complete set should be excluded by default")
        }
    }

    @Nested
    @Story("Performance and stress tests")
    @DisplayName("Performance and stress tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should handle moderately large input efficiently")
        fun testModeratelyLargeInput() {
            val input = (1..10).toList()
            val result = getSubsetsOfValues(input)

            assertEquals((1 shl 10) - 2, result.size, "Should have 2^10 - 2 = 1022 subsets")
        }

        @Test
        @DisplayName("Should not create duplicate references")
        fun testNoDuplicateReferences() {
            val input = listOf("a", "b")
            val result = getSubsetsOfValues(input, includeCompleteSet = true, includeEmptySet = true)

            // Verify each subset is a separate list instance
            result.forEachIndexed { i, subset1 ->
                result.forEachIndexed { j, subset2 ->
                    if (i != j && subset1.toSet() == subset2.toSet()) {
                        fail("Found duplicate subsets at indices $i and $j")
                    }
                }
            }
        }
    }

    @Nested
    @Story("Bit manipulation verification")
    @DisplayName("Bit manipulation verification")
    inner class BitManipulationTests {

        @Test
        @DisplayName("Should generate subsets in binary order")
        fun testBinaryOrder() {
            val input = listOf("a", "b", "c")
            val result = getSubsetsOfValues(input, includeEmptySet = true)

            // mask 0: {}
            assertEquals(emptyList<String>(), result[0])
            // mask 1: {a}
            assertEquals(listOf("a"), result[1])
            // mask 2: {b}
            assertEquals(listOf("b"), result[2])
            // mask 3: {a, b}
            assertEquals(listOf("a", "b"), result[3])
            // mask 4: {c}
            assertEquals(listOf("c"), result[4])
            // mask 5: {a, c}
            assertEquals(listOf("a", "c"), result[5])
            // mask 6: {b, c}
            assertEquals(listOf("b", "c"), result[6])
        }

        @Test
        @DisplayName("Mask progression should be continuous")
        fun testMaskProgression() {
            val input = listOf(1, 2, 3, 4)
            val result = getSubsetsOfValues(input, includeCompleteSet = true, includeEmptySet = true)

            // Verify sizes progress correctly based on bit count
            val expectedSizes = listOf(0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4)
            val actualSizes = result.map { it.size }

            assertEquals(expectedSizes, actualSizes)
        }
    }
}
