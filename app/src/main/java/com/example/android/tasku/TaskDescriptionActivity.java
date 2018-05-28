package com.example.android.tasku;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.example.android.tasku.Adapters.FriendAvatarAdapter;
import com.example.android.tasku.Adapters.TaskAdapter;
import com.example.android.tasku.Adapters.TaskMessagesAdapter;
import com.example.android.tasku.JavaUtils.AvatarImagesUtils;
import com.example.android.tasku.JavaUtils.TaskUtils;
import com.example.android.tasku.JavaUtils.UserUtils;
import com.example.android.tasku.fjd.JobDispatcherUtils;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by john.osorio on 31/08/2017.
 */

public class TaskDescriptionActivity extends AppCompatActivity {

    private static final String TAG = TaskDescriptionActivity.class.getSimpleName();

    @BindView(R.id.my_toolbar) Toolbar mToolbar;

    @BindView(R.id.current_task_name) TextView currentTaskName;
    @BindView(R.id.current_task_description) TextView currentTaskDescription;
    @BindView(R.id.task_creator_avatar) ImageView currentTaskCreatorAvatar;
    @BindView(R.id.current_task_message_send) ImageView currentTaskMessageSend;
    @BindView(R.id.assignee_avatar_recyclerview) RecyclerView avatarRecyclerView;
    @BindView(R.id.task_feed_recyclerview) RecyclerView taskFeedRecyclerView;
    @BindView(R.id.current_task_message) EditText currentTaskMessage;
    @BindView(R.id.my_toolbar_title) TextView myToolbarTitle;
    @BindView(R.id.task_creator_avatar_progress_bar) ProgressBar progressBar;

    private String taskKey;
    private String taskName;
    private int adapterPosition;
    private int highestMessageIndex = 0;
    private int reminderFrequency = 0;

    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference taskRef;
    private DatabaseReference userRef;
    private FirebaseStorage mStorage;
    private StorageReference userImageRef;

    private FriendAvatarAdapter friendAvatarAdapter;
    private List<String> friendAvatarUidList;
    private LinearLayoutManager friendAvatarLayoutManager;

    private TaskMessagesAdapter taskMessagesAdapter;
    private List<HashMap<String, String>> taskMessagesList = new ArrayList<>();
    private LinearLayoutManager taskMessagesLayoutManager;

    private JobDispatcherUtils jobDispatcherUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_description);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        myToolbarTitle.setText(getString(R.string.task_description_toolbar_title));

        Intent fromMyTaskActivityIntent = getIntent();
        Bundle fromMyTaskActivityBundle = fromMyTaskActivityIntent.getExtras();

        if(fromMyTaskActivityBundle != null) {

            taskKey = fromMyTaskActivityBundle.getString(getString(R.string.task_key));
            taskName = fromMyTaskActivityBundle.getString(getString(R.string.task_name));
            adapterPosition = fromMyTaskActivityBundle.getInt(getString(R.string.adapter_position));
            currentTaskName.setText(taskName);
        }

        if(findViewById(R.id.task_description_600dp_portrait) != null) {
            //Check the orientation first if landscape
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

                //go back to My Task Activity
                Intent toMyTaskActivity = new Intent(this, MyTaskActivity.class);
                Bundle toMyTaskActivityBundle = new Bundle();
                toMyTaskActivityBundle.putString(getString(R.string.taskKey), taskKey);
                toMyTaskActivityBundle.putString(getString(R.string.taskName), taskName);
                toMyTaskActivityBundle.putInt(getString(R.string.adapter_position), adapterPosition);
                toMyTaskActivity.putExtras(toMyTaskActivityBundle);
                startActivity(toMyTaskActivity);
            }
        }

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        mDatabase = FirebaseDatabase.getInstance();
        taskRef = mDatabase.getReference(getString(R.string.tasks));
        userRef = mDatabase.getReference(getString(R.string.users));
        mStorage = FirebaseStorage.getInstance();

        getHighestTaskMessageIndex();

        jobDispatcherUtils = new JobDispatcherUtils(this, taskKey, taskName, reminderFrequency);

        //************************************************
        //This populates the task details
        taskRef.child(taskKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot snap : dataSnapshot.getChildren()) {
                    if(snap.getKey().equals(getString(R.string.taskCreator))){
                        //Set the task creator's avatar
                        String taskCreator = snap.getValue().toString();
                        Log.d(TAG, "TASK CREATOR IS: " + taskCreator);

                        try {
                            AvatarImagesUtils avatarImagesUtils = new AvatarImagesUtils(TaskDescriptionActivity.this, taskCreator, currentTaskCreatorAvatar, progressBar);
                            avatarImagesUtils.execute();
//                            showUserAvatar(taskCreator);

                        } catch (Exception e) {
                            if(e instanceof IOException) {
                                Log.d(TAG, "Storage error: " + e.getMessage());
                            }
                        }
                    }
                    if (snap.getKey().equals(getString(R.string.taskDescription))){
                        Log.d(TAG, "TASK DESCRIPTION IS: " + snap.getValue().toString());
                        String taskDescription = snap.getValue().toString();
                        currentTaskDescription.setText(taskDescription);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //************************************************
        //This populates the assignee avatars
        setFriendAssigneeAvatar();

        showTaskMessages();

    }


    /**
     * Populate the assignee avatar recyclerview field
     */
    private void setFriendAssigneeAvatar() {
        friendAvatarUidList = new ArrayList<>();
        friendAvatarLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        avatarRecyclerView.setLayoutManager(friendAvatarLayoutManager);

        taskRef.child(taskKey).child(getString(R.string.task_assignee)).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                String friendAvatarUid = dataSnapshot.getKey().toString();
                friendAvatarUidList.add(friendAvatarUid);
//                Log.d(TAG, "FRIEND AVATAR UID: " + friendAvatarUid);
                friendAvatarAdapter = new FriendAvatarAdapter(TaskDescriptionActivity.this, friendAvatarUidList);
                avatarRecyclerView.setAdapter(friendAvatarAdapter);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * This makes sure that messages stored in Firebase database are indexed chronologically and not alphabetically
     */
    private void getHighestTaskMessageIndex() {
        taskRef.child(taskKey).child(getString(R.string.task_messages)).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    for(DataSnapshot snap : dataSnapshot.getChildren()) {
                        String index = snap.getKey().toString();
                        highestMessageIndex = Integer.valueOf(index) + 1;
//                        Log.d(TAG, "Highest message index is: " + index);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void showTaskMessages() {
        taskRef.child(taskKey).child(getString(R.string.task_messages)).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                for(DataSnapshot snap : dataSnapshot.getChildren()) {
                    Log.d(TAG, "TASK KEY: " + snap.getKey() + " TASK MESSAGES: " + snap.getValue());
                    HashMap<String, String> taskMessageMap = new HashMap<>();
                    String taskCreatorUid = snap.getKey().toString();
                    String taskMessage = snap.getValue().toString();
                    taskMessageMap.put(taskCreatorUid, taskMessage);

                    taskMessagesList.add(taskMessageMap);
                    taskMessagesAdapter = new TaskMessagesAdapter(TaskDescriptionActivity.this, taskMessagesList);

                    taskMessagesLayoutManager = new LinearLayoutManager(TaskDescriptionActivity.this, LinearLayoutManager.VERTICAL, false);
                    taskFeedRecyclerView.setLayoutManager(taskMessagesLayoutManager);
                    taskFeedRecyclerView.setAdapter(taskMessagesAdapter);
                }


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void sendTaskMessage(View view) {
        int index = highestMessageIndex;
        String message = currentTaskMessage.getText().toString();

        if(message != null || !message.isEmpty()) {
            taskRef.child(taskKey).child(getString(R.string.task_messages)).child(String.valueOf(index)).child(user.getUid()).setValue(message);
            currentTaskMessage.setText("");

            //TODO: remove the soft keypad
           try{
               InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
               imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
           } catch (Exception e) {
               Log.d(TAG, "Error: " + e.getMessage());
           }
            highestMessageIndex++;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {


        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.task_description_menu, menu);

       return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {

        //Find out first if the task description is a closed or open task
        Log.d(TAG, "TASK KEY IS: " + taskKey);
        taskRef.child(taskKey).child(getString(R.string.taskStatus)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String taskStatus = dataSnapshot.getValue().toString();
                if(taskStatus.equals(getString(R.string.close))) {

                    menu.findItem(R.id.task_description_menu_close_task).setVisible(false);
                    menu.findItem(R.id.task_description_menu_edit_task).setVisible(false);
                    menu.findItem(R.id.task_description_menu_reopen_task).setVisible(true);
                    menu.findItem(R.id.task_description_menu_delete_task).setVisible(true);

                } else if(taskStatus.equals(getString(R.string.open))) {
                    menu.findItem(R.id.task_description_menu_reopen_task).setVisible(false);
                    menu.findItem(R.id.task_description_menu_delete_task).setVisible(false);
                    menu.findItem(R.id.task_description_menu_close_task).setVisible(true);
                    menu.findItem(R.id.task_description_menu_edit_task).setVisible(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {

            case R.id.task_description_menu_edit_task:
                Intent editTaskIntent = new Intent(this, EditTaskActivity.class);
                Bundle editTaskBundle = new Bundle();
                editTaskBundle.putString(getString(R.string.taskKey), taskKey);
                editTaskIntent.putExtras(editTaskBundle);
                startActivity(editTaskIntent);

                break;

            case R.id.task_description_menu_close_task:

                //set the task status to close but don't delete it yet
                taskRef.child(taskKey).child(getString(R.string.taskStatus)).setValue(getString(R.string.close));

                //set the user's task to close
                taskRef.child(taskKey).child(getString(R.string.taskAssignee)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot snap : dataSnapshot.getChildren()){
                            String taskAssigneeUserId = snap.getKey();

                            userRef.child(taskAssigneeUserId).child(getString(R.string.tasks)).child(taskKey).setValue(getString(R.string.close));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //Cancel the firebase job dispatcher
                jobDispatcherUtils.cancelJob(taskKey);

                //Then go back to My task activity and remove task from position
                Intent toMyTaskActivityClosed = new Intent(this, MyTaskActivity.class);
//                Bundle toMyTaskActivityBundle = new Bundle();
//                toMyTaskActivityBundle.putInt("adapterPosition", adapterPosition);
//                toMyTaskActivityClosed.putExtras(toMyTaskActivityBundle);
                startActivity(toMyTaskActivityClosed);

                break;

            case R.id.task_description_menu_reopen_task:

                //set the task status to close but don't delete it yet
                taskRef.child(taskKey).child(getString(R.string.taskStatus)).setValue(getString(R.string.open));

                //set the user's task to close
                taskRef.child(taskKey).child(getString(R.string.taskAssignee)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot snap : dataSnapshot.getChildren()){
                            String taskAssigneeUserId = snap.getKey();

                            userRef.child(taskAssigneeUserId).child(getString(R.string.tasks)).child(taskKey).setValue(getString(R.string.open));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //Re-open the firebase job dispatcher
                jobDispatcherUtils.scheduleJob();

                //Go back to my task list
                Intent toMyTaskActivityReOpen = new Intent(this, MyTaskActivity.class);
                startActivity(toMyTaskActivityReOpen);

                break;

            case R.id.task_description_menu_delete_task:

                //remove the user's task
                taskRef.child(taskKey).child(getString(R.string.taskAssignee)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot snap : dataSnapshot.getChildren()){
                            String taskAssigneeUserId = snap.getKey();

                            //delete the task from the user's task list
                            userRef.child(taskAssigneeUserId).child(getString(R.string.tasks)).child(taskKey).removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //Delete the task from the task database
                taskRef.child(taskKey).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(TaskDescriptionActivity.this, getString(R.string.task_deleted), Toast.LENGTH_SHORT).show();
                    }
                });

                //Then finally go back to the open task page
                Intent backToTaskList = new Intent(this, MyTaskActivity.class);
                startActivity(backToTaskList);

                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

}
