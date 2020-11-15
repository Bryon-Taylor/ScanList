package com.bryontaylor.scanlist;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bryontaylor.scanlist.adapters.RecyclerAdapterMain;
import com.bryontaylor.scanlist.architecture_components.ListItemViewModel;
import com.bryontaylor.scanlist.util.VerticalSpaceDecor;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  private ImageView imgAddItem;
  private EditText edtAddItem;
  private List<ListItem> mainList;
  private RecyclerView recyclerMain;
  private RecyclerAdapterMain adapterMain;
  private ListItemViewModel viewModel;
  private ConstraintLayout constraintLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initComponents();
    initRecyclerView();
    setListeners();
    setListObserver();
    adapterMain.setItemList(mainList); // so app doesn't crash on adapter.getItemCount();
  }

  private void setListObserver() {
    viewModel.getAllItems().observe(this, new Observer<List<ListItem>>() {
      @Override
      public void onChanged(List<ListItem> listItems) {
        adapterMain.setItemList(listItems);
      }
    });
  }

  private void initComponents() {
    imgAddItem = findViewById(R.id.img_add_item_main);
    edtAddItem = findViewById(R.id.edt_add_item_main);
    mainList = new ArrayList<>();
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

    // to respond to checkBox being clicked
//    adapterMain.setCheckBoxListener(new RecyclerAdapterMain.CheckBoxListener() {
//      @Override
//      public void onCheckBoxClicked() {
//        Log.i(TAG, "onCheckBoxClicked: ");
//      }
//    });
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
    }
  }
}