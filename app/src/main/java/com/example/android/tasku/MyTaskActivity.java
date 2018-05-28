package com.example.android.tasku;


import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.tasku.Adapters.TaskAdapter;
import com.example.android.tasku.Adapters.TaskAdapter_ver1;
import com.example.android.tasku.Fragments.TaskDescriptionFragment;
import com.example.android.tasku.Fragments.TaskListCloseRecyclerViewFragment;
import com.example.android.tasku.Fragments.TaskListOpenRecyclerViewFragment;
import com.example.android.tasku.JavaUtils.InternetUtils;
import com.example.android.tasku.fjd.JobDispatcherUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by john.osorio on 17/08/2017.
 * This will contain all available tasks and will be viewed using RecyclerView
 */

public class MyTaskActivity extends AppCompatActivity implements TaskAdapter_ver1.TaskAdapater_ver1OnClickHandler {

    private static final String TAG = MyTaskActivity.class.getSimpleName();

    @BindView(R.id.task_list_title) TextView mTaskListTitle;
    @BindView(R.id.open_tasks) TextView mOpenTasks;
    @BindView(R.id.closed_tasks) TextView mClosedTasks;
    @BindView(R.id.my_toolbar) Toolbar mToolbar;
    @BindView(R.id.my_toolbar_title) TextView mToolbarTitle;
    @BindView(R.id.open_task_background) FrameLayout openTaskBackground;
    @BindView(R.id.closed_task_background) FrameLayout closedTaskBackground;
    @BindView(R.id.new_task_fab) FloatingActionButton newTaskFab;

    @BindView(R.id.open_recyclerview_tasks) RecyclerView mOpenTaskRecyclerView;
    @BindView(R.id.close_recyclerview_tasks) RecyclerView mCloseTaskRecyclerView;
    private TaskAdapter_ver1 mOpenTaskAdapter_ver1;
    private TaskAdapter_ver1 mCloseTaskAdapter_ver1;
    private TaskDescriptionFragment mTaskDescriptionFragment;
    private FragmentManager fragmentManager;

    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;
    private DatabaseReference taskRef;
    private FirebaseUser user;

    private FirebaseAuth myTaskActivityAuth;

    private Boolean isTwoPane;
    private String taskKey;
    private String taskName;
    private int reminderFrequency;
    private int scrollProgress = 0;

    private AlertDialog signOutDialog;

    private int adapterPosition;

    private JobDispatcherUtils jobDispatcherUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        //Check for internet connection
        InternetUtils internetUtils = new InternetUtils(this);
        internetUtils.checkInternet();

        ButterKnife.bind(this);
        jobDispatcherUtils = new JobDispatcherUtils(this, taskKey, taskName, reminderFrequency);

        myTaskActivityAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference(getString(R.string.users));
        taskRef = mDatabase.getReference(getString(R.string.tasks));
        user = myTaskActivityAuth.getCurrentUser();

        if(savedInstanceState != null) {
            String visibleTaskList = savedInstanceState.getString(getString(R.string.visible_task_list));
            scrollProgress = savedInstanceState.getInt(getString(R.string.scroll_progress));
            Log.d(TAG, "SAVED VISIBLE TASK LIST IS: " + visibleTaskList);

            if(visibleTaskList.equals(getString(R.string.m_open_task_recyclerview))) {

                //Show the open task list recyclerview plus add in the scroll progress
                mOpenTaskRecyclerView.setVisibility(View.VISIBLE);
                mCloseTaskRecyclerView.setVisibility(View.GONE);
                showOpenTaskListRecyclerView(scrollProgress);
                openTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                closedTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));

            }if(visibleTaskList.equals(getString(R.string. m_close_task_recyclerview))) {

                //Show the close task list recyclerview pluss add in the scroll progress
                mCloseTaskRecyclerView.setVisibility(View.VISIBLE);
                mOpenTaskRecyclerView.setVisibility(View.GONE);
                showCloseTaskListRecyclerView(scrollProgress);
                closedTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                openTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));
            }

            isTwoPane = savedInstanceState.getBoolean(getString(R.string.is_two_pane));
            taskKey = savedInstanceState.getString(getString(R.string.taskKey));
            taskName = savedInstanceState.getString(getString(R.string.taskName));

        } else {

            //Show the open task list recyclerview if no saved instance
                mOpenTaskRecyclerView.setVisibility(View.VISIBLE);
                mCloseTaskRecyclerView.setVisibility(View.GONE);
                showOpenTaskListRecyclerView(scrollProgress);
                openTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                closedTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));

        }

        //Check if layout is two pane tablet
        if(findViewById(R.id.two_pane_layout) != null) {
            isTwoPane = true;

            //You should only get a bundle if it's coming from task Description sudden orientation change to landscape
            Intent fromTaskDescriptionIntent = getIntent();
            Bundle fromTaskDescriptionBundle = fromTaskDescriptionIntent.getExtras();

            if(fromTaskDescriptionBundle != null) {
                adapterPosition = fromTaskDescriptionBundle.getInt(getString(R.string.adapter_position));
                String taskKey = fromTaskDescriptionBundle.getString(getString(R.string.taskKey));
                String taskName = fromTaskDescriptionBundle.getString(getString(R.string.taskName));
                String[] taskDataArray = new String[]{taskKey, taskName};

                //Don't know if this will actually work
                onClick(taskDataArray, adapterPosition);
            }


        } else {
            isTwoPane = false;
        }

        setSupportActionBar(mToolbar);
        mToolbarTitle.setText(getString(R.string.task_list));


        mOpenTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenTaskRecyclerView.setVisibility(View.VISIBLE);
                mCloseTaskRecyclerView.setVisibility(View.GONE);
                showOpenTaskListRecyclerView(scrollProgress);
                openTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                closedTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));

            }
        });

        mClosedTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCloseTaskRecyclerView.setVisibility(View.VISIBLE);
                mOpenTaskRecyclerView.setVisibility(View.GONE);
                showCloseTaskListRecyclerView(scrollProgress);
                closedTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                openTaskBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));
            }
        });

        newTaskFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Create a new task by heading to NewTaskActivity
                Intent newTaskIntent = new Intent(MyTaskActivity.this, NewTaskActivity.class);
                startActivity(newTaskIntent);
            }
        });
    }


    @Override
    public void onClick(String[] taskDataArray, int currentAdapterPosition) {

        taskKey = taskDataArray[0];
        taskName = taskDataArray[1];

        Bundle toTaskDescriptionBundle = new Bundle();
        toTaskDescriptionBundle.putString(getString(R.string.taskKey), taskKey);
        toTaskDescriptionBundle.putString(getString(R.string.taskName), taskName);
        toTaskDescriptionBundle.putInt(getString(R.string.adapter_position), currentAdapterPosition);

        if(isTwoPane) {

            //Clear any existing task fragments
            if(mTaskDescriptionFragment != null) {
                fragmentManager.beginTransaction().detach(mTaskDescriptionFragment).commit();
            }

            //Start the Task Description Fragment
            showTaskDescriptionFragment(toTaskDescriptionBundle);

        } else {

            //If not two pane then just send it to TaskDescriptionActivity
            Intent toTaskDescriptionIntent = new Intent(this, TaskDescriptionActivity.class);
            toTaskDescriptionIntent.putExtras(toTaskDescriptionBundle);
            startActivity(toTaskDescriptionIntent);
        }

    }

    private void showOpenTaskListRecyclerView(int scrollProgress) {

        mOpenTaskAdapter_ver1 = new TaskAdapter_ver1(this);
        RecyclerView.LayoutManager openTaskLayoutManager;
        final List<HashMap<String, String>> taskNameList = new ArrayList<>();
        openTaskLayoutManager = new LinearLayoutManager(this);
        mOpenTaskRecyclerView.setLayoutManager(openTaskLayoutManager);
        mOpenTaskRecyclerView.setVerticalScrollbarPosition(scrollProgress);

        //Update the data just in case
        mOpenTaskAdapter_ver1.updateDataSet();

        userRef.child(user.getUid()).child(getString(R.string.tasks)).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final String taskKey = dataSnapshot.getKey();
                final String taskStatus = dataSnapshot.getValue().toString();

                Log.d(TAG, "Task ID is: " + taskKey + " Task Status is: " + taskStatus);

                    if (taskStatus.equals(getString(R.string.open))) {

                        taskRef.child(taskKey).child(getString(R.string.taskName)).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                HashMap<String, String> taskMap = new HashMap<>();
                                String taskName = dataSnapshot.getValue().toString();
                                taskMap.put(taskKey, taskName);
                                taskNameList.add(taskMap);

                                    mOpenTaskRecyclerView.setAdapter(mOpenTaskAdapter_ver1);

                                mOpenTaskAdapter_ver1.setTaskDataList(taskNameList);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

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

    private void showCloseTaskListRecyclerView(int scrollProgress) {

        mCloseTaskAdapter_ver1 = new TaskAdapter_ver1(this);
        RecyclerView.LayoutManager closeTaskLayoutManager;
        final List<HashMap<String, String>> taskNameList = new ArrayList<>();
        closeTaskLayoutManager = new LinearLayoutManager(this);
        mCloseTaskRecyclerView.setLayoutManager(closeTaskLayoutManager);
        mCloseTaskRecyclerView.setVerticalScrollbarPosition(scrollProgress);

        //Update the data just in case
        mCloseTaskAdapter_ver1.updateDataSet();

        userRef.child(user.getUid()).child(getString(R.string.tasks)).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final String taskKey = dataSnapshot.getKey();
                final String taskStatus = dataSnapshot.getValue().toString();

                if (taskStatus.equals(getString(R.string.close))) {

                    taskRef.child(taskKey).child(getString(R.string.taskName)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            HashMap<String, String> taskMap = new HashMap<>();
                            String taskName = dataSnapshot.getValue().toString();
                            taskMap.put(taskKey, taskName);
                            taskNameList.add(taskMap);

                                mCloseTaskRecyclerView.setAdapter(mCloseTaskAdapter_ver1);

                            mCloseTaskAdapter_ver1.setTaskDataList(taskNameList);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

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

    private void showTaskDescriptionFragment(Bundle taskDescriptionBundle) {

        fragmentManager = getSupportFragmentManager();

        mTaskDescriptionFragment = new TaskDescriptionFragment();
        mTaskDescriptionFragment.setTaskDescriptionBundle(taskDescriptionBundle);
        fragmentManager.beginTransaction().add(R.id.task_description_fragment_container, mTaskDescriptionFragment).commit();
    }

    //TODO 7: Consider using a FAB-floating action button to add new tasks instead of action bar menu


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();


        if(isTwoPane) {
//            menuInflater.inflate(R.menu.task_description_menu_600dp_land, menu);
            return false;

        } else {
            menuInflater.inflate(R.menu.task_activity_menu, menu);
            return true;
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
            switch (id) {

                case R.id.task_activity_menu_create_new_task:

                    //Create a new task by heading to NewTaskActivity
                    Intent newTaskIntent = new Intent(this, NewTaskActivity.class);
                    startActivity(newTaskIntent);

                    break;

                case R.id.task_activity_menu_settings_page:

                    Intent editUserProfileIntent = new Intent(this, SettingsActivity.class);
                    startActivity(editUserProfileIntent);

                    break;

                case R.id.task_activity_menu_sign_out_user:

                    //Sign out the current user and go back to login page (Main Activity)
                    myTaskActivityAuth.signOut();

                    //Unsubscribe from the topic
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(getString(R.string.new_task_notifications));
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(getString(R.string.new_friend_request_notifications));

                    Intent signOutIntent = new Intent(this, SignInActivity.class);
                    startActivity(signOutIntent);

                    break;

                default:
                    break;
            }
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "THIS IS ON PAUSE");

        //Remove the task description fragment before changing to portrait orientation
        if(isTwoPane) {
            fragmentManager.beginTransaction().remove(mTaskDescriptionFragment).commit();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "THIS IS ON STOP");


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "THIS IS ON DESTROY");

    }

    @Override
    public void onBackPressed() {

//        showSignOutDialog();

        //Sign out user and go back to sign in page
        myTaskActivityAuth.signOut();
//        Intent goBackToSignIn = new Intent(this, SignInActivity.class);
//        startActivity(goBackToSignIn);

        super.onBackPressed();
    }

//    private void showSignOutDialog() {
//
//        //Show a dialog asking if user wanted to be signed out
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(getString(R.string.sign_out_message));
//
//
//        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//                //Sign out the user
//                myTaskActivityAuth.signOut();
//                Intent goBackToSignInIntent = new Intent(MyTaskActivity.this, SignInActivity.class);
//                startActivity(goBackToSignInIntent);
//            }
//        });
//
//        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//            }
//        });
//
//        signOutDialog = builder.create();
//        signOutDialog.show();
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, "THIS IS ON SAVED INSTANCE");

        String visibleTaskList = null;

        //Check which task recyclerview is showing: open / close
        if(mOpenTaskRecyclerView.getVisibility() == View.VISIBLE) {
            scrollProgress = mOpenTaskRecyclerView.computeVerticalScrollOffset();
            visibleTaskList = getString(R.string.m_open_task_recyclerview);

        } else if(mCloseTaskRecyclerView.getVisibility() == View.VISIBLE) {
            scrollProgress = mCloseTaskRecyclerView.computeVerticalScrollOffset();
            visibleTaskList = getString(R.string.m_close_task_recyclerview);
        }

        Log.d(TAG, "VISIBLE TASK LIST IS: " + visibleTaskList);

        Boolean isTwoPaneSaved = isTwoPane;
        String taskKeySaved = taskKey;
        String taskNameSaved = taskName;

        outState.putBoolean(getString(R.string.is_two_pane), isTwoPaneSaved);
        outState.putString(getString(R.string.taskKey), taskKeySaved);
        outState.putString(getString(R.string.taskName), taskNameSaved);

        outState.putInt(getString(R.string.scroll_progress), scrollProgress);
        outState.putString(getString(R.string.visible_task_list), visibleTaskList);
    }
}


