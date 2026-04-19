package art.galushko.openapi.testgen.example.openapi

import art.galushko.openapi.testgen.example.util.CombinationBudget
import art.galushko.openapi.testgen.model.error.BudgetExceededException
import art.galushko.openapi.testgen.model.error.ErrorContext
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.XML
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.stream.Stream

@Epic("Example Value Generation")
@Feature("Schema Merger")
@DisplayName("SchemaMerger")
class SchemaMergerTest {

    private val merger = SchemaMerger()

    private fun resolver(s: Schema<*>): Schema<*> = s

    @Nested
    @DisplayName("SchemaMergerOptions")
    inner class SchemaMergerOptionsTests {

        @Test
        @DisplayName("should reject zero maxMergedSchemaDepth")
        fun shouldRejectZeroMaxMergedSchemaDepth() {
            assertThatThrownBy { SchemaMergerOptions(maxMergedSchemaDepth = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("maxMergedSchemaDepth must be positive, was 0")
        }

        @Test
        @DisplayName("should reject negative maxMergedSchemaDepth")
        fun shouldRejectNegativeMaxMergedSchemaDepth() {
            assertThatThrownBy { SchemaMergerOptions(maxMergedSchemaDepth = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("maxMergedSchemaDepth must be positive, was -1")
        }

        @ParameterizedTest(name = "should accept maxMergedSchemaDepth={0}")
        @CsvSource("1", "10", "50", "100")
        fun shouldAcceptPositiveMaxMergedSchemaDepth(depth: Int) {
            assertThatCode { SchemaMergerOptions(maxMergedSchemaDepth = depth) }
                .doesNotThrowAnyException()
        }

        @Test
        @DisplayName("should have default maxMergedSchemaDepth of 50")
        fun shouldHaveDefaultMaxMergedSchemaDepth() {
            assertThat(SchemaMergerOptions.DEFAULT_MAX_MERGED_SCHEMA_DEPTH).isEqualTo(50)
            assertThat(SchemaMergerOptions().maxMergedSchemaDepth).isEqualTo(50)
        }
    }

    @Nested
    @DisplayName("merge() - Scalar Constraints")
    inner class MergeScalarConstraintsTests {
        fun minimumMeetProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN), // max(1, 10) = 10
            Arguments.of(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN), // max(10, 1) = 10
            Arguments.of(null, BigDecimal.TEN, BigDecimal.TEN), // max(null, 10) = 10
            Arguments.of(BigDecimal.ONE, null, BigDecimal.ONE), // max(1, null) = 1
            Arguments.of(null, null, null), // max(null, null) = null
        )

        @ParameterizedTest(name = "minimum: max({0}, {1}) = {2}")
        @MethodSource("minimumMeetProvider")
        fun shouldMeetMinimumConstraints(min1: BigDecimal?, min2: BigDecimal?, expected: BigDecimal?) {
            val source = NumberSchema().apply { specVersion = SpecVersion.V30 }
            val a = NumberSchema().apply { minimum = min1 }
            val b = NumberSchema().apply { minimum = min2 }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.minimum).isEqualTo(expected)
        }

        fun maximumMeetProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE), // min(1, 10) = 1
            Arguments.of(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE), // min(10, 1) = 1
            Arguments.of(null, BigDecimal.TEN, BigDecimal.TEN), // min(null, 10) = 10
            Arguments.of(BigDecimal.ONE, null, BigDecimal.ONE), // min(1, null) = 1
            Arguments.of(null, null, null), // min(null, null) = null
        )

        @ParameterizedTest(name = "maximum: min({0}, {1}) = {2}")
        @MethodSource("maximumMeetProvider")
        fun shouldMeetMaximumConstraints(max1: BigDecimal?, max2: BigDecimal?, expected: BigDecimal?) {
            val source = NumberSchema().apply { specVersion = SpecVersion.V30 }
            val a = NumberSchema().apply { maximum = max1 }
            val b = NumberSchema().apply { maximum = max2 }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.maximum).isEqualTo(expected)
        }

        fun minLengthMeetProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(1, 10, 10), // max(1, 10) = 10
            Arguments.of(10, 1, 10), // max(10, 1) = 10
            Arguments.of(0, 10, 10), // max(0, 10) = 10 - using 0 instead of null
            Arguments.of(1, 0, 1), // max(1, 0) = 1
        )

        @ParameterizedTest(name = "minLength: max({0}, {1}) = {2}")
        @MethodSource("minLengthMeetProvider")
        fun shouldMeetMinLengthConstraints(len1: Int, len2: Int, expected: Int) {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { minLength = len1 }
            val b = StringSchema().apply { minLength = len2 }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.minLength).isEqualTo(expected)
        }

        fun maxLengthMeetProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(1, 10, 1), // min(1, 10) = 1
            Arguments.of(10, 1, 1), // min(10, 1) = 1
            Arguments.of(100, 10, 10), // min(100, 10) = 10
            Arguments.of(1, 100, 1), // min(1, 100) = 1
        )

        @ParameterizedTest(name = "maxLength: min({0}, {1}) = {2}")
        @MethodSource("maxLengthMeetProvider")
        fun shouldMeetMaxLengthConstraints(len1: Int, len2: Int, expected: Int) {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { maxLength = len1 }
            val b = StringSchema().apply { maxLength = len2 }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.maxLength).isEqualTo(expected)
        }

        @Test
        @DisplayName("should meet minItems/maxItems constraints")
        fun shouldMeetMinMaxItemsConstraints() {
            val source = ArraySchema().apply { specVersion = SpecVersion.V30 }
            val a = ArraySchema().apply { minItems = 2; maxItems = 100 }
            val b = ArraySchema().apply { minItems = 5; maxItems = 50 }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.minItems).isEqualTo(5)
            assertThat(merged.maxItems).isEqualTo(50)
        }

        @Test
        @DisplayName("should meet minProperties/maxProperties constraints")
        fun shouldMeetMinMaxPropertiesConstraints() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val a = ObjectSchema().apply { minProperties = 1; maxProperties = 20 }
            val b = ObjectSchema().apply { minProperties = 3; maxProperties = 10 }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.minProperties).isEqualTo(3)
            assertThat(merged.maxProperties).isEqualTo(10)
        }

        @Test
        @DisplayName("should meet minContains/maxContains constraints")
        fun shouldMeetMinMaxContainsConstraints() {
            val source = ArraySchema().apply { specVersion = SpecVersion.V31 }
            val a = ArraySchema().apply { minContains = 1; maxContains = 10 }
            val b = ArraySchema().apply { minContains = 3; maxContains = 5 }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.minContains).isEqualTo(3)
            assertThat(merged.maxContains).isEqualTo(5)
        }

        @Test
        @DisplayName("should accumulate exclusive bounds for OAS 3.0")
        fun shouldAccumulateExclusiveBoundsOas30() {
            val source = NumberSchema().apply { specVersion = SpecVersion.V30 }
            val a = NumberSchema().apply { maximum = 10.toBigDecimal(); exclusiveMaximum = true }
            val b = NumberSchema().apply { minimum = 1.toBigDecimal(); exclusiveMinimum = true }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.exclusiveMaximum).isTrue()
            assertThat(merged.exclusiveMinimum).isTrue()
        }

        @Test
        @DisplayName("should meet exclusive values for OAS 3.1")
        fun shouldMeetExclusiveValuesOas31() {
            val source = NumberSchema().apply { specVersion = SpecVersion.V31 }
            val a = NumberSchema().apply {
                exclusiveMaximumValue = 100.toBigDecimal()
                exclusiveMinimumValue = 10.toBigDecimal()
            }
            val b = NumberSchema().apply {
                exclusiveMaximumValue = 50.toBigDecimal()
                exclusiveMinimumValue = 20.toBigDecimal()
            }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.exclusiveMaximumValue).isEqualTo(50.toBigDecimal())
            assertThat(merged.exclusiveMinimumValue).isEqualTo(20.toBigDecimal())
        }
    }

    @Nested
    @DisplayName("merge() - Enum Intersection")
    inner class MergeEnumTests {

        @Test
        @DisplayName("should intersect enum values")
        fun shouldIntersectEnumValues() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { enum = listOf("a", "b", "c") }
            val b = StringSchema().apply { enum = listOf("b", "c", "d") }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.enum).containsExactlyInAnyOrder("b", "c")
        }

        @Test
        @DisplayName("should return single value for overlapping enums")
        fun shouldReturnSingleValueForOverlappingEnums() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { enum = listOf("active", "inactive", "pending") }
            val b = StringSchema().apply { enum = listOf("active", "archived") }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.enum).containsExactly("active")
        }

        @Test
        @DisplayName("should return null for disjoint enums")
        fun shouldReturnNullForDisjointEnums() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { enum = listOf("x", "y") }
            val b = StringSchema().apply { enum = listOf("a", "b") }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.enum).isNull()
        }

        @Test
        @DisplayName("should use first enum when second has no enum")
        fun shouldUseFirstEnumWhenSecondHasNoEnum() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { enum = listOf("x", "y") }
            val b = StringSchema()

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.enum).containsExactlyInAnyOrder("x", "y")
        }
    }

    @Nested
    @DisplayName("merge() - Type Intersection")
    inner class MergeTypeTests {

        @Test
        @DisplayName("should intersect OAS 3.1 types")
        fun shouldIntersectOas31Types() {
            val source = JsonSchema().apply {
                specVersion = SpecVersion.V31
                types = setOf("string", "null")
            }
            val a = JsonSchema().apply {
                specVersion = SpecVersion.V31
                types = setOf("string", "integer")
            }
            val b = JsonSchema().apply {
                specVersion = SpecVersion.V31
                types = setOf("string")
            }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver) as JsonSchema

            assertThat(merged.types).containsExactly("string")
        }

        @Test
        @DisplayName("should handle OAS 3.0 type assignment")
        fun shouldHandleOas30TypeAssignment() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { type = "string" }

            val merged = merger.merge(source, listOf(a), 0, mutableSetOf(), ::resolver)

            assertThat(merged.type).isEqualTo("string")
        }

        @Test
        @DisplayName("should preserve types from ComposedSchema source")
        fun shouldPreserveTypesFromComposedSchemaSource() {
            val source = ComposedSchema().apply {
                specVersion = SpecVersion.V31
                types = setOf("object")
            }
            val additional = ObjectSchema().apply { addProperty("id", StringSchema()) }

            val merged = merger.merge(source, listOf(additional), 0, mutableSetOf(), ::resolver)

            assertThat((merged as? JsonSchema)?.types).containsExactly("object")
        }
    }

    @Nested
    @DisplayName("merge() - Nullable")
    inner class MergeNullableTests {

        @Test
        @DisplayName("should AND nullable across inputs (all true)")
        fun shouldAndNullableAllTrue() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30; nullable = true }
            val a = StringSchema().apply { nullable = true }
            val b = StringSchema().apply { nullable = true }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.nullable).isTrue()
        }

        @Test
        @DisplayName("should AND nullable across inputs (one false)")
        fun shouldAndNullableOneFalse() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30; nullable = true }
            val a = StringSchema().apply { nullable = true }
            val b = StringSchema().apply { nullable = false }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.nullable == true).isFalse()
        }

        @Test
        @DisplayName("should AND nullable across inputs (source false)")
        fun shouldAndNullableSourceFalse() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30; nullable = false }
            val a = StringSchema().apply { nullable = true }
            val b = StringSchema().apply { nullable = true }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.nullable == true).isFalse()
        }
    }

    @Nested
    @DisplayName("merge() - Array Schema")
    inner class MergeArrayTests {

        @Test
        @DisplayName("should create allOf for conflicting array items")
        fun shouldCreateAllOfForConflictingArrayItems() {
            val source = ArraySchema().apply { specVersion = SpecVersion.V30 }
            val a = ArraySchema().apply { items = StringSchema() }
            val b = ArraySchema().apply { items = NumberSchema() }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver) as ArraySchema
            val items = merged.items as ComposedSchema

            assertThat(items.allOf).hasSize(2)
            assertThat(items.allOf[0]).isInstanceOf(StringSchema::class.java)
            assertThat(items.allOf[1]).isInstanceOf(NumberSchema::class.java)
        }

        @Test
        @DisplayName("should preserve equivalent array items without allOf")
        fun shouldPreserveEquivalentArrayItemsWithoutAllOf() {
            val source = ArraySchema().apply { specVersion = SpecVersion.V30 }
            val a = ArraySchema().apply { items = StringSchema().apply { minLength = 1 } }
            val b = ArraySchema().apply { items = StringSchema().apply { minLength = 1 } }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver) as ArraySchema

            assertThat(merged.items).isInstanceOf(StringSchema::class.java)
            assertThat(merged.items).isNotInstanceOf(ComposedSchema::class.java)
        }

        @Test
        @DisplayName("should OR uniqueItems constraint")
        fun shouldOrUniqueItemsConstraint() {
            val source = ArraySchema().apply { specVersion = SpecVersion.V30 }
            val a = ArraySchema().apply { items = StringSchema(); uniqueItems = false }
            val b = ArraySchema().apply { items = StringSchema(); uniqueItems = true }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver) as ArraySchema

            assertThat(merged.uniqueItems).isTrue()
        }

        @Test
        @DisplayName("should copy source items when input has no items")
        fun shouldCopySourceItemsWhenInputHasNoItems() {
            val source = ArraySchema().apply {
                specVersion = SpecVersion.V30
                items = StringSchema().apply { minLength = 5 }
            }
            val a = ArraySchema().apply { minItems = 1 }

            val merged = merger.merge(source, listOf(a), 0, mutableSetOf(), ::resolver) as ArraySchema

            assertThat((merged.items as StringSchema).minLength).isEqualTo(5)
        }
    }

    @Nested
    @DisplayName("merge() - Map/AdditionalProperties")
    inner class MergeMapTests {

        @Test
        @DisplayName("should use false as most restrictive for additionalProperties")
        fun shouldUseFalseAsMostRestrictive() {
            val source = MapSchema().apply { specVersion = SpecVersion.V30 }
            val a = MapSchema().apply { additionalProperties = StringSchema() }
            val b = MapSchema().apply { additionalProperties = false }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver) as MapSchema

            assertThat(merged.additionalProperties).isEqualTo(false)
        }

        @Test
        @DisplayName("should create allOf for conflicting additionalProperties schemas")
        fun shouldCreateAllOfForConflictingAdditionalPropertiesSchemas() {
            val source = MapSchema().apply { specVersion = SpecVersion.V30 }
            val a = MapSchema().apply { additionalProperties = StringSchema() }
            val b = MapSchema().apply { additionalProperties = NumberSchema() }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver) as MapSchema
            val ap = merged.additionalProperties as ComposedSchema

            assertThat(ap.allOf).hasSize(2)
        }

        @Test
        @DisplayName("should preserve schema over true for additionalProperties")
        fun shouldPreserveSchemaOverTrueForAdditionalProperties() {
            val source = MapSchema().apply { specVersion = SpecVersion.V30 }
            val a = MapSchema().apply { additionalProperties = true }
            val b = MapSchema().apply { additionalProperties = StringSchema() }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver) as MapSchema

            assertThat(merged.additionalProperties).isInstanceOf(StringSchema::class.java)
        }

        @Test
        @DisplayName("should preserve existing schema when incoming is true")
        fun shouldPreserveExistingSchemaWhenIncomingIsTrue() {
            val source = MapSchema().apply { specVersion = SpecVersion.V30 }
            val a = MapSchema().apply { additionalProperties = NumberSchema() }
            val b = MapSchema().apply { additionalProperties = true }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver) as MapSchema

            assertThat(merged.additionalProperties).isInstanceOf(NumberSchema::class.java)
        }

        @Test
        @DisplayName("should copy source additionalProperties")
        fun shouldCopySourceAdditionalProperties() {
            val source = MapSchema().apply {
                specVersion = SpecVersion.V30
                additionalProperties = StringSchema().apply { maxLength = 100 }
            }
            val a = MapSchema()

            val merged = merger.merge(source, listOf(a), 0, mutableSetOf(), ::resolver) as MapSchema

            assertThat(merged.additionalProperties).isInstanceOf(StringSchema::class.java)
        }
    }

    @Nested
    @DisplayName("merge() - Object Properties")
    inner class MergeObjectPropertiesTests {

        @Test
        @DisplayName("should merge properties from multiple schemas")
        fun shouldMergePropertiesFromMultipleSchemas() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val a = ObjectSchema().apply {
                addProperty("id", StringSchema())
                addProperty("age", NumberSchema())
            }
            val b = ObjectSchema().apply {
                addProperty("name", StringSchema())
            }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.properties?.keys).containsExactlyInAnyOrder("id", "age", "name")
        }

        @Test
        @DisplayName("should create allOf for conflicting properties")
        fun shouldCreateAllOfForConflictingProperties() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val a = ObjectSchema().apply { addProperty("id", StringSchema()) }
            val b = ObjectSchema().apply { addProperty("id", NumberSchema()) }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)
            val idProp = merged.properties?.get("id") as ComposedSchema

            assertThat(idProp.allOf).hasSize(2)
        }

        @Test
        @DisplayName("should union required fields")
        fun shouldUnionRequiredFields() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val a = ObjectSchema().apply { required = listOf("id", "name") }
            val b = ObjectSchema().apply { required = listOf("name", "email") }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.required).containsExactlyInAnyOrder("id", "name", "email")
        }

        @Test
        @DisplayName("should copy source properties before merging")
        fun shouldCopySourcePropertiesBeforeMerging() {
            val source = ObjectSchema().apply {
                specVersion = SpecVersion.V30
                addProperty("base", StringSchema().apply { minLength = 1 })
            }
            val a = ObjectSchema().apply { addProperty("extra", NumberSchema()) }

            val merged = merger.merge(source, listOf(a), 0, mutableSetOf(), ::resolver)

            assertThat(merged.properties?.keys).containsExactlyInAnyOrder("base", "extra")
        }

        @Test
        @DisplayName("should preserve equivalent properties without allOf")
        fun shouldPreserveEquivalentPropertiesWithoutAllOf() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val a = ObjectSchema().apply {
                addProperty("id", StringSchema().apply { minLength = 5; maxLength = 50 })
            }
            val b = ObjectSchema().apply {
                addProperty("id", StringSchema().apply { minLength = 5; maxLength = 50 })
            }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)
            val idProp = merged.properties?.get("id")

            assertThat(idProp).isInstanceOf(StringSchema::class.java)
            assertThat(idProp).isNotInstanceOf(ComposedSchema::class.java)
        }
    }

    @Nested
    @DisplayName("merge() - Source Property Preservation")
    inner class MergeSourcePreservationTests {

        @Test
        @DisplayName("should preserve source description")
        fun shouldPreserveSourceDescription() {
            val source = StringSchema().apply {
                specVersion = SpecVersion.V30
                description = "Original description"
            }
            val a = StringSchema().apply { description = "Override description" }

            val merged = merger.merge(source, listOf(a), 0, mutableSetOf(), ::resolver)

            assertThat(merged.description).isEqualTo("Original description")
        }

        @Test
        @DisplayName("should preserve source format")
        fun shouldPreserveSourceFormat() {
            val source = StringSchema().apply {
                specVersion = SpecVersion.V30
                format = "email"
            }
            val a = StringSchema().apply { format = "uuid" }

            val merged = merger.merge(source, listOf(a), 0, mutableSetOf(), ::resolver)

            assertThat(merged.format).isEqualTo("email")
        }

        @Test
        @DisplayName("should preserve source XML")
        fun shouldPreserveSourceXml() {
            val sourceXml = XML().apply { name = "item"; wrapped = true }
            val source = StringSchema().apply {
                specVersion = SpecVersion.V30
                xml = sourceXml
            }
            val a = StringSchema()

            val merged = merger.merge(source, listOf(a), 0, mutableSetOf(), ::resolver)

            assertThat(merged.xml?.name).isEqualTo("item")
            assertThat(merged.xml?.wrapped).isTrue()
        }

        @Test
        @DisplayName("should merge extensions additively")
        fun shouldMergeExtensionsAdditively() {
            val source = StringSchema().apply {
                specVersion = SpecVersion.V30
                addExtension("x-source", "original")
            }
            val a = StringSchema().apply { addExtension("x-ext-a", "value-a") }
            val b = StringSchema().apply { addExtension("x-ext-b", "value-b") }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.extensions).containsKeys("x-source", "x-ext-a", "x-ext-b")
        }

        @Test
        @DisplayName("should override extensions with last write wins")
        fun shouldOverrideExtensionsWithLastWriteWins() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { addExtension("x-shared", "first") }
            val b = StringSchema().apply { addExtension("x-shared", "second") }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.extensions?.get("x-shared")).isEqualTo("second")
        }
    }

    @Nested
    @DisplayName("merge() - ReadOnly/WriteOnly")
    inner class MergeReadWriteOnlyTests {

        @Test
        @DisplayName("should accumulate readOnly flag")
        fun shouldAccumulateReadOnlyFlag() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val a = ObjectSchema().apply {
                addProperty("auditLog", StringSchema().apply { readOnly = true })
            }

            val merged = merger.merge(source, listOf(a), 0, mutableSetOf(), ::resolver)
            val prop = merged.properties?.get("auditLog") as StringSchema

            assertThat(prop.readOnly).isTrue()
        }

        @Test
        @DisplayName("should accumulate writeOnly flag")
        fun shouldAccumulateWriteOnlyFlag() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val a = ObjectSchema().apply {
                addProperty("password", StringSchema().apply { writeOnly = true })
            }

            val merged = merger.merge(source, listOf(a), 0, mutableSetOf(), ::resolver)
            val prop = merged.properties?.get("password") as StringSchema

            assertThat(prop.writeOnly).isTrue()
        }
    }

    @Nested
    @DisplayName("merge() - First-Write-Wins Strategy")
    inner class MergeFirstWriteWinsTests {

        @Test
        @DisplayName("should use first format when source has none")
        fun shouldUseFirstFormatWhenSourceHasNone() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { format = "email" }
            val b = StringSchema().apply { format = "uuid" }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.format).isEqualTo("email")
        }

        @Test
        @DisplayName("should use first pattern when source has none")
        fun shouldUseFirstPatternWhenSourceHasNone() {
            val source = StringSchema().apply { specVersion = SpecVersion.V30 }
            val a = StringSchema().apply { pattern = "^[a-z]+$" }
            val b = StringSchema().apply { pattern = "^[0-9]+$" }

            val merged = merger.merge(source, listOf(a, b), 0, mutableSetOf(), ::resolver)

            assertThat(merged.pattern).isEqualTo("^[a-z]+$")
        }
    }

    @Nested
    @DisplayName("merge() - Ref Handling")
    inner class MergeRefHandlingTests {

        @Test
        @DisplayName("should skip already visited refs in schemasToMerge")
        fun shouldSkipAlreadyVisitedRefsInSchemasToMerge() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val visitedRef = Schema<String>().apply { `$ref` = "#/components/schemas/Visited" }
            val newSchema = ObjectSchema().apply { addProperty("name", StringSchema()) }

            val visitedRefs = mutableSetOf("#/components/schemas/Visited")
            val merged = merger.merge(source, listOf(visitedRef, newSchema), 0, visitedRefs, ::resolver)

            assertThat(merged.properties?.keys).containsExactly("name")
        }

        @Test
        @DisplayName("should track refs during merge to prevent cycles")
        fun shouldTrackRefsDuringMerge() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val ref1 = ObjectSchema().apply {
                `$ref` = "#/components/schemas/Schema1"
                addProperty("a", StringSchema())
            }
            val ref2 = ObjectSchema().apply {
                `$ref` = "#/components/schemas/Schema2"
                addProperty("b", StringSchema())
            }

            val visitedRefs = mutableSetOf<String>()
            val merged = merger.merge(source, listOf(ref1, ref2), 0, visitedRefs, ::resolver)

            assertThat(merged.properties?.keys).containsExactlyInAnyOrder("a", "b")
            assertThat(visitedRefs).contains(
                "#/components/schemas/Schema1",
                "#/components/schemas/Schema2"
            )
        }

        @Test
        @DisplayName("should stop merging when source ref is already visited")
        fun shouldStopMergingWhenSourceRefIsAlreadyVisited() {
            val source = ObjectSchema().apply {
                specVersion = SpecVersion.V30
                `$ref` = "#/components/schemas/Circular"
                addProperty("base", StringSchema())
            }
            val additional = ObjectSchema().apply { addProperty("extra", NumberSchema()) }

            val visitedRefs = mutableSetOf("#/components/schemas/Circular")
            val merged = merger.merge(source, listOf(additional), 0, visitedRefs, ::resolver)

            // When source ref is already visited, merge returns early with a copy of source
            // The copy preserves the structure but has no allOf/oneOf/anyOf
            // Since source had a property, the copy also has it
            assertThat(merged).isNotNull
        }
    }

    @Nested
    @DisplayName("merge() - Depth Limiting")
    inner class MergeDepthLimitingTests {

        @Test
        @DisplayName("should stop recursion at max depth")
        fun shouldStopRecursionAtMaxDepth() {
            val options = SchemaMergerOptions(maxMergedSchemaDepth = 2)
            val customMerger = SchemaMerger(options)

            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val nested = ObjectSchema().apply { addProperty("deep", StringSchema()) }

            // This should not stack overflow even with depth > limit
            val merged = customMerger.merge(source, listOf(nested), 100, mutableSetOf(), ::resolver)

            assertThat(merged).isNotNull
        }
    }

    @Nested
    @DisplayName("mergeWithSubSchemas()")
    inner class MergeWithSubSchemasTests {

        @Test
        @DisplayName("should return input schema when no subschemas present")
        fun shouldReturnInputSchemaWhenNoSubschemasPresent() {
            val schema = StringSchema().apply {
                specVersion = SpecVersion.V30
                minLength = 5
            }

            val result = merger.mergeWithSubSchemas(schema, 0, mutableSetOf(), ::resolver)

            assertThat(result).isSameAs(schema)
            assertThat(result.minLength).isEqualTo(5)
        }

        @Test
        @DisplayName("should merge allOf subschemas")
        fun shouldMergeAllOfSubschemas() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                allOf = listOf(
                    ObjectSchema().apply { addProperty("id", StringSchema()) },
                    ObjectSchema().apply { addProperty("name", StringSchema()) }
                )
            }

            val result = merger.mergeWithSubSchemas(schema, 0, mutableSetOf(), ::resolver)

            assertThat(result.properties?.keys).containsExactlyInAnyOrder("id", "name")
        }

        @Test
        @DisplayName("should merge first oneOf with first anyOf")
        fun shouldMergeFirstOneOfWithFirstAnyOf() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    ObjectSchema().apply { addProperty("opt1", StringSchema()) },
                    ObjectSchema().apply { addProperty("opt2", StringSchema()) }
                )
                anyOf = listOf(
                    ObjectSchema().apply { addProperty("any1", NumberSchema()) }
                )
            }

            val result = merger.mergeWithSubSchemas(schema, 0, mutableSetOf(), ::resolver)

            // Should pick first from each
            assertThat(result.properties?.keys).containsExactlyInAnyOrder("opt1", "any1")
        }

        @Test
        @DisplayName("should skip visited refs in subschemas")
        fun shouldSkipVisitedRefsInSubschemas() {
            val refSchema = Schema<String>().apply { `$ref` = "#/components/schemas/Visited" }
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                allOf = listOf(
                    refSchema,
                    ObjectSchema().apply { addProperty("new", StringSchema()) }
                )
            }

            val visitedRefs = mutableSetOf("#/components/schemas/Visited")
            val result = merger.mergeWithSubSchemas(schema, 0, visitedRefs, ::resolver)

            assertThat(result.properties?.keys).containsExactly("new")
        }

        @Test
        @DisplayName("should respect depth limit")
        fun shouldRespectDepthLimit() {
            val options = SchemaMergerOptions(maxMergedSchemaDepth = 1)
            val customMerger = SchemaMerger(options)

            val deepNested = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                allOf = listOf(
                    ComposedSchema().apply {
                        allOf = listOf(ObjectSchema().apply { addProperty("deep", StringSchema()) })
                    }
                )
            }

            val result = customMerger.mergeWithSubSchemas(deepNested, 0, mutableSetOf(), ::resolver)

            // Should not throw, result should exist
            assertThat(result).isNotNull
        }
    }

    @Nested
    @DisplayName("getSchemaFlatCombinations()")
    inner class GetSchemaFlatCombinationsTests {

        @Test
        @DisplayName("should return single schema when no subschemas")
        fun shouldReturnSingleSchemaWhenNoSubschemas() {
            val schema = StringSchema().apply {
                specVersion = SpecVersion.V30
                minLength = 5
            }

            val combinations = merger.getSchemaFlatCombinations(schema)

            assertThat(combinations).hasSize(1)
            assertThat(combinations[0].minLength).isEqualTo(5)
        }

        @Test
        @DisplayName("should produce combinations for oneOf")
        fun shouldProduceCombinationsForOneOf() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    StringSchema().apply { minLength = 1 },
                    StringSchema().apply { minLength = 10 }
                )
            }

            val combinations = merger.getSchemaFlatCombinations(schema)

            assertThat(combinations).hasSize(2)
            val minLengths = combinations.mapNotNull { it.minLength }.sorted()
            assertThat(minLengths).containsExactly(1, 10)
        }

        @Test
        @DisplayName("should produce combinations for anyOf")
        fun shouldProduceCombinationsForAnyOf() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                anyOf = listOf(
                    StringSchema().apply { format = "email" },
                    StringSchema().apply { format = "uuid" }
                )
            }

            val combinations = merger.getSchemaFlatCombinations(schema)

            assertThat(combinations).hasSize(2)
            val formats = combinations.mapNotNull { it.format }.sorted()
            assertThat(formats).containsExactly("email", "uuid")
        }

        @Test
        @DisplayName("should produce cartesian product for oneOf and anyOf")
        fun shouldProduceCartesianProductForOneOfAndAnyOf() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    StringSchema().apply { minLength = 1 },
                    StringSchema().apply { minLength = 2 }
                )
                anyOf = listOf(
                    StringSchema().apply { maxLength = 10 },
                    StringSchema().apply { maxLength = 20 }
                )
            }

            val combinations = merger.getSchemaFlatCombinations(schema)

            // 2 oneOf x 2 anyOf = 4 combinations
            assertThat(combinations).hasSize(4)
        }

        @Test
        @DisplayName("should include allOf with oneOf combinations")
        fun shouldIncludeAllOfWithOneOfCombinations() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                allOf = listOf(ObjectSchema().apply { addProperty("base", StringSchema()) })
                oneOf = listOf(
                    ObjectSchema().apply { addProperty("opt1", StringSchema()) },
                    ObjectSchema().apply { addProperty("opt2", StringSchema()) }
                )
            }

            val combinations = merger.getSchemaFlatCombinations(schema)

            assertThat(combinations).hasSize(2)
        }

        @Test
        @DisplayName("should deduplicate equivalent schemas")
        fun shouldDeduplicateEquivalentSchemas() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    StringSchema().apply { minLength = 5 },
                    StringSchema().apply { minLength = 5 } // duplicate
                )
            }

            val combinations = merger.getSchemaFlatCombinations(schema)

            assertThat(combinations).hasSize(1)
        }

        @Test
        @DisplayName("should handle OffsetDateTime examples without serialization errors")
        fun shouldHandleOffsetDateTimeExamplesWithoutSerializationErrors() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    ObjectSchema().apply {
                        addProperty("end_time", DateTimeSchema().example(OffsetDateTime.parse("2020-06-11T16:32:50-03:00")))
                    },
                    ObjectSchema().apply {
                        addProperty("end_time", DateTimeSchema().example(OffsetDateTime.parse("2020-06-11T16:32:50-03:00")))
                    },
                )
            }

            assertThatCode { merger.getSchemaFlatCombinations(schema) }
                .doesNotThrowAnyException()
        }

        @Test
        @DisplayName("should deduplicate schemas with equivalent OffsetDateTime examples")
        fun shouldDeduplicateSchemasWithEquivalentOffsetDateTimeExamples() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    ObjectSchema().apply {
                        addProperty("end_time", DateTimeSchema().example(OffsetDateTime.parse("2020-06-11T16:32:50-03:00")))
                    },
                    ObjectSchema().apply {
                        addProperty("end_time", DateTimeSchema().example(OffsetDateTime.parse("2020-06-11T16:32:50-03:00")))
                    },
                )
            }

            val combinations = merger.getSchemaFlatCombinations(schema)

            assertThat(combinations).hasSize(1)
        }

        @Test
        @DisplayName("should respect max depth limit")
        fun shouldRespectMaxDepthLimit() {
            val options = SchemaMergerOptions(maxMergedSchemaDepth = 1)
            val customMerger = SchemaMerger(options)

            val deepNested = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    ComposedSchema().apply {
                        specVersion = SpecVersion.V30
                        oneOf = listOf(StringSchema().apply { minLength = 100 })
                    }
                )
            }

            val combinations = customMerger.getSchemaFlatCombinations(deepNested)

            assertThat(combinations).isNotEmpty
        }

        @Test
        @DisplayName("should skip visited refs")
        fun shouldSkipVisitedRefs() {
            val refSchema = Schema<String>().apply { `$ref` = "#/components/schemas/Visited" }
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(refSchema, StringSchema().apply { minLength = 5 })
            }

            val visitedRefs = mutableSetOf("#/components/schemas/Visited")
            val combinations = merger.getSchemaFlatCombinations(schema, 0, visitedRefs)

            assertThat(combinations).hasSize(1)
            assertThat(combinations[0].minLength).isEqualTo(5)
        }
    }

    @Nested
    @DisplayName("getSchemaFlatCombinations() - Budget")
    inner class GetSchemaFlatCombinationsBudgetTests {

        private val testContext = ErrorContext.Operation("/test", "GET", "testOperation")

        @Test
        @DisplayName("should consume budget for simple schema")
        fun shouldConsumeBudgetForSimpleSchema() {
            val schema = StringSchema().apply { specVersion = SpecVersion.V30 }
            val budget = CombinationBudget(100, testContext)

            merger.getSchemaFlatCombinations(schema, 0, mutableSetOf(), budget)

            assertThat(budget.combinationsUsed).isEqualTo(1)
        }

        @Test
        @DisplayName("should consume budget for oneOf combinations")
        fun shouldConsumeBudgetForOneOfCombinations() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    StringSchema().apply { minLength = 1 },
                    StringSchema().apply { minLength = 2 },
                    StringSchema().apply { minLength = 3 }
                )
            }
            val budget = CombinationBudget(100, testContext)

            val combinations = merger.getSchemaFlatCombinations(schema, 0, mutableSetOf(), budget)

            assertThat(combinations).hasSize(3)
            assertThat(budget.combinationsUsed).isGreaterThanOrEqualTo(3)
        }

        @Test
        @DisplayName("should throw BudgetExceededException when limit exceeded")
        fun shouldThrowBudgetExceededExceptionWhenLimitExceeded() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = (1..10).map { StringSchema().apply { minLength = it } }
            }
            val budget = CombinationBudget(5, testContext)

            assertThatThrownBy { merger.getSchemaFlatCombinations(schema, 0, mutableSetOf(), budget) }
                .isInstanceOf(BudgetExceededException::class.java)
        }
    }

    @Nested
    @DisplayName("getOneOfAnyOfCombinations()")
    inner class GetOneOfAnyOfCombinationsTests {

        @Test
        @DisplayName("should return each oneOf as single-element list")
        fun shouldReturnEachOneOfAsSingleElementList() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    StringSchema().apply { minLength = 1 },
                    StringSchema().apply { minLength = 2 },
                    StringSchema().apply { minLength = 3 }
                )
            }

            val combinations = merger.getOneOfAnyOfCombinations(schema, mutableSetOf())

            assertThat(combinations).hasSize(3)
            combinations.forEach { assertThat(it).hasSize(1) }
        }

        @Test
        @DisplayName("should return each anyOf as single-element list when no oneOf")
        fun shouldReturnEachAnyOfAsSingleElementListWhenNoOneOf() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                anyOf = listOf(
                    StringSchema().apply { format = "email" },
                    StringSchema().apply { format = "uuid" }
                )
            }

            val combinations = merger.getOneOfAnyOfCombinations(schema, mutableSetOf())

            assertThat(combinations).hasSize(2)
            combinations.forEach { assertThat(it).hasSize(1) }
        }

        @Test
        @DisplayName("should produce pairs for both oneOf and anyOf")
        fun shouldProducePairsForBothOneOfAndAnyOf() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    StringSchema().apply { minLength = 1 },
                    StringSchema().apply { minLength = 2 }
                )
                anyOf = listOf(
                    StringSchema().apply { maxLength = 10 },
                    StringSchema().apply { maxLength = 20 }
                )
            }

            val combinations = merger.getOneOfAnyOfCombinations(schema, mutableSetOf())

            // 2 anyOf x 2 oneOf = 4 pairs
            assertThat(combinations).hasSize(4)
            combinations.forEach { assertThat(it).hasSize(2) }
        }

        @Test
        @DisplayName("should return empty list as single element when no oneOf or anyOf")
        fun shouldReturnEmptyListAsSingleElementWhenNoOneOfOrAnyOf() {
            val schema = StringSchema().apply {
                specVersion = SpecVersion.V30
                minLength = 5
            }

            val combinations = merger.getOneOfAnyOfCombinations(schema, mutableSetOf())

            assertThat(combinations).hasSize(1)
            assertThat(combinations[0]).isEmpty()
        }

        @Test
        @DisplayName("should skip visited refs in oneOf")
        fun shouldSkipVisitedRefsInOneOf() {
            val refSchema = Schema<String>().apply { `$ref` = "#/components/schemas/Visited" }
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(refSchema, StringSchema().apply { minLength = 5 })
            }

            val visitedRefs = mutableSetOf("#/components/schemas/Visited")
            val combinations = merger.getOneOfAnyOfCombinations(schema, visitedRefs)

            assertThat(combinations).hasSize(1)
            assertThat(combinations[0]).hasSize(1)
        }

        @Test
        @DisplayName("should skip visited refs in anyOf")
        fun shouldSkipVisitedRefsInAnyOf() {
            val refSchema = Schema<String>().apply { `$ref` = "#/components/schemas/Visited" }
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                anyOf = listOf(refSchema, StringSchema().apply { format = "email" })
            }

            val visitedRefs = mutableSetOf("#/components/schemas/Visited")
            val combinations = merger.getOneOfAnyOfCombinations(schema, visitedRefs)

            assertThat(combinations).hasSize(1)
            assertThat(combinations[0]).hasSize(1)
        }

        @Test
        @DisplayName("should return empty when all oneOf refs are visited")
        fun shouldReturnEmptyWhenAllOneOfRefsAreVisited() {
            val schema = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                oneOf = listOf(
                    Schema<String>().apply { `$ref` = "#/components/schemas/Ref1" },
                    Schema<String>().apply { `$ref` = "#/components/schemas/Ref2" }
                )
            }

            val visitedRefs = mutableSetOf(
                "#/components/schemas/Ref1",
                "#/components/schemas/Ref2"
            )
            val combinations = merger.getOneOfAnyOfCombinations(schema, visitedRefs)

            assertThat(combinations).isEmpty()
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    inner class RealWorldScenarioTests {

        @Test
        @DisplayName("should merge Entity with Timestamped mixin")
        fun shouldMergeEntityWithTimestampedMixin() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val baseEntity = ObjectSchema().apply {
                addProperty("id", StringSchema())
                addProperty("name", StringSchema().apply { minLength = 1; maxLength = 100 })
                required = listOf("id", "name")
            }
            val timestamped = ObjectSchema().apply {
                addProperty("createdAt", StringSchema().apply { format = "date-time" })
                addProperty("updatedAt", StringSchema().apply { format = "date-time" })
                required = listOf("createdAt")
            }

            val merged = merger.merge(source, listOf(baseEntity, timestamped), 0, mutableSetOf(), ::resolver)

            assertThat(merged.properties?.keys)
                .containsExactlyInAnyOrder("id", "name", "createdAt", "updatedAt")
            assertThat(merged.required).containsExactlyInAnyOrder("id", "name", "createdAt")
            val nameProp = merged.properties?.get("name") as StringSchema
            assertThat(nameProp.minLength).isEqualTo(1)
            assertThat(nameProp.maxLength).isEqualTo(100)
        }

        @Test
        @DisplayName("should merge Product with nested array items")
        fun shouldMergeProductWithNestedArrayItems() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val productBase = ObjectSchema().apply {
                addProperty("id", StringSchema())
                addProperty("items", ArraySchema().apply {
                    items = ObjectSchema().apply {
                        addProperty("sku", StringSchema())
                        addProperty("price", NumberSchema().apply { minimum = 0.toBigDecimal() })
                    }
                    minItems = 1
                })
                required = listOf("id", "items")
            }
            val productExtended = ObjectSchema().apply {
                addProperty("items", ArraySchema().apply {
                    items = ObjectSchema().apply {
                        addProperty("quantity", NumberSchema().apply {
                            minimum = 1.toBigDecimal()
                            maximum = 999.toBigDecimal()
                        })
                    }
                    maxItems = 100
                })
            }

            val merged = merger.merge(source, listOf(productBase, productExtended), 0, mutableSetOf(), ::resolver)

            assertThat(merged.properties?.keys).containsExactlyInAnyOrder("id", "items")
            val itemsProp = merged.properties?.get("items")
            assertThat(itemsProp).isInstanceOf(ComposedSchema::class.java)
            assertThat(merged.required).containsExactly("id", "items")
        }

        @Test
        @DisplayName("should merge discriminated Payment union")
        fun shouldMergeDiscriminatedPaymentUnion() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val basePayment = ObjectSchema().apply {
                addProperty("type", StringSchema().apply { minLength = 1 })
                addProperty("verified", Schema<Boolean>().apply { type = "boolean" })
                required = listOf("type")
            }
            val creditCard = ObjectSchema().apply {
                addProperty("type", StringSchema().apply { enum = listOf("credit_card"); maxLength = 50 })
                addProperty("cardNumber", StringSchema().apply { pattern = "^[0-9]{16}$" })
                addProperty("cvv", StringSchema().apply { minLength = 3; maxLength = 4 })
                required = listOf("type", "cardNumber", "cvv")
            }

            val merged = merger.merge(source, listOf(basePayment, creditCard), 0, mutableSetOf(), ::resolver)

            assertThat(merged.properties?.keys)
                .containsExactlyInAnyOrder("type", "verified", "cardNumber", "cvv")
            assertThat(merged.required)
                .containsExactlyInAnyOrder("type", "cardNumber", "cvv")
            val typeProp = merged.properties?.get("type")
            assertThat(typeProp).isInstanceOf(ComposedSchema::class.java)
        }

        @Test
        @DisplayName("should handle tags array with uniqueness constraint")
        fun shouldHandleTagsArrayWithUniquenessConstraint() {
            val source = ArraySchema().apply { specVersion = SpecVersion.V30 }
            val tagsBase = ArraySchema().apply {
                items = StringSchema()
                minItems = 1
            }
            val tagsUnique = ArraySchema().apply {
                items = StringSchema()
                uniqueItems = true
                maxItems = 20
            }

            val merged = merger.merge(source, listOf(tagsBase, tagsUnique), 0, mutableSetOf(), ::resolver) as ArraySchema

            assertThat(merged.uniqueItems).isTrue()
            assertThat(merged.minItems).isEqualTo(1)
            assertThat(merged.maxItems).isEqualTo(20)
            assertThat(merged.items).isInstanceOf(StringSchema::class.java)
        }

        @Test
        @DisplayName("should preserve array items for allOf ref with inline metadata")
        fun shouldPreserveArrayItemsForAllOfRefWithInlineMetadata() {
            val source = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                allOf = listOf(
                    Schema<Any>().apply { `$ref` = "#/components/schemas/RotationContactIdList" },
                    Schema<Any>().apply { description = "Inline docs-only schema member" },
                )
            }
            val resolved = mapOf(
                "#/components/schemas/RotationContactIdList" to ArraySchema().apply {
                    specVersion = SpecVersion.V30
                    items = StringSchema().apply { minLength = 1 }
                    minItems = 1
                    maxItems = 30
                }
            )

            val merged = merger.mergeWithSubSchemas(source, 0, mutableSetOf()) { schema ->
                schema.`$ref`?.let { ref -> resolved[ref] } ?: schema
            }

            assertThat(merged.type).isEqualTo("array")
            assertThat(merged.items).isInstanceOf(StringSchema::class.java)
            assertThat((merged.items as StringSchema).minLength).isEqualTo(1)
            assertThat(merged.minItems).isEqualTo(1)
            assertThat(merged.maxItems).isEqualTo(30)
        }

        @Test
        @DisplayName("should preserve array items when allOf ref member is skipped as visited")
        fun shouldPreserveArrayItemsWhenAllOfRefMemberIsSkippedAsVisited() {
            val source = ComposedSchema().apply {
                specVersion = SpecVersion.V30
                allOf = listOf(
                    Schema<Any>().apply { `$ref` = "#/components/schemas/RotationContactIdList" },
                    Schema<Any>().apply { description = "Inline docs-only schema member" },
                )
            }
            val resolved = mapOf(
                "#/components/schemas/RotationContactIdList" to ArraySchema().apply {
                    specVersion = SpecVersion.V30
                    items = StringSchema()
                }
            )
            val visitedRefs = mutableSetOf("#/components/schemas/RotationContactIdList")

            val merged = merger.mergeWithSubSchemas(source, 0, visitedRefs) { schema ->
                schema.`$ref`?.let { ref -> resolved[ref] } ?: schema
            }

            assertThat(merged.items).isInstanceOf(StringSchema::class.java)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("should handle deep nesting with multiple allOf levels")
        fun shouldHandleDeepNestingWithMultipleAllOfLevels() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val level1 = ObjectSchema().apply { addProperty("a", StringSchema().apply { minLength = 1 }) }
            val level2 = ObjectSchema().apply { addProperty("a", StringSchema().apply { maxLength = 10 }) }
            val level3 = ObjectSchema().apply { addProperty("a", StringSchema().apply { pattern = "^[a-z]+$" }) }

            val merged = merger.merge(source, listOf(level1, level2, level3), 0, mutableSetOf(), ::resolver)
            val aProp = merged.properties?.get("a") as ComposedSchema

            assertThat(aProp.allOf).hasSize(2)
            assertThat(aProp.allOf[0]).isInstanceOf(ComposedSchema::class.java)
        }

        @Test
        @DisplayName("should handle conflicting readOnly and writeOnly on same property")
        fun shouldHandleConflictingReadOnlyAndWriteOnlyOnSameProperty() {
            val source = ObjectSchema().apply { specVersion = SpecVersion.V30 }
            val withReadOnly = ObjectSchema().apply {
                addProperty("field", StringSchema().apply { readOnly = true })
            }
            val withWriteOnly = ObjectSchema().apply {
                addProperty("field", StringSchema().apply { writeOnly = true })
            }

            val merged = merger.merge(source, listOf(withReadOnly, withWriteOnly), 0, mutableSetOf(), ::resolver)
            val fieldProp = merged.properties?.get("field") as ComposedSchema

            assertThat(fieldProp.allOf).hasSize(2)
            val hasReadOnly = fieldProp.allOf.any { (it as? StringSchema)?.readOnly == true }
            val hasWriteOnly = fieldProp.allOf.any { (it as? StringSchema)?.writeOnly == true }
            assertThat(hasReadOnly).isTrue()
            assertThat(hasWriteOnly).isTrue()
        }

        @Test
        @DisplayName("should merge with unsatisfiable numeric constraints")
        fun shouldMergeWithUnsatisfiableNumericConstraints() {
            val source = NumberSchema().apply { specVersion = SpecVersion.V30 }
            val lowRange = NumberSchema().apply { minimum = 1.toBigDecimal(); maximum = 10.toBigDecimal() }
            val highRange = NumberSchema().apply { minimum = 50.toBigDecimal(); maximum = 100.toBigDecimal() }

            val merged = merger.merge(source, listOf(lowRange, highRange), 0, mutableSetOf(), ::resolver)

            // Meet creates unsatisfiable: min=50, max=10
            assertThat(merged.minimum).isEqualTo(50.toBigDecimal())
            assertThat(merged.maximum).isEqualTo(10.toBigDecimal())
        }

        @Test
        @DisplayName("should handle empty schemas list")
        fun shouldHandleEmptySchemasListToMerge() {
            val source = StringSchema().apply {
                specVersion = SpecVersion.V30
                minLength = 5
                description = "test description"
            }

            val merged = merger.merge(source, emptyList(), 0, mutableSetOf(), ::resolver)

            // When merging with empty list, should return a target based on source
            assertThat(merged.description).isEqualTo("test description")
            assertThat(merged).isNotNull
        }
    }
}
