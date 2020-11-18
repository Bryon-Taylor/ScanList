package com.bryontaylor.scanlist;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bryontaylor.scanlist.adapters.RecyclerAdapterMain;
import com.bryontaylor.scanlist.architecture_components.ListItemViewModel;
import com.bryontaylor.scanlist.util.VerticalSpaceDecor;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  private ImageView imgAddItem;
  private EditText edtAddItem;
  private RecyclerView recyclerMain;
  private RecyclerAdapterMain adapterMain;
  private ListItemViewModel viewModel;
  private ConstraintLayout constraintLayout;
  private static final String TAG = "MainActivity";

  // for image capture from camera
  private Uri imageUri;
  private final String AUTHORITY = "com.bryontaylor.scanlist.provider";
  private final int REQUEST_CODE_TAKE_PHOTO = 1000;
  private final int REQUEST_CODE_SCANNED_LINES = 2000;
  private Bitmap bitmap;

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
        for (ListItem item : listItems) {
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
    switch (v.getId()) {
      case R.id.img_add_item_main:
        String itemName = String.valueOf(edtAddItem.getText());
        if (!itemName.trim().equals("")) { // if EditText is not empty
          ListItem item = new ListItem();
          item.setItemName(itemName);
          viewModel.insert(item);
        }
        edtAddItem.setText(""); // clear the EditText field
    }
  }

  // ItemTouchHelper class handles swipes on the recycler view
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
          showUndoSnackBar(deletedItem);
        } else {

          // edit item on left swipe
          showEditItemAlert(adapterMain.getItemAt(position));

          // restores the foreground layer after swiping left to edit and cancelling
          adapterMain.notifyItemChanged(position);
        }
      }

      @Override
      public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {

          // change the item view's color to indicate it is being swiped
          if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            viewHolder.itemView.findViewById(R.id.foreground_layout)
                .setBackgroundColor(getResources()
                    .getColor(R.color.swipeColor, null));
          }
        }
      }

      @Override
      public void clearView(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder) {

        // bring foreground layer back on top when delete operation is undone
        final View foregroundLayout = ((RecyclerAdapterMain.ListItemHolder) viewHolder)
            .getForegroundLayout();
        getDefaultUIUtil().clearView(foregroundLayout);

        // change back to original white color when swipe is released
        viewHolder.itemView.findViewById(R.id.foreground_layout)
            .setBackgroundColor(getResources().getColor(R.color.swipeReleased, null));
      }

      @Override
      public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                              int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

          // use getDefaultUIUtil to have background stay stationary while foreground is swiped
          final View foregroundLayout = ((RecyclerAdapterMain.ListItemHolder) viewHolder)
              .getForegroundLayout();
          getDefaultUIUtil().onDraw(canvas, recyclerView, foregroundLayout, dX, dY,
              actionState, isCurrentlyActive);

          if (dX > 0) { // if swiped right to delete
            // change background to red
            viewHolder.itemView.findViewById(R.id.background_layout)
                .setBackgroundColor(getResources()
                    .getColor(R.color.itemviewBackgroundColorDelete, null));

            // hide edit icon and edit text after 3/4 swipe to delete
            if (dX > 0.75) {
              viewHolder.itemView.findViewById(R.id.img_edit_icon).setVisibility(View.GONE);
              viewHolder.itemView.findViewById(R.id.txt_edit).setVisibility(View.GONE);
            }

            // make delete icon and text visible if they were set to View.GONE
            viewHolder.itemView.findViewById(R.id.txt_delete).setVisibility(View.VISIBLE);
            viewHolder.itemView.findViewById(R.id.img_delete_icon).setVisibility(View.VISIBLE);

          } else if (dX < 0) { // user swiped left to edit

            // change background color to orange
            viewHolder.itemView.findViewById(R.id.background_layout)
                .setBackgroundColor(getResources()
                    .getColor(R.color.itemViewBackgroundColorEdit, null));

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
      public boolean onMove(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            @NonNull RecyclerView.ViewHolder target) {
        // drag and drop not used
        return false;
      }
    }).attachToRecyclerView(recyclerMain);
  }

  // to undo a delete operation
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

  // edit a swiped list item
  private void showEditItemAlert(ListItem listItem) {

    // set the EditText with the previous value and highlight all
    EditText edtItemName = new EditText(this);
    edtItemName.setPadding(30, 100, 0, 30);
    edtItemName.setText(listItem.getItemName());
    edtItemName.setSelectAllOnFocus(true);
    edtItemName.requestFocus();

    // create dialog to edit the item
    AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
        .setTitle("Edit item").setView(edtItemName)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

        // update list with edited value
        String editedItemName = String.valueOf(edtItemName.getText());
        listItem.setItemName(editedItemName);
        viewModel.update(listItem);
      }
    }).setNegativeButton("Cancel", null).create();

    // display keyboard
    alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    alert.show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    switch(item.getItemId()) {
      case R.id.icon_launch_camera:
        launchCamera();
        break;
      case R.id.icon_delete_all:
        viewModel.deleteAllItems();
        break;
      case R.id.icon_delete_checked:
        viewModel.deleteCheckedItems();
        break;
    }
    return true;
  }

  // launches the phone's camera and stores the image in a temporary file in the phone's cache
  public void launchCamera() {
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    File photoFile = null;

    // create a temp file in the phone's cache to store the image
    try {
      File tempDir = this.getCacheDir();
      photoFile = File.createTempFile("jpeg_", ".jpg", tempDir);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Uri will locate image file
    if(Build.VERSION.SDK_INT > 24) {
      imageUri = FileProvider.getUriForFile(this, AUTHORITY, photoFile);
    } else {
      imageUri = Uri.fromFile(photoFile); // for Android version < 24
    }

    // for full size image, thumbnails have poor resolution for the text OCR to be accurate
    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    // Ensure that there's a camera activity to handle the intent
    if (intent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
    }
    photoFile.deleteOnExit(); // delete temp file
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent); // necessary?

    // required to retrieve image from MediaStore
    ContentResolver contentResolver = this.getContentResolver();

    // retrieve the bitmap returned by the photo app
    if(requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
      try {
        bitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
        int width = bitmap.getWidth(); // debugging purposes
        int height = bitmap.getHeight();
        launchCropImageActivity();
      } catch (IOException e) {
        e.printStackTrace();
      }

      // get the cropped image
    } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
      CropImage.ActivityResult result = CropImage.getActivityResult(intent);
      Uri resultUri = result.getUri();
      try {
        bitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, resultUri);

        // detect text from the cropped image
        detectTextFromImage();
      } catch (IOException e) {
        e.printStackTrace();
      }

      // get the list of added items back from ScannedTextActivity
    } else if (requestCode == REQUEST_CODE_SCANNED_LINES && resultCode == RESULT_OK) {
      ArrayList<String> resultsList = intent.getStringArrayListExtra("addedItemsList");
      for(String itemName : resultsList) {
        ListItem newItem = new ListItem();
        newItem.setItemName(itemName);
        viewModel.insert(newItem);
      }
    }
  }

  // launch activity to crop image
  // CREDIT: https://github.com/ArthurHub/Android-Image-Cropper library used
  private void launchCropImageActivity() {
    CropImage.activity(imageUri)
        .setAllowRotation(false)
        .setAllowFlipping(false)
        .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
        .setOutputCompressQuality(100)
        .start(this);
  }

  // detect text to add to list
  private void detectTextFromImage() {
    Log.d(TAG, "detectTextFromImage has been called ");
    ArrayList<String> scannedLines = new ArrayList<>();

    // Firebase ML Kit to detect text from images
    FirebaseVisionImage fbVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
    FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
        .getOnDeviceTextRecognizer();
    Task<FirebaseVisionText> detectTextTask = textRecognizer.processImage(fbVisionImage);
    detectTextTask.addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
      @Override
      public void onSuccess(FirebaseVisionText firebaseVisionText) {

        Log.d(TAG, "onSuccess: has been called ");

        // text that has enough white space separating them will be captured in different TextBlocks
        // loop through each block to retrieve all lines
        List<FirebaseVisionText.TextBlock> textBlocks = firebaseVisionText.getTextBlocks();
        for(FirebaseVisionText.TextBlock block : textBlocks) {

          // text in close proximity will be stored in individual Lines in a list
          // lines are stored in blocks
          List<FirebaseVisionText.Line> lines = block.getLines();
          for(FirebaseVisionText.Line line : lines) {
            scannedLines.add(line.getText());
          }
        }
        Log.i(TAG, "scannedLines: " + scannedLines);
        // pass the new list to ScannedTextActivity to display in a recycler view
        Intent i = new Intent(MainActivity.this, ScannedTextActivity.class);
        i.putStringArrayListExtra("scannedLines", scannedLines);
        startActivityForResult(i, REQUEST_CODE_SCANNED_LINES);
      }
    }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        e.printStackTrace();
      }
    });
  }
}