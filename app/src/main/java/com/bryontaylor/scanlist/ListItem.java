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

  // testing
  private double positionInList;

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

  // testing
  public void setPositionInList(double positionInList) {
    this.positionInList = positionInList;
  }

  public double getPositionInList() {
    return positionInList;
  }

  public boolean equals(@Nullable ListItem listItem) {
    return this.itemName.equals(listItem.getItemName()) && this.isChecked == listItem.getIsChecked();
  }
}
