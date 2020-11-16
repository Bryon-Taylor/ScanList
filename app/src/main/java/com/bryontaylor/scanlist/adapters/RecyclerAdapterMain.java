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
import androidx.recyclerview.widget.RecyclerView;

import com.bryontaylor.scanlist.ListItem;
import com.bryontaylor.scanlist.R;

import java.util.List;

public class RecyclerAdapterMain extends RecyclerView.Adapter<RecyclerAdapterMain.ListItemHolder> {

  //private List<String> itemList;
  private List<ListItem> itemList;
  private CheckBoxListener checkBoxListener;

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

  @Override
  public void onBindViewHolder(@NonNull ListItemHolder holder, int position) {
    // set the holder's values
    ListItem item = itemList.get(position);
    holder.txtItemName.setText(item.getItemName());
    holder.checkBox.setChecked(item.getIsChecked());
    if(item.getIsChecked()) {
      holder.txtItemName.setTextColor(Color.LTGRAY);
    } else {
      holder.txtItemName.setTextColor(Color.BLACK);
    }
  }

  @Override
  public int getItemCount() {
    return itemList == null ? 0 : itemList.size();
  }

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
//          if(checkBox.isChecked()) {
//            txtItemName.setTextColor(Color.LTGRAY);
//          } else {
//            txtItemName.setTextColor(Color.BLACK);
//          }

          // TODO: update item in database from MAIN to persist checkbox state, maybe with interface callback in Main?
          Log.i("tag", "checkbox clicked: ");
          ListItem item = itemList.get(getAdapterPosition());
          String itemName = item.getItemName();
          Log.i("tag", itemName + "'s checkbox was clicked");
          checkBoxListener.onCheckBoxClicked(itemList.get(getAdapterPosition()));
        }
      });
    }
    public LinearLayout getForegroundLayout() {
      return foregroundLayout;
    }

    public RelativeLayout getBackgroundLayout() {
      return backgroundLayout;
    }
  }

  public void setItemList(List<ListItem> itemList) {
    this.itemList = itemList;
    notifyDataSetChanged();
  }

  // register MainActivity as a listener
  public void setCheckBoxListener(CheckBoxListener checkBoxListener) {
    this.checkBoxListener = checkBoxListener;
  }

  public ListItem getItemAt(int position) {
    return itemList.get(position);
  }
}


