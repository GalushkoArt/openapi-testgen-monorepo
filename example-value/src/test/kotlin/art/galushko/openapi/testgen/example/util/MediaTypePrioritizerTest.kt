package art.galushko.openapi.testgen.example.util

import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MediaTypePrioritizerTest {

    @Test
    fun `orderedMediaTypeKeys should follow documented priority order`() {
        val content = Content()
            .addMediaType("application/problem+json", MediaType())
            .addMediaType("application/json", MediaType())
            .addMediaType("text/json", MediaType())
            .addMediaType("application/jwt", MediaType())
            .addMediaType("application/secevent+jwt", MediaType())
            .addMediaType("application/atom+xml", MediaType())
            .addMediaType("application/x-www-form-urlencoded", MediaType())
            .addMediaType("application/xml", MediaType())
            .addMediaType("text/xml", MediaType())
            .addMediaType("text/plain", MediaType())

        val ordered = MediaTypePrioritizer.orderedMediaTypeKeys(content)

        assertThat(ordered).containsExactly(
            "application/json",
            "text/json",
            "application/problem+json",
            "application/jwt",
            "application/secevent+jwt",
            "application/xml",
            "text/xml",
            "application/atom+xml",
            "application/x-www-form-urlencoded",
            "text/plain",
        )
    }

    @Test
    fun `orderedMediaTypeKeys should normalize parameters and casing for sorting`() {
        val content = Content()
            .addMediaType("APPLICATION/JSON; charset=utf-8", MediaType())
            .addMediaType("Text/Json; profile=test", MediaType())
            .addMediaType("application/hal+JSON; profile=test", MediaType())
            .addMediaType("APPLICATION/JWT; profile=test", MediaType())
            .addMediaType("Application/XML; charset=UTF-8", MediaType())

        val ordered = MediaTypePrioritizer.orderedMediaTypeKeys(content)

        assertThat(ordered).containsExactly(
            "APPLICATION/JSON; charset=utf-8",
            "Text/Json; profile=test",
            "application/hal+JSON; profile=test",
            "APPLICATION/JWT; profile=test",
            "Application/XML; charset=UTF-8",
        )
    }

    @Test
    fun `isJsonOrJwtLike should detect both json and jwt-like media types`() {
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/json")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("text/json")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("Text/Json; charset=utf-8")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/hal+json")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/hal+json; charset=utf-8")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/jwt")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/YAML")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/x-www-form-urlencoded")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/secevent+jwt")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/secevent+xml")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/xml")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("text/xml")).isTrue()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("multipart/form-data")).isFalse()
        assertThat(MediaTypePrioritizer.isExpectedStructuredSchema("application/cbor")).isFalse()
    }
}
