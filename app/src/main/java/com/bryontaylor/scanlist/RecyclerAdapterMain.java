package com.bryontaylor.scanlist;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerAdapterMain extends ListAdapter<ListItem, RecyclerAdapterMain.ListItemHolder> {

  // Registers MainActivity as a listener to checkbox clicks. Main will update database accordingly.
  private CheckBoxListener checkBoxListener;

  public interface CheckBoxListener {
    void onCheckBoxClicked(ListItem item); // Method implemented in MainActivity
  }

  public void setCheckBoxListener(CheckBoxListener checkBoxListener) {
    this.checkBoxListener = checkBoxListener;
  }

  public RecyclerAdapterMain() {
    super(DIFF_CALLBACK);
  }

    // Static keyword makes DIFF_CALLBACK variable available to the constructor when it is called
    // DiffUtil will compare two objects to determine if updates are needed
    private static final DiffUtil.ItemCallback<ListItem> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<ListItem>() {
    @Override
    public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
      return oldItem.getId() == newItem.getId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
      return oldItem.equals(newItem);
    }
  };

  @NonNull
  @Override
  public ListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.recycler_item_layout_main, parent, false);
    return new ListItemHolder(itemView);
  }

  @Override
  public void onBindViewHolder(@NonNull ListItemHolder holder, int position) {
    ListItem item = getItem(position);
    Resources resources = holder.itemView.getContext().getResources();
    holder.txtItemName.setText(item.getItemName());
    holder.checkBox.setChecked(item.getIsChecked());

    // Set the item to "greyed out" if checkbox is checked, normal color otherwise
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

  public class ListItemHolder extends RecyclerView.ViewHolder {
    private TextView txtItemName;
    private CheckBox checkBox;

    public ListItemHolder(@NonNull View itemView) {
      super(itemView);
      txtItemName = itemView.findViewById(R.id.txt_item_name);

      // Toggle checkbox state
      checkBox = itemView.findViewById(R.id.checkBox);
      checkBox.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          checkBoxListener.onCheckBoxClicked(getItem(getAdapterPosition()));
        }
      });
    }
  }

  public ListItem getItemAt(int position) {
    return getItem(position);
  }
}


