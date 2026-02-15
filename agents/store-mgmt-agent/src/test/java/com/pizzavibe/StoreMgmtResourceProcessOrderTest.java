package com.pizzavibe;

import com.pizzavibe.agent.StoreMgmtAgent;
import com.pizzavibe.model.OrderStatus;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class StoreMgmtResourceProcessOrderTest {

    @InjectMock
    StoreMgmtAgent storeMgmtAgent;

    @Test
    void shouldReturnJsonContentType() {
        Mockito.when(storeMgmtAgent.processOrder(Mockito.anyString()))
            .thenReturn(new OrderStatus("cooking", "delivering"));

        given()
            .contentType(ContentType.JSON)
            .body("{\"orderId\":\"order-1\",\"orderItems\":[{\"pizzaType\":\"Margherita\",\"quantity\":1}]}")
            .when().post("/mgmt/processOrder")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void shouldReturnOrderStatusWithKitchenAndDeliveryStatus() {
        Mockito.when(storeMgmtAgent.processOrder(Mockito.anyString()))
            .thenReturn(new OrderStatus("cooked successfully", "delivered successfully"));

        given()
            .contentType(ContentType.JSON)
            .body("{\"orderId\":\"order-1\",\"orderItems\":[{\"pizzaType\":\"Margherita\",\"quantity\":1}]}")
            .when().post("/mgmt/processOrder")
            .then()
            .statusCode(200)
            .body("kitchenStatus", is("cooked successfully"))
            .body("deliveryStatus", is("delivered successfully"));
    }

    @Test
    void shouldPassOrderInfoToAgent() {
        Mockito.when(storeMgmtAgent.processOrder(Mockito.anyString()))
            .thenReturn(new OrderStatus("done", "done"));

        given()
            .contentType(ContentType.JSON)
            .body("{\"orderId\":\"order-abc\",\"orderItems\":[{\"pizzaType\":\"Hawaiian\",\"quantity\":3}]}")
            .when().post("/mgmt/processOrder")
            .then()
            .statusCode(200);

        Mockito.verify(storeMgmtAgent)
            .processOrder(Mockito.contains("order-abc"));
    }
}
