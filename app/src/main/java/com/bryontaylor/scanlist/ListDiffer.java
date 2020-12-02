package com.bryontaylor.scanlist;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import static android.content.ContentValues.TAG;

public class ListDiffer extends DiffUtil.Callback {

  private List<ListItem> oldList, newList;

  public ListDiffer(List<ListItem> oldList, List<ListItem> newList) {
    this.oldList = oldList;
    this.newList = newList;
    Log.i(TAG, "ListDiffer: oldList " + oldList + " newList " + newList);
  }

  @Override
  public int getOldListSize() {
    return oldList == null ? 0 : oldList.size();
  }

  @Override
  public int getNewListSize() {
    return newList == null ? 0 : newList.size();
  }

  @Override
  public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
    boolean areItemsSame = oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
    Log.i(TAG, "areItemsTheSame: " + areItemsSame);
//    return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
    return areItemsSame;
  }

  @Override // TODO: override equals method in POJO
  public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
    Log.i(TAG, "areContentsTheSame: oldItem " + oldList.get(oldItemPosition).getItemName() + " newItem " + newList.get(newItemPosition).getItemName());
    boolean areContentsSame = oldList.get(oldItemPosition).getItemName().equals(newList.get(newItemPosition)) &&
        oldList.get(oldItemPosition).getIsChecked() == newList.get(newItemPosition).getIsChecked();
    Log.i(TAG, "areContentsTheSame: " + areContentsSame);
    Log.i(TAG, "areContentsTheSame: oldItem " + oldList.get(oldItemPosition).getIsChecked() + " newItem " + newList.get(newItemPosition).getIsChecked());
//    return oldList.get(oldItemPosition).getItemName().equals(newList.get(newItemPosition)) &&
//        oldList.get(oldItemPosition).getIsChecked() == newList.get(newItemPosition).getIsChecked();
    return areContentsSame;
  }

  @Nullable
  @Override
  public Object getChangePayload(int oldItemPosition, int newItemPosition) {
    Log.i(TAG, "getChangePayload: called");
    ListItem oldListItem = oldList.get(oldItemPosition);
    ListItem newListItem = newList.get(newItemPosition);
    Log.i(TAG, "getChangePayload: oldListItem " + oldListItem.getItemName());
    Log.i(TAG, "getChangePayload: newListItem " + newListItem.getItemName());

    Bundle diffBundle = new Bundle();

    // if itemName doesn't match, put new name in bundle
    if(!oldListItem.getItemName().equals(newListItem.getItemName())) {
      diffBundle.putString("itemName", newListItem.getItemName());
      Log.i(TAG, "getChangePayload: itemNames are NOT equal " + oldListItem.getItemName() + " - " + newListItem.getItemName());
    }

    // if isChecked doesn't match, put the new isChecked value in bundle
    if(oldListItem.getIsChecked() != newListItem.getIsChecked()) {
      diffBundle.putBoolean("isChecked", newListItem.getIsChecked());
    }
    return diffBundle.size() == 0 ? null : diffBundle;
  }
}
