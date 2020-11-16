package com.bryontaylor.scanlist;

import android.content.DialogInterface;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bryontaylor.scanlist.adapters.RecyclerAdapterMain;
import com.bryontaylor.scanlist.architecture_components.ListItemViewModel;
import com.bryontaylor.scanlist.util.VerticalSpaceDecor;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  private ImageView imgAddItem;
  private EditText edtAddItem;
  private RecyclerView recyclerMain;
  private RecyclerAdapterMain adapterMain;
  private ListItemViewModel viewModel;
  private ConstraintLayout constraintLayout;
  private static final String TAG = "MainActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initComponents();
    initRecyclerView();
    setListeners();
    setListObserver();
    createItemTouchHelper();
  }

  private void setListObserver() {
    viewModel.getAllItems().observe(this, new Observer<List<ListItem>>() {
      @Override
      public void onChanged(List<ListItem> listItems) {
        adapterMain.setItemList(listItems);
        Log.i(TAG, "onChanged: called");
        for(ListItem item : listItems) {
          Log.i(TAG, "item's isChecked value for : " + item.getItemName() + " " + item.getIsChecked());
        }
      }
    });
  }

  private void initComponents() {
    imgAddItem = findViewById(R.id.img_add_item_main);
    edtAddItem = findViewById(R.id.edt_add_item_main);
    viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
            .create(ListItemViewModel.class);
    constraintLayout = findViewById(R.id.constraint_layout);
  }

  private void initRecyclerView() {
    recyclerMain = findViewById(R.id.recycler_main);
    recyclerMain.setLayoutManager(new LinearLayoutManager(this));
    RecyclerView.ItemDecoration vertSpace = new VerticalSpaceDecor(2);
    recyclerMain.addItemDecoration(vertSpace);
    adapterMain = new RecyclerAdapterMain();
    recyclerMain.setAdapter(adapterMain);
  }

  private void setListeners() {
    imgAddItem.setOnClickListener(this);
    adapterMain.setCheckBoxListener(new RecyclerAdapterMain.CheckBoxListener() {
      @Override
      public void onCheckBoxClicked(ListItem item) {
        item.setChecked(!item.getIsChecked()); // toggle isChecked value
        viewModel.update(item);
        Log.i(TAG, "onCheckBoxClicked: called update(item)");
      }
    });
  }

  @Override
  public void onClick(View v) {
    switch(v.getId()) {
      case R.id.img_add_item_main:
        String itemName = String.valueOf(edtAddItem.getText());
        if(!itemName.trim().equals("")) { // if EditText is not empty
          ListItem item = new ListItem();
          item.setItemName(itemName);
          viewModel.insert(item);
        }
        edtAddItem.setText(""); // clear the EditText field
    }
  }

  private void createItemTouchHelper() {

    new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
            // support left and right swipes
            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

      @Override
      public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        int position = viewHolder.getAdapterPosition();

        // delete item on right swipe
        if (direction == ItemTouchHelper.RIGHT) {

          ListItem deletedItem = adapterMain.getItemAt(position);
          viewModel.delete(deletedItem);
//          mainList.remove(position);
//          listHolder.setItemList(mainList);
//          viewModel.updateListHolder(listHolder);
          showUndoSnackBar(deletedItem);
        } else {

          // edit item on left swipe
          //String itemToEdit = mainList.get(position);
          showEditItemAlert(adapterMain.getItemAt(position));

          // this restores the view holder's initial state after swiping left to edit
          adapterMain.notifyItemChanged(position);
        }
      }

      @Override
      public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {

          // change the item view's color to indicate it is being swiped
          if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            viewHolder.itemView.findViewById(R.id.foreground_layout).setBackgroundColor(getResources().getColor(R.color.swipeColor, null));
          }
        }
      }

      @Override
      public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

        // bring foreground layer back on top when delete operation is undone
        final View foregroundLayout = ((RecyclerAdapterMain.ListItemHolder) viewHolder).getForegroundLayout();
        getDefaultUIUtil().clearView(foregroundLayout);
        //makeToast("clearView called");

        // change back to original white color when swipe is released
        viewHolder.itemView.findViewById(R.id.foreground_layout).setBackgroundColor(getResources().getColor(R.color.swipeReleased, null));
      }

      @Override
      public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        //super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

          // use getDefaultUIUtil to have background stay stationary while foreground is swiped
          final View foregroundLayout = ((RecyclerAdapterMain.ListItemHolder) viewHolder).getForegroundLayout();
          getDefaultUIUtil().onDraw(canvas, recyclerView, foregroundLayout, dX, dY, actionState, isCurrentlyActive);

          if (dX > 0) { // if swiped right to delete
            // change background to red
            viewHolder.itemView.findViewById(R.id.background_layout).setBackgroundColor(getResources().getColor(R.color.itemviewBackgroundColorDelete, null));

            // hide edit icon and text after 3/4 swipe to delete
            if (dX > 0.75) {
              viewHolder.itemView.findViewById(R.id.img_edit_icon).setVisibility(View.GONE);
              viewHolder.itemView.findViewById(R.id.txt_edit).setVisibility(View.GONE);
            }

            // make delete icon and text visible if they were set to View.GONE
            viewHolder.itemView.findViewById(R.id.txt_delete).setVisibility(View.VISIBLE);
            viewHolder.itemView.findViewById(R.id.img_delete_icon).setVisibility(View.VISIBLE);

          } else if (dX < 0) { // user swiped left to edit

            // change background color to orange
            viewHolder.itemView.findViewById(R.id.background_layout).setBackgroundColor(getResources().getColor(R.color.itemViewBackgroundColorEdit, null));

            // hide delete icon and text after 3/4 swipe to edit
            if (dX < -0.75) {
              viewHolder.itemView.findViewById(R.id.img_delete_icon).setVisibility(View.GONE);
              viewHolder.itemView.findViewById(R.id.txt_delete).setVisibility(View.GONE);
            }

            // set edit icon and text visible if they were set to View.GONE
            viewHolder.itemView.findViewById(R.id.txt_edit).setVisibility(View.VISIBLE);
            viewHolder.itemView.findViewById(R.id.img_edit_icon).setVisibility(View.VISIBLE);
          }
        }
      }

      @Override
      public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        // unused
        return false;
      }
    }).attachToRecyclerView(recyclerMain);
  }

  public void showUndoSnackBar(ListItem deletedItem) {
    Snackbar undoSnackBar = Snackbar.make(constraintLayout, "Undo deleted Item",
            Snackbar.LENGTH_LONG).setAction("UNDO", new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // restore deleted item to its original position in the list
        viewModel.insert(deletedItem);
      }
    });
    undoSnackBar.show();
  }

  private void showEditItemAlert(ListItem listItem) {

    // set the EditText with previous value
    EditText edtItemName = new EditText(this);
    edtItemName.setText(listItem.getItemName());
    AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
            .setTitle("Edit item")
            .setView(edtItemName)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                // update list with edited value
                String editedItemName = String.valueOf(edtItemName.getText());
                listItem.setItemName(editedItemName);
                viewModel.update(listItem);
              }
            })
            .setNegativeButton("Cancel", null)
            .create();
    alert.show();
  }

}