package com.bryontaylor.scanlist;

import android.content.res.ColorStateList;
import android.content.res.Resources;
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

import java.util.List;


// ListAdapter has the getItem(position) method that allows access to list items instead of
// having to keep a local ArrayList to access. i.e. myList.get(position)
public class RecyclerAdapterMain extends ListAdapter<ListItem, RecyclerAdapterMain.ListItemHolder> {
//    implements ItemTouchHelperAdapter {
//public class RecyclerAdapterMain extends RecyclerView.Adapter<RecyclerAdapterMain.ListItemHolder>
//    implements ItemTouchHelperAdapter {

  private static final String TAG = "RecyclerAdapterMain";
  // registers MainActivity as a listener to checkbox clicks. Main will update database accordingly.
  private CheckBoxListener checkBoxListener;

  public interface CheckBoxListener {
    void onCheckBoxClicked(ListItem item);
  }

  // register MainActivity as a listener
  public void setCheckBoxListener(CheckBoxListener checkBoxListener) {
    this.checkBoxListener = checkBoxListener;
  }

  public RecyclerAdapterMain() {
    super(DIFF_CALLBACK);
  }

    // Static keyword makes DIFF_CALLBACK variable available to the constructor when it is called
    // DiffUtil will compare two objects to determine if updates are needed
    private static final DiffUtil.ItemCallback<ListItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ListItem>() {
    @Override
    public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
      return oldItem.getId() == newItem.getId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
      return oldItem.equals(newItem);
    }
  };


  //private List<ListItem> currentList = new ArrayList<>();



  @NonNull
  @Override
  public ListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    // inflate the view's layout and instantiate a ViewHolder with it
    View itemView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.recycler_item_layout_main, parent, false);
    return new ListItemHolder(itemView);
  }

  @Override
  public void onBindViewHolder(@NonNull ListItemHolder holder, int position) {

    //Log.i("name", "onBindViewHolder: without payloads called");
    // set the holder views values
    //ListItem item = currentList.get(position);


    ListItem item = getItem(position);
    Resources resources = holder.itemView.getContext().getResources();
//    String itemName = item.getItemName();
    Log.i("tag", "onBindViewHolder: itemName " + item.getItemName());
    holder.txtItemName.setText(item.getItemName());
    holder.checkBox.setChecked(item.getIsChecked());
//    if(holder.checkBox.isChecked()) {
//      holder.checkBox.setButtonTintList(ColorStateList.valueOf(Resources.getSystem()
//          .getColor(R.color.checkedColor, null)));
//    }

    // set the item to "greyed out" if checkbox is checked
    if(item.getIsChecked()) {
      holder.txtItemName.setTextColor(Color.LTGRAY);
      holder.checkBox.setButtonTintList(ColorStateList
          .valueOf(resources.getColor(R.color.checkedColor, null)));
    } else {
      holder.txtItemName.setTextColor(Color.BLACK);
      holder.checkBox.setButtonTintList(ColorStateList
          .valueOf(resources.getColor(R.color.uncheckedColor, null)));
    }
  }
//
//  @Override
//  public long getItemId(int position) {
//    return getItemAt(position).getId();
//  }
//  @Override
//  public int getItemCount() {
//    return currentList == null ? 0 : currentList.size();
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
          //ListItem item = currentList.get(getAdapterPosition());
          ListItem item = getItem(getAdapterPosition());
          String itemName = item.getItemName();
          Log.i("tag", itemName + "'s checkbox was clicked " + checkBox.isChecked());
          //checkBoxListener.onCheckBoxClicked(currentList.get(getAdapterPosition()));
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


//  public void setItemList(List<ListItem> newItemList) {
//    if(currentList != null) {
//      for(ListItem listItem : currentList) {
//        Log.i(TAG, "setItemList1: itemName: " + listItem.getItemName());
//      }
//    }
//    //runListDiffer(currentList, newItemList);
//    ListDiffer listDiffer = new ListDiffer(currentList, newItemList);
//    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(listDiffer);
//    currentList.clear();
//    currentList.addAll(newItemList);
//    diffResult.dispatchUpdatesTo(this);
////    if(itemList != null) {
////      itemList.clear();
////      itemList.addAll(newItemList);
////    }
//
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

  public List<ListItem> getItemList() {
    //return currentList;
    return getCurrentList();
  }

  public ListItem getItemAt(int position) {
    //return currentList.get(position);
    return getItem(position);
  }

//  @Override
//  public void onItemMove(int fromPosition, int toPosition) { // TODO: try using local arraylist = getCurrentList()
////    List<ListItem> currentList = new ArrayList<>(getCurrentList());
////    ListItem fromItem = currentList.get(fromPosition);
////    currentList.remove(fromPosition);
////    currentList.add(toPosition, fromItem);
////    //notifyItemMoved(fromPosition, toPosition);
////    submitList(currentList);
//  }
//
//  @Override
//  public void onItemDelete(int position) {
//
//  }
//
//  @Override
//  public void onItemUpdate(int position) {
//
//  }


//  private void runListDiffer(List<ListItem> oldItemList, List<ListItem> newItemList) {
//    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ListDiffer(oldItemList, newItemList));
//    diffResult.dispatchUpdatesTo(this);
//  }
}


