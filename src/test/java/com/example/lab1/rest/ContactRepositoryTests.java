package com.example.lab1.rest;

import com.example.lab1.model.Contact;
import com.example.lab1.repository.ContactRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.concurrent.atomic.AtomicLong;

import static com.example.lab1.rest.LambdaMatcher.matches;
import static io.restassured.RestAssured.with;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContactRepositoryTests {

    private static final String NAME = "My contact";

    private static final String ZIP_CODE = "0000AA";

    private static final String CITY = "Arnhem";

    private static final String HOUSE_NUMBER = "12a";

    @Value("${spring.data.rest.base-path}")
    private String restApiBasePath;

    @LocalServerPort
    private int serverPort;

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
                .post("/contacts")
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

        String jsonBasePath = String.format("_embedded.contacts[%d].", createdContactId.get() - 1);

        buildRequestSpecification()
                .get("/contacts")
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
                .put("/contacts/" + createdContactId.get())
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
                .delete("/contacts/" + createdContactId.get())
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(contactRepository.findById(createdContactId.get())).isEmpty();
    }

    private RequestSpecification buildRequestSpecification() {
        String baseUri = String.format("http://localhost:%d%s", serverPort, restApiBasePath);

        return with().baseUri(baseUri).config(
                RestAssured.config()
                        .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)));
    }

}
