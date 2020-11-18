package com.bryontaylor.scanlist;

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

  private List<String> scannedLines;
  private List<String> listAdditions;
  private OnAddBtnListener listener;

  // pass scanned text lines to constructor
  public RecyclerAdapterScannedText(List<String> scannedLines) {
    this.scannedLines = scannedLines;
    listAdditions = new ArrayList<>();
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
  }

  @Override
  public int getItemCount() {
    return scannedLines == null ? 0 : scannedLines.size();
  }

  // inner class for the ViewHolder
  class ScannedTextViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    EditText edtAddItem;
    ImageView imgAddItem;

    public ScannedTextViewHolder(@NonNull View itemView) {
      super(itemView);
      edtAddItem = itemView.findViewById(R.id.edt_add_item_scanned);
      imgAddItem = itemView.findViewById(R.id.img_add_item_scanned);
      imgAddItem.setOnClickListener(this);
    }

    // if user clicks the add item button, get the current value in case user modified it
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

  // get all CURRENT values of the EditTexts in case user modified them
  private void getEditTextsCurrentValue() {
    for(int i = 0; i < scannedLines.size(); i++) {
      // i need the edittext value at i

    }
  }
}
