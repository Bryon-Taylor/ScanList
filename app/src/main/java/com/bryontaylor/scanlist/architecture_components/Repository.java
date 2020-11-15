package com.bryontaylor.scanlist.architecture_components;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.bryontaylor.scanlist.ListItem;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class Repository {

  private ListItemDao listItemDao;
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

  public void deleteAll() {
    executorSvc.execute(() -> listItemDao.deleteAll());
  }

  public LiveData<List<ListItem>> getAllItems() {
    return listItemDao.getAllItems();
  }
}
