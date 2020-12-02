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

  // get all Items
  @Query("SELECT * FROM list_item_table")
  LiveData<List<ListItem>> getAllItems();

  // delete all items
  @Query("DELETE FROM list_item_table")
  void deleteAllItems();

  // delete all items whose checkbox is checked
  @Query("DELETE FROM list_item_table WHERE isChecked = 1")
  void deleteCheckedItems();

  @Query("SELECT itemName FROM list_item_table")
  List<String> getItemNames();
}