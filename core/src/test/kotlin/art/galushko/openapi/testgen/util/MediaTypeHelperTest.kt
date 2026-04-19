package art.galushko.openapi.testgen.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MediaTypeHelperTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "application/json",
            "application/json; charset=utf-8",
            "text/json",
            "text/json; charset=utf-8",
            "application/hal+json",
            "application/hal+json; charset=utf-8",
            "application/jwt",
            "application/jwt; charset=utf-8",
            "application/secevent+jwt",
            "application/secevent+jwt; charset=utf-8",
            "application/xml",
            "text/xml",
            "application/atom+xml",
            "application/x-www-form-urlencoded",
            "APPLICATION/JSON",
            "TEXT/JSON",
            "APPLICATION/JWT",
            " Application/XML ; charset=utf-8 ",
        ]
    )
    fun `isMediaTypeSupported should return true for supported media types`(mediaType: String) {
        assertThat(isMediaTypeSupported(mediaType)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "text/html",
            "text/plain",
            "application/octet-stream",
            "multipart/form-data",
            "application/javascript",
        ]
    )
    fun `isMediaTypeSupported should return false for unsupported media types`(mediaType: String) {
        assertThat(isMediaTypeSupported(mediaType)).isFalse()
    }
}
