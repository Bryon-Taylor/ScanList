package com.bryontaylor.scanlist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ScannedTextActivity extends AppCompatActivity implements View.OnClickListener {

  private static final String TAG = "tag";
  private ArrayList<String> scannedLines, addedItemsList;
  private RecyclerAdapterScannedText scannedTextAdapter;
  private RecyclerView recyclerView;
  private Button btnAddAll, btnDone;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_scanned_text);

    initComponents();
    getScannedLinesIntent();
    initRecyclerView();
    attachListeners();
    createItemTouchHelper();

  }

  private void initComponents() {
    setTitle("Add items to List");
    addedItemsList = new ArrayList<>();
    btnAddAll = findViewById(R.id.btn_add_all);
    btnDone = findViewById(R.id.btn_done);
  }

  // get the scanned text passed from MainActivity and set local ArrayList
  private void getScannedLinesIntent() {
    Intent i = getIntent();
    scannedLines = i.getStringArrayListExtra("scannedLines");
  }

  private void initRecyclerView() {
    recyclerView = findViewById(R.id.recycler_scanned_text);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    scannedTextAdapter = new RecyclerAdapterScannedText(scannedLines);
    recyclerView.setAdapter(scannedTextAdapter);
  }

  // listen to additions in the adapter
  private void attachListeners() {
    btnAddAll.setOnClickListener(this);
    btnDone.setOnClickListener(this);

    // callback from RecyclerAdapterScannedText
    scannedTextAdapter.setBtnAddListener(new RecyclerAdapterScannedText.OnAddBtnListener() {
      @Override
      public void addItemToList(String itemToAdd, int position) {
        addedItemsList.add(itemToAdd);
        scannedLines.remove(position);
        scannedTextAdapter.notifyItemRemoved(position);
        if(scannedLines.isEmpty()) {
          returnListAndFinish();
        }
      }
    });
  }


  @Override
  public void onClick(View view) {
    switch(view.getId()) {
      case R.id.btn_add_all:
        addAllItems();
        returnListAndFinish();
        break;
      case R.id.btn_done:
        returnListAndFinish();
        break;
    }
  }

  private void addAllItems() {
    List<String> allItems = scannedTextAdapter.getAllItems();
    addedItemsList.addAll(allItems);
//    for(int i = 0; i < scannedTextAdapter.getItemCount(); i++) {
////      RecyclerView.ViewHolder holder =
//
//      Log.i(TAG, "addAllItems: adapter item count " + scannedTextAdapter.getItemCount());
//      EditText editedItem = (recyclerView.findViewHolderForAdapterPosition(i).itemView.findViewById(R.id.edt_add_item_scanned));
//      String editItemName = String.valueOf(editedItem.getText());
//      addedItemsList.add(editItemName);
//    }

  }

  // return list back to MainActivity's onActivityResult method
  private void returnListAndFinish() {
    Intent i = new Intent();
    for(String line : addedItemsList) {
      Log.i(TAG, "returnListAndFinish: " + line);
    }
    i.putStringArrayListExtra("addedItemsList", addedItemsList);
    setResult(RESULT_OK, i);
    finish();
  }

  private void createItemTouchHelper() {
    new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
        0, ItemTouchHelper.START | ItemTouchHelper.END) {

      @Override
      public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        scannedLines.remove(position);
        scannedTextAdapter.notifyItemRemoved(position);

      }
      @Override
      public boolean onMove(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            @NonNull RecyclerView.ViewHolder target) {
        return false;
      }
    }).attachToRecyclerView(recyclerView);
  }
}