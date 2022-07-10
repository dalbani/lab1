package com.example.lab1.rest;

import com.example.lab1.model.Contact;
import com.example.lab1.repository.ContactRepository;
import com.example.lab1.repository.ProductionInstallationRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.with;
import static io.restassured.config.EncoderConfig.encoderConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractRepositoryTests {

    @Value("${spring.data.rest.base-path}")
    private String restApiBasePath;

    @LocalServerPort
    private int serverPort;

    @Autowired
    protected ContactRepository contactRepository;

    @Autowired
    protected ProductionInstallationRepository productionInstallationRepository;

    public static class Fixtures {
        public static class Contact {

            public static final String NAME = "My contact";

            public static final String ZIP_CODE = "0000AA";

            public static final String CITY = "Arnhem";

            public static final String HOUSE_NUMBER = "12a";

        }

        public static class ProductionInstallation {

            public static final String NAME = "My installation";

            public static final Double OUTPUT_POWER = 0.123;

        }
    }

    protected Response createValidContact() {
        return buildRequestSpecification()
                .body(Contact.builder()
                        .name(Fixtures.Contact.NAME)
                        .zipCode(Fixtures.Contact.ZIP_CODE)
                        .city(Fixtures.Contact.CITY)
                        .houseNumber(Fixtures.Contact.HOUSE_NUMBER)
                        .build())
                .contentType(ContentType.JSON)
                .post(ContactRepositoryTests.URI_BASE_PATH);
    }

    protected Response createInvalidContact() {
        return buildRequestSpecification()
                .body(Contact.builder()
                        .name("")
                        .zipCode(Fixtures.Contact.ZIP_CODE)
                        .city(Fixtures.Contact.CITY)
                        .houseNumber(Fixtures.Contact.HOUSE_NUMBER)
                        .build())
                .contentType(ContentType.JSON)
                .post(ContactRepositoryTests.URI_BASE_PATH);
    }

    protected RequestSpecification buildRequestSpecification() {
        String baseUri = String.format("http://localhost:%d%s", serverPort, restApiBasePath);

        return with().baseUri(baseUri).config(
                RestAssured.config()
                        .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)));
    }

}
