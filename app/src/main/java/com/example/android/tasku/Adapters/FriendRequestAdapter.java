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
 * Created by john.osorio on 13/02/2018.
 */

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapterViewHolder> {

    private static final String TAG = FriendRequestAdapter.class.getSimpleName();

    private List<HashMap<String, String>> friends;
    private Context context;
    private FirebaseStorage mStorage;
    private StorageReference mUserImageRef;

    public FriendRequestAdapter(Context context, List<HashMap<String, String>> friends) {
        this.context = context;
        this.friends = friends;
    }


    @Override
    public FriendRequestAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        FriendRequestAdapterViewHolder viewHolder = null;
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_request_list_item, parent, false);
        viewHolder = new FriendRequestAdapterViewHolder(layoutView, friends);

        final ImageView approve = viewHolder.mApproveRequest;
        final ImageView reject = viewHolder.mRejectRequest;
        final TextView friendUserIdTV = viewHolder.mFriendUserId;

        Log.d(TAG, "FRIEND REQUEST VIEWHOLDER CREATED");


        viewHolder.setOnClickListener(new FriendRequestAdapterViewHolder.ClickListener() {
            @Override
            public void onItemClick(final View view, final int position) {

                //This is to distinguish between clickable views in the recyclerview items
                if(view == approve){
//                    Toast.makeText(context, "Approved: " + position, Toast.LENGTH_SHORT).show();
                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String friendUserId = friendUserIdTV.getText().toString();

                    //set friend boolean "isFriend" to true
                    FirebaseDatabase.getInstance().getReference(context.getString(R.string.userpiclocation) + currentUserId + context.getString(R.string.friendpiclocation) + friendUserId).setValue(true);
                    FirebaseDatabase.getInstance().getReference(context.getString(R.string.userpiclocation) + friendUserId + context.getString(R.string.friendpiclocation) + currentUserId).setValue(true);

                    //Remove from Firebase
                    FirebaseDatabase.getInstance().getReference(context.getString(R.string.userpiclocation) + currentUserId + context.getString(R.string.friendrequestpiclocation) + friendUserId).removeValue();
                    FirebaseDatabase.getInstance().getReference(context.getString(R.string.userpiclocation) + friendUserId + context.getString(R.string.friendrequestsentpiclocation) + currentUserId).removeValue();

                    removeAt(position);

                } else if (view == reject) {
//                    Toast.makeText(context, "Rejected: " + position, Toast.LENGTH_SHORT).show();
                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String friendUserId = friendUserIdTV.getText().toString();

                    //Remove from Firebase
                    FirebaseDatabase.getInstance().getReference(context.getString(R.string.userpiclocation) + currentUserId + context.getString(R.string.friendrequestpiclocation) + friendUserId).removeValue();
                    FirebaseDatabase.getInstance().getReference(context.getString(R.string.userpiclocation) + friendUserId + context.getString(R.string.friendrequestsentpiclocation) + currentUserId).removeValue();

                    //Remove from the RecyclerView
                    removeAt(position);


                } else {
//                    Toast.makeText(context, "Whatever: " + position, Toast.LENGTH_SHORT).show();

                    //Perhaps lets the current user view this friend's profile
                }
            }


        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final FriendRequestAdapterViewHolder holder, int position) {

        //Show the progress bar
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.mFriendProfilePic.setVisibility(View.GONE);

        HashMap<String, String> friendMap = friends.get(position);

        //Separate the user ID as key and the user Email as value
        for(Map.Entry map : friendMap.entrySet()) {
            String friendUid = map.getKey().toString();
            Log.d(TAG, "FRIEND UID IS: " + friendUid);
            String friendEmail = map.getValue().toString();
            Log.d(TAG, "FRIEND EMAIL IS: " + friendEmail);
            holder.mFriendEmail.setText(friendEmail);
            holder.mFriendUserId.setText(friendUid);
            //Log.d(TAG, "Friend ID is: " + friendUid);

            //Set the friend profile avatar
            mStorage = FirebaseStorage.getInstance();
            mUserImageRef = mStorage.getReference(context.getString(R.string.images) + friendUid + context.getString(R.string.png));

            mUserImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Glide.with(context).load(uri).asBitmap().centerCrop().error(R.drawable.assigned_user_icon).into(new BitmapImageViewTarget(holder.mFriendProfilePic) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            holder.mFriendProfilePic.setImageDrawable(circularBitmapDrawable);
                        }
                    });
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    //Hide the progress bar
                    holder.progressBar.setVisibility(View.GONE);
                    holder.mFriendProfilePic.setVisibility(View.VISIBLE);
                }
            });
        }

    }

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
