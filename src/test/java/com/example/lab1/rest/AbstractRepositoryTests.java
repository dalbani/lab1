package com.example.lab1.rest;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
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

    protected RequestSpecification buildRequestSpecification() {
        String baseUri = String.format("http://localhost:%d%s", serverPort, restApiBasePath);

        return with().baseUri(baseUri).config(
                RestAssured.config()
                        .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)));
    }

}
