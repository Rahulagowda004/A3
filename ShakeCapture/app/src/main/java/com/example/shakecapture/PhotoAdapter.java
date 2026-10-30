package com.example.shakecapture;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    public interface Listener {
        void onPhotoClicked(Photo photo);
        void onDeleteClicked(Photo photo);
    }

    private final List<Photo> photos = new ArrayList<>();
    private final Listener listener;
    private final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(2);

    public PhotoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setPhotos(List<Photo> newPhotos) {
        photos.clear();
        photos.addAll(newPhotos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);
        holder.image.setImageResource(R.drawable.bg_photo_placeholder);
        holder.image.setContentDescription(photo.getName());
        holder.itemView.setOnClickListener(v -> listener.onPhotoClicked(photo));
        holder.delete.setOnClickListener(v -> listener.onDeleteClicked(photo));
        thumbnailExecutor.execute(() -> {
            Bitmap bitmap = PhotoRepository.decodeThumbnail(photo, 600, 600);
            holder.image.post(() -> {
                if (holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION
                        && bitmap != null) {
                    holder.image.setImageBitmap(bitmap);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public void shutdown() {
        thumbnailExecutor.shutdownNow();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final ImageButton delete;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.photo_image);
            delete = itemView.findViewById(R.id.delete_photo);
        }
    }
}
