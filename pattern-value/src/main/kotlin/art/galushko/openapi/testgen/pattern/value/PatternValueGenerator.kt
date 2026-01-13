package art.galushko.openapi.testgen.pattern.value

import org.cornutum.regexpgen.RandomGen
import org.cornutum.regexpgen.RegExpGen
import org.cornutum.regexpgen.RegExpGenBuilder
import org.cornutum.regexpgen.js.Provider
import org.cornutum.regexpgen.random.RandomBoundsGen
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Generates string values based on regular expression patterns using the regexp-gen library.
 *
 * This class encapsulates the Cornutum regexp-gen library for generating strings that either
 * match or do not match a given ECMA-262 (JavaScript-style) regular expression pattern.
 *
 * ## Usage
 *
 * ```kotlin
 * // Use default options
 * val generator = PatternValueGenerator()
 * val value = generator.generateValidValue("^[A-Z]{3}$", null, null, 0)
 *
 * // Or use the shared default instance
 * val value = PatternValueGenerator.getDefault().generateValidValue("^[A-Z]{3}$", null, null, 0)
 *
 * // Custom options
 * val customGenerator = PatternValueGenerator(PatternGenerationOptions(defaultMinLength = 10))
 * ```
 *
 * ## Options
 *
 * Generation behavior is controlled by [PatternGenerationOptions] passed to the constructor:
 * - [PatternGenerationOptions.defaultMinLength]: fallback minimum length when not specified
 * - [PatternGenerationOptions.spaceChars]: characters that match `\s` (and affect `\S`)
 * - [PatternGenerationOptions.anyPrintableChars]: characters that match `.` (any printable)
 *
 * ## Library Limitations
 *
 * The underlying regexp-gen library has the following limitations:
 * - **No negative lookahead/lookbehind**: Patterns like `(?!...)` or `(?<!...)` are not supported
 * - **No word boundary assertions**: `\b` and `\B` are not supported
 * - **No backreferences**: Patterns referencing earlier capture groups are not supported
 * - **Some patterns accept all strings**: For patterns like `.*`, generating non-matching strings is impossible
 *
 * When these limitations are encountered, the generator will return `null` and log a warning.
 *
 * Determinism:
 * - Valid generation uses `variationIndex` as the random seed.
 * - Invalid generation uses a fixed seed and applies length hints only.
 *
 * @property options configuration options for pattern generation
 * @see PatternGenerationOptions
 * @see <a href="https://github.com/cornutum/regexp-gen">Cornutum RegExp-Gen</a>
 */
public class PatternValueGenerator(
    private val options: PatternGenerationOptions = PatternGenerationOptions(),
) {
    private val log = LoggerFactory.getLogger(PatternValueGenerator::class.java)

    /**
     * Generates a string that matches the given pattern, respecting length constraints.
     *
     * The generator uses [PatternGenerationOptions.spaceChars] for `\s` matching and
     * [PatternGenerationOptions.anyPrintableChars] for `.` matching.
     *
     * @param pattern ECMA-262 regular expression pattern
     * @param minLength minimum length constraint from schema (nullable)
     * @param maxLength maximum length constraint from schema (nullable)
     * @param variationIndex index used as seed for deterministic random generation
     * @return a string matching the pattern within length constraints, or null if generation fails
     */
    public fun generateValidValue(
        pattern: String,
        minLength: Int?,
        maxLength: Int?,
        variationIndex: Int,
    ): String? {
        return try {
            val generator = createMatchingGenerator(pattern) ?: return null
            val random = createSeededRandom(variationIndex)
            generatePatternValue(generator, minLength, maxLength, pattern, random)
        } catch (e: Exception) {
            log.warn("Failed to generate valid value for pattern '{}': {}", pattern, e.message)
            null
        }
    }

    private fun generatePatternValue(
        generator: RegExpGen,
        minLength: Int?,
        maxLength: Int?,
        pattern: String,
        random: RandomGen,
    ): String? {
        val effectiveMinLength = calculateEffectiveMinLength(generator, minLength, maxLength)
        val effectiveMaxLength = calculateEffectiveMaxLength(generator, maxLength)

        if (effectiveMaxLength != null && effectiveMinLength > effectiveMaxLength) {
            log.warn(
                "Infeasible length constraints for pattern '{}': minLength={} > maxLength={}",
                pattern, effectiveMinLength, effectiveMaxLength
            )
            return null
        }

        return generator.generate(random, effectiveMinLength, effectiveMaxLength)
    }

    /**
     * Generates a string that does NOT match the given pattern.
     *
     * The generator uses [PatternGenerationOptions.spaceChars] for `\s` matching and
     * [PatternGenerationOptions.anyPrintableChars] for `.` matching in the non-matching generator.
     *
     * @param pattern ECMA-262 regular expression pattern
     * @param minLength minimum length constraint from schema (nullable, used for hint)
     * @param maxLength maximum length constraint from schema (nullable, used for hint)
     * @return a string not matching the pattern, or null if generation fails
     */
    public fun generateInvalidValue(
        pattern: String,
        minLength: Int?,
        maxLength: Int?,
    ): String? {
        return try {
            val generatorOpt = createBuilder().notMatching(pattern)

            if (generatorOpt.isEmpty) {
                log.warn(
                    "Unable to generate non-matching string for pattern '{}' - pattern may match all strings",
                    pattern
                )
                return null
            }

            val generator = generatorOpt.get()
            val random = createSeededRandom(1)
            generateWithLengthHints(generator, random, minLength, maxLength)
        } catch (e: Exception) {
            log.warn("Failed to generate invalid value for pattern '{}': {}", pattern, e.message)
            null
        }
    }

    private fun createMatchingGenerator(pattern: String): RegExpGen? {
        return try {
            createBuilder().exactly().matching(pattern)
        } catch (e: Exception) {
            log.warn("Failed to parse pattern '{}': {}", pattern, e.message)
            null
        }
    }

    /**
     * Creates a builder configured with the current options.
     */
    private fun createBuilder() = RegExpGenBuilder.generateRegExp(Provider.forEcmaScript()).withSpace(options.spaceChars)
        .apply {
            options.anyPrintableChars?.let { chars ->
                withAny(chars)
            }
        }

    private fun createSeededRandom(seed: Int): RandomGen {
        return RandomBoundsGen(Random(seed.toLong()))
    }

    private fun calculateEffectiveMinLength(
        generator: RegExpGen,
        schemaMinLength: Int?,
        schemaMaxLength: Int?,
    ): Int {
        val patternMinLength = generator.minLength
        val patternMaxLength = generator.maxLength
        return when {
            patternMinLength > (schemaMinLength ?: 0) -> patternMinLength
            schemaMinLength != null && schemaMinLength > 0 -> schemaMinLength
            else -> minOf(options.defaultMinLength, patternMaxLength, schemaMaxLength ?: Int.MAX_VALUE)
        }
    }

    private fun calculateEffectiveMaxLength(generator: RegExpGen, schemaMaxLength: Int?): Int? {
        val patternMaxLength = generator.maxLength
        return when {
            schemaMaxLength == null -> if (patternMaxLength == Int.MAX_VALUE) null else patternMaxLength
            patternMaxLength == Int.MAX_VALUE -> schemaMaxLength
            patternMaxLength < schemaMaxLength -> patternMaxLength
            else -> schemaMaxLength
        }
    }

    private fun generateWithLengthHints(
        generator: RegExpGen,
        random: RandomGen,
        minLength: Int?,
        maxLength: Int?,
    ): String? {
        // For non-matching generator, length constraints are hints, not strict requirements
        return when {
            minLength != null && maxLength != null -> generator.generate(random, minLength, maxLength)
            else -> generator.generate(random)
        }
    }
}


