package com.bryontaylor.scanlist;

import android.content.Intent;
import android.os.Bundle;
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

  private ArrayList<String> scannedLines, addedItemsList;
  private static final String SCANNED_LINES_KEY = "scannedLines";
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
    setTitle(getString(R.string.choose_scanned_items));
    addedItemsList = new ArrayList<>();
    btnAddAll = findViewById(R.id.btn_add_all);
    btnDone = findViewById(R.id.btn_done);
  }

  // Get the scanned text from image passed from MainActivity and set local ArrayList
  private void getScannedLinesIntent() {
    Intent i = getIntent();
    scannedLines = i.getStringArrayListExtra(SCANNED_LINES_KEY);
  }

  private void initRecyclerView() {
    recyclerView = findViewById(R.id.recycler_scanned_text);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    scannedTextAdapter = new RecyclerAdapterScannedText(scannedLines);
    recyclerView.setAdapter(scannedTextAdapter);
  }

  // Set button listeners
  private void attachListeners() {
    btnAddAll.setOnClickListener(this);
    btnDone.setOnClickListener(this);

    // Callback from RecyclerAdapterScannedText
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
  }

  // Return list back to MainActivity's onActivityResult method
  private void returnListAndFinish() {
    Intent i = new Intent();
    i.putStringArrayListExtra("addedItemsList", addedItemsList);
    setResult(RESULT_OK, i);
    finish();
  }

  // To support swipe to delete
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
        // Drag and drop not supported
        return false;
      }
    }).attachToRecyclerView(recyclerView);
  }
}