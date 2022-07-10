package com.example.lab1.rest;

import com.example.lab1.model.ProductionInstallation;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.example.lab1.LambdaMatcher.matches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductionInstallationRepositoryTests extends AbstractRepositoryTests {

    private static final String URI_BASE_PATH = "/production-installations";

    private static final String JSON_BASE_PATH = "_embedded.productionInstallations";

    private static Long createdInstallationId;

    private static ExtractableResponse<Response> createInstallationResponse;

    @Test
    @Order(1)
    void testCreateValidInstallation() {
        clearRepositories();

        createInstallationResponse = buildRequestSpecification()
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

        createdInstallationId = createInstallationResponse.jsonPath().getLong("id");

        ProductionInstallation installation = productionInstallationRepository.findById(createdInstallationId).orElseThrow();
        assertThat(installation.getId()).isEqualTo(createdInstallationId);
        assertThat(installation.getName()).isEqualTo(Fixtures.ProductionInstallation.NAME);
        assertThat(installation.getOutputPower()).isEqualTo(Fixtures.ProductionInstallation.OUTPUT_POWER);
        assertThat(installation.getContact()).isNull();

        //
        // check that newly created production installation appears in the list
        //
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
    @Order(2)
    void testAssignContactToInstallation() {
        String installationContactUri = createInstallationResponse.jsonPath().getString("_links.contact.href");

        //
        // check that no contact is currently associated with the production installation
        //
        buildRequestSpecification()
                .get(installationContactUri)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());

        Response createContactResponse = createValidContact();
        JsonPath createContactJsonPath = createContactResponse.thenReturn().jsonPath();
        Long contactId = createContactJsonPath.getLong("id");
        String contactUri = createContactJsonPath.getString("_links.self.href");

        //
        // associate production installation with contact
        //
        buildRequestSpecification()
                .body(contactUri)
                .contentType(RestMediaTypes.TEXT_URI_LIST.toString())
                .put(installationContactUri)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        //
        // check that a contact is now associated with the production installation
        //
        buildRequestSpecification()
                .get(installationContactUri)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)));

        assertThat(productionInstallationRepository.findById(createdInstallationId).orElseThrow().getContact()).isEqualTo(
                contactRepository.findById(contactId).orElseThrow()
        );
    }

    @Test
    @Order(3)
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
    @Order(4)
    void testFindInstallationsByMatchingName() {
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
    @Order(5)
    void testFindInstallationsByNonMatchingName() {
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
    @Order(6)
    void testFindInstallationsByMatchingOutputPower() {
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
    @Order(7)
    void whenFindInstallationsByNonMatchingOutputPower() {
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
