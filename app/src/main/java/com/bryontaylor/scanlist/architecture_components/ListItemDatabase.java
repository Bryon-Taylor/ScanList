package com.bryontaylor.scanlist.architecture_components;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.bryontaylor.scanlist.ListItem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = ListItem.class, version = 1, exportSchema = false) // TODO: change this to true eventually
public abstract class ListItemDatabase extends RoomDatabase {

  private static ListItemDatabase instance;
  private static final String DB_NAME = "list_item_database";
  private static final int NUM_THREADS = 4;

  final static ExecutorService executorSvc = Executors.newFixedThreadPool(NUM_THREADS);
  public abstract ListItemDao getDao();

  public static synchronized ListItemDatabase getInstance(Context context) {
    if(instance == null) {
      instance = Room.databaseBuilder(
              context.getApplicationContext(),
              ListItemDatabase.class, DB_NAME)
              .fallbackToDestructiveMigration()
              .build();
    }
    return instance;
  }

  public static ExecutorService getExecutorSvc() {
    return executorSvc;
  }
}
