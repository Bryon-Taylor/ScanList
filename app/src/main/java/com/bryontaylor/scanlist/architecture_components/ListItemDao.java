package com.bryontaylor.scanlist.architecture_components;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.bryontaylor.scanlist.ListItem;

import java.util.List;

@Dao
public interface ListItemDao {

  @Insert
  void insert(ListItem item);

  @Update
  void update(ListItem item);

  @Delete
  void delete(ListItem item);

  // Get all items
  @Query("SELECT * FROM list_item_table ORDER BY positionInList ASC")
  LiveData<List<ListItem>> getAllItems();

  // Delete all items
  @Query("DELETE FROM list_item_table")
  void deleteAllItems();

  // Delete all items whose checkbox is checked
  @Query("DELETE FROM list_item_table WHERE isChecked = 1")
  void deleteCheckedItems();

  // Get all item names for sharing a list
  @Query("SELECT itemName FROM list_item_table ORDER BY positionInList ASC")
  List<String> getItemNames();


}