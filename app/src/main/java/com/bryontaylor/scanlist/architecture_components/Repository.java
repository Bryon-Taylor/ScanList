package com.bryontaylor.scanlist.architecture_components;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.bryontaylor.scanlist.ListItem;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Repository {

  private ListItemDao listItemDao; // Data Access Object interacts with the database

  // For executing database operations on a background thread
  private ExecutorService executorSvc = ListItemDatabase.getExecutorSvc();

  public Repository(Application application) {
    ListItemDatabase listItemDB = ListItemDatabase.getInstance(application);
    listItemDao = listItemDB.getDao();
  }

  public void insert(ListItem item) {
    executorSvc.execute(() -> listItemDao.insert(item));
  }

  public void update(ListItem item) {
    executorSvc.execute(() -> listItemDao.update(item));
  }

  public void delete(ListItem item) {
    executorSvc.execute(() -> listItemDao.delete(item));
  }

  public LiveData<List<ListItem>> getAllItems() {
    return listItemDao.getAllItems();
  }

  public void deleteAllItems() {
    executorSvc.execute(() -> listItemDao.deleteAllItems());
  }

  public void deleteCheckedItems() {
    executorSvc.execute(() -> listItemDao.deleteCheckedItems());
  }

  // Get a list of names for sharing e.g. via text or email
  public List<String> getItemNames() throws ExecutionException, InterruptedException {

    // Use the Future class to ensure that the operation is completed before returning values
    Future<List<String>> future = executorSvc.submit(() -> listItemDao.getItemNames());
    List<String> itemNames = future.get();
    return itemNames;
  }
}
