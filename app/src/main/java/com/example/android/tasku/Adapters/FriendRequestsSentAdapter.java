package com.example.android.tasku.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
 * Created by john.osorio on 27/02/2018.
 */

public class FriendRequestsSentAdapter extends RecyclerView.Adapter<FriendRequestsSentAdapter.ViewHolder> {

    private Context context;
    private List<HashMap<String, String>> friendList;
    private FirebaseStorage mStorage;
    private StorageReference mUserImageRef;

    public FriendRequestsSentAdapter (Context context, List<HashMap<String, String>> friendList){
        this.context = context;
        this.friendList = friendList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ViewHolder viewHolder = null;
        View view = LayoutInflater.from(context).inflate(R.layout.friend_list_item, parent, false);
        viewHolder = new ViewHolder(view, friendList);

        final ImageView cancel = viewHolder.cancelFriendRequest;
        final TextView friendUserIdTV = viewHolder.friendListUserId;

        viewHolder.setOnClickListener(new ViewHolder.ClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                if(view == cancel) {
                    Toast.makeText(context, context.getString(R.string.friend_request_cancelled), Toast.LENGTH_SHORT).show();

                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String friendUserId = friendUserIdTV.getText().toString();

                    //Remove this friend from the list
                    FirebaseDatabase.getInstance().getReference(context.getString(R.string.userpiclocation) + currentUserId + context.getString(R.string.friendrequestsentpiclocation) + friendUserId).removeValue();
                    FirebaseDatabase.getInstance().getReference(context.getString(R.string.userpiclocation) + friendUserId + context.getString(R.string.friendrequestpiclocation) + currentUserId).removeValue();

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
    public void onBindViewHolder(final ViewHolder holder, int position) {

        //Show the progress bar
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.friendProfilePic.setVisibility(View.GONE);

        HashMap<String, String> friendMap = friendList.get(position);
        for(Map.Entry entry : friendMap.entrySet()) {
            String friendUid = entry.getKey().toString();
            String friendEmail = entry.getValue().toString();

            holder.friendListUserEmail.setText(friendEmail);
            holder.friendListUserId.setText(friendUid);

            //Set the friend profile avatar
            mStorage = FirebaseStorage.getInstance();
            mUserImageRef = mStorage.getReference(context.getString(R.string.images) + friendUid + context.getString(R.string.png));

            mUserImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Glide.with(context.getApplicationContext()).load(uri).asBitmap().centerCrop().error(R.drawable.assigned_user_icon).into(new BitmapImageViewTarget(holder.friendProfilePic) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            holder.friendProfilePic.setImageDrawable(circularBitmapDrawable);
                        }
                    });
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    //Hide the progress bar
                    holder.progressBar.setVisibility(View.GONE);
                    holder.friendProfilePic.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    private void removeAt(int position) {
        friendList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, friendList.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private List<HashMap<String, String>> friendList;
        private ImageView friendProfilePic;
        private ImageView cancelFriendRequest;
        private TextView friendListUserId;
        private TextView friendListUserEmail;
        private ProgressBar progressBar;

        public ViewHolder(View itemView, List<HashMap<String,String>> friendList) {
            super(itemView);
            this.friendList = friendList;
            friendProfilePic = (ImageView) itemView.findViewById(R.id.friend_list_profile_pic);
            friendListUserId = (TextView) itemView.findViewById(R.id.friend_list_profile_id);
            friendListUserEmail = (TextView) itemView.findViewById(R.id.friend_list_profile_name);
            cancelFriendRequest = (ImageView)itemView.findViewById(R.id.remove_friend);
            progressBar = (ProgressBar) itemView.findViewById(R.id.friend_list_item_progress_bar);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onItemClick(v, getAdapterPosition());
                }
            });
            cancelFriendRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onItemClick(v, getAdapterPosition());
                }
            });
        }

        private ViewHolder.ClickListener mClickListener;

        public interface ClickListener {
            void onItemClick(View view, int position);
        }

        public void setOnClickListener(ViewHolder.ClickListener clickListener) {
            mClickListener = clickListener;
        }
    }
}
