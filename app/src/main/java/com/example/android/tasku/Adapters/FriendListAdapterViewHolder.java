package com.example.android.tasku.Adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.tasku.R;

import java.util.HashMap;
import java.util.List;

/**
 * Created by john.osorio on 20/02/2018.
 */

public class FriendListAdapterViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = FriendListAdapterViewHolder.class.getSimpleName();

    public TextView mFriendListEmail;
    public TextView mFriendListUserId;
    public ImageView mFriendListProfilePic;
    public ImageView mRemoveFriend;
    public List<HashMap<String, String>> friendsListObject;

    public ProgressBar progressBar;

    public FriendListAdapterViewHolder(final View itemView, final List<HashMap<String, String>> friendsListObject) {
        super(itemView);
        this.friendsListObject = friendsListObject;
        mFriendListEmail = (TextView) itemView.findViewById(R.id.friend_list_profile_name);
        mFriendListUserId = (TextView) itemView.findViewById(R.id.friend_list_profile_id);
        mFriendListProfilePic = (ImageView) itemView.findViewById(R.id.friend_list_profile_pic);
        mRemoveFriend = (ImageView) itemView.findViewById(R.id.remove_friend);
        progressBar = (ProgressBar) itemView.findViewById(R.id.friend_list_item_progress_bar);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(v, getAdapterPosition());
            }
        });

        mRemoveFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(v, getAdapterPosition());
            }
        });

        Log.d(TAG, "LIST ADAPTER INSTANTIATED");

    }

    private FriendListAdapterViewHolder.ClickListener mClickListener;

    public interface ClickListener {
        void onItemClick(View view, int position);
    }

    public void setOnClickListener(FriendListAdapterViewHolder.ClickListener clickListener){
        mClickListener = clickListener;
    }
}
