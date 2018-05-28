package com.example.android.tasku.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.tasku.R;

import java.util.HashMap;
import java.util.List;

/**
 * Created by john.osorio on 13/02/2018.
 */

public class FriendRequestAdapterViewHolder extends RecyclerView.ViewHolder {

    public TextView mFriendEmail;
    public TextView mFriendUserId;
    public ImageView mFriendProfilePic;
    public ImageView mRejectRequest;
    public ImageView mApproveRequest;
    public List<HashMap<String, String>> friendsObject;
    public ProgressBar progressBar;

    public FriendRequestAdapterViewHolder(final View itemView, final List<HashMap<String, String>> friendsObject) {
        super(itemView);
        this.friendsObject = friendsObject;
        mFriendEmail = (TextView) itemView.findViewById(R.id.friend_profile_name);
        mFriendUserId = (TextView) itemView.findViewById(R.id.friend_profile_id);
        mFriendProfilePic = (ImageView) itemView.findViewById(R.id.friend_profile_pic);
        mRejectRequest = (ImageView) itemView.findViewById(R.id.reject_friend_request);
        mApproveRequest = (ImageView) itemView.findViewById(R.id.approve_friend_request);
        progressBar = (ProgressBar) itemView.findViewById(R.id.friend_request_list_avatar_progress_bar);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(v, getAdapterPosition());
            }
        });

        mApproveRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(v, getAdapterPosition());
            }
        });

        mRejectRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(v, getAdapterPosition());
            }
        });

    }

    private FriendRequestAdapterViewHolder.ClickListener mClickListener;

    public interface ClickListener {
        void onItemClick(View view, int position);
    }

    public void setOnClickListener(FriendRequestAdapterViewHolder.ClickListener clickListener){
        mClickListener = clickListener;
    }

}
