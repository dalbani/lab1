package com.example.lab1.rest;

import com.example.lab1.model.Contact;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.example.lab1.LambdaMatcher.matches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContactRepositoryTests extends AbstractRepositoryTests {

    public static final String URI_BASE_PATH = "/contacts";

    private static final String JSON_BASE_PATH = "_embedded.contacts";

    private static Long createdContactId;

    @Test
    @Order(1)
    void testCreateContact() {
        clearRepositories();

        ExtractableResponse<Response> response = createValidContact()
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body("id", greaterThan(0))
                .body("name", equalTo(Fixtures.Contact.NAME))
                .body("zipCode", equalTo(Fixtures.Contact.ZIP_CODE))
                .body("city", equalTo(Fixtures.Contact.CITY))
                .body("houseNumber", equalTo(Fixtures.Contact.HOUSE_NUMBER))
                .extract();

        createdContactId = response.jsonPath().getLong("id");

        //
        // check that the repository reflects the changes made via REST
        //
        Contact contact = contactRepository.findById(createdContactId).orElseThrow();
        assertThat(contact.getId()).isEqualTo(createdContactId);
        assertThat(contact.getName()).isEqualTo(Fixtures.Contact.NAME);
        assertThat(contact.getZipCode()).isEqualTo(Fixtures.Contact.ZIP_CODE);
        assertThat(contact.getCity()).isEqualTo(Fixtures.Contact.CITY);
        assertThat(contact.getHouseNumber()).isEqualTo(Fixtures.Contact.HOUSE_NUMBER);

        //
        // check that newly created contact appears in the list
        //
        buildRequestSpecification()
                .get(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(JSON_BASE_PATH + "[0].id", equalTo(createdContactId.intValue()))
                .body(JSON_BASE_PATH + "[0].name", equalTo(Fixtures.Contact.NAME))
                .body(JSON_BASE_PATH + "[0].zipCode", equalTo(Fixtures.Contact.ZIP_CODE))
                .body(JSON_BASE_PATH + "[0].city", equalTo(Fixtures.Contact.CITY))
                .body(JSON_BASE_PATH + "[0].houseNumber", equalTo(Fixtures.Contact.HOUSE_NUMBER));
    }

    @Test
    @Order(2)
    void testCreateContactWithoutName() {
        HttpStatus expectedStatus = HttpStatus.BAD_REQUEST;

        createInvalidContact()
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
                        .name(Fixtures.Contact.NAME.toUpperCase())
                        .zipCode(Fixtures.Contact.ZIP_CODE)
                        .city(Fixtures.Contact.CITY)
                        .houseNumber(Fixtures.Contact.HOUSE_NUMBER)
                        .build())
                .contentType(ContentType.JSON)
                .put(URI_BASE_PATH + "/" + createdContactId)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body("id", equalTo(createdContactId.intValue()))
                .body("name", equalTo(Fixtures.Contact.NAME.toUpperCase()))
                .body("zipCode", equalTo(Fixtures.Contact.ZIP_CODE))
                .body("city", equalTo(Fixtures.Contact.CITY))
                .body("houseNumber", equalTo(Fixtures.Contact.HOUSE_NUMBER));

        //
        // check that the repository reflects the changes made via REST
        //
        Contact contact = contactRepository.findById(createdContactId).orElseThrow();
        assertThat(contact.getId()).isEqualTo(createdContactId);
        assertThat(contact.getName()).isEqualTo(Fixtures.Contact.NAME.toUpperCase());
        assertThat(contact.getZipCode()).isEqualTo(Fixtures.Contact.ZIP_CODE);
        assertThat(contact.getCity()).isEqualTo(Fixtures.Contact.CITY);
        assertThat(contact.getHouseNumber()).isEqualTo(Fixtures.Contact.HOUSE_NUMBER);
    }

    @Test
    @Order(4)
    void testDeleteContact() {
        buildRequestSpecification()
                .delete(URI_BASE_PATH + "/" + createdContactId)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        //
        // check that the repository reflects the changes made via REST
        //
        assertThat(contactRepository.findById(createdContactId)).isEmpty();
    }

}
