package com.example.lab1.rest;

import com.example.lab1.model.ProductionInstallation;
import com.example.lab1.repository.ProductionInstallationRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.concurrent.atomic.AtomicLong;

import static com.example.lab1.LambdaMatcher.matches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

class ProductionInstallationRepositoryTests extends AbstractRepositoryTests {

    private static final String URI_BASE_PATH = "/production-installations";

    private static final String JSON_BASE_PATH = "_embedded.productionInstallations";

    private static final String NAME = "My installation";

    private static final Double OUTPUT_POWER = 0.123;

    @Autowired
    private ProductionInstallationRepository productionInstallationRepository;

    private final AtomicLong createdInstallationId = new AtomicLong();

    @BeforeEach
    void beforeEach() {
        productionInstallationRepository.deleteAll();
    }

    @Test
    void whenCreateValidInstallation_thenSucceed() {
        buildRequestSpecification()
                .body(ProductionInstallation.builder()
                        .name(NAME)
                        .outputPower(OUTPUT_POWER)
                        .build())
                .contentType(ContentType.JSON)
                .post(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body("id", matches((Integer id) -> { createdInstallationId.set(id); return id > 0; }))
                .body("name", equalTo(NAME))
                .body("outputPower", equalTo(OUTPUT_POWER.floatValue()));

        ProductionInstallation installation = productionInstallationRepository.findById(createdInstallationId.get()).orElseThrow();
        assertThat(installation.getId()).isEqualTo(createdInstallationId.get());
        assertThat(installation.getName()).isEqualTo(NAME);
        assertThat(installation.getOutputPower()).isEqualTo(OUTPUT_POWER);

        buildRequestSpecification()
                .get(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH + "[0].id", equalTo(Long.valueOf(createdInstallationId.get()).intValue()))
                .body(JSON_BASE_PATH + "[0].name", equalTo(NAME))
                .body(JSON_BASE_PATH + "[0].outputPower", equalTo(OUTPUT_POWER.floatValue()));
    }

    @Test
    void whenCreateInvalidInstallation_thenFail() {
        HttpStatus expectedStatus = HttpStatus.BAD_REQUEST;

        buildRequestSpecification()
                .body(ProductionInstallation.builder()
                        .name(NAME)
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
    void whenFindInstallationsByMatchingName_thenSucceed() {
        whenCreateValidInstallation_thenSucceed();

        buildRequestSpecification()
                .queryParam("name", NAME)
                .get(URI_BASE_PATH + "/search/findAllByName")
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH + "[0].id", equalTo(Long.valueOf(createdInstallationId.get()).intValue()))
                .body(JSON_BASE_PATH + "[0].name", equalTo(NAME))
                .body(JSON_BASE_PATH + "[0].outputPower", equalTo(OUTPUT_POWER.floatValue()));
    }

    @Test
    void whenFindInstallationsByNonMatchingName_thenFail() {
        whenCreateValidInstallation_thenSucceed();

        buildRequestSpecification()
                .queryParam("name", NAME.toUpperCase())
                .get(URI_BASE_PATH + "/search/findAllByName")
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH, empty());
    }

    @Test
    void whenFindInstallationsByMatchingOutputPower_thenSucceed() {
        whenCreateValidInstallation_thenSucceed();

        buildRequestSpecification()
                .queryParam("powerGreaterThan", 0.1)
                .queryParam("powerLowerThan", 0.9)
                .get(URI_BASE_PATH + "/search/findAllByOutputPowerBetween")
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH + "[0].id", equalTo(Long.valueOf(createdInstallationId.get()).intValue()))
                .body(JSON_BASE_PATH + "[0].name", equalTo(NAME))
                .body(JSON_BASE_PATH + "[0].outputPower", equalTo(OUTPUT_POWER.floatValue()));
    }

    @Test
    void whenFindInstallationsByNonMatchingOutputPower_thenFail() {
        whenCreateValidInstallation_thenSucceed();

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
