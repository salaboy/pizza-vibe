package com.pizzavibe.store.agent;

import com.pizzavibe.store.model.DrinkItem;
import com.pizzavibe.store.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderSubmissionToolTest {

    @Test
    void shouldParseOrderItems() {
        String input = "OrderItem[pizzaType=Pepperoni, quantity=2], OrderItem[pizzaType=Margherita, quantity=1]";
        List<OrderItem> items = OrderSubmissionTool.parseOrderItems(input);
        assertEquals(2, items.size());
        assertEquals("Pepperoni", items.get(0).pizzaType());
        assertEquals(2, items.get(0).quantity());
        assertEquals("Margherita", items.get(1).pizzaType());
        assertEquals(1, items.get(1).quantity());
    }

    @Test
    void shouldParseDrinkItems() {
        String input = "DrinkItem[drinkType=Beer, quantity=1], DrinkItem[drinkType=Coke, quantity=2]";
        List<DrinkItem> items = OrderSubmissionTool.parseDrinkItems(input);
        assertEquals(2, items.size());
        assertEquals("Beer", items.get(0).drinkType());
        assertEquals(1, items.get(0).quantity());
        assertEquals("Coke", items.get(1).drinkType());
        assertEquals(2, items.get(1).quantity());
    }

    @Test
    void shouldReturnEmptyForBlankInput() {
        assertTrue(OrderSubmissionTool.parseOrderItems("").isEmpty());
        assertTrue(OrderSubmissionTool.parseOrderItems(null).isEmpty());
        assertTrue(OrderSubmissionTool.parseDrinkItems("").isEmpty());
        assertTrue(OrderSubmissionTool.parseDrinkItems(null).isEmpty());
    }

    @Test
    void shouldParseSingleItem() {
        String input = "OrderItem[pizzaType=Hawaiian, quantity=3]";
        List<OrderItem> items = OrderSubmissionTool.parseOrderItems(input);
        assertEquals(1, items.size());
        assertEquals("Hawaiian", items.get(0).pizzaType());
        assertEquals(3, items.get(0).quantity());
    }
}
