package art.galushko.openapi.testgen.example.util

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Epic("Utilities")
@Feature("Cartesian Product")
@DisplayName("CartesianProduct")
class CartesianProductTest {

    @Test
    @DisplayName("should return empty list for empty input")
    fun shouldReturnEmptyListForEmptyInput() {
        val input = emptyList<List<Int>>()

        val result = input.cartesianProduct()

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("should return single list for single input list")
    fun shouldReturnSingleListForSingleInputList() {
        val input = listOf(listOf(1, 2, 3))

        val result = input.cartesianProduct()

        assertThat(result).containsExactly(
            listOf(1),
            listOf(2),
            listOf(3)
        )
    }

    @Test
    @DisplayName("should compute cartesian product of two lists")
    fun shouldComputeCartesianProductOfTwoLists() {
        val input = listOf(
            listOf(1, 2),
            listOf("a", "b")
        )

        val result = input.cartesianProduct()

        assertThat(result).containsExactlyInAnyOrder(
            listOf(1, "a"),
            listOf(1, "b"),
            listOf(2, "a"),
            listOf(2, "b")
        )
    }

    @Test
    @DisplayName("should compute cartesian product of three lists")
    fun shouldComputeCartesianProductOfThreeLists() {
        val input = listOf(
            listOf(1, 2),
            listOf("a", "b"),
            listOf(true, false)
        )

        val result = input.cartesianProduct()

        assertThat(result).hasSize(8)
        assertThat(result).containsExactlyInAnyOrder(
            listOf(1, "a", true),
            listOf(1, "a", false),
            listOf(1, "b", true),
            listOf(1, "b", false),
            listOf(2, "a", true),
            listOf(2, "a", false),
            listOf(2, "b", true),
            listOf(2, "b", false)
        )
    }

    @Test
    @DisplayName("should handle list with single element in each")
    fun shouldHandleListWithSingleElementInEach() {
        val input = listOf(
            listOf(1),
            listOf("a"),
            listOf(true)
        )

        val result = input.cartesianProduct()

        assertThat(result).containsExactly(
            listOf(1, "a", true)
        )
    }

    @ParameterizedTest(name = "sizes {0}x{1} should produce {2} combinations")
    @MethodSource("sizeProvider")
    @DisplayName("should produce expected number of combinations")
    fun shouldProduceExpectedNumberOfCombinations(
        size1: Int,
        size2: Int,
        expectedSize: Int
    ) {
        val input = listOf(
            (1..size1).toList(),
            (1..size2).toList()
        )

        val result = input.cartesianProduct()

        assertThat(result).hasSize(expectedSize)
    }

    companion object {
        @JvmStatic
        fun sizeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(1, 1, 1),
            Arguments.of(2, 2, 4),
            Arguments.of(3, 2, 6),
            Arguments.of(4, 3, 12),
            Arguments.of(5, 5, 25)
        )
    }
}
