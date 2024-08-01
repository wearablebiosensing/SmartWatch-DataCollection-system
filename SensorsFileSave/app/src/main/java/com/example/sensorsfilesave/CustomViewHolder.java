package com.example.sensorsfilesave;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
public class CustomViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public TextView textView;
    private ItemClickListener itemClickListener;

    public CustomViewHolder(View itemView, ItemClickListener itemClickListener) {
        super(itemView);
        textView = itemView.findViewById(R.id.text_view);
        this.itemClickListener = itemClickListener;
        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (itemClickListener != null) {
            itemClickListener.onItemClick(getAdapterPosition());
        }
    }
}