package com.example.android.tasku;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.tasku.Adapters.AssignFriendAdapter;
import com.example.android.tasku.Adapters.FriendAvatarAdapter;
import com.example.android.tasku.JavaUtils.SparseBooleanArrayParcelable;
import com.example.android.tasku.fjd.JobDispatcherUtils;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by john.osorio on 12/04/2018.
 */

public class EditTaskActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = EditTaskActivity.class.getSimpleName();

    @BindView(R.id.my_toolbar_title) TextView myToolbarTitle;
    @BindView(R.id.edit_task_task_name) EditText mTaskName;
    @BindView(R.id.edit_task_task_description) EditText mTaskDescription;
    @BindView(R.id.edit_task_assign_to_textview) TextView mAssignToTextView;
    @BindView(R.id.edit_task_reminder_frequency) Spinner mReminderFrequencySpinner;
    @BindView(R.id.edit_task_user_avatar_recyclerview) RecyclerView mUserAvatarRecyclerView;
    @BindView(R.id.update_task_button) Button mUpdateTaskButton;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseDatabase mDatabase;
    private DatabaseReference taskRef;
    private DatabaseReference userRef;
    private DatabaseReference mRef;

    private String taskKey;
    private String taskName;
    private String taskDescription;
    private String taskReminder;

    //Assign friend dialog
    private SparseBooleanArray checkboxState = new SparseBooleanArray();
    private RecyclerView.LayoutManager mLayoutManager;
    private AssignFriendAdapter assignFriendAdapter;
    private AlertDialog friendsAlertDialog;

    //Show friend avatar recyclerView
    private LinearLayoutManager layoutManager;
    private FriendAvatarAdapter friendAvatarAdapter;
    private ArrayList<String> friendAvatarListSaved = new ArrayList<String>();

    //Task reminder frequency
    private List<String> reminderFrequencyList = new ArrayList<>();
    private int[] reminderFrequencyDuration = new int[]{3600, 86400, 604800};
    private int reminderFrequency = 3600; //default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_edit);

        ButterKnife.bind(this);
        myToolbarTitle.setText(getString(R.string.edit_task_toolbar_title));


        //This won't let the keypad come out immediately on start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mDatabase = FirebaseDatabase.getInstance();
        taskRef = mDatabase.getReference(getString(R.string.tasks));
        userRef = mDatabase.getReference(getString(R.string.users));

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mUserAvatarRecyclerView.setLayoutManager(layoutManager);

        if(savedInstanceState != null) {

            Log.d(TAG, "Saved instance has something!!");
            if (savedInstanceState.getBoolean(getString(R.string.friend_alert_dialog_shown), false)) {
                SparseBooleanArray checkboxStatus = (SparseBooleanArray) savedInstanceState.getParcelable(getString(R.string.checkbox_state_instance));
                showFriendsDialog(checkboxStatus);
            }
            if (savedInstanceState.getStringArrayList(getString(R.string.friend_avatar_list)) != null) {
                ArrayList<String> savedFriendAvatarList = savedInstanceState.getStringArrayList(getString(R.string.friend_avatar_list));
                showFriendAvatars(savedFriendAvatarList);
            }

            mTaskName.setText(savedInstanceState.getString(getString(R.string.taskName)));
            mTaskDescription.setText(savedInstanceState.getString(getString(R.string.taskDescription)));
            reminderFrequency = savedInstanceState.getInt(getString(R.string.task_reminder));

        } else {

            Intent fromTaskDescriptionIntent = getIntent();
            if(fromTaskDescriptionIntent != null) {

                Bundle fromTaskDescriptionBundle = fromTaskDescriptionIntent.getExtras();
                taskKey = fromTaskDescriptionBundle.getString(getString(R.string.taskKey));
            }

            //Populate all the edit texts
            taskRef.child(taskKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot snap : dataSnapshot.getChildren()) {
                        if(snap.getKey().equals(getString(R.string.taskName))) {
                            taskName = snap.getValue().toString();
                            mTaskName.setText(taskName);

                        } if(snap.getKey().equals(getString(R.string.taskDescription))) {
                            taskDescription = snap.getValue().toString();
                            mTaskDescription.setText(taskDescription);

                        } if(snap.getKey().equals(getString(R.string.task_reminder))) {
                            taskReminder = snap.getValue().toString();
                            reminderFrequency = Integer.valueOf(taskReminder);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            //Get all the task assignees and populate the list
            taskRef.child(taskKey).child(getString(R.string.taskAssignee)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for(DataSnapshot snap : dataSnapshot.getChildren()){
                        String taskAssigneeUid = snap.getKey();
                        friendAvatarListSaved.add(taskAssigneeUid);
                        friendAvatarAdapter = new FriendAvatarAdapter(EditTaskActivity.this, friendAvatarListSaved);
                        mUserAvatarRecyclerView.setAdapter(friendAvatarAdapter);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            mAssignToTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //Show the friends dialog box
                    showFriendsDialog(checkboxState);
                }
            });
        }

        mReminderFrequencySpinner.setOnItemSelectedListener(this);
        reminderFrequencyList.add("1 hour");
        reminderFrequencyList.add("daily");
        reminderFrequencyList.add("weekly");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, reminderFrequencyList);

        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        mReminderFrequencySpinner.setAdapter(arrayAdapter);

        mUpdateTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTask();
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String item = parent.getItemAtPosition(position).toString();
        Toast.makeText(this, getString(R.string.reminder_is) + item, Toast.LENGTH_SHORT).show();
        reminderFrequency = reminderFrequencyDuration[position];
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        reminderFrequency = 3600;
    }


    /**
     * This shows all check list of all friends you can assign the task to
     */
    private void showFriendsDialog(final SparseBooleanArray checkboxStatus) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View friendListDialogView = inflater.inflate(R.layout.custom_dialog_friends_list, null);
        dialogBuilder.setView(friendListDialogView);

        dialogBuilder.setTitle("Please select friends");
        dialogBuilder.setMessage("Assign to: ");

        //Initialise the recyclerview here
        final RecyclerView assignUserRecyclerView = (RecyclerView) friendListDialogView.findViewById(R.id.assign_friend_recyclerview);
        mLayoutManager = new LinearLayoutManager(this);
        assignUserRecyclerView.setLayoutManager(mLayoutManager);
        final List<HashMap<String, String>> friendList = new ArrayList<>();

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference(getString(R.string.users));

        checkboxState = checkboxStatus;

        //**************************************************************************
        // Need this to be able to include the current user in the assignments list
        final HashMap<String, String> currentUserMap = new HashMap<>();
        currentUserMap.put(user.getUid(), user.getEmail());
        friendList.add(currentUserMap);
        //**************************************************************************

        mRef.child(user.getUid()).child(getString(R.string.friends)).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final String friendUserId = dataSnapshot.getKey();
                Boolean friendUserStatus = (Boolean) dataSnapshot.getValue();

                //Make sure that the friend user status is TRUE which means invite was approved already
                mRef.child(friendUserId).child(getString(R.string.emailAddress)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String friendEmailAdd = (String) dataSnapshot.getValue();
                        final HashMap<String, String> friendMap = new HashMap<>();
                        friendMap.put(friendUserId, friendEmailAdd);

                        friendList.add(friendMap);

                        assignFriendAdapter = new AssignFriendAdapter(EditTaskActivity.this, friendList, checkboxStatus);
                        assignUserRecyclerView.setAdapter(assignFriendAdapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

                Log.d(TAG,"CHILD REMOVED");
                assignFriendAdapter.notifyDataSetChanged();

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //set the listener to null so the dialog won't close upon clicking the button
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                checkboxState = assignFriendAdapter.getCheckboxState();
                ArrayList<String> friendAvatarList = new ArrayList<>();

                for(int k = 0; k < assignFriendAdapter.getItemCount(); k++) {
                    Log.d(TAG, "FRIEND CHECKED?: " + checkboxState.get(k));
                    if(checkboxState.get(k)){
                        for(Map.Entry entry : friendList.get(k).entrySet()) {
                            String userID = entry.getKey().toString();
                            friendAvatarList.add(userID);
                        }
                    }
                }

                showFriendAvatars(friendAvatarList);
            }
        });

        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
            }
        });

        friendsAlertDialog = dialogBuilder.create();

        friendsAlertDialog.show();
    }

    /**
     * This will show the selected friends for the given task
     * @param friendAvatarList
     */
    private void showFriendAvatars(ArrayList<String> friendAvatarList) {
        friendAvatarListSaved = friendAvatarList;

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mUserAvatarRecyclerView.setLayoutManager(layoutManager);
        friendAvatarAdapter = new FriendAvatarAdapter(this, friendAvatarList);
        mUserAvatarRecyclerView.setAdapter(friendAvatarAdapter);
        friendAvatarAdapter.notifyDataSetChanged();
        mUserAvatarRecyclerView.invalidate();
    }

    private void updateTask() {

        //TODO: Firstly, delete this task in all the previous assignees!!
        taskRef.child(taskKey).child(getString(R.string.taskAssignee)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snap : dataSnapshot.getChildren()) {
                    String assigneeUid = snap.getKey();

                    //Remove the task from the user's list
                    userRef.child(assigneeUid).child(getString(R.string.tasks)).child(taskKey).removeValue();
                    Log.d(TAG, "Task key removed! "+ taskKey);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //Create a new task Key
        String newTaskKey = taskRef.push().getKey();

        List<String> firebaseFriendAvatarList = friendAvatarListSaved;
        String taskStatus = getString(R.string.open);
        String taskName = mTaskName.getText().toString();
        String taskDescription = mTaskDescription.getText().toString();

        taskRef.child(newTaskKey).child(getString(R.string.taskName)).setValue(taskName);
        taskRef.child(newTaskKey).child(getString(R.string.taskDescription)).setValue(taskDescription);
        taskRef.child(newTaskKey).child(getString(R.string.taskKey)).setValue(newTaskKey);
        taskRef.child(newTaskKey).child(getString(R.string.taskCreator)).setValue(user.getUid());
        taskRef.child(newTaskKey).child(getString(R.string.task_reminder)).setValue(reminderFrequency);
        taskRef.child(newTaskKey).child(getString(R.string.taskStatus)).setValue(getString(R.string.open));

        //Then re-assign the task to the new assignees
        for(String item : firebaseFriendAvatarList) {
            taskRef.child(newTaskKey).child(getString(R.string.taskAssignee)).child(item).setValue(true);

            //Put the task under the assignee as well but CREATE A NEW TASK KEY

            userRef.child(item).child(getString(R.string.tasks)).child(newTaskKey).setValue(taskStatus);
            Log.d(TAG,"Updated Task assigned to: " + item);
        }

        //Cancel the previous Job Dispatcher
        JobDispatcherUtils jobDispatcherUtils = new JobDispatcherUtils(this, taskKey, taskName, reminderFrequency);
        jobDispatcherUtils.cancelJob(taskKey);

        //Re-schedule that same job again
        jobDispatcherUtils.scheduleJob();

        //Secondly, delete the whole task
        taskRef.child(taskKey).removeValue();

        //Go back to the task list
        Intent intent = new Intent(this, MyTaskActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (friendsAlertDialog != null && friendsAlertDialog.isShowing()) {

            //Get the checkbox state by saving the checkboxState Array
            SparseBooleanArray checkboxStateInstance = checkboxState;

            friendsAlertDialog.dismiss();

            outState.putBoolean(getString(R.string.friend_alert_dialog_shown), true);
            outState.putParcelable(getString(R.string.checkbox_state_instance), new SparseBooleanArrayParcelable(checkboxStateInstance));
        }
        ArrayList<String> savedFriendAvatarList = friendAvatarListSaved;
        outState.putStringArrayList(getString(R.string.friend_avatar_list), savedFriendAvatarList);

        String taskNameSaved = mTaskName.getText().toString();
        String taskDescriptionSaved = mTaskDescription.getText().toString();
        int reminderFrequencySaved = reminderFrequency;

        outState.putString(getString(R.string.taskName), taskNameSaved);
        outState.putString(getString(R.string.taskDescription), taskDescriptionSaved);
        outState.putInt(getString(R.string.task_reminder), reminderFrequencySaved);

    }
}
