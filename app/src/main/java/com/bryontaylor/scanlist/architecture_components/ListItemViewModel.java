package com.bryontaylor.scanlist.architecture_components;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bryontaylor.scanlist.ListItem;

import java.util.List;

public class ListItemViewModel extends AndroidViewModel {

  private Repository repository;

  public ListItemViewModel(@NonNull Application application) {
    super(application);
    repository = new Repository(application);
  }

  public void insert(ListItem item) {
    repository.insert(item);
  }

  public void update(ListItem item) {
    repository.update(item);
  }

  public void delete(ListItem item) {
    repository.delete(item);
  }

  public void deleteAll() {
    repository.deleteAll();
  }

  public LiveData<List<ListItem>> getAllItems() {
    return repository.getAllItems();
  }
}
