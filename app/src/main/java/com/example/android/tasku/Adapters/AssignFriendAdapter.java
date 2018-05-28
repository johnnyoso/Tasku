package com.example.android.tasku.Adapters;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.example.android.tasku.EditUserActivity;
import com.example.android.tasku.NewTaskActivity;
import com.example.android.tasku.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by john.osorio on 24/02/2018.
 */

public class AssignFriendAdapter extends RecyclerView.Adapter<AssignFriendAdapter.ViewHolder> {

    private static final String TAG = AssignFriendAdapter.class.getSimpleName();
    private List<HashMap<String, String>> friends;
    private SparseArray friendsArray = new SparseArray();
    private List<String> friendUidList = new ArrayList<>();
    private SparseBooleanArray checkboxState = new SparseBooleanArray();
    private Context context;

    private FirebaseStorage mStorage;
    private StorageReference userImageStorage;

    public AssignFriendAdapter(Context context, List<HashMap<String, String>> friends, SparseBooleanArray checkboxState) {
        this.friends = friends;
        this.checkboxState = checkboxState;
        this.context = context;

    }

    @Override
    public AssignFriendAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        AssignFriendAdapter.ViewHolder viewHolder = null;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.assign_friend_list_item, parent, false);
        viewHolder = new ViewHolder(view, friends);

        final CheckBox checkBox = viewHolder.assignFriendCheckbox;

        viewHolder.setOnClickListener(new ViewHolder.ClickListener() {
            @Override
            public void onItemClick(View view, final int position) {
//                Toast.makeText(context, "Item: " + position, Toast.LENGTH_SHORT).show();

            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final AssignFriendAdapter.ViewHolder holder, final int position) {

        //Turn on the progress bar
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.assignFriendImageView.setVisibility(View.GONE);

        boolean isChecked = checkboxState.get(position);

        final HashMap<String, String> friendMap = friends.get(position);
        for(Map.Entry entry : friendMap.entrySet()){

            final String friendUid = entry.getKey().toString();
            String friendEmail = entry.getValue().toString();

            holder.assignFriendProfileId.setText(friendUid);
            holder.assignFriendProfileName.setText(friendEmail);

            mStorage = FirebaseStorage.getInstance();
            userImageStorage = mStorage.getReference().child(context.getString(R.string.images) + friendUid + context.getString(R.string.png));
            userImageStorage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Log.d(TAG, "USER PROFILE PIC URL IS: " + uri);

                    Glide.with(context.getApplicationContext()).load(uri).asBitmap().centerCrop().error(R.drawable.assigned_user_icon).into(new BitmapImageViewTarget(holder.assignFriendImageView) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            holder.assignFriendImageView.setImageDrawable(circularBitmapDrawable);
                        }
                    });
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    //Turn off the progress bar
                    holder.progressBar.setVisibility(View.GONE);
                    holder.assignFriendImageView.setVisibility(View.VISIBLE);
                }
            });

            holder.assignFriendCheckbox.setChecked(isChecked);

            if(isChecked) {
                Log.d(TAG, "Checkbox is ticked?: " + holder.assignFriendCheckbox.isChecked());
                //TODO: check if this friend is already in the friendsArray
                if(friendsArray.get(position) == null) {
                    friendsArray.put(position, friendUid);
                }
                friendUidList.add(friendUid);

            }

            holder.assignFriendCheckbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(holder.assignFriendCheckbox.isChecked()) {
                        //TODO: check if this friend is already in the friendsArray
                        if(friendsArray.get(position) == null) {
                            friendsArray.put(position, friendUid);
                        }

                        //TODO: add the checkbox state in the checkbox statearray
                        checkboxState.put(position, true);

                    } else {
                        if(friendsArray.get(position) != null) {
                            friendsArray.remove(position);
                        }
                        //TODO: add the checkbox state in the checkbox statearray
                        checkboxState.put(position, false);
                    }
                }
            });

        }

    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public SparseBooleanArray getCheckboxState() {
        return checkboxState;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private List<HashMap<String, String>> friendsList;
        private CheckBox assignFriendCheckbox;
        private ImageView assignFriendImageView;
        private TextView assignFriendProfileName;
        private TextView assignFriendProfileId;
        private ProgressBar progressBar;

        public ViewHolder(View itemView, List<HashMap<String, String>> friendsList) {
            super(itemView);
            this.friendsList = friendsList;
            assignFriendCheckbox = (CheckBox) itemView.findViewById(R.id.assign_friend_checkbox);
            assignFriendImageView = (ImageView) itemView.findViewById(R.id.assign_friend_list_profile_pic);
            assignFriendProfileName = (TextView) itemView.findViewById(R.id.assign_friend_list_profile_name);
            assignFriendProfileId = (TextView) itemView.findViewById(R.id.assign_friend_list_profile_id);
            progressBar = (ProgressBar) itemView.findViewById(R.id.assign_friend_list_avatar_progress_bar);

            itemView.setOnClickListener(new View.OnClickListener() {
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
