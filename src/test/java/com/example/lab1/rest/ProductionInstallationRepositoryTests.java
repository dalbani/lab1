package com.example.lab1.rest;

import com.example.lab1.model.ProductionInstallation;
import com.example.lab1.repository.ProductionInstallationRepository;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.example.lab1.LambdaMatcher.matches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

class ProductionInstallationRepositoryTests extends AbstractRepositoryTests {

    private static final String URI_BASE_PATH = "/production-installations";

    private static final String JSON_BASE_PATH = "_embedded.productionInstallations";

    @Autowired
    private ProductionInstallationRepository productionInstallationRepository;

    private Long createdInstallationId;

    @BeforeEach
    void beforeEach() {
        productionInstallationRepository.deleteAll();
    }

    @Test
    void testCreateValidInstallation() {
        ExtractableResponse<Response> response = buildRequestSpecification()
                .body(ProductionInstallation.builder()
                        .name(Fixtures.ProductionInstallation.NAME)
                        .outputPower(Fixtures.ProductionInstallation.OUTPUT_POWER)
                        .build())
                .contentType(ContentType.JSON)
                .post(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body("id", greaterThan(0))
                .body("name", equalTo(Fixtures.ProductionInstallation.NAME))
                .body("outputPower", equalTo(Fixtures.ProductionInstallation.OUTPUT_POWER.floatValue()))
                .extract();

        createdInstallationId = response.jsonPath().getLong("id");

        ProductionInstallation installation = productionInstallationRepository.findById(createdInstallationId).orElseThrow();
        assertThat(installation.getId()).isEqualTo(createdInstallationId);
        assertThat(installation.getName()).isEqualTo(Fixtures.ProductionInstallation.NAME);
        assertThat(installation.getOutputPower()).isEqualTo(Fixtures.ProductionInstallation.OUTPUT_POWER);
        assertThat(installation.getContact()).isNull();

        buildRequestSpecification()
                .get(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH + "[0].id", equalTo(createdInstallationId.intValue()))
                .body(JSON_BASE_PATH + "[0].name", equalTo(Fixtures.ProductionInstallation.NAME))
                .body(JSON_BASE_PATH + "[0].outputPower", equalTo(Fixtures.ProductionInstallation.OUTPUT_POWER.floatValue()));
    }

    @Test
    void testCreateInvalidInstallation() {
        HttpStatus expectedStatus = HttpStatus.BAD_REQUEST;

        buildRequestSpecification()
                .body(ProductionInstallation.builder()
                        .name(Fixtures.ProductionInstallation.NAME)
                        .outputPower(0.00)
                        .build())
                .contentType(ContentType.JSON)
                .post(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(expectedStatus.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)))
                .body("status", equalTo(expectedStatus.value()))
                .body("error", equalTo(expectedStatus.getReasonPhrase()))
                .body("problems[0]", equalTo("Property \"outputPower\": must be greater than or equal to 0.0001."));
    }

    @Test
    void testFindInstallationsByMatchingName() {
        testCreateValidInstallation();

        buildRequestSpecification()
                .queryParam("name", Fixtures.ProductionInstallation.NAME)
                .get(URI_BASE_PATH + "/search/findAllByName")
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH + "[0].id", equalTo(createdInstallationId.intValue()))
                .body(JSON_BASE_PATH + "[0].name", equalTo(Fixtures.ProductionInstallation.NAME))
                .body(JSON_BASE_PATH + "[0].outputPower", equalTo(Fixtures.ProductionInstallation.OUTPUT_POWER.floatValue()));
    }

    @Test
    void testFindInstallationsByNonMatchingName() {
        testCreateValidInstallation();

        buildRequestSpecification()
                .queryParam("name", Fixtures.ProductionInstallation.NAME.toUpperCase())
                .get(URI_BASE_PATH + "/search/findAllByName")
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH, empty());
    }

    @Test
    void testFindInstallationsByMatchingOutputPower() {
        testCreateValidInstallation();

        buildRequestSpecification()
                .queryParam("powerGreaterThan", 0.1)
                .queryParam("powerLowerThan", 0.9)
                .get(URI_BASE_PATH + "/search/findAllByOutputPowerBetween")
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH + "[0].id", equalTo(createdInstallationId.intValue()))
                .body(JSON_BASE_PATH + "[0].name", equalTo(Fixtures.ProductionInstallation.NAME))
                .body(JSON_BASE_PATH + "[0].outputPower", equalTo(Fixtures.ProductionInstallation.OUTPUT_POWER.floatValue()));
    }

    @Test
    void whenFindInstallationsByNonMatchingOutputPower_thenFail() {
        testCreateValidInstallation();

        buildRequestSpecification()
                .queryParam("powerGreaterThan", 0.5)
                .queryParam("powerLowerThan", 0.6)
                .get(URI_BASE_PATH + "/search/findAllByOutputPowerBetween")
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH, empty());
    }

}
