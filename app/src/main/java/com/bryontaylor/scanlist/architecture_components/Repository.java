package com.bryontaylor.scanlist.architecture_components;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.bryontaylor.scanlist.ListItem;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Repository {

  private ListItemDao listItemDao;
  private List<String> itemNames;
  private static final String TAG = "Repository";
  //private long id;

  // for executing database operations on background thread
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

  public List<String> getItemNames() throws ExecutionException, InterruptedException {

    Future<List<String>> future = executorSvc.submit(() -> listItemDao.getItemNames());
    List<String> itemNames = future.get();
    return itemNames;
//    executorSvc.execute(new Runnable() {
//      @Override
//      public void run() {
//        itemNames = listItemDao.getItemNames();
//      }
//    });
//    return itemNames;
  }
}
