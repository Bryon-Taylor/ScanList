package com.bryontaylor.scanlist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bryontaylor.scanlist.architecture_components.ListItemViewModel;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * ScanList is a list creation app that allows the user to enter list items traditionally through
 * typing as well as through faster input methods such as scanned voice entries and scanned images
 * taken from the phone's camera for text.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  // UI, RecyclerView and architecture components
  private ImageView imgAddItem;
  private EditText edtAddItem;
  private RecyclerView recyclerMain;
  private RecyclerAdapterMain adapterMain;
  private DividerItemDecoration recyclerDivider;
  private ListItemViewModel viewModel;
  private ConstraintLayout constraintLayout;
  private LinearLayoutManager layoutManager;

  // For image capture from camera
  private Uri imageUri;
  private final String AUTHORITY = "com.bryontaylor.scanlist.provider";
  private static final int REQUEST_CODE_TAKE_PHOTO = 1000;
  private static final int REQUEST_CODE_SCANNED_LINES = 2000;
  private Bitmap bitmap;

  // For voice recognition
  private static final int REQUEST_CODE_VOICE_RECOGNITION = 3000;
  private SpeechRecognizer speechRecognizer;

  // For dragging and swiping
  private int recyclerLastIndex;
  private boolean wasDragged;
  private boolean wasSwiped;
  private ListItem movedItem;
  private static final float DRAG_ELEVATION = 60f;

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

  // Initialize components
  private void initComponents() {
    imgAddItem = findViewById(R.id.img_add_item_main);
    edtAddItem = findViewById(R.id.edt_add_item_main);
    viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        .create(ListItemViewModel.class);
    constraintLayout = findViewById(R.id.constraint_layout);
    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
  }

  // Initialize RecyclerView and Adapter
  private void initRecyclerView() {
    recyclerMain = findViewById(R.id.recycler_main);
    layoutManager = new LinearLayoutManager(this);
    recyclerMain.setLayoutManager(layoutManager);
    recyclerDivider = new DividerItemDecoration(recyclerMain.getContext(),
        layoutManager.getOrientation());
    recyclerMain.addItemDecoration(recyclerDivider);
    adapterMain = new RecyclerAdapterMain();
    recyclerMain.setAdapter(adapterMain);
  }

  private void setListeners() {
    imgAddItem.setOnClickListener(this); // Adds single list items from the EditText in MainActivity

    // This toggles the checkbox on an item displayed in the recycler view
    adapterMain.setCheckBoxListener(new RecyclerAdapterMain.CheckBoxListener() {
      @Override
      public void onCheckBoxClicked(ListItem item) {
        // Create a new item so onBindViewHolder is called and the colors for the checked or
        // unchecked state are set properly
        ListItem newListItem = new ListItem();
        newListItem.setId(item.getId());
        newListItem.setItemName(item.getItemName());
        newListItem.setChecked(!item.getIsChecked()); // Toggle isChecked value
        newListItem.setPositionInList(item.getPositionInList());
        viewModel.update(newListItem);
      }
    });
  }

  // LiveData keeps the list current whenever a change in the local database occurs
  private void setListObserver() {
    viewModel.getAllItems().observe(this, new Observer<List<ListItem>>() {
      @Override
      public void onChanged(List<ListItem> newList) {
        // Submits list to the ListAdapter
        adapterMain.submitList(newList);
      }
    });
  }

  // ItemTouchHelper class handles swipes & drags on the recycler view
  private void createItemTouchHelper() {
    new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP | ItemTouchHelper.DOWN, // Support dragging
        ItemTouchHelper.START | ItemTouchHelper.END) { // Support swiping

      private int originalTextColor; // To restore original text color after dragging
      private List<ListItem> currentList; // To track swapped items through drag and drop

      @Override
      public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();

        // Right swipes are to delete items
        if (direction == ItemTouchHelper.END) {
          ListItem deletedItem = adapterMain.getItemAt(position);
          viewModel.delete(deletedItem); // Delete from database
          showUndoSnackBar(deletedItem, position); // Show a snackBar to allow undo

        } else { // Left swipes are to edit items

          // Shows the user a dialog with an EditText to modify the item
          showEditItemDialog(position);
          edtAddItem.clearFocus(); // So keyboard does not appear on returning to main screen
        }
      }

      // Called at the start of a swipe or drag
      @Override
      public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (viewHolder != null) {
          View foregroundLayout = viewHolder.itemView.findViewById(R.id.foreground_layout);
          if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) { // If user swipes
            wasSwiped = true;

            // Change text color while being swiped
            TextView textView = viewHolder.itemView.findViewById(R.id.txt_item_name);
            originalTextColor = textView.getCurrentTextColor();
            textView.setTextColor(getResources().getColor(R.color.swipeTextColor, null));

            // Change checkbox color while being swiped
            CheckBox checkBox = viewHolder.itemView.findViewById(R.id.checkBox);
            checkBox.setButtonTintList(ColorStateList
                .valueOf(getResources().getColor(R.color.swipeTextColor, null)));

            // Change the background color to indicate it is being swiped
            foregroundLayout.setBackgroundColor(getResources()
                .getColor(R.color.swipeColor, null));

          } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) { // If user drags

            currentList = new ArrayList<>(adapterMain.getCurrentList()); // Used in onMove()
            wasDragged = true;
            movedItem = adapterMain.getItemAt(viewHolder.getAdapterPosition()); // Item being dragged
            recyclerLastIndex = adapterMain.getItemCount() - 1; // Used in onChildDraw method

            // Change the background color to indicate it is being dragged
            foregroundLayout.setBackgroundColor(getResources()
                .getColor(R.color.backgroundDragColor, null));

            // Change text color while being dragged
            TextView textView = viewHolder.itemView.findViewById(R.id.txt_item_name);
            originalTextColor = textView.getCurrentTextColor(); // To restore color after drop
            textView.setTextColor(getResources().getColor(R.color.viewDragColor, null));

            // Set checkbox color while being dragged to match the text
            CheckBox checkBox = viewHolder.itemView.findViewById(R.id.checkBox);
            checkBox.setButtonTintList(ColorStateList
                .valueOf(getResources().getColor(R.color.viewDragColor, null)));

            // Change the elevation to visually "pick up" a list item to drag it
            viewHolder.itemView.setElevation(DRAG_ELEVATION);
          }
        }
      }

      // Called when swiping or dragging is over
      @Override
      public void clearView(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder) {

        if(wasDragged) { // Restore original text and checkbox colors after drop
          TextView textView = viewHolder.itemView.findViewById(R.id.txt_item_name);
          textView.setTextColor(originalTextColor);

          CheckBox checkBox = viewHolder.itemView.findViewById(R.id.checkBox);
          if(checkBox.isChecked()) { // Checked
            checkBox.setButtonTintList(ColorStateList.valueOf(getResources()
                .getColor(R.color.checkedColor, null)));
          } else if(!checkBox.isChecked()) { // Unchecked
            checkBox.setButtonTintList(ColorStateList.valueOf(getResources()
                .getColor(R.color.uncheckedColor, null)));
          }
        }

        if(wasSwiped) { // Restore text and checkbox colors after being swiped
          TextView textView = viewHolder.itemView.findViewById(R.id.txt_item_name);
          textView.setTextColor(originalTextColor);
          CheckBox checkBox = viewHolder.itemView.findViewById(R.id.checkBox);

          if(checkBox.isChecked()) { // Checked
            textView.setTextColor(Color.LTGRAY);
            checkBox.setButtonTintList(ColorStateList
                .valueOf(getResources().getColor(R.color.checkedColor, null)));
          } else { // Unchecked
            checkBox.setButtonTintList(ColorStateList
                .valueOf(getResources().getColor(R.color.uncheckedColor, null)));
          }
        }

        // Bring foreground layer back on top when delete operation is undone
        final View foregroundLayout = viewHolder.itemView.findViewById(R.id.foreground_layout);
        getDefaultUIUtil().clearView(foregroundLayout);

        // Change back to original color when partial swipe is released
        foregroundLayout.setBackgroundColor(getResources().getColor(R.color.swipeReleased, null));

        // Used to calculate a moved item's new position
        int position = viewHolder.getAdapterPosition();
        double movedItemNewPos, itemBeforePos, itemAfterPos;

        // Skip this code if item was deleted (indicated by -1) or was swiped.
        // Otherwise, update the dragged item
        if(position != -1 && !wasSwiped) {

          // If dragged to the beginning of the list (index 0) subtract 1 from the previously lowest
          // positionInList value (the item after it at index 1) and assign it the moved item.
          // It now has the lowest positionInList value and will be ordered first.
          if(position == 0) {
            itemAfterPos = currentList.get(position + 1).getPositionInList();
            movedItemNewPos = itemAfterPos - 1;

            // If dragged to the end of list, add 1 to the positionInList value of the previously
            // largest value (the item before it at index currentList.size() - 2) and assign to the
            // moved item to order it last.
          } else if(position == currentList.size() - 1) {
            itemBeforePos = currentList.get(position - 1).getPositionInList();
            movedItemNewPos = itemBeforePos + 1;

            // If dragged somewhere in the middle of list, get the positionInList variable value of
            // the items before and after it. They are used to compute the moved item's new
            // positionInList value, which will be half way between the values above and below it.
          } else {
            itemBeforePos = currentList.get(position - 1).getPositionInList();
            itemAfterPos = currentList.get(position + 1).getPositionInList();

            // Calculates the moved item's positionInList variable to be half way between the
            // item above it and item below it
            movedItemNewPos = itemBeforePos + ((itemAfterPos - itemBeforePos) / 2.0);
          }
          updateItemPosInDB(movedItem, movedItemNewPos); // Update the moved item's position in DB
        }
        // To visually "drop" a dragged item
        viewHolder.itemView.setElevation(0);

        // Reset flags
        wasSwiped = false;
        wasDragged = false;

      }

      // This will update the the moved item's positionInList variable in the database to persist
      // its order in the list. Elements are ordered (ascending) by the positionInList variable.
      private void updateItemPosInDB(ListItem movedItem, double movedItemNewPos) {
        movedItem.setPositionInList(movedItemNewPos);
        viewModel.update(movedItem);
      }

      @Override
      public void onChildDraw(@NonNull Canvas canvas,
                              @NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View v = viewHolder.itemView; // to shorten the code below

        // Used to change the element colors and visibility of a ViewHolder
        View foregroundLayout = v.findViewById(R.id.foreground_layout);
        View backgroundLayout = v.findViewById(R.id.background_layout);
        View editIcon = v.findViewById(R.id.img_edit_icon);
        View editText = v.findViewById(R.id.txt_edit);
        View deleteIcon = v.findViewById(R.id.img_delete_icon);
        View deleteText = v.findViewById(R.id.txt_delete);

        if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

          // Use getDefaultUIUtil to have background stay stationary while foreground is swiped
          getDefaultUIUtil().onDraw(canvas, recyclerView, foregroundLayout,
              dX, dY, actionState, isCurrentlyActive);

          if(dX > 0) { // User swiped right to delete

            // Change background color to indicate "delete mode"
            backgroundLayout.setBackgroundColor(getResources()
                .getColor(R.color.backgroundColorDelete, null));

            // Hide edit icon and edit text if user swipes to delete
            editIcon.setVisibility(View.GONE);
            editText.setVisibility(View.GONE);

            // Make delete icon and text visible if they were hidden
            deleteIcon.setVisibility(View.VISIBLE);
            deleteText.setVisibility(View.VISIBLE);

          } else if (dX < 0) { // User swiped left to edit

            // Change background color to indicate "edit mode"
            backgroundLayout.setBackgroundColor(getResources()
                    .getColor(R.color.backgroundColorEdit, null));

            // Hide delete icon if user swipes to edit
            deleteIcon.setVisibility(View.GONE);
            deleteText.setVisibility(View.GONE);

            // Set edit icon and text visible if they were hidden
            editIcon.setVisibility(View.VISIBLE);
            editText.setVisibility(View.VISIBLE);
          }
        } else if(actionState == ItemTouchHelper.ACTION_STATE_DRAG) {

          // To compare the viewHolder's index with the beginning and end of list indices
          int viewHolderIndex = viewHolder.getAdapterPosition();

          // Calling onChildDraw with dY = 0 will stop dragging outside recyclerView's bounds
          if((viewHolderIndex == recyclerLastIndex && dY > 0) || (viewHolderIndex == 0 && dY < 0)) {
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, 0, // dY == 0
                actionState, isCurrentlyActive);
          } else {
            super.onChildDraw(canvas, recyclerView, viewHolder, dX,
                dY, actionState, isCurrentlyActive); // Normal dY
          }
        }
      }

      // Called when user drags an item around it the list
      @Override
      public boolean onMove(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder source,
                            @NonNull RecyclerView.ViewHolder target) {

        int fromPosition = source.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        Collections.swap(currentList, toPosition, fromPosition);
        adapterMain.onItemMoved(fromPosition, toPosition);
        return true;
      }
    }).attachToRecyclerView(recyclerMain);
  }

  // Inserts a new ListItem when tapping the add button and MainActivity's EditText has content
  public void onClick(@NonNull View v) {
    if (v.getId() == R.id.img_add_item_main) {
      String itemName = String.valueOf(edtAddItem.getText()); // Get user input
      if (!itemName.trim().equals("")) { // Insert new list item only if the EditText is not empty
        ListItem item = new ListItem();
        item.setItemName(itemName);
        List<ListItem> currentList = adapterMain.getCurrentList();
        if(!currentList.isEmpty()) { // If the currentList is not empty

          // Add 1 to the positionInList value of the last item to have the new item ordered last
          ListItem lastItem = adapterMain.getItemAt(currentList.size() - 1);
          item.setPositionInList(lastItem.getPositionInList() + 1);
        } else {
          item.setPositionInList(1); // The list is empty and this is the 1st item to be added
        }
        viewModel.insert(item);
      }
      // Clear EditText and resent hint
      edtAddItem.setText("");
      edtAddItem.setHint(R.string.add_item);
    }
  }

  // SnackBar to allow a user to undo a delete operation
  private void showUndoSnackBar(ListItem deletedItem, int position) {
    Snackbar undoSnackBar = Snackbar.make(constraintLayout, R.string.undo_deleted_item,
        Snackbar.LENGTH_LONG).setAction(R.string.undo, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // Restore deleted item if UNDO is clicked
        viewModel.insert(deletedItem);
      }
    });
    undoSnackBar.show();
  }

  // Edit a swiped list item in an AlertDialog popup
  private void showEditItemDialog(int position) {

    ListItem currentListItem = adapterMain.getItemAt(position);

    // Presents a dialog box to edit the swiped item
    View customAlertDialog = LayoutInflater.from(MainActivity.this)
        .inflate(R.layout.custom_edit_alert_dialog, null);
    EditText edtItemInDialog = customAlertDialog.findViewById(R.id.edt_item_dialog);
    edtItemInDialog.setText(currentListItem.getItemName().trim()); // Show current value
    edtItemInDialog.requestFocus(); // To have keyboard appear
    edtItemInDialog.setSelection(0); // Put the cursor at the beginning of the EditText

    // Create dialog to edit the list item
    AlertDialog.Builder alertBuilder =
        new AlertDialog.Builder(MainActivity.this)
            .setView(customAlertDialog)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

        // Update list with the EditText value
        String editedItemName = String.valueOf(edtItemInDialog.getText());
        if(!editedItemName.trim().equals("")) { // Prevent empty values from being inserted
          currentListItem.setItemName(editedItemName);
          viewModel.update(currentListItem);
          adapterMain.notifyItemChanged(position); // restore foreground
        }
      }
    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

        // This restores swiped away foreground layout when user clicks the cancel button
        adapterMain.notifyItemChanged(position);
      }
    });
    AlertDialog alertDialog = alertBuilder.show();

    // Disable positive button until user makes changes to the EditText
    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

    // Display keyboard when alertDialog appears
    alertDialog.getWindow()
        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

    // To detect changes in the EditText field
    edtItemInDialog.addTextChangedListener(new TextWatcher() {
      @Override
      public void afterTextChanged(Editable s) {
        // Enable positive button to be clicked after user makes changes
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        if(s.length() == 0) {
          // Disable the positive button if EditText is empty. Prevents empty entries
          alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
      }

      // Unused method
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

      // Unused method
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) { }
    });
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
          requestPermission();
      } else {
          startSpeechRecognizer();
          hideKeyboard(); // Voice input does not require the keyboard so hide it
      }
        break;
      default:
    }
    return super.onOptionsItemSelected(item);
  }

  // Launches the phone's camera and stores the image in a temporary file in the phone's cache
  private void launchCamera() {
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    File photoFile = null;

    // Create a temp file in the phone's cache to store the image
    try {
      File tempDir = this.getCacheDir();
      photoFile = File.createTempFile("jpeg_", ".jpg", tempDir);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Uri to locate image file
    if (Build.VERSION.SDK_INT > 24) {
      imageUri = FileProvider.getUriForFile(this, AUTHORITY, photoFile);
    } else {
      imageUri = Uri.fromFile(photoFile); // for Android version < 24
    }

    // For full size image, thumbnails have poor resolution for the text OCR to be accurate
    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    // Ensure that there's a camera activity to handle the intent
    if (intent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
    }
    photoFile.deleteOnExit(); // Delete temp file when app is closed
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);

    // Resolver required to retrieve image from MediaStore
    ContentResolver contentResolver = this.getContentResolver();

    // Retrieve the bitmap returned by the photo app and open cropping activity
    if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
      try {
        bitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
        launchCropImageActivity();
      } catch (IOException e) {
        e.printStackTrace();
      }

      // Get the cropped image
    } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
      CropImage.ActivityResult result = CropImage.getActivityResult(intent);
      Uri resultUri = result.getUri();
      try {
        bitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, resultUri);

        // Detect text from the cropped image
        detectTextFromImage();
      } catch (IOException e) {
        e.printStackTrace();
      }

      // Get the list of added items back from ScannedTextActivity. This list contains all the items
      // obtained from the scanned image that the user wants to add to the main list
    } else if (requestCode == REQUEST_CODE_SCANNED_LINES && resultCode == RESULT_OK) {
      List<ListItem> currentList = adapterMain.getCurrentList();
      double currentListPos;
      if(!currentList.isEmpty()) {

        // Get the last item's positionInList value and add 1 to order new item last
        currentListPos = adapterMain.getItemAt(currentList.size() - 1).getPositionInList() + 1;
      } else {
        currentListPos = 1; // else the list was empty and this is the first insertion
      }

      // Loop through list and insert new items into the database
      ArrayList<String> resultsList = intent.getStringArrayListExtra("addedItemsList");
      for (String itemName : resultsList) {
        ListItem newItem = new ListItem();
        newItem.setItemName(itemName);
        newItem.setPositionInList(currentListPos);
        currentListPos++;
        viewModel.insert(newItem);
      }
    }
  }

  // Launch activity to crop image
  // CREDIT: https://github.com/ArthurHub/Android-Image-Cropper library used
  private void launchCropImageActivity() {
    CropImage.activity(imageUri)
        .setAllowRotation(false)
        .setAllowFlipping(false)
        .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
        .setOutputCompressQuality(100)
        .start(this);
  }

  // Detect text from an image to add to the list
  private void detectTextFromImage() {
    ArrayList<String> scannedLines = new ArrayList<>();

    // Firebase ML Kit to detect text from images
    FirebaseVisionImage fbVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
    FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    Task<FirebaseVisionText> detectTextTask = textRecognizer.processImage(fbVisionImage);
    detectTextTask.addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
      @Override
      public void onSuccess(FirebaseVisionText firebaseVisionText) {

        // Text that has enough white space separating them will be captured in different TextBlocks
        // Loop through each block to retrieve all lines
        List<FirebaseVisionText.TextBlock> textBlocks = firebaseVisionText.getTextBlocks();
        for (FirebaseVisionText.TextBlock block : textBlocks) {

          // Text in close proximity will be stored in individual Line objects in a list
          // Lines are stored in TextBlocks
          List<FirebaseVisionText.Line> lines = block.getLines();
          for (FirebaseVisionText.Line line : lines) {
            scannedLines.add(line.getText());
          }
        }
        // Pass the new list to ScannedTextActivity to display in a recycler view
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

  // Share the list via an intent. The system will display apps capable of sending text.
  private void showShareIntent() throws ExecutionException, InterruptedException {
    String sharedItems = getItemsToShare(); // Creates a single String from a list
    Intent i = new Intent();
    i.setAction(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_TEXT, sharedItems);
    startActivity(Intent.createChooser(i, "share list"));
  }

  // Create a single String from a list to share
  private String getItemsToShare() throws ExecutionException, InterruptedException {
    List<String> itemNames = viewModel.getItemNames();
    StringBuilder sb = new StringBuilder();
    for (String name : itemNames) {
      sb.append(name).append("\n");
    }
    sb.append("\n"); // Add a new line
    return sb.toString();
  }

  // Request permission to access audio recording feature, required on versions >= Marshmallow
  private void requestPermission() {
    // API level 23, Marshmallow
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ActivityCompat.requestPermissions(this,
          new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_VOICE_RECOGNITION);
    }
  }

  // If the user grants permission to use the device's microphone, start the speech recognizer
  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_CODE_VOICE_RECOGNITION && grantResults.length > 0) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        startSpeechRecognizer();
      }
    }
  }

  // Start listening for voice input
  @SuppressLint("ClickableViewAccessibility")
  private void startSpeechRecognizer() {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    edtAddItem.setText("");
    edtAddItem.setHint(R.string.speech_input_hint);
    speechRecognizer.startListening(intent);
    speechRecognizer.setRecognitionListener(new RecognitionListener() {
      @Override
      public void onResults(Bundle results) {
        String voiceResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
        if(voiceResults.isEmpty()) {
          edtAddItem.setHint(R.string.add_item);
        }

        // SpeechRecognizer converts the word "times" to "*" (multiplication symbol)
        if(voiceResults.contains("*")) {
          String[] splitResults = voiceResults.split("\\*"); // Use "*" as delimiter
          String quantity = splitResults[1].trim();

          // Add a the quantity separated by a dash for readability
          String resultsWithQtys = quantity + " - " + splitResults[0];
          edtAddItem.setText(resultsWithQtys);
        } else { // Otherwise just add spoken results
          edtAddItem.setText(voiceResults);
        }
      }

      @Override
      public void onError(int error) {
        edtAddItem.setHint(R.string.add_item); // Restore hint to default if no speech detected
      }

      // Following 7 methods are unused
      @Override
      public void onReadyForSpeech(Bundle params) { }

      @Override
      public void onBeginningOfSpeech() { }

      @Override
      public void onRmsChanged(float rmsdB) { }

      @Override
      public void onBufferReceived(byte[] buffer) { }

      @Override
      public void onEndOfSpeech() { }

      @Override
      public void onPartialResults(Bundle partialResults) { }

      @Override
      public void onEvent(int eventType, Bundle params) { }
    });
  }

  private void hideKeyboard() {
    if(edtAddItem.hasFocus()) {
      edtAddItem.clearFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(constraintLayout.getWindowToken(), 0);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    edtAddItem.clearFocus(); // so keyboard doesn't popup
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    speechRecognizer.destroy();
  }
}