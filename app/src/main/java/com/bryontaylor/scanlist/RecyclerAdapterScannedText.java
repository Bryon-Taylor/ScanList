package com.bryontaylor.scanlist;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerAdapterScannedText extends
    RecyclerView.Adapter<RecyclerAdapterScannedText.ScannedTextViewHolder> {

  private static final String TAG = "scannedAdapter";
  private List<String> scannedLines;
  private OnAddBtnListener listener;

  private ArrayList<RecyclerView.ViewHolder> viewHolders;

  // Pass scanned (OCR) text lines from the image to the constructor
  public RecyclerAdapterScannedText(List<String> scannedLines) {
    this.scannedLines = scannedLines;
  }

  @NonNull
  @Override
  public ScannedTextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View itemView = inflater.inflate(R.layout.recycler_item_layout_scanned, parent, false);
    return new ScannedTextViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(@NonNull ScannedTextViewHolder holder, int position) {
    holder.edtAddItem.setText(scannedLines.get(position));

    // Track changes to EditTexts so any modifications can be passed back to ScannedTextActivity
    holder.edtAddItem.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Update String at this position when text changes. Use holder.getAdapterPosition()
        // instead of provided "position" variable as it produces inaccurate results.
        scannedLines.set(holder.getAdapterPosition(), String.valueOf(s));
        Log.i(TAG, "position: " + position + " " + s);
      }

      @Override
      public void afterTextChanged(Editable s) {

      }
    });
  }

  @Override
  public int getItemCount() {
    return scannedLines == null ? 0 : scannedLines.size();
  }

  // Inner class for the ViewHolder
  class ScannedTextViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    EditText edtAddItem;
    ImageView imgAddItem;

    public ScannedTextViewHolder(@NonNull View itemView) {
      super(itemView);
      edtAddItem = itemView.findViewById(R.id.edt_add_item_scanned);
      imgAddItem = itemView.findViewById(R.id.img_add_item_scanned);
      imgAddItem.setOnClickListener(this);

    }

    // If user clicks the add item button, get the current value in case user modified it
    @Override
    public void onClick(View v) {

      // get current value of EditText in case user modified it
      String edtTextValue = String.valueOf(edtAddItem.getText());

      // send values to ScannedTextActivity to add items to list
      listener.addItemToList(edtTextValue, getAdapterPosition());
    }
  }

  // implement a callback for ScannedTextActivity
  interface OnAddBtnListener {
    void addItemToList(String itemToAdd, int position);
  }

  // called from ScannedTextActivity
  public void setBtnAddListener (OnAddBtnListener listener) {
    this.listener = listener;
  }

  // scannedLines ArrayList contains all current values of EditText (in case they were modified)
  public List<String> getAllItems() {
    return scannedLines;
  }
}
