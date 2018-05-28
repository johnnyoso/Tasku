package com.example.android.tasku.Fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.example.android.tasku.Adapters.FriendAvatarAdapter;
import com.example.android.tasku.Adapters.TaskMessagesAdapter;
import com.example.android.tasku.EditTaskActivity;
import com.example.android.tasku.MyTaskActivity;
import com.example.android.tasku.NewTaskActivity;
import com.example.android.tasku.R;
import com.example.android.tasku.SettingsActivity;
import com.example.android.tasku.SignInActivity;
import com.example.android.tasku.TaskDescriptionActivity;
import com.example.android.tasku.fjd.JobDispatcherUtils;
import com.google.android.gms.tasks.OnCompleteListener;
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
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Created by john.osorio on 10/05/2018.
 */

public class TaskDescriptionFragment extends Fragment {

    private static final String TAG = TaskDescriptionActivity.class.getSimpleName();

    private Bundle mTaskDescriptionBundle;

    @BindView(R.id.current_task_name_fragment) TextView currentTaskName;
    @BindView(R.id.current_task_description_fragment) TextView currentTaskDescription;
    @BindView(R.id.task_creator_avatar_fragment) ImageView currentTaskCreatorAvatar;
    @BindView(R.id.current_task_message_send_fragment) ImageView currentTaskMessageSend;
    @BindView(R.id.assignee_avatar_recyclerview_fragment) RecyclerView avatarRecyclerView;
    @BindView(R.id.task_feed_recyclerview_fragment) RecyclerView taskFeedRecyclerView;
    @BindView(R.id.current_task_message_fragment) EditText currentTaskMessage;
    @BindView(R.id.task_creator_avatar_fragment_progress_bar) ProgressBar progressBar;
    private Unbinder unbinder;

    private String taskKey;
    private String taskName;
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

    public TaskDescriptionFragment(){}

    public void setTaskDescriptionBundle(Bundle TaskDescriptionBundle){
        mTaskDescriptionBundle = TaskDescriptionBundle;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "TASK DESCRIPTION FRAGMENT ATTACHED");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_task_description, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        currentTaskMessageSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendTaskMessage();
            }
        });

        if(mTaskDescriptionBundle != null) {

            taskKey = mTaskDescriptionBundle.getString(getString(R.string.taskKey));
            taskName = mTaskDescriptionBundle.getString(getString(R.string.taskName));
        }

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        mDatabase = FirebaseDatabase.getInstance();
        taskRef = mDatabase.getReference(getString(R.string.tasks));
        userRef = mDatabase.getReference(getString(R.string.users));
        mStorage = FirebaseStorage.getInstance();

        getHighestTaskMessageIndex();

        jobDispatcherUtils = new JobDispatcherUtils(getActivity(), taskKey, taskName, reminderFrequency);

        if(isAdded()) {

            //************************************************
            //This populates the task details
            taskRef.child(taskKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for (DataSnapshot snap : dataSnapshot.getChildren()) {
                        if (snap.getKey().equals(getString(R.string.taskCreator))) {
                            //Set the task creator's avatar
                            String taskCreator = snap.getValue().toString();
                            Log.d(TAG, "TASK CREATOR IS: " + taskCreator);
                            userImageRef = mStorage.getReference().child(getString(R.string.images) + taskCreator + getString(R.string.png));

                            userImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {

                                @Override
                                public void onSuccess(Uri uri) {
                                    //Show the progress bar
                                    progressBar.setVisibility(View.VISIBLE);
                                    currentTaskCreatorAvatar.setVisibility(View.GONE);

                                    setCreatorAvatar(uri);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "TASK CREATOR doesn't exist anymore");
                                }
                            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {

                                    //Hide the progress bar
                                    progressBar.setVisibility(View.GONE);
                                    currentTaskCreatorAvatar.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                        if (snap.getKey().equals(getString(R.string.taskDescription))) {
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
        }

        //************************************************
        //This populates the assignee avatars
        setFriendAssigneeAvatar();

        showTaskMessages();

        return rootView;
    }


    /**
     * Get the task creator's avatar
     * @param uri
     */
    private void setCreatorAvatar(Uri uri) {


        Glide.with(this)
                .load(uri)
                .asBitmap()
                .centerCrop()
                .error(R.drawable.assigned_user_icon)
                .into(new BitmapImageViewTarget(currentTaskCreatorAvatar) {

                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        currentTaskCreatorAvatar.setImageDrawable(circularBitmapDrawable);
                    }
                });
    }

    /**
     * Populate the assignee avatar recyclerview field
     */
    private void setFriendAssigneeAvatar() {
        friendAvatarUidList = new ArrayList<>();
        friendAvatarLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        avatarRecyclerView.setLayoutManager(friendAvatarLayoutManager);

        taskRef.child(taskKey).child(getString(R.string.task_assignee)).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                String friendAvatarUid = dataSnapshot.getKey().toString();
                friendAvatarUidList.add(friendAvatarUid);
//                Log.d(TAG, "FRIEND AVATAR UID: " + friendAvatarUid);
                friendAvatarAdapter = new FriendAvatarAdapter(getActivity(), friendAvatarUidList);
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
                    taskMessagesAdapter = new TaskMessagesAdapter(getActivity(), taskMessagesList);

                    taskMessagesLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
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

    public void sendTaskMessage() {
        int index = highestMessageIndex;
        String message = currentTaskMessage.getText().toString();

        if(message != null || !message.isEmpty()) {
            taskRef.child(taskKey).child(getString(R.string.task_messages)).child(String.valueOf(index)).child(user.getUid()).setValue(message);
            currentTaskMessage.setText("");

            //TODO: remove the soft keypad
            try{
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                Log.d(TAG, "Error: " + e.getMessage());
            }
            highestMessageIndex++;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuInflater menuInflater = getActivity().getMenuInflater();
        menuInflater.inflate(R.menu.task_description_menu_600dp_land, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {

        taskRef.child(taskKey).child(getString(R.string.taskStatus)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String taskStatus = dataSnapshot.getValue().toString();
                if(taskStatus.equals(getString(R.string.close))) {

                    menu.findItem(R.id.task_description_600dp_land_menu_close_task).setVisible(false);
                    menu.findItem(R.id.task_description_600dp_land_menu_edit_task).setVisible(false);
                    menu.findItem(R.id.task_description_600dp_land_menu_reopen_task).setVisible(true);
                    menu.findItem(R.id.task_description_600dp_land_menu_delete_task).setVisible(true);

                } else if(taskStatus.equals(getString(R.string.open))) {
                    menu.findItem(R.id.task_description_600dp_land_menu_reopen_task).setVisible(false);
                    menu.findItem(R.id.task_description_600dp_land_menu_delete_task).setVisible(false);
                    menu.findItem(R.id.task_description_600dp_land_menu_close_task).setVisible(true);
                    menu.findItem(R.id.task_description_600dp_land_menu_edit_task).setVisible(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch(id) {

            case R.id.task_description_600dp_land_menu_edit_task:
                Intent editTaskIntent = new Intent(getActivity(), EditTaskActivity.class);
                Bundle editTaskBundle = new Bundle();
                editTaskBundle.putString(getString(R.string.taskKey), taskKey);
                editTaskIntent.putExtras(editTaskBundle);
                startActivity(editTaskIntent);

                break;

            case R.id.task_description_600dp_land_menu_close_task:

                //set the task status to close but don't delete it yet
                taskRef.child(taskKey).child(getString(R.string.taskStatus)).setValue("close");

                //set the user's task to close
                taskRef.child(taskKey).child(getString(R.string.taskAssignee)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snap : dataSnapshot.getChildren()) {
                            String taskAssigneeUserId = snap.getKey();

                            userRef.child(taskAssigneeUserId).child(getString(R.string.tasks)).child(taskKey).setValue("close");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //Cancel the firebase job dispatcher
                jobDispatcherUtils.cancelJob(taskKey);

                //Then go back to My task activity and remove task from position
                Intent toMyTaskActivityClosed = new Intent(getActivity(), MyTaskActivity.class);
//                Bundle toMyTaskActivityBundle = new Bundle();
//                toMyTaskActivityBundle.putInt("adapterPosition", adapterPosition);
//                toMyTaskActivityClosed.putExtras(toMyTaskActivityBundle);
                startActivity(toMyTaskActivityClosed);

                break;

            case R.id.task_description_600dp_land_menu_reopen_task:

                //set the task status to close but don't delete it yet
                taskRef.child(taskKey).child(getString(R.string.taskStatus)).setValue(getString(R.string.open));

                //set the user's task to close
                taskRef.child(taskKey).child(getString(R.string.taskAssignee)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snap : dataSnapshot.getChildren()) {
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
                Intent toMyTaskActivityReOpen = new Intent(getActivity(), MyTaskActivity.class);
                startActivity(toMyTaskActivityReOpen);

                break;

            case R.id.task_description_600dp_land_menu_delete_task:

                //remove the user's task
                taskRef.child(taskKey).child(getString(R.string.taskAssignee)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snap : dataSnapshot.getChildren()) {
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
                        Toast.makeText(getActivity(), getString(R.string.task_deleted), Toast.LENGTH_SHORT).show();
                    }
                });

                //Then finally go back to the open task page
                Intent backToTaskList = new Intent(getActivity(), MyTaskActivity.class);
                startActivity(backToTaskList);

                break;

            case R.id.task_activity_600dp_land_menu_create_new_task:

                //Create a new task by heading to NewTaskActivity
                Intent newTaskIntent = new Intent(getActivity(), NewTaskActivity.class);
                startActivity(newTaskIntent);

                break;

            case R.id.task_activity_600dp_land_menu_settings_page:

                Intent editUserProfileIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(editUserProfileIntent);

                break;

            case R.id.task_activity_600dp_land_menu_sign_out_user:

                //Sign out the current user and go back to login page (Main Activity)
                mAuth.signOut();

                //Unsubscribe from the topic
                FirebaseMessaging.getInstance().unsubscribeFromTopic(getString(R.string.new_task_notifications));
                FirebaseMessaging.getInstance().unsubscribeFromTopic(getString(R.string.new_friend_request_notifications));

                Intent signOutIntent = new Intent(getActivity(), SignInActivity.class);
                startActivity(signOutIntent);

                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);

    }
}
