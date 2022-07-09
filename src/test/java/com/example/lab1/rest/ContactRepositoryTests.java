package com.example.lab1.rest;

import com.example.lab1.model.Contact;
import com.example.lab1.repository.ContactRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.concurrent.atomic.AtomicLong;

import static com.example.lab1.rest.LambdaMatcher.matches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ContactRepositoryTests extends AbstractRepositoryTests {

    private static final String URI_BASE_PATH = "/contacts";

    private static final String JSON_BASE_PATH = "_embedded.contacts";

    private static final String NAME = "My contact";

    private static final String ZIP_CODE = "0000AA";

    private static final String CITY = "Arnhem";

    private static final String HOUSE_NUMBER = "12a";

    @Autowired
    private ContactRepository contactRepository;

    private final AtomicLong createdContactId = new AtomicLong();

    @Test
    void whenCreateContact_thenSuccess() {
        buildRequestSpecification()
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
                .body("id", matches((Integer id) -> { createdContactId.set(id); return id > 0; }))
                .body("name", equalTo(NAME))
                .body("zipCode", equalTo(ZIP_CODE))
                .body("city", equalTo(CITY))
                .body("houseNumber", equalTo(HOUSE_NUMBER));

        Contact contact = contactRepository.findById(createdContactId.get()).orElseThrow();
        assertThat(contact.getId()).isEqualTo(createdContactId.get());
        assertThat(contact.getName()).isEqualTo(NAME);
        assertThat(contact.getZipCode()).isEqualTo(ZIP_CODE);
        assertThat(contact.getCity()).isEqualTo(CITY);
        assertThat(contact.getHouseNumber()).isEqualTo(HOUSE_NUMBER);

        String jsonBasePath = String.format(JSON_BASE_PATH + "[%d].", createdContactId.get() - 1);

        buildRequestSpecification()
                .get(URI_BASE_PATH)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body(jsonBasePath + "id", equalTo(Long.valueOf(createdContactId.get()).intValue()))
                .body(jsonBasePath + "name", equalTo(NAME))
                .body(jsonBasePath + "zipCode", equalTo(ZIP_CODE))
                .body(jsonBasePath + "city", equalTo(CITY))
                .body(jsonBasePath + "houseNumber", equalTo(HOUSE_NUMBER));
    }

    @Test
    void whenCreateContactWithoutName_thenFail() {
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
    void whenModifyContact_thenSuccess() {
        whenCreateContact_thenSuccess();

        buildRequestSpecification()
                .body(Contact.builder()
                        .name(NAME.toUpperCase())
                        .zipCode(ZIP_CODE)
                        .city(CITY)
                        .houseNumber(HOUSE_NUMBER)
                        .build())
                .contentType(ContentType.JSON)
                .put(URI_BASE_PATH + "/" + createdContactId.get())
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(matches(contentType -> MediaType.parseMediaType(contentType).isCompatibleWith(MediaTypes.HAL_JSON)))
                .body("id", equalTo(Long.valueOf(createdContactId.get()).intValue()))
                .body("name", equalTo(NAME.toUpperCase()))
                .body("zipCode", equalTo(ZIP_CODE))
                .body("city", equalTo(CITY))
                .body("houseNumber", equalTo(HOUSE_NUMBER));

        Contact contact = contactRepository.findById(createdContactId.get()).orElseThrow();
        assertThat(contact.getId()).isEqualTo(createdContactId.get());
        assertThat(contact.getName()).isEqualTo(NAME.toUpperCase());
        assertThat(contact.getZipCode()).isEqualTo(ZIP_CODE);
        assertThat(contact.getCity()).isEqualTo(CITY);
        assertThat(contact.getHouseNumber()).isEqualTo(HOUSE_NUMBER);
    }

    @Test
    void whenDeleteContact_thenSuccess() {
        whenCreateContact_thenSuccess();

        buildRequestSpecification()
                .delete(URI_BASE_PATH + "/" + createdContactId.get())
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(contactRepository.findById(createdContactId.get())).isEmpty();
    }

}
