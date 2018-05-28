package com.example.android.tasku;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.Toolbar;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by john.osorio on 17/08/2017.
 * This is where the user will create a new task or perhaps modify an existing one
 */
public class NewTaskActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private static final String TAG = NewTaskActivity.class.getSimpleName();

    @BindView(R.id.task_name) EditText mTaskName;
    @BindView(R.id.task_description) EditText mTaskDescription;
    @BindView(R.id.reminder_frequency) Spinner mReminderFrequencySpinner;
    @BindView(R.id.user_avatar_recyclerview) RecyclerView mUserAvatarRecyclerView;
    @BindView(R.id.create_button) Button mTaskCreateButton;
    @BindView(R.id.assign_to_textview) TextView mAssignUserTextView;
//    @BindView(R.id.my_toolbar) Toolbar mToolbar;
    @BindView(R.id.my_toolbar_title) TextView mToolbarTitle;

    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;

    private AssignFriendAdapter assignFriendAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private AlertDialog friendsAlertDialog;

    private SparseBooleanArray checkboxState = new SparseBooleanArray();
    private LinearLayoutManager layoutManager;
    private FriendAvatarAdapter friendAvatarAdapter;

    private ArrayList<String> friendAvatarListSaved = new ArrayList<String>();
    private List<String> reminderFrequencyList = new ArrayList<>();
    private int[] reminderFrequencyDuration = new int[]{3600, 86400, 604800};

    private int reminderFrequency = 3600;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        ButterKnife.bind(this);

        mToolbarTitle.setText(getString(R.string.new_task_toolbar_title));

        //This won't let the keypad come out immediately on start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if(savedInstanceState != null) {
            if(savedInstanceState.getBoolean(getString(R.string.friend_alert_dialog_shown), false)) {
                SparseBooleanArray checkboxStatus = (SparseBooleanArray)savedInstanceState.getParcelable(getString(R.string.checkbox_state_instance));
                showFriendsDialog(checkboxStatus);
            }
            if(savedInstanceState.getStringArrayList(getString(R.string.friend_avatar_list)) != null) {
                ArrayList<String> savedFriendAvatarList = savedInstanceState.getStringArrayList(getString(R.string.friend_avatar_list));
                showFriendAvatars(savedFriendAvatarList);
            }

            mTaskName.setText(savedInstanceState.getString(getString(R.string.task_name)));
            mTaskDescription.setText(savedInstanceState.getString(getString(R.string.task_description)));
            reminderFrequency = savedInstanceState.getInt(getString(R.string.task_reminder));
        }

        mReminderFrequencySpinner.setOnItemSelectedListener(this);

        reminderFrequencyList.add("1 hour");
        reminderFrequencyList.add("daily");
        reminderFrequencyList.add("weekly");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, reminderFrequencyList);

        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        mReminderFrequencySpinner.setAdapter(arrayAdapter);

        mAssignUserTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFriendsDialog(checkboxState);
            }
        });

        mTaskCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                NewTaskActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(NewTaskActivity.this, getString(R.string.new_task_created), Toast.LENGTH_LONG).show();
                        createNewTask();
                        // this will send the broadcast to update the appwidget
                        TaskListWidgetProvider.sendRefreshBroadcast(NewTaskActivity.this);
                    }
                });

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

        //************************************************************************************
        // Need this to be able to include the current user in the assignments list
        final HashMap<String, String> currentUserMap = new HashMap<>();
        currentUserMap.put(user.getUid(), user.getEmail());
        friendList.add(currentUserMap);

        //If you still don't have any friends yet do this so it's just the user in the list
        mRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChild(getString(R.string.friends))) {

                    //Automatically tick the user's checkbox
                    checkboxStatus.put(0, true);

                    assignFriendAdapter = new AssignFriendAdapter(NewTaskActivity.this, friendList, checkboxStatus);
                    assignUserRecyclerView.setAdapter(assignFriendAdapter);

                    //Then just clear the boolean array
//                    checkboxStatus.clear();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //************************************************************************************

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

                            assignFriendAdapter = new AssignFriendAdapter(NewTaskActivity.this, friendList, checkboxStatus);
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



    private void createNewTask() {
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference();

        String taskKey = mRef.child(getString(R.string.tasks)).push().getKey();
        String taskName = mTaskName.getText().toString();
        String taskDescription = mTaskDescription.getText().toString();
        String taskCreator = user.getUid();
        String taskStatus = getString(R.string.open);
        ArrayList<String> firebaseFriendAvatarList = friendAvatarListSaved;

        if(taskName.equals("") || taskName.isEmpty()) {
            Toast.makeText(this, getString(R.string.put_in_task_name), Toast.LENGTH_SHORT).show();
        } else {
            mRef.child(getString(R.string.tasks)).child(taskKey).child(getString(R.string.taskName)).setValue(taskName);
            mRef.child(getString(R.string.tasks)).child(taskKey).child(getString(R.string.taskDescription)).setValue(taskDescription);
            mRef.child(getString(R.string.tasks)).child(taskKey).child(getString(R.string.taskCreator)).setValue(taskCreator);
            mRef.child(getString(R.string.tasks)).child(taskKey).child(getString(R.string.taskStatus)).setValue(taskStatus);
            mRef.child(getString(R.string.tasks)).child(taskKey).child(getString(R.string.taskKey)).setValue(taskKey);
            mRef.child(getString(R.string.tasks)).child(taskKey).child(getString(R.string.task_reminder)).setValue(reminderFrequency);
            Log.d(TAG, "Task Reminder is: " + reminderFrequency);

            for (String item : firebaseFriendAvatarList) {
                mRef.child(getString(R.string.tasks)).child(taskKey).child(getString(R.string.taskAssignee)).child(item).setValue(true);

                //TODO: For a more structured database, add the task under the user account as well
                mRef.child(getString(R.string.users)).child(item).child(getString(R.string.tasks)).child(taskKey).setValue(taskStatus);
            }


            //Create the firebase job dispatcher
            JobDispatcherUtils jobDispatcherUtils = new JobDispatcherUtils(this, taskKey, taskName, reminderFrequency);
            jobDispatcherUtils.scheduleJob();

            //Go back to the task list
            Intent intent = new Intent(this, MyTaskActivity.class);
            startActivity(intent);
        }
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

        String taskName = mTaskName.getText().toString();
        String taskDescription = mTaskDescription.getText().toString();
        int reminderFrequencySaved = reminderFrequency;

        outState.putString(getString(R.string.task_name), taskName);
        outState.putString(getString(R.string.task_description), taskDescription);
        outState.putInt(getString(R.string.task_reminder), reminderFrequencySaved);
    }


}
