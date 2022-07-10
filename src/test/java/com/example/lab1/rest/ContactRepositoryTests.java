package com.example.lab1.rest;

import com.example.lab1.model.Contact;
import com.example.lab1.repository.ContactRepository;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.example.lab1.LambdaMatcher.matches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContactRepositoryTests extends AbstractRepositoryTests {

    private static final String URI_BASE_PATH = "/contacts";

    private static final String JSON_BASE_PATH = "_embedded.contacts";

    private static final String NAME = "My contact";

    private static final String ZIP_CODE = "0000AA";

    private static final String CITY = "Arnhem";

    private static final String HOUSE_NUMBER = "12a";

    private static Long createdContactId;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    @Order(1)
    void testCreateContact() {
        ExtractableResponse<Response> response = buildRequestSpecification()
                .body(Contact.builder()
                        .name(NAME)
                        .zipCode(ZIP_CODE)
                        .city(CITY)
                        .houseNumber(HOUSE_NUMBER)
                        .build())
                .contentType(ContentType.JSON)
                .post(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body("id", greaterThan(0))
                .body("name", equalTo(NAME))
                .body("zipCode", equalTo(ZIP_CODE))
                .body("city", equalTo(CITY))
                .body("houseNumber", equalTo(HOUSE_NUMBER))
                .extract();

        createdContactId = response.jsonPath().getLong("id");

        Contact contact = contactRepository.findById(createdContactId).orElseThrow();
        assertThat(contact.getId()).isEqualTo(createdContactId);
        assertThat(contact.getName()).isEqualTo(NAME);
        assertThat(contact.getZipCode()).isEqualTo(ZIP_CODE);
        assertThat(contact.getCity()).isEqualTo(CITY);
        assertThat(contact.getHouseNumber()).isEqualTo(HOUSE_NUMBER);

        buildRequestSpecification()
                .get(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH + "[0].id", equalTo(createdContactId.intValue()))
                .body(JSON_BASE_PATH + "[0].name", equalTo(NAME))
                .body(JSON_BASE_PATH + "[0].zipCode", equalTo(ZIP_CODE))
                .body(JSON_BASE_PATH + "[0].city", equalTo(CITY))
                .body(JSON_BASE_PATH + "[0].houseNumber", equalTo(HOUSE_NUMBER));
    }

    @Test
    @Order(2)
    void testCreateContactWithoutName() {
        HttpStatus expectedStatus = HttpStatus.BAD_REQUEST;

        buildRequestSpecification()
                .body(Contact.builder()
                        .name("")
                        .zipCode(ZIP_CODE)
                        .city(CITY)
                        .houseNumber(HOUSE_NUMBER)
                        .build())
                .contentType(ContentType.JSON)
                .post(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(expectedStatus.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)))
                .body("status", equalTo(expectedStatus.value()))
                .body("error", equalTo(expectedStatus.getReasonPhrase()))
                .body("problems[0]", equalTo("Property \"name\": must not be blank."));
    }

    @Test
    @Order(3)
    void testModifyContact() {
        buildRequestSpecification()
                .body(Contact.builder()
                        .name(NAME.toUpperCase())
                        .zipCode(ZIP_CODE)
                        .city(CITY)
                        .houseNumber(HOUSE_NUMBER)
                        .build())
                .contentType(ContentType.JSON)
                .put(URI_BASE_PATH + "/" + createdContactId)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body("id", equalTo(createdContactId.intValue()))
                .body("name", equalTo(NAME.toUpperCase()))
                .body("zipCode", equalTo(ZIP_CODE))
                .body("city", equalTo(CITY))
                .body("houseNumber", equalTo(HOUSE_NUMBER));

        Contact contact = contactRepository.findById(createdContactId).orElseThrow();
        assertThat(contact.getId()).isEqualTo(createdContactId);
        assertThat(contact.getName()).isEqualTo(NAME.toUpperCase());
        assertThat(contact.getZipCode()).isEqualTo(ZIP_CODE);
        assertThat(contact.getCity()).isEqualTo(CITY);
        assertThat(contact.getHouseNumber()).isEqualTo(HOUSE_NUMBER);
    }

    @Test
    @Order(4)
    void testDeleteContact() {
        buildRequestSpecification()
                .delete(URI_BASE_PATH + "/" + createdContactId)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(contactRepository.findById(createdContactId)).isEmpty();
    }

}
