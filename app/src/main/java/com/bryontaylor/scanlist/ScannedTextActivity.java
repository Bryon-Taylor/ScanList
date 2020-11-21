package com.bryontaylor.scanlist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ScannedTextActivity extends AppCompatActivity implements View.OnClickListener {

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
    // recyclerView.setHasFixedSize(true);
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
        scannedTextAdapter.addAllItems();
        returnListAndFinish();

      case R.id.btn_done:
        returnListAndFinish();
    }
  }

  // return list back to MainActivity's onActivityResult method
  private void returnListAndFinish() {
    Intent i = new Intent();
    i.putStringArrayListExtra("addedItemsList", addedItemsList);
    setResult(RESULT_OK, i);
    finish();
  }
}