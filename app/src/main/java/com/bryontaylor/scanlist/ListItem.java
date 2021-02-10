package com.bryontaylor.scanlist;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "list_item_table")
public class ListItem {

  @PrimaryKey(autoGenerate = true)
  private long id;

  private String itemName;
  private boolean isChecked;
  private double positionInList;

  public ListItem() { // Default constructor
  }

  // Constructor used for testing
  public ListItem(String itemName, boolean isChecked, double positionInList) {
    this.itemName = itemName;
    this.isChecked = isChecked;
    this.positionInList = positionInList;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public void setChecked(boolean isChecked) {
    this.isChecked = isChecked;
  }

  public boolean getIsChecked() {
    return isChecked;
  }

  public void setPositionInList(double positionInList) {
    this.positionInList = positionInList;
  }

  public double getPositionInList() {
    return positionInList;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    ListItem item = new ListItem();
    if(obj instanceof ListItem) {
       item = (ListItem) obj;
    }

    // Compare the 3 fields. If all are identical then objects are equal
    return this.getItemName().equals(item.getItemName()) &&
        this.getIsChecked() == item.getIsChecked() &&
        this.getPositionInList() == item.getPositionInList();
  }

  @Override
  public String toString() {
    return "ListItem {" + "id = " + id + ", " +
        "itemName = '" + itemName + '\'' + ", " +
        "isChecked = " + isChecked + ", " +
        "positionInList = " + positionInList + '}';
  }
}
