package com.bryontaylor.scanlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/*
   Tests the correctness of the overridden equals() method in ListItem.java
 */
public class ListItemTest {

  // Compare two ListItems that are equal
  @Test
  void areListItemsEqual_identicalProperties_returnTrue() {

    // Arrange
    ListItem item1 = new ListItem("tea", true, 1);
    ListItem item2 = new ListItem("tea", true, 1);

    // Assert
    assertEquals(item1, item2);
    System.out.println("Passed. The items are equal because 'itemName', 'isChecked', " +
        "and 'positionInList' values are all the same.");
  }

  // Compare ListItems with different item names
  @ParameterizedTest
  @ValueSource(strings = {"1", "two", "bryon", "taylor", "cat"})
  void areListItemsEqual_differentNames_returnFalse(String name) {

    // Arrange
    ListItem item1 = new ListItem("tea", true, 1);
    ListItem item2 = new ListItem(name, true, 1);

    // Assert
    assertNotEquals(item1, item2);
    System.out.println("Passed. The items are NOT equal because of different 'itemName' values.");
  }

  // Compare two ListItems with different checkbox states
  @Test
  void areListItemsEqual_differentCheckboxStates_returnFalse() {

    // Arrange
    ListItem item1 = new ListItem("tea", true, 1);
    ListItem item2 = new ListItem("tea", false, 1);

    // Assert
    assertNotEquals(item1, item2);
    System.out.println("Passed. The items are NOT equal because of different 'isChecked' values.");
  }

  // Compare two ListItems with different positionInList values
  @ParameterizedTest
  @ValueSource(doubles = {10.982, -1998, 0, 12345.678910, -3.14159965})
  void areListItemsEqual_differentPositionInListValues_returnFalse(double position) {

    // Arrange
    ListItem item1 = new ListItem("tea", true, 1);
    ListItem item2 = new ListItem("tea", true, position);

    // Assert
    assertNotEquals(item1, item2);
    System.out.println("Passed. The items are NOT equal because of different " +
        "'positionInList' values");
  }
}
