package com.bryontaylor.scanlist;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public class MyRecyclerViewAnimator extends DefaultItemAnimator {

  @Override
  public boolean animateAdd(RecyclerView.ViewHolder holder) {
    dispatchAddFinished(holder); // call this method immediately to avoid animations causing flashes
    return true;
  }

  /* default implementation - setAlpha(0) causing flashes when inserting items into list
  @Override
    public boolean animateAdd(final RecyclerView.ViewHolder holder) {
        resetAnimation(holder);
        holder.itemView.setAlpha(0); // this code caused flashing/blinking when adding items
        mPendingAdditions.add(holder);
        return true;
    }
  */
}
