package art.galushko.openapi.testgen.example;

import art.galushko.openapi.testgen.example.config.ExampleValueSettings;
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGenerator;
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorFactory;
import art.galushko.openapi.testgen.example.generator.SchemaExampleValueGeneratorOptions;
import art.galushko.openapi.testgen.example.openapi.SchemaMerger;
import art.galushko.openapi.testgen.example.openapi.SchemaTypeHelpers;
import art.galushko.openapi.testgen.example.response.ResponseExampleExtractor;
import art.galushko.openapi.testgen.example.response.ExtractedResponseExample;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Java-facing ergonomics of the example-value API: presets and withers instead of
 * positional flags, SAM-convertible fallback generator, and default-argument overloads.
 */
@Epic("Example Value Generation")
@Feature("Java Interop")
@DisplayName("Java interop usage")
class JavaInteropUsageTest {

    @Test
    @DisplayName("options presets are static fields and withers express deltas")
    void optionsPresetsAndWithersAreAccessible() {
        SchemaExampleValueGeneratorOptions options =
                SchemaExampleValueGeneratorOptions.RESPONSE_DEFAULTS.withFullExample(true);

        assertThat(options).isEqualTo(new SchemaExampleValueGeneratorOptions(
                SchemaExampleValueGeneratorOptions.DEFAULT_MAX_EXAMPLE_DEPTH,
                true,
                false,
                true,
                true
        ));
    }

    @Test
    @DisplayName("factory is reachable without arguments and generator overloads omit variationIndex")
    void factoryCreatesGeneratorWithoutArguments() {
        SchemaExampleValueGenerator generator = new SchemaExampleValueGeneratorFactory().create();

        Object value = generator.getExampleValue("param", new StringSchema().example("x"), new OpenAPI());

        assertThat(value).isEqualTo("x");
    }

    @Test
    @DisplayName("settings expose a defaults() entry point")
    void settingsExposeDefaultsEntryPoint() {
        assertThat(ExampleValueSettings.defaults()).isEqualTo(new ExampleValueSettings());
    }

    @Test
    @DisplayName("extractor accepts a lambda body generator via SAM conversion")
    void extractorAcceptsLambdaBodyGenerator() {
        ResponseExampleExtractor extractor =
                new ResponseExampleExtractor((schema, api) -> Map.of("id", 1));
        Operation operation = new Operation().responses(
                new ApiResponses().addApiResponse(
                        "200",
                        new ApiResponse().content(
                                new Content().addMediaType(
                                        "application/json",
                                        new MediaType().schema(new ObjectSchema())
                                )
                        )
                )
        );

        ExtractedResponseExample extracted =
                extractor.extractExpectedResponseExampleWithMediaType(operation, new OpenAPI(), 200);

        assertThat(extracted.getBody()).isEqualTo(Map.of("id", 1));
        assertThat(extracted.getMediaType()).isEqualTo("application/json");
    }

    @Test
    @DisplayName("merger resolves component refs via the (input, openAPI) overload")
    void mergerResolvesRefsAgainstOpenApi() {
        ObjectSchema baseSchema = new ObjectSchema();
        baseSchema.addProperty("id", new StringSchema());
        baseSchema.setRequired(List.of("id"));
        OpenAPI openAPI = new OpenAPI().components(new Components().addSchemas("Base", baseSchema));

        Schema<Object> refSchema = new Schema<>();
        refSchema.set$ref("#/components/schemas/Base");
        ObjectSchema input = new ObjectSchema();
        input.setAllOf(List.of(refSchema));

        Schema<?> merged = new SchemaMerger().mergeWithSubSchemas(input, openAPI);

        assertThat(merged.getRequired()).containsExactly("id");
        assertThat(merged.getProperties().keySet()).containsExactly("id");
    }

    @Test
    @DisplayName("resolveSchemaRef is null-tolerant")
    void resolveSchemaRefIsNullTolerant() {
        StringSchema schema = new StringSchema();

        assertThat(SchemaTypeHelpers.resolveSchemaRef(null, new OpenAPI())).isNull();
        assertThat(SchemaTypeHelpers.resolveSchemaRef(schema, null)).isSameAs(schema);
    }
}
