package com.bryontaylor.scanlist;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "list_item_table")
public class ListItem {

  @PrimaryKey(autoGenerate = true)
  private long id;

  private String itemName;
  private boolean isChecked;

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

  public boolean isChecked() {
    return isChecked;
  }

  public void setChecked(boolean checked) {
    isChecked = checked;
  }
}
