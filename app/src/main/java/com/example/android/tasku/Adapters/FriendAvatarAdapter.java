package com.example.android.tasku.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.example.android.tasku.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;

/**
 * Created by john.osorio on 10/03/2018.
 */

public class FriendAvatarAdapter extends RecyclerView.Adapter<FriendAvatarAdapter.ViewHolder> {

    private static final String TAG = FriendAvatarAdapter.class.getSimpleName();
    private Context context;
    private List<String> friendAvatarList;

    private FirebaseStorage mStorage;
    private StorageReference userImageStorageRef;

    public FriendAvatarAdapter (Context context, List<String> friendAvatarList) {

        this.context = context;
        this.friendAvatarList = friendAvatarList;
    }

    @Override
    public FriendAvatarAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mStorage = FirebaseStorage.getInstance();

        FriendAvatarAdapter.ViewHolder viewHolder = null;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_avatar_item, parent, false);
        viewHolder = new ViewHolder(view, friendAvatarList);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final FriendAvatarAdapter.ViewHolder holder, int position) {

        //Turn on the progress bar
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.friendAvatar.setVisibility(View.GONE);

        String friendUID = friendAvatarList.get(position);

        userImageStorageRef = mStorage.getReference().child(context.getString(R.string.images) + friendUID + context.getString(R.string.png));
        userImageStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.d(TAG, "USER PROFILE PIC URL IS: " + uri);

                Glide.with(context.getApplicationContext())
                        .load(uri)
                        .asBitmap()
                        .centerCrop()
                        .error(R.drawable.assigned_user_icon)
                        .into(new BitmapImageViewTarget(holder.friendAvatar) {

                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        holder.friendAvatar.setImageDrawable(circularBitmapDrawable);
                    }
                });
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {

                //Turn off the progress bar
                holder.progressBar.setVisibility(View.GONE);
                holder.friendAvatar.setVisibility(View.VISIBLE);
            }
        });

    }

    @Override
    public int getItemCount() {
        return friendAvatarList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private List<String> friendAvatarList;
        private ImageView friendAvatar;
        private ProgressBar progressBar;

        public ViewHolder(View itemView, List<String> friendAvatarList) {
            super(itemView);

            this.friendAvatarList = friendAvatarList;
            friendAvatar = (ImageView) itemView.findViewById(R.id.friend_avatar);
            progressBar = (ProgressBar) itemView.findViewById(R.id.friend_avatar_item_progress_bar);
        }
    }

    public void removeAvatar(int position) {
            friendAvatarList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, friendAvatarList.size());

    }
}
