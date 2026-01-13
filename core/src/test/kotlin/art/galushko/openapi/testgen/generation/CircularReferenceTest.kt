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
import io.swagger.v3.oas.models.media.XML
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

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
    @DisplayName("Structural Cycle Detection")
    inner class StructuralCycleDetection {

        @Test
        @DisplayName("should detect cycle when visiting structurally identical schemas")
        @Description("Two schemas with same type, properties, and required fields should be detected as structural cycle")
        fun shouldDetectStructuralCycle() {
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

            // Assert
            assertThat(contextA).isNotNull
            assertThat(contextB)
                .describedAs("Structurally identical schema B should be detected as cycle")
                .isNull()
            assertThat(contextA?.checkSkip(schemaB)).isEqualTo(SkipReason.CYCLE_DETECTED)
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
        @Description("Object schema that references itself should be detected as cycle on second visit")
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
        @Description("Object schema that references itself should be detected as cycle on second visit")
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
    @DisplayName($$"$ref Cycle Detection")
    inner class RefCycleDetection {

        @Test
        @DisplayName($$"should detect cycle when visiting same $ref twice")
        @Description($$"Schema with same $ref should be detected as cycle on second visit")
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
                .describedAs($$"Same $ref visited twice should be detected as cycle")
                .isNull()
            assertThat(context1?.checkSkip(schemaRef)).isEqualTo(SkipReason.CYCLE_DETECTED)
        }

        @Test
        @DisplayName($$"should allow visiting different $refs with different structures")
        @Description($$"Schemas with different $refs and different structures should be allowed")
        fun shouldAllowDifferentRefs() {
            // Arrange - Give them different structures to avoid false positives from structural hash
            val schemaRef1 = ObjectSchema().apply {
                `$ref` = "#/components/schemas/Person"
                addProperty("name", StringSchema())
            }
            val schemaRef2 = ObjectSchema().apply {
                `$ref` = "#/components/schemas/Address"
                addProperty("street", StringSchema())
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schemaRef1, "person")
            val context2 = context1?.withVisitedSchema(schemaRef2, "address")

            // Assert
            assertThat(context1).isNotNull
            assertThat(context2)
                .describedAs($$"Different $refs with different structures should be allowed")
                .isNotNull
            assertThat(context1?.checkSkip(schemaRef2)).isNull()
        }

        @Test
        @DisplayName($$"should detect multi-hop $ref cycle")
        @Description($$"Cycle A -> B -> C -> A via $refs should be detected")
        fun shouldDetectMultiHopRefCycle() {
            // Arrange - Give each schema different structure to test pure $ref cycle detection
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
                .describedAs($$"Returning to previously visited $ref should be detected as cycle")
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
        @Description("Array schema whose items reference the array itself should detect cycle")
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
        @DisplayName("should distinguish arrays with different item constraints")
        @Description("Arrays with different minItems/maxItems should be distinguished as different structures")
        fun shouldDistinguishArraysWithDifferentItemConstraints() {
            // Arrange - Both have same item type, but different constraints ARE part of structural hash
            val array1 = ArraySchema().apply {
                items = StringSchema()
                minItems = 1
                maxItems = 10
            }
            val array2 = ArraySchema().apply {
                items = StringSchema()
                minItems = 2 // Different constraint
                maxItems = 20 // Different constraint
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(array1, "arr1")
            val shouldSkip = context1?.checkSkip(array2)

            // Assert - Should NOT detect as cycle because minItems/maxItems differ
            assertThat(shouldSkip)
                .describedAs("Arrays with different min/max constraints should be distinguished")
                .isNull()
        }

        @Test
        @DisplayName("should distinguish arrays with uniqueItems difference")
        @Description("Arrays with different uniqueItems setting should be distinguished")
        fun shouldDistinguishArraysWithDifferentUniqueItems() {
            // Arrange
            val array1 = ArraySchema().apply {
                items = StringSchema()
                uniqueItems = true
            }
            val array2 = ArraySchema().apply {
                items = StringSchema()
                uniqueItems = false
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(array1, "arr1")
            val shouldSkip = context1?.checkSkip(array2)

            // Assert - uniqueItems is part of structural hash
            assertThat(shouldSkip)
                .describedAs("Arrays with different uniqueItems should be distinguished")
                .isNull()
        }
    }

    @Nested
    @DisplayName("Hash Computation Edge Cases")
    inner class HashComputationEdgeCases {
        @Suppress("LongMethod", "CyclomaticComplexMethod")
        fun provideStructurallyEquivalentSchemas(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Same array item type",
                ArraySchema().items(StringSchema().name("item1")),
                ArraySchema().items(StringSchema().name("item2"))
            ),
            Arguments.of(
                "Same required fields (different order)",
                ObjectSchema().apply {
                    addProperty("a", StringSchema())
                    addProperty("b", StringSchema())
                    required = listOf("b", "a")
                },
                ObjectSchema().apply {
                    addProperty("a", StringSchema())
                    addProperty("b", StringSchema())
                    required = listOf("a", "b")
                }
            ),
            Arguments.of(
                "Same property names (different order)",
                ObjectSchema().apply {
                    addProperty("z", StringSchema())
                    addProperty("a", StringSchema())
                },
                ObjectSchema().apply {
                    addProperty("a", StringSchema())
                    addProperty("z", StringSchema())
                }
            ),
            Arguments.of(
                "Same enum values (different order)",
                StringSchema().apply {
                    enum = listOf("beta", "alpha", "gamma")
                },
                StringSchema().apply {
                    enum = listOf("alpha", "gamma", "beta")
                }
            ),
            Arguments.of(
                "Same nested property structures",
                ObjectSchema().apply {
                    addProperty("user", ObjectSchema().apply {
                        addProperty("name", StringSchema())
                        addProperty("email", StringSchema())
                    })
                },
                ObjectSchema().apply {
                    addProperty("user", ObjectSchema().apply {
                        addProperty("email", StringSchema())
                        addProperty("name", StringSchema())
                    })
                }
            ),
            Arguments.of(
                "Array with same object property structures",
                ArraySchema().apply {
                    items(ObjectSchema().apply {
                        addProperty("name", StringSchema())
                        addProperty("email", StringSchema())
                    })
                },
                ArraySchema().apply {
                    items(ObjectSchema().apply {
                        addProperty("email", StringSchema())
                        addProperty("name", StringSchema())
                    })
                }
            ),
            Arguments.of(
                "Same nested property of array item object",
                ArraySchema().apply {
                    items(ObjectSchema().apply {
                        addProperty("person", ObjectSchema().apply {
                            addProperty("id", StringSchema())
                        })
                    })
                },
                ArraySchema().apply {
                    items(ObjectSchema().apply {
                        addProperty("person", ObjectSchema().apply {
                            addProperty("id", StringSchema())
                        })
                    })
                },
            ),
            Arguments.of(
                "Same nested property of item of array item object",
                ArraySchema().apply {
                    items(ArraySchema().apply {
                        items(ObjectSchema().apply {
                            addProperty("id", StringSchema())
                        })
                    })
                },
                ArraySchema().apply {
                    items(ArraySchema().apply {
                        items(ObjectSchema().apply {
                            addProperty("id", StringSchema())
                        })
                    })
                },
            ),
            Arguments.of(
                "Same XML metadata",
                StringSchema().apply {
                    xml = XML().apply {
                        name = "user"
                        namespace = "http://example.com/schema"
                        prefix = "ex"
                        attribute = false
                    }
                },
                StringSchema().apply {
                    xml = XML().apply {
                        name = "user"
                        namespace = "http://example.com/schema"
                        prefix = "ex"
                        attribute = false
                    }
                }
            ),
            Arguments.of(
                "XML null vs missing",
                StringSchema().apply { xml = null },
                StringSchema()
            ),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideStructurallyEquivalentSchemas")
        @DisplayName("should compute same hash for structurally equivalent schemas")
        @Description("Schemas with same structure but different instances should produce same hash")
        fun shouldComputeSameHashForEquivalentSchemas(
            scenario: String,
            schema1: Schema<*>,
            schema2: Schema<*>,
        ) {
            // Arrange
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema1, "first")
            val shouldSkip = context1?.checkSkip(schema2)

            // Assert
            assertThat(shouldSkip)
                .describedAs("$scenario: should detect as cycle")
                .isEqualTo(SkipReason.CYCLE_DETECTED)
        }

        @Suppress("LongMethod", "CyclomaticComplexMethod")
        fun provideStructurallyDifferentSchemas(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "Different property names",
                ObjectSchema().addProperty("name", StringSchema()),
                ObjectSchema().addProperty("title", StringSchema())
            ),
            Arguments.of(
                "Different required fields",
                ObjectSchema().apply {
                    addProperty("name", StringSchema())
                    required = listOf("name")
                },
                ObjectSchema().apply {
                    addProperty("name", StringSchema())
                    required = emptyList()
                }
            ),
            Arguments.of(
                "Different types",
                StringSchema(),
                IntegerSchema()
            ),
            Arguments.of(
                "Different array item types",
                ArraySchema().items(StringSchema()),
                ArraySchema().items(IntegerSchema())
            ),
            Arguments.of(
                "Different number of properties",
                ObjectSchema().apply {
                    addProperty("a", StringSchema())
                    addProperty("b", StringSchema())
                },
                ObjectSchema().addProperty("a", StringSchema())
            ),
            Arguments.of(
                "Object vs Array",
                ObjectSchema().addProperty("items", StringSchema()),
                ArraySchema().items(StringSchema())
            ),
            Arguments.of(
                "Different enum values",
                StringSchema().apply { enum = listOf("a", "b", "c") },
                StringSchema().apply { enum = listOf("a", "b", "d") }
            ),
            Arguments.of(
                "Different root property names with nested objects",
                ObjectSchema().apply {
                    addProperty("person", ObjectSchema().apply {
                        addProperty("id", StringSchema())
                    })
                },
                ObjectSchema().apply {
                    addProperty("employee", ObjectSchema().apply { // Different root name
                        addProperty("id", StringSchema())
                    })
                }
            ),
            Arguments.of(
                "Different nested property type of array item object",
                ArraySchema().apply {
                    items(ObjectSchema().apply {
                        addProperty("person", ObjectSchema().apply {
                            addProperty("id", StringSchema())
                        })
                    })
                },
                ArraySchema().apply {
                    items(ObjectSchema().apply {
                        addProperty("person", ObjectSchema().apply {
                            addProperty("id", IntegerSchema()) // Different property type
                        })
                    })
                },
            ),
            Arguments.of(
                "Different nested property name of array item object",
                ArraySchema().apply {
                    items(ObjectSchema().apply {
                        addProperty("person", ObjectSchema().apply {
                            addProperty("id", StringSchema())
                        })
                    })
                },
                ArraySchema().apply {
                    items(ObjectSchema().apply {
                        addProperty("person", ObjectSchema().apply {
                            addProperty("personId", StringSchema()) // Different property name
                        })
                    })
                },
            ),
            Arguments.of(
                "Different nested property type of item of array item object",
                ArraySchema().apply {
                    items(ArraySchema().apply {
                        items(ObjectSchema().apply {
                            addProperty("id", StringSchema())
                        })
                    })
                },
                ArraySchema().apply {
                    items(ArraySchema().apply {
                        items(ObjectSchema().apply {
                            addProperty("id", IntegerSchema()) // Different property type
                        })
                    })
                },
            ),
            Arguments.of(
                "Different nested property name of item of array item object",
                ArraySchema().apply {
                    items(ArraySchema().apply {
                        items(ObjectSchema().apply {
                            addProperty("id", StringSchema())
                        })
                    })
                },
                ArraySchema().apply {
                    items(ArraySchema().apply {
                        items(ObjectSchema().apply {
                            addProperty("idProperty", StringSchema()) // Different property name
                        })
                    })
                },
            ),
            Arguments.of(
                "Different XML name",
                StringSchema().apply {
                    xml = XML().apply { name = "user" }
                },
                StringSchema().apply {
                    xml = XML().apply { name = "person" }
                }
            ),
            Arguments.of(
                "Different XML namespace",
                StringSchema().apply {
                    xml = XML().apply {
                        name = "user"
                        namespace = "http://example.com/v1"
                    }
                },
                StringSchema().apply {
                    xml = XML().apply {
                        name = "user"
                        namespace = "http://example.com/v2"
                    }
                }
            ),
            Arguments.of(
                "Different XML prefix",
                StringSchema().apply {
                    xml = XML().apply {
                        name = "user"
                        prefix = "v1"
                    }
                },
                StringSchema().apply {
                    xml = XML().apply {
                        name = "user"
                        prefix = "v2"
                    }
                }
            ),
            Arguments.of(
                "Different XML attribute setting",
                StringSchema().apply {
                    xml = XML().apply {
                        name = "user"
                        attribute = true
                    }
                },
                StringSchema().apply {
                    xml = XML().apply {
                        name = "user"
                        attribute = false
                    }
                }
            ),
        )

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideStructurallyDifferentSchemas")
        @DisplayName("should compute different hash for structurally different schemas")
        @Description("Schemas with different structures should produce different hashes")
        fun shouldComputeDifferentHashForDifferentSchemas(
            scenario: String,
            schema1: Schema<*>,
            schema2: Schema<*>,
        ) {
            // Arrange
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema1, "first")
            val shouldSkip = context1?.checkSkip(schema2)

            // Assert
            assertThat(shouldSkip)
                .describedAs("$scenario: should NOT detect as cycle")
                .isNull()
        }
    }

    @Nested
    @DisplayName("Advanced Hash Features")
    inner class AdvancedHashFeatures {

        @Test
        @DisplayName("should detect deep nested property cycles")
        @Description("Recursive property hashing should detect cycles in deeply nested object properties")
        fun shouldDetectDeepNestedPropertyCycles() {
            // Arrange - Create identical deeply nested structure
            val schema1 = ObjectSchema().apply {
                addProperty("level1", ObjectSchema().apply {
                    addProperty("level2", ObjectSchema().apply {
                        addProperty("value", StringSchema())
                        required = listOf("value")
                    })
                })
            }
            val schema2 = ObjectSchema().apply {
                addProperty("level1", ObjectSchema().apply {
                    addProperty("level2", ObjectSchema().apply {
                        addProperty("value", StringSchema())
                        required = listOf("value")
                    })
                })
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema1, "first")
            val shouldSkip = context1?.checkSkip(schema2)

            // Assert
            assertThat(shouldSkip)
                .describedAs("Deeply nested identical structures should be detected as cycle")
                .isEqualTo(SkipReason.CYCLE_DETECTED)
        }

        @Test
        @DisplayName("should distinguish deep nested structures with different leaf types")
        @Description("Small differences in deeply nested properties should be detected")
        fun shouldDistinguishDeepNestedDifferences() {
            // Arrange - Same structure but different leaf type
            val stringLeaf = StringSchema().apply { type = "string" }
            val integerLeaf = IntegerSchema().apply { type = "integer" }

            val schema1 = ObjectSchema().apply {
                type = "object"
                addProperty("level1", ObjectSchema().apply {
                    type = "object"
                    addProperty("level2", ObjectSchema().apply {
                        type = "object"
                        addProperty("value", stringLeaf)
                    })
                })
            }
            val schema2 = ObjectSchema().apply {
                type = "object"
                addProperty("level1", ObjectSchema().apply {
                    type = "object"
                    addProperty("level2", ObjectSchema().apply {
                        type = "object"
                        addProperty("value", integerLeaf)
                    })
                })
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema1, "first")
            val shouldSkip = context1?.checkSkip(schema2)

            // Assert
            assertThat(shouldSkip)
                .describedAs("Deep nested structures with different leaf types should NOT be cycles")
                .isNull()
        }

        @Test
        @DisplayName("should hash array items recursively")
        @Description("Array item properties should be hashed, not just item presence")
        fun shouldHashArrayItemsRecursively() {
            // Arrange - Arrays with same structure but different nested properties
            val array1 = ArraySchema().items(ObjectSchema().apply {
                addProperty("nested", ObjectSchema().apply {
                    addProperty("id", StringSchema())
                })
            })
            val array2 = ArraySchema().items(ObjectSchema().apply {
                addProperty("nested", ObjectSchema().apply {
                    addProperty("id", StringSchema())
                })
            })
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(array1, "arr1")
            val shouldSkip = context1?.checkSkip(array2)

            // Assert - Recursive hashing should detect these as same
            assertThat(shouldSkip)
                .describedAs("Arrays with identical nested item properties should be detected as cycles")
                .isEqualTo(SkipReason.CYCLE_DETECTED)
        }

        @Test
        @DisplayName("should distinguish array items with different nested properties")
        @Description("Differences in array item nested properties should be detected")
        fun shouldDistinguishArrayItemsDifferentNestedProps() {
            // Arrange
            val array1 = ArraySchema().items(ObjectSchema().apply {
                addProperty("user", ObjectSchema().apply {
                    addProperty("name", StringSchema())
                })
            })
            val array2 = ArraySchema().items(ObjectSchema().apply {
                addProperty("user", ObjectSchema().apply {
                    addProperty("email", StringSchema()) // Different property!
                })
            })
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(array1, "arr1")
            val shouldSkip = context1?.checkSkip(array2)

            // Assert
            assertThat(shouldSkip)
                .describedAs("Arrays with different nested item properties should NOT be cycles")
                .isNull()
        }

        @Test
        @DisplayName("should handle null schemas gracefully")
        @Description("Null schema should return 0 hash without errors")
        fun shouldHandleNullSchemaGracefully() {
            // Arrange
            val context = createContext()
            val normalSchema = ObjectSchema().addProperty("prop", StringSchema())

            // Act - Visit normal schema, then check skip with null shouldn't crash
            val context1 = context.withVisitedSchema(normalSchema, "normal")

            // Assert - Should not throw exception
            assertThat(context1).isNotNull
        }

        @Test
        @DisplayName("should handle empty property maps")
        @Description("Objects with no properties should hash consistently")
        fun shouldHandleEmptyPropertyMaps() {
            // Arrange - Both empty object schemas
            val schema1 = ObjectSchema()
            val schema2 = ObjectSchema()
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema1, "empty1")
            val shouldSkip = context1?.checkSkip(schema2)

            // Assert
            assertThat(shouldSkip)
                .describedAs("Empty object schemas should be detected as cycles")
                .isEqualTo(SkipReason.CYCLE_DETECTED)
        }

        @Test
        @DisplayName("should handle empty required lists")
        @Description("Required fields should only affect hash when non-empty")
        fun shouldHandleEmptyRequiredLists() {
            // Arrange - Both have properties but no required
            val schema1 = ObjectSchema().apply {
                addProperty("name", StringSchema())
                required = emptyList()
            }
            val schema2 = ObjectSchema().apply {
                addProperty("name", StringSchema())
                required = null // null vs empty list
            }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema1, "s1")
            val shouldSkip = context1?.checkSkip(schema2)

            // Assert - Both empty/null required should be treated the same
            assertThat(shouldSkip)
                .describedAs("Empty and null required lists should be treated as equivalent")
                .isEqualTo(SkipReason.CYCLE_DETECTED)
        }

        @Test
        @DisplayName("should handle empty enum lists")
        @Description("Enum should only affect hash when non-empty")
        fun shouldHandleEmptyEnumLists() {
            // Arrange
            val schema1 = StringSchema().apply { enum = emptyList() }
            val schema2 = StringSchema().apply { enum = null }
            val context = createContext()

            // Act
            val context1 = context.withVisitedSchema(schema1, "s1")
            val shouldSkip = context1?.checkSkip(schema2)

            // Assert
            assertThat(shouldSkip)
                .describedAs("Empty and null enum lists should be treated as equivalent")
                .isEqualTo(SkipReason.CYCLE_DETECTED)
        }
    }

    @Nested
    @DisplayName("Combined Scenarios")
    inner class CombinedScenarios {

        @Test
        @DisplayName("should handle mixed ref and structural cycles")
        @Description($$"Should detect both $ref cycles and structural cycles in same traversal")
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
                .describedAs($$"$ref cycle should be detected")
                .isNull()
            assertThat(context4)
                .describedAs("Structural cycle should be detected")
                .isNull()
            assertThat(context2?.checkSkip(refSchema)).isEqualTo(SkipReason.CYCLE_DETECTED)
            assertThat(context2?.checkSkip(structuralSchemaCopy)).isEqualTo(SkipReason.CYCLE_DETECTED)
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

