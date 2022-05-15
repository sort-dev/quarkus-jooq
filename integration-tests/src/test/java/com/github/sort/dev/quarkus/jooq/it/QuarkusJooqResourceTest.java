package com.github.sort.dev.quarkus.jooq.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusJooqResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/quarkus-jooq")
                .then()
                .statusCode(200)
                .body(is("Hello quarkus-jooq"));
    }
}
