package art.galushko.openapi.testgen.generation

import art.galushko.openapi.testgen.openapi.SkipReason
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Epic("Test Generator")
@Feature("Circular Reference Detection")
@DisplayName("Circular Reference Detection Tests")
class CircularReferenceTest {

    private val dummyTestCase = createBasicTestCase(
        name = "test",
        method = "GET",
        path = "/",
    )

    private fun createContext(maxDepth: Int = 50): TestGenerationContext {
        return createTestContext(
            validCase = dummyTestCase,
            maxDepth = maxDepth,
        )
    }

    @Nested
    @DisplayName("Identity Cycle Detection")
    inner class IdentityCycleDetection {

        @Test
        @DisplayName("should not flag different instances with identical structure as a cycle")
        @Description("Two separate schema instances with the same structure are NOT cycles — identity check only")
        fun shouldNotDetectStructuralCycleForDifferentInstances() {
            // Arrange
            val schemaA = ObjectSchema().name("A").apply {
                addProperty("prop", StringSchema())
                required = listOf("prop")
            }
            val schemaB = ObjectSchema().name("B").apply {
                addProperty("prop", StringSchema())
                required = listOf("prop")
            }
            val context = createContext()

            // Act
            val contextA = context.withVisitedSchema(schemaA, "A")
            val contextB = contextA?.withVisitedSchema(schemaB, "B")

            // Assert — different instances, not the same Java object → not a cycle
            assertThat(contextA).isNotNull
            assertThat(contextB)
                .describedAs("Structurally identical but distinct instances should NOT be detected as a cycle")
                .isNotNull
            assertThat(contextA?.checkSkip(schemaB))
                .describedAs("Different instance should return null (no cycle)")
                .isNull()
        }

        @Test
        @DisplayName("should allow visiting structurally different schemas")
        @Description("Schemas with different properties should be allowed")
        fun shouldAllowDifferentStructures() {
            // Arrange
            val schemaA = ObjectSchema().name("A").apply {
                addProperty("prop1", StringSchema())
            }
            val schemaB = ObjectSchema().name("B").apply {
                addProperty("prop2", StringSchema())
            }
            val context = createContext()

            // Act
            val contextA = context.withVisitedSchema(schemaA, "A")
            val contextB = contextA?.withVisitedSchema(schemaB, "B")

            // Assert
            assertThat(contextA).isNotNull
            assertThat(contextB)
                .describedAs("Different schema structures should be allowed")
                .isNotNull
            assertThat(contextA?.checkSkip(schemaB)).isNull()
        }

        @Test
        @DisplayName("should detect cycle in self-referencing object schema")
        @Description("Object schema that references itself (same Java instance) should be detected as cycle on second visit")
        fun shouldDetectSelfReferencingCycle() {
            // Arrange
            val schema = ObjectSchema().name("LinkedList").apply {
                addProperty("value", StringSchema())
                addProperty("next", this)
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema, "root")
            val context2 = context1?.withVisitedSchema(schema, "next")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2)
                .describedAs("Self-referencing schema should be detected as cycle on second visit")
                .isNull()
            assertThat(context1?.checkSkip(schema)).isEqualTo(SkipReason.CYCLE_DETECTED)
        }

        @Test
        @DisplayName("should track schema path correctly")
        @Description("Schema path should accumulate names as schemas are visited")
        fun shouldTrackSchemaPath() {
            // Arrange
            val schema1 = ObjectSchema().name("Level1").apply {
                addProperty("prop1", StringSchema())
            }
            val schema2 = ObjectSchema().name("Level2").apply {
                addProperty("prop2", IntegerSchema())
            }
            val schema3 = ObjectSchema().name("Level3").apply {
                addProperty("prop3", NumberSchema())
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema1, "level1")
            val context2 = context1?.withVisitedSchema(schema2, "level2")
            val context3 = context2?.withVisitedSchema(schema3, "level3")

            // Assert
            assertThat(context1?.schemaPath).containsExactly("level1")
            assertThat(context2?.schemaPath).containsExactly("level1", "level2")
            assertThat(context3?.schemaPath).containsExactly("level1", "level2", "level3")
        }

        @Test
        @DisplayName("should detect cycle in self-referencing array schema")
        @Description("Array schema that references itself (same Java instance) should be detected as cycle on second visit")
        fun shouldDetectSelfReferencingInArraysCycle() {
            // Arrange
            val schema = ArraySchema().name("Nods").apply {
                items(ObjectSchema().addProperty("nods", this))
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema, "root")
            val context2 = context1?.withVisitedSchema(schema, "next")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2)
                .describedAs("Self-referencing schema should be detected as cycle on second visit")
                .isNull()
            assertThat(context1?.checkSkip(schema)).isEqualTo(SkipReason.CYCLE_DETECTED)
        }
    }

    @Nested
    @DisplayName("False Positive Prevention")
    inner class FalsePositivePrevention {

        @Test
        @DisplayName("should not flag sibling properties with identical anyOf structure")
        @Description("Simulates bcc/cc/to pattern: different instances with same anyOf structure must not trigger CYCLE_DETECTED")
        fun shouldNotFlagSiblingPropertiesWithIdenticalAnyOfStructure() {
            // Arrange — three separate instances, same anyOf structure
            val schemaA = Schema<Any>().apply {
                anyOf = listOf(StringSchema(), ArraySchema().items(StringSchema()))
            }
            val schemaB = Schema<Any>().apply {
                anyOf = listOf(StringSchema(), ArraySchema().items(StringSchema()))
            }
            val schemaC = Schema<Any>().apply {
                anyOf = listOf(StringSchema(), ArraySchema().items(StringSchema()))
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schemaA, "to")

            // Assert
            assertThat(context1?.checkSkip(schemaB))
                .describedAs("'cc' field (different instance) should not be flagged as CYCLE_DETECTED")
                .isNull()
            assertThat(context1?.checkSkip(schemaC))
                .describedAs("'bcc' field (different instance) should not be flagged as CYCLE_DETECTED")
                .isNull()
        }

        @Test
        @DisplayName("should not flag a shared type used at multiple positions")
        @Description("Same schema type appearing in unrelated positions should only be a cycle when the exact instance was an ancestor")
        fun shouldNotFlagSharedTypeAtMultiplePositions() {
            // Arrange
            val recipient = StringSchema()
            val context = createContext().withVisitedSchema(ObjectSchema(), "data")

            // Assert — recipient has not been visited on this path
            assertThat(context?.checkSkip(recipient))
                .describedAs("Unvisited instance should not be flagged as CYCLE_DETECTED")
                .isNull()
        }

        @Test
        @DisplayName("should allow same schema type reused across unrelated sibling branches")
        @Description("Schema instances that share structure but are not ancestor-descendant should be traversed independently")
        fun shouldAllowSameTypedSchemasInSiblingBranches() {
            // Arrange — two different StringSchema instances
            val prop1Schema = StringSchema()
            val prop2Schema = StringSchema()
            val context = createContext()

            // Act — visit prop1Schema on a branch
            val context1 = context.withVisitedSchema(prop1Schema, "prop1")

            // Assert — prop2Schema (different instance) should be fine
            assertThat(context1?.checkSkip(prop2Schema))
                .describedAs("Different string schema instance should not trigger CYCLE_DETECTED")
                .isNull()
        }
    }

    @Nested
    @DisplayName("\$ref Cycle Detection")
    inner class RefCycleDetection {

        @Test
        @DisplayName("should detect cycle when visiting same \$ref twice")
        @Description("Schema with same \$ref should be detected as cycle on second visit")
        fun shouldDetectRefCycle() {
            // Arrange
            val schemaRef = Schema<Any>().apply { `$ref` = "#/components/schemas/Person" }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schemaRef, "person1")
            val context2 = context1?.withVisitedSchema(schemaRef, "person2")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2)
                .describedAs("Same \$ref visited twice should be detected as cycle")
                .isNull()
            assertThat(context1?.checkSkip(schemaRef)).isEqualTo(SkipReason.CYCLE_DETECTED)
        }

        @Test
        @DisplayName("should allow visiting different \$refs")
        @Description("Schemas with different \$refs should be allowed regardless of structure")
        fun shouldAllowDifferentRefs() {
            // Arrange
            val schemaRef1 = ObjectSchema().apply {
                `$ref` = "#/components/schemas/Person"
                addProperty("name", StringSchema())
            }
            val schemaRef2 = ObjectSchema().apply {
                `$ref` = "#/components/schemas/Address"
                addProperty("name", StringSchema())
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schemaRef1, "person")
            val context2 = context1?.withVisitedSchema(schemaRef2, "address")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2)
                .describedAs("Different \$refs should be allowed")
                .isNotNull
            assertThat(context1?.checkSkip(schemaRef2)).isNull()
        }

        @Test
        @DisplayName("should detect multi-hop \$ref cycle")
        @Description("Cycle A -> B -> C -> A via \$refs should be detected")
        fun shouldDetectMultiHopRefCycle() {
            // Arrange
            val refA = ObjectSchema().apply {
                `$ref` = "#/components/schemas/A"
                addProperty("a", StringSchema())
            }
            val refB = ObjectSchema().apply {
                `$ref` = "#/components/schemas/B"
                addProperty("b", StringSchema())
            }
            val refC = ObjectSchema().apply {
                `$ref` = "#/components/schemas/C"
                addProperty("c", StringSchema())
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(refA, "A")
            val context2 = context1?.withVisitedSchema(refB, "B")
            val context3 = context2?.withVisitedSchema(refC, "C")
            val context4 = context3?.withVisitedSchema(refA, "back-to-A")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2).isNotNull
            assertThat(context3).isNotNull
            assertThat(context4)
                .describedAs("Returning to previously visited \$ref should be detected as cycle")
                .isNull()
            assertThat(context3?.checkSkip(refA)).isEqualTo(SkipReason.CYCLE_DETECTED)
        }
    }

    @Nested
    @DisplayName("Depth Limit Detection")
    inner class DepthLimitDetection {

        @Test
        @DisplayName("should detect when max depth is exceeded")
        @Description("Visiting schemas beyond maxDepth should return null and DEPTH_EXCEEDED")
        fun shouldDetectDepthExceeded() {
            // Arrange
            val context = createContext(maxDepth = 2)
            val schema1 = ObjectSchema().addProperty("p1", StringSchema())
            val schema2 = ObjectSchema().addProperty("p2", StringSchema())
            val schema3 = ObjectSchema().addProperty("p3", StringSchema())

            // Act
            val context1 = context.withVisitedSchema(schema1, "level1")
            val context2 = context1?.withVisitedSchema(schema2, "level2")
            val context3 = context2?.withVisitedSchema(schema3, "level3")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2).isNotNull
            assertThat(context3)
                .describedAs("Visiting beyond maxDepth should return null")
                .isNull()
            assertThat(context2?.checkSkip(schema3)).isEqualTo(SkipReason.DEPTH_EXCEEDED)
        }

        @Test
        @DisplayName("should allow deep nesting up to maxDepth")
        @Description("Should successfully visit schemas up to maxDepth limit")
        fun shouldAllowDeepNestingUpToLimit() {
            // Arrange
            val context = createContext(maxDepth = 5)

            // Act & Assert
            var currentContext: TestGenerationContext? = context
            for (i in 1..5) {
                val schema = ObjectSchema().addProperty("prop$i", StringSchema())
                currentContext = currentContext?.withVisitedSchema(schema, "level$i")
                assertThat(currentContext)
                    .describedAs("Level $i should be allowed (maxDepth=5)")
                    .isNotNull
            }

            // Next level should exceed
            val schema6 = ObjectSchema().addProperty("prop6", StringSchema())
            val context6 = currentContext?.withVisitedSchema(schema6, "level6")
            assertThat(context6)
                .describedAs("Level 6 should exceed maxDepth=5")
                .isNull()
        }

        @Test
        @DisplayName("should handle depth=50 default limit correctly")
        @Description("Default maxDepth of 50 should allow legitimate deep nesting")
        fun shouldHandleDefaultDepthLimit() {
            // Arrange
            val context = createContext()

            // Act - Build deep nesting to 45 levels
            var currentContext: TestGenerationContext? = context
            for (i in 1..45) {
                val schema = ObjectSchema().addProperty("prop$i", StringSchema())
                currentContext = currentContext?.withVisitedSchema(schema, "level$i")
            }

            // Assert
            assertThat(currentContext)
                .describedAs("Should allow 45 levels of nesting with default maxDepth=50")
                .isNotNull
            assertThat(currentContext?.depth).isEqualTo(45)
        }
    }

    @Nested
    @DisplayName("Array Schema Cycle Detection")
    inner class ArrayCycleDetection {

        @Test
        @DisplayName("should detect self-referencing array schema")
        @Description("Array schema whose items reference the exact same array instance should detect cycle")
        fun shouldDetectSelfReferencingArray() {
            // Arrange
            val arraySchema = ArraySchema()
            arraySchema.items = arraySchema
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(arraySchema, "array1")
            val context2 = context1?.withVisitedSchema(arraySchema, "array2")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2)
                .describedAs("Self-referencing array should be detected as cycle")
                .isNull()
            assertThat(context1?.checkSkip(arraySchema)).isEqualTo(SkipReason.CYCLE_DETECTED)
        }

        @Test
        @DisplayName("should allow arrays with different item instances")
        @Description("Two distinct array schemas with identical structure are allowed (identity-based check)")
        fun shouldAllowDistinctArrayInstances() {
            // Arrange — two separate instances, same structure
            val array1 = ArraySchema().apply {
                items = StringSchema()
                minItems = 1
                maxItems = 10
            }
            val array2 = ArraySchema().apply {
                items = StringSchema()
                minItems = 1
                maxItems = 10
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(array1, "arr1")
            val shouldSkip = context1?.checkSkip(array2)

            // Assert — different instances → not a cycle
            assertThat(shouldSkip)
                .describedAs("Distinct array instances should not trigger CYCLE_DETECTED")
                .isNull()
        }
    }

    @Nested
    @DisplayName("Combined Scenarios")
    inner class CombinedScenarios {

        @Test
        @DisplayName("should detect \$ref cycle even when structural copy is allowed")
        @Description("Should detect \$ref cycles; structural copies of visited schemas are NOT cycles")
        fun shouldHandleMixedCycles() {
            // Arrange
            val refSchema = Schema<Any>().apply { `$ref` = "#/components/schemas/Person" }
            val structuralSchema = ObjectSchema().apply {
                addProperty("prop", StringSchema())
                required = listOf("prop")
            }
            val structuralSchemaCopy = ObjectSchema().apply {
                addProperty("prop", StringSchema())
                required = listOf("prop")
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(refSchema, "person")
            val context2 = context1?.withVisitedSchema(structuralSchema, "struct1")
            val context3 = context2?.withVisitedSchema(refSchema, "person-again")
            val context4 = context2?.withVisitedSchema(structuralSchemaCopy, "struct2")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2).isNotNull
            assertThat(context3)
                .describedAs("\$ref cycle should be detected")
                .isNull()
            assertThat(context4)
                .describedAs("Structural copy is a different instance — must NOT be detected as a cycle")
                .isNotNull
            assertThat(context2?.checkSkip(refSchema)).isEqualTo(SkipReason.CYCLE_DETECTED)
            assertThat(context2?.checkSkip(structuralSchemaCopy)).isNull()
        }

        @Test
        @DisplayName("should prioritize depth limit over cycle detection")
        @Description("When depth is exceeded, should return DEPTH_EXCEEDED even if cycle exists")
        fun shouldPrioritizeDepthLimit() {
            // Arrange
            val context = createContext(maxDepth = 1)
            val schema = ObjectSchema().addProperty("prop", StringSchema())

            // Act
            val context1 = context.withVisitedSchema(schema, "level1")
            val reason = context1?.checkSkip(schema)

            // Assert
            assertThat(reason)
                .describedAs("DEPTH_EXCEEDED should take priority over CYCLE_DETECTED")
                .isEqualTo(SkipReason.DEPTH_EXCEEDED)
        }

        @Test
        @DisplayName("should handle complex nested structure without false positives")
        @Description("Complex but valid nested structure should not trigger cycle detection")
        fun shouldHandleComplexNestingWithoutFalsePositives() {
            // Arrange
            val person = ObjectSchema().apply {
                addProperty("name", StringSchema())
                addProperty("age", IntegerSchema())
            }
            val address = ObjectSchema().apply {
                addProperty("street", StringSchema())
                addProperty("city", StringSchema())
            }
            val company = ObjectSchema().apply {
                addProperty("name", StringSchema())
                addProperty("employees", ArraySchema().items(person))
            }
            val context = createContext()

            // Act - Complex but non-cyclic traversal
            val context1 = context.withVisitedSchema(person, "person")
            val context2 = context1?.withVisitedSchema(address, "address")
            val context3 = context2?.withVisitedSchema(company, "company")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2).isNotNull
            assertThat(context3).isNotNull
            assertThat(context3?.schemaPath).containsExactly("person", "address", "company")
        }
    }

    @Nested
    @DisplayName("checkSkip Method Behavior")
    inner class CheckSkipBehavior {

        @Test
        @DisplayName("should return null when no skip condition exists")
        @Description("checkSkip should return null for unvisited schema within depth limit")
        fun shouldReturnNullWhenNoSkipCondition() {
            // Arrange
            val context = createContext()
            val schema = ObjectSchema().addProperty("prop", StringSchema())

            // Act
            val result = context.checkSkip(schema)

            // Assert
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("should return DEPTH_EXCEEDED when at max depth")
        @Description("checkSkip should return DEPTH_EXCEEDED when context.depth >= maxDepth")
        fun shouldReturnDepthExceededAtMaxDepth() {
            // Arrange
            val context = createContext(maxDepth = 1)
            val schema1 = ObjectSchema().addProperty("p1", StringSchema())
            val schema2 = ObjectSchema().addProperty("p2", StringSchema())

            val context1 = context.withVisitedSchema(schema1, "level1")

            // Act
            val result = context1?.checkSkip(schema2)

            // Assert
            assertThat(result).isEqualTo(SkipReason.DEPTH_EXCEEDED)
        }

        @Test
        @DisplayName("should not mutate context when called")
        @Description("checkSkip is a query method and should not modify context state")
        fun shouldNotMutateContext() {
            // Arrange
            val context = createContext()
            val schema = ObjectSchema().addProperty("prop", StringSchema())

            // Act
            val schemaPath = context.schemaPath
            val visitedSchemaRefs = context.visitedSchemaRefs
            val depth = context.depth

            context.checkSkip(schema)

            // Assert
            assertThat(context.schemaPath).isSameAs(schemaPath)
            assertThat(context.visitedSchemaRefs).isSameAs(visitedSchemaRefs)
            assertThat(context.depth).isEqualTo(depth)
        }
    }
}
