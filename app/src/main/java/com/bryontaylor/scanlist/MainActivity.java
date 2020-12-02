package com.bryontaylor.scanlist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  private ImageView imgAddItem;
  private EditText edtAddItem;
  private RecyclerView recyclerMain;
  private RecyclerAdapterMain adapterMain;
  private ListItemViewModel viewModel;
  private ConstraintLayout constraintLayout;
  private Toolbar toolbar;
  private static final String TAG = "MainActivity";
  private List<ListItem> oldList;

  // for image capture from camera
  private Uri imageUri;
  private final String AUTHORITY = "com.bryontaylor.scanlist.provider";
  private final int REQUEST_CODE_TAKE_PHOTO = 1000;
  private final int REQUEST_CODE_SCANNED_LINES = 2000;
  private Bitmap bitmap;

  // for voice recognition
  private static final int REQUEST_CODE_VOICE_RECOGNITION = 3000;
  private boolean permissionGranted;
  private ImageView imgVoiceRecognizer;
  private SpeechRecognizer speechRecognizer;

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
      public void onChanged(List<ListItem> newList) {

        adapterMain.submitList(newList);
//        oldList = adapterMain.getItemList();
//        if(oldList != null) {
//          for(ListItem listItem : oldList) {
//            Log.i(TAG, "onChanged oldList listItemName: " + listItem.getItemName());
//          }
//        }
//
//        for(ListItem listItem : newList) {
//          Log.i(TAG, "onChanged newList listItemName: " + listItem.getItemName());
//        }
//        adapterMain.setItemList(oldList, newList);
//        oldList = new ArrayList<>(newList);   // ....................   oldList already updated to newList
        //adapterMain.submitList(newList);
//        List<ListItem> oldList = adapterMain.getCurrentList();
//        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ListDiffer(oldList, listItems));
//        diffResult.dispatchUpdatesTo(adapterMain);



      }
    });
  }

  private void initComponents() {
    imgAddItem = findViewById(R.id.img_add_item_main);
    edtAddItem = findViewById(R.id.edt_add_item_main);
    viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ListItemViewModel.class);
    constraintLayout = findViewById(R.id.constraint_layout);
    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
    oldList = new ArrayList<>();  // testing purposes
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
        ListItem newListItem = new ListItem();
        newListItem.setId(item.getId());
        newListItem.setItemName(item.getItemName());
        newListItem.setChecked(!item.getIsChecked()); // toggle isChecked value
        viewModel.update(newListItem);
        Log.i(TAG, "onCheckBoxClicked: called update " + item.getItemName());
      }
    });
  }

  @Override // TODO: use if statement instead of switch
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.img_add_item_main:
        String itemName = String.valueOf(edtAddItem.getText());
        if (!itemName.trim().equals("")) { // if EditText is not empty
          ListItem item = new ListItem();
          item.setItemName(itemName);
          viewModel.insert(item);
        }

        // clear EditText and resent hint
        edtAddItem.setText("");
        edtAddItem.setHint("Add item");
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

          // edit item on left swipe //
          showEditItemAlert(viewHolder, position);

          // restores the foreground layer after swiping left to edit and clicking cancel button ------ TODO: get foreground instead
          //adapterMain.notifyItemChanged(position);

        }
      }

      @Override
      public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {

          // change the item view's color to indicate it is being swiped
//          if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
//            viewHolder.itemView.findViewById(R.id.foreground_layout).setBackgroundColor(getResources().getColor(R.color.swipeColor, null));
//            // TODO: change text color
//          }
        }
      }

      @Override
      public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

        // bring foreground layer back on top when delete operation is undone
        final View foregroundLayout = ((RecyclerAdapterMain.ListItemHolder) viewHolder).getForegroundLayout();
        getDefaultUIUtil().clearView(foregroundLayout);

        // change back to original white color when swipe is released
        //viewHolder.itemView.findViewById(R.id.foreground_layout).setBackgroundColor(getResources().getColor(R.color.swipeReleased, null));
      }

      @Override
      public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

          // use getDefaultUIUtil to have background stay stationary while foreground is swiped
          final View foregroundLayout = ((RecyclerAdapterMain.ListItemHolder) viewHolder).getForegroundLayout();
          getDefaultUIUtil().onDraw(canvas, recyclerView, foregroundLayout, dX, dY, actionState, isCurrentlyActive);

          if (dX > 0) { // if swiped right to delete
            // change background to red
            ((RecyclerAdapterMain.ListItemHolder) viewHolder).getBackgroundLayout().setBackgroundColor(getResources().getColor(R.color.itemviewBackgroundColorDelete, null));

            // hide edit icon and edit text after 3/4 swipe to delete
            if (dX > 1) {
              viewHolder.itemView.findViewById(R.id.img_edit_icon).setVisibility(View.GONE);
              viewHolder.itemView.findViewById(R.id.txt_edit).setVisibility(View.GONE);
            }

            // make delete icon and text visible if they were set to View.GONE
            viewHolder.itemView.findViewById(R.id.txt_delete).setVisibility(View.VISIBLE);
            viewHolder.itemView.findViewById(R.id.img_delete_icon).setVisibility(View.VISIBLE);

          } else if (dX < 0) { // user swiped left to edit

            // change background color to orange
            ((RecyclerAdapterMain.ListItemHolder) viewHolder).getBackgroundLayout().setBackgroundColor(getResources().getColor(R.color.itemViewBackgroundColorEdit, null));

            // hide delete icon and text after 3/4 swipe to edit
            if (dX < -1) {
              viewHolder.itemView.findViewById(R.id.img_delete_icon).setVisibility(View.GONE);
              viewHolder.itemView.findViewById(R.id.txt_delete).setVisibility(View.GONE);
            }

            // set edit icon and text visible if they were set to View.GONE
            viewHolder.itemView.findViewById(R.id.txt_edit).setVisibility(View.VISIBLE);
            viewHolder.itemView.findViewById(R.id.img_edit_icon).setVisibility(View.VISIBLE);

            //Log.i(TAG, "dX value: " + dX);
           //clearView(recyclerMain, viewHolder);

            if(dX < -1400) {
              //Log.i(TAG, "dX value: " + dX);
              // cancel swipe
//              final View backgroundLayout = ((RecyclerAdapterMain.ListItemHolder) viewHolder).getBackgroundLayout();
//              float backgroundTranslation = backgroundLayout.getTranslationX();
//              Log.i(TAG, "background translation: " + backgroundTranslation);
            }
            //getDefaultUIUtil().clearView(foregroundLayout);
          }
        }
      }

      @Override
      public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        // drag and drop not used
        return false;
      }
    }).attachToRecyclerView(recyclerMain);
  }

  // to undo a delete operation
  public void showUndoSnackBar(ListItem deletedItem) {
    Snackbar undoSnackBar = Snackbar.make(constraintLayout, "Undo deleted Item", Snackbar.LENGTH_LONG).setAction("UNDO", new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // restore deleted item to its original position in the list
        viewModel.insert(deletedItem);
      }
    });
    undoSnackBar.show();
  }

  // edit a swiped list item
  private void showEditItemAlert(RecyclerView.ViewHolder viewHolder, int position) {
    //List<ListItem> oldList = adapterMain.getCurrentList();
    ListItem listItem = adapterMain.getItemAt(position);
    ListItem newListItem = new ListItem();
    newListItem.setId(listItem.getId());

    // set the EditText with the previous value and highlight all
    EditText edtItemName = new EditText(this);
    edtItemName.setPadding(30, 100, 0, 30);
    edtItemName.setText(listItem.getItemName());
    edtItemName.setSelectAllOnFocus(true);
    edtItemName.requestFocus();

    // create dialog to edit the item
    AlertDialog alert = new AlertDialog.Builder(MainActivity.this).setTitle("Edit item")
        .setView(edtItemName).setPositiveButton("OK", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

        // update list with edited value
        String editedItemName = String.valueOf(edtItemName.getText());
        newListItem.setItemName(editedItemName);
        viewModel.update(newListItem);

//        List<ListItem> itemList = new ArrayList<>(adapterMain.getCurrentList());
//        ListItem item = itemList.get(position);
//        item.setItemName(editedItemName);
//        adapterMain.submitList(itemList);
        //adapterMain.notifyItemChanged(position);
      }
    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        // this restores swiped away foreground layout
        adapterMain.notifyItemChanged(position);
      }
    }).create();
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
    switch (item.getItemId()) {

      case R.id.icon_delete_all:
        viewModel.deleteAllItems();
        break;
      case R.id.icon_delete_checked:
        viewModel.deleteCheckedItems();
        break;
      case R.id.icon_share_list:
        try {
          showShareIntent();
        } catch (ExecutionException|InterruptedException e) {
          e.printStackTrace();
        }
        break;
      case R.id.icon_launch_camera:
        launchCamera();
        break;
      case R.id.icon_voice_recognition:
        Log.i(TAG, "onOptionsItemSelected: startSpeechRecognizer1");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        requestPermission();
      } else {
        Log.i(TAG, "onOptionsItemSelected: startSpeechRecognizer2");
        startSpeechRecognizer();
      }
        break;
    } return super.onOptionsItemSelected(item);
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
    if (Build.VERSION.SDK_INT > 24) {
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
    if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
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

      // get the list of added items back from ScannedTextActivity and insert into database
    } else if (requestCode == REQUEST_CODE_SCANNED_LINES && resultCode == RESULT_OK) {
      ArrayList<String> resultsList = intent.getStringArrayListExtra("addedItemsList");
      for (String itemName : resultsList) {
        ListItem newItem = new ListItem();
        newItem.setItemName(itemName);
        viewModel.insert(newItem);
      }
    }
  }

  // launch activity to crop image
  // CREDIT: https://github.com/ArthurHub/Android-Image-Cropper library used
  private void launchCropImageActivity() {
    CropImage.activity(imageUri).setAllowRotation(false).setAllowFlipping(false).setOutputCompressFormat(Bitmap.CompressFormat.JPEG).setOutputCompressQuality(100).start(this);
  }

  // detect text to add to list
  private void detectTextFromImage() {
    Log.d(TAG, "detectTextFromImage has been called ");
    ArrayList<String> scannedLines = new ArrayList<>();

    // Firebase ML Kit to detect text from images
    FirebaseVisionImage fbVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
    FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    Task<FirebaseVisionText> detectTextTask = textRecognizer.processImage(fbVisionImage);
    detectTextTask.addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
      @Override
      public void onSuccess(FirebaseVisionText firebaseVisionText) {

        Log.d(TAG, "onSuccess: has been called ");

        // text that has enough white space separating them will be captured in different TextBlocks
        // loop through each block to retrieve all lines
        List<FirebaseVisionText.TextBlock> textBlocks = firebaseVisionText.getTextBlocks();
        for (FirebaseVisionText.TextBlock block : textBlocks) {

          // text in close proximity will be stored in individual Lines in a list
          // lines are stored in blocks
          List<FirebaseVisionText.Line> lines = block.getLines();
          for (FirebaseVisionText.Line line : lines) {
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

  // Share the list via an intent
  private void showShareIntent() throws ExecutionException, InterruptedException {
    String sharedItems = getItemsToShare();
    Intent i = new Intent();
    i.setAction(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_TEXT, sharedItems);
    startActivity(Intent.createChooser(i, "share list"));
  }

  // get a single String list from ListItem ArrayList to share
  private String getItemsToShare() throws ExecutionException, InterruptedException {
    // itemList = adapterMain.getItemList();
    List<String> itemNames = viewModel.getItemNames();
    String sharedItems = "\n\n";
    for (String name : itemNames) {
      sharedItems += name + "\n";
    }
    return sharedItems;
  }

  // request permission to access audio recording feature
  private void requestPermission() {
    // API level 23, Marshmallow
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ActivityCompat.requestPermissions(this,
          new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_VOICE_RECOGNITION);
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_CODE_VOICE_RECOGNITION && grantResults.length > 0) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        startSpeechRecognizer();
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private void startSpeechRecognizer() {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    edtAddItem.setText("");
    edtAddItem.setHint("Talk to input items");  // use String resource
    speechRecognizer.startListening(intent);
    speechRecognizer.setRecognitionListener(new RecognitionListener() {
      @Override
      public void onReadyForSpeech(Bundle params) {

      }

      @Override
      public void onBeginningOfSpeech() {

      }

      @Override
      public void onRmsChanged(float rmsdB) {

      }

      @Override
      public void onBufferReceived(byte[] buffer) {

      }

      @Override
      public void onEndOfSpeech() {

      }

      @Override
      public void onError(int error) {

      }

      @Override
      public void onResults(Bundle results) {
        String voiceResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
        if(voiceResults.toLowerCase().contains("amount")) {
          Log.i(TAG, "results of voice recognition: contains the word amount!");
          String[] splitResults = voiceResults.split("amount");
          String quantity = splitResults[1].trim();
          quantity = convertQuantity(quantity); // convert for numbers 2 or 4


          String resultsWithQtys = quantity + "  " + splitResults[0];
          edtAddItem.setText(resultsWithQtys);
        } else {
          edtAddItem.setText(voiceResults);
        }
      }

      private String convertQuantity(String quantity) {
        switch (quantity) {
          case "two":
          case "to":
          case "too":
            return "2";

          case "three":
            return "3";

          case "four":
          case "for":
            return "4";

          case "five":
            return "5";

          case "six":
            return "6";

          case "seven":
            return "7";
          case "ate":
          case "eight":
            return "8";
          case "nine":
            return "9";
        }
        return quantity;
      }

      @Override
      public void onPartialResults(Bundle partialResults) {

      }

      @Override
      public void onEvent(int eventType, Bundle params) {
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    speechRecognizer.destroy();
  }
}