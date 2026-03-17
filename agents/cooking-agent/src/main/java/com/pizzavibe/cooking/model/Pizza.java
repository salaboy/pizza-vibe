package com.pizzavibe.cooking.model;

import java.util.List;
import java.util.Map;

public record Pizza(
    String name,
    Map<Ingredient, Integer> requiredIngredients
) {
  public static final Pizza MARGHERITA = new Pizza(
      "Margherita",
      Map.of(
          Ingredient.DOUGH, 1,
          Ingredient.TOMATO_SAUCE, 1,
          Ingredient.MOZZARELLA, 2,
          Ingredient.BASIL, 1
      )
  );

  public static final Pizza PEPPERONI = new Pizza(
      "Pepperoni",
      Map.of(
          Ingredient.DOUGH, 1,
          Ingredient.TOMATO_SAUCE, 1,
          Ingredient.MOZZARELLA, 2,
          Ingredient.PEPPERONI, 3
      )
  );

  public static final Pizza VEGGIE = new Pizza(
      "Veggie",
      Map.of(
          Ingredient.DOUGH, 1,
          Ingredient.TOMATO_SAUCE, 1,
          Ingredient.MOZZARELLA, 1,
          Ingredient.MUSHROOMS, 2,
          Ingredient.BELL_PEPPER, 2,
          Ingredient.OLIVES, 2,
          Ingredient.ONION, 1
      )
  );

  public static final Pizza HAWAIIAN = new Pizza(
      "Hawaiian",
      Map.of(
          Ingredient.DOUGH, 1,
          Ingredient.TOMATO_SAUCE, 1,
          Ingredient.MOZZARELLA, 2,
          Ingredient.HAM, 2,
          Ingredient.PINEAPPLE, 2
      )
  );

  public static List<Pizza> getAllPizzaTypes() {
    return List.of(MARGHERITA, PEPPERONI, VEGGIE, HAWAIIAN);
  }

  @Override
  public String toString() {
    return "Pizza{" +
        "name='" + name + '\'' +
        ", requiredIngredients=" + requiredIngredients +
        '}';
  }
}
