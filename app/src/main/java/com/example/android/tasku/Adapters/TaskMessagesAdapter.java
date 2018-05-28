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
import android.widget.TextView;

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
import java.util.Map;

/**
 * Created by john.osorio on 27/03/2018.
 */

public class TaskMessagesAdapter extends RecyclerView.Adapter<TaskMessagesAdapter.ViewHolder> {

    Context context;
    List<HashMap<String, String>> taskMessagesList;
    private FirebaseStorage mStorage;
    private StorageReference taskStorageRef;

    public TaskMessagesAdapter(Context context, List<HashMap<String, String>> taskMessagesList) {
        this.context = context;
        this.taskMessagesList = taskMessagesList;
    }

    @Override
    public TaskMessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mStorage = FirebaseStorage.getInstance();

        ViewHolder viewHolder = null;
        View view = LayoutInflater.from(context).inflate(R.layout.task_messages_item, parent, false);
        viewHolder = new TaskMessagesAdapter.ViewHolder(view, taskMessagesList);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final TaskMessagesAdapter.ViewHolder holder, int position) {

        //Show the progress bar at the start
        holder.userAvatar.setVisibility(View.GONE);
        holder.progressBar.setVisibility(View.VISIBLE);

        HashMap<String, String> taskMessageMap = taskMessagesList.get(position);
        for(Map.Entry item : taskMessageMap.entrySet()) {
            String taskMessageKey = item.getKey().toString();
            String taskMessageValue = item.getValue().toString();

            taskStorageRef = mStorage.getReference().child(context.getString(R.string.images) + taskMessageKey + context.getString(R.string.png));

            holder.taskMessage.setText(taskMessageValue);
            taskStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {

                    Glide.with(context.getApplicationContext())
                            .load(uri)
                            .asBitmap()
                            .centerCrop()
                            .error(R.drawable.assigned_user_icon)
                            .into(new BitmapImageViewTarget(holder.userAvatar) {

                                @Override
                                protected void setResource(Bitmap resource) {
                                    RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                                    circularBitmapDrawable.setCircular(true);
                                    holder.userAvatar.setImageDrawable(circularBitmapDrawable);
                                }
                            });
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    //Show the avatar and remove the loading bar
                    holder.progressBar.setVisibility(View.GONE);
                    holder.userAvatar.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return taskMessagesList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView taskMessage;
        private ImageView userAvatar;
        private ProgressBar progressBar;

        public ViewHolder(View itemView, List<HashMap<String, String>> taskMessagesList) {
            super(itemView);

            taskMessage = (TextView) itemView.findViewById(R.id.task_message_content);
            userAvatar = (ImageView) itemView.findViewById(R.id.task_message_avatar);
            progressBar = (ProgressBar) itemView.findViewById(R.id.task_messages_avatar_progress_bar);
        }
    }
}
