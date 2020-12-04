package com.bryontaylor.scanlist.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bryontaylor.scanlist.ListDiffer;
import com.bryontaylor.scanlist.ListItem;
import com.bryontaylor.scanlist.R;

import java.util.List;


// ListAdapter has the getItem(int position) method that allows access to list items instead of
// having to keep a local ArrayList to access. i.e. myList.get(int position)
public class RecyclerAdapterMain extends ListAdapter<ListItem, RecyclerAdapterMain.ListItemHolder> {
//public class RecyclerAdapterMain extends RecyclerView.Adapter<RecyclerAdapterMain.ListItemHolder> {

  public RecyclerAdapterMain() {
    super(DIFF_CALLBACK);
  }

   //static keyword makes DIFF_CALLBACK variable available to the constructor when it is called
    private static final DiffUtil.ItemCallback<ListItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ListItem>() {
    @Override
    public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
      return oldItem.getId() == newItem.getId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
      //Log.i(TAG, "areContentsTheSame: old item checked " + oldItem.getItemName() + " " + oldItem.getIsChecked() + " new item checked " + oldItem.getItemName() + " " + newItem.getIsChecked() );
//      return oldItem.getItemName().equals(newItem.getItemName()) &&
//          oldItem.getIsChecked() == newItem.getIsChecked();  // TODO override compareTo/equals method in POJO
      return oldItem.equals(newItem);
    }
//
//    // method gets called if areItemsTheSame returns true and areContentsTheSame returns false
////    @Nullable
////    @Override
////    public Object getChangePayload(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
////      Bundle diffBundle = new Bundle();
////      if(!oldItem.getItemName().equals(newItem.getItemName())) {
////        diffBundle.putString("itemName", newItem.getItemName());
////      }
////      if(!oldItem.getIsChecked() != newItem.getIsChecked()) {
////        diffBundle.putBoolean("isChecked", newItem.getIsChecked());
////      }
////      if(diffBundle.size() == 0) {
////        Log.i(TAG, "diffBundleSize " + diffBundle.size());
////        return null;
////      }
////      return diffBundle;
////    }
  };


  //private List<String> itemList;
  //private List<ListItem> itemList;

  // registers MainActivity as a listener to checkbox clicks. Main will update database accordingly.
  private CheckBoxListener checkBoxListener;




//  public static final DiffUtil.ItemCallback<ListItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ListItem>() {
//    @Override
//    public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
//      return oldItem.getId() == newItem.getId();
//    }
//
//    @Override
//    public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
//      return oldItem.getItemName().equals(newItem.getItemName()) &&
//          oldItem.getIsChecked() == newItem.getIsChecked();
//    }
//  };


  public interface CheckBoxListener {
    void onCheckBoxClicked(ListItem item);
  }

  @NonNull
  @Override
  public ListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    // inflate the view's layout and instantiate a ViewHolder with it
    View itemView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.recycler_item_layout_main, parent, false);
    return new ListItemHolder(itemView);
  }

//  @Override
//  public void onBindViewHolder(@NonNull ListItemHolder holder, int position, @NonNull List<Object> payloads) {
//    //Log.i(TAG, "onBindViewHolder with payload called : " + payloads.size());
//    if(payloads.isEmpty()) {
//      super.onBindViewHolder(holder, position, payloads);
//
//    } else {
//      Log.i(TAG, "payloads size is: " + payloads.size());
//
//      Bundle diffBundle = (Bundle) payloads.get(0);
//      String itemName = diffBundle.getString("itemName");
//      Log.i(TAG, "itemName in payload is: " + itemName);
//      boolean isChecked = diffBundle.getBoolean("isChecked");
//      holder.txtItemName.setText(itemName);
//      holder.checkBox.setChecked(isChecked);
//      //notifyItemChanged(position);
//
//    }

//  }

  @Override
  public void onBindViewHolder(@NonNull ListItemHolder holder, int position) {

    //Log.i(TAG, "onBindViewHolder: without payloads called");
    // set the holder views values
    //ListItem item = itemList.get(position);

    ListItem item = getItem(position);
//    String itemName = item.getItemName();
//    Log.i(TAG, "onBindViewHolder: itemName " + item.getItemName());
    holder.txtItemName.setText(item.getItemName());
    holder.checkBox.setChecked(item.getIsChecked());

    // set the item to "greyed out" if checkbox is checked, indicates item has been retrieved
    if(item.getIsChecked()) {
      holder.txtItemName.setTextColor(Color.LTGRAY);
    } else {
      holder.txtItemName.setTextColor(Color.BLACK);
    }
  }

//  @Override
//  public int getItemCount() {
//    return itemList == null ? 0 : itemList.size();
//  }

  public class ListItemHolder extends RecyclerView.ViewHolder {

    private TextView txtItemName;
    private LinearLayout foregroundLayout;
    private RelativeLayout backgroundLayout;
    private CheckBox checkBox;

    public ListItemHolder(@NonNull View itemView) {
      super(itemView);
      txtItemName = itemView.findViewById(R.id.txt_item_name);
      foregroundLayout = itemView.findViewById(R.id.foreground_layout);
      backgroundLayout = itemView.findViewById(R.id.background_layout);

      // toggle checkbox state
      checkBox = itemView.findViewById(R.id.checkBox);
      checkBox.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {

          Log.i("tag", "checkbox clicked: ");
          //ListItem item = itemList.get(getAdapterPosition());
          ListItem item = getItem(getAdapterPosition());
          String itemName = item.getItemName();
          Log.i("tag", itemName + "'s checkbox was clicked");
//          checkBoxListener.onCheckBoxClicked(itemList.get(getAdapterPosition()));
          checkBoxListener.onCheckBoxClicked(getItem(getAdapterPosition()));
        }
      });
    }
    public LinearLayout getForegroundLayout() { // TODO: not used?
      return foregroundLayout;
    }

    public RelativeLayout getBackgroundLayout() { // TODO: not used?
      return backgroundLayout;
    }
  }


  //  public void setItemList(List<ListItem> oldItemList, List<ListItem> newItemList) {
//    if(oldItemList != null) {
//      for(ListItem listItem : oldItemList) {
//        Log.i(TAG, "setItemList1: itemName: " + listItem.getItemName());
//      }
//    }
//    runListDiffer(oldItemList, newItemList);
//    ListDiffer listDiffer = new ListDiffer(oldItemList, newItemList);
//    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(listDiffer);
//    diffResult.dispatchUpdatesTo(this);
////    if(itemList != null) {
////      itemList.clear();
////      itemList.addAll(newItemList);
////    }
//    itemList = newItemList;
////    for(ListItem listItem : itemList) {
////      Log.i(TAG, "setItemList2: itemName: " + listItem.getItemName());
////    }
////
////    List<ListItem> oldItemList = new ArrayList<>();
////
////    //List<ListItem> oldItemList = itemList;
////    if(itemList != null) {
////
////      oldItemList.addAll(itemList);
////      for(ListItem listItem : itemList) {
////        Log.i(TAG, "setItemList: itemName: " + listItem.getItemName());
////      }
////    }
//
//    //itemList = newItemList;
//
//    //notifyDataSetChanged();
//  }

//  public List<ListItem> getItemList() {
//    return itemList;
//  }

  // register MainActivity as a listener
  public void setCheckBoxListener(CheckBoxListener checkBoxListener) {
    this.checkBoxListener = checkBoxListener;
  }

  public ListItem getItemAt(int position) {
    //return itemList.get(position);
    return getItem(position);
  }

  private void runListDiffer(List<ListItem> oldItemList, List<ListItem> newItemList) {
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ListDiffer(oldItemList, newItemList));
    diffResult.dispatchUpdatesTo(this);
  }



}


