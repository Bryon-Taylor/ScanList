package com.bryontaylor.scanlist.util;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class VerticalSpaceDecor extends RecyclerView.ItemDecoration {

  // defines the vertical space height between RecyclerView list items
  private final int vertSpaceHeight;

  public VerticalSpaceDecor(int vertSpaceHeight) {
    this.vertSpaceHeight = vertSpaceHeight;
  }

  @Override
  public void getItemOffsets(@NonNull Rect outRect,
                             @NonNull View view,
                             @NonNull RecyclerView parent,
                             @NonNull RecyclerView.State state) {
    super.getItemOffsets(outRect, view, parent, state);
    outRect.top = vertSpaceHeight;
  }
}
