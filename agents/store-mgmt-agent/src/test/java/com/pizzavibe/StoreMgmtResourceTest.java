package com.pizzavibe;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class StoreMgmtResourceTest {

    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/mgmt")
          .then()
             .statusCode(200)
             .body(is("Hello from Store Management Agent"));
    }
}
