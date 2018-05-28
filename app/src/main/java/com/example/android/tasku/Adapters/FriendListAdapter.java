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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.example.android.tasku.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by john.osorio on 20/02/2018.
 */

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapterViewHolder> {

    private static final String TAG = FriendListAdapter.class.getSimpleName();
    private List<HashMap<String, String>> friends;
    private Context context;
    private FirebaseStorage mStorage;
    private StorageReference mUserImageRef;

    public FriendListAdapter(Context context, List<HashMap<String, String>> friends) {
        this.context = context;
        this.friends = friends;
        Log.d(TAG, "FRIEND LIST ADAPTER CREATED");
    }


    @Override
    public FriendListAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        FriendListAdapterViewHolder viewHolder = null;
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_list_item, parent, false);
        viewHolder = new FriendListAdapterViewHolder(layoutView, friends);

        final ImageView remove = viewHolder.mRemoveFriend;
        final TextView friendUserIdTV = viewHolder.mFriendListUserId;

        Log.d(TAG, "FRIEND LIST VIEWHOLDER CREATED");

        viewHolder.setOnClickListener(new FriendListAdapterViewHolder.ClickListener() {
            @Override
            public void onItemClick(final View view, final int position) {

                //This is to distinguish between clickable views in the recyclerview items
                if(view == remove){
                    Toast.makeText(context, context.getString(R.string.friend_removed) + position, Toast.LENGTH_SHORT).show();
                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String friendUserId = friendUserIdTV.getText().toString();

                    //Remove this friend from the list
                    FirebaseDatabase.getInstance().getReference(context.getString(R.string.userpiclocation) + currentUserId + context.getString(R.string.friendpiclocation) + friendUserId).removeValue();

                    removeAt(position);
                }

                else {
//                    Toast.makeText(context, "Whatever: " + position, Toast.LENGTH_SHORT).show();
                    //Perhaps lets the current user view this friend's profile
                }
            }


        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final FriendListAdapterViewHolder holder, int position) {

        //Turn on the progress bar
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.mFriendListProfilePic.setVisibility(View.GONE);

        HashMap<String, String> friendMap = friends.get(position);
        Log.d(TAG, "MAP SIZE IS: " + friendMap.size());

        //Separate the user ID as key and the user Email as value
        for(Map.Entry map : friendMap.entrySet()) {
            String friendUid = map.getKey().toString();
            Log.d(TAG, "FRIEND UID IS: " + friendUid);
            String friendEmail = map.getValue().toString();
            Log.d(TAG, "FRIEND EMAIL IS: " + friendEmail);
            holder.mFriendListEmail.setText(friendEmail);
            holder.mFriendListUserId.setText(friendUid);
            //Log.d(TAG, "Friend ID is: " + friendUid);

            //Set the friend profile avatar
            mStorage = FirebaseStorage.getInstance();
            mUserImageRef = mStorage.getReference(context.getString(R.string.images) + friendUid + context.getString(R.string.png));

            mUserImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Glide.with(context.getApplicationContext()).load(uri).asBitmap().centerCrop().error(R.drawable.assigned_user_icon).into(new BitmapImageViewTarget(holder.mFriendListProfilePic) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            holder.mFriendListProfilePic.setImageDrawable(circularBitmapDrawable);
                        }
                    });
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    //Turn off the progress bar
                    holder.progressBar.setVisibility(View.GONE);
                    holder.mFriendListProfilePic.setVisibility(View.VISIBLE);
                }
            });

        }
    }

    //TODO: Important! If this is not set e.g. default to zero then recyclerview won't show anything and won't start onCreateViewHolder!!
    @Override
    public int getItemCount() {
        return this.friends.size();
    }

    private void removeAt(int position) {
        friends.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, friends.size());
    }


}
