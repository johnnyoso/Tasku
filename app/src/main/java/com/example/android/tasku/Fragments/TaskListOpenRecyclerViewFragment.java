package com.example.android.tasku.Fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.tasku.Adapters.TaskAdapter;
import com.example.android.tasku.Adapters.TaskAdapter_ver1;
import com.example.android.tasku.MyTaskActivity;
import com.example.android.tasku.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by john.osorio on 22/03/2018.
 */

public class TaskListOpenRecyclerViewFragment extends Fragment {

    private static final String TAG = TaskListOpenRecyclerViewFragment.class.getSimpleName();

    private TaskAdapter taskAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private List<HashMap<String, String>> taskNameList;

    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;
    private DatabaseReference taskRef;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private RecyclerView mTaskListRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference(getString(R.string.users));
        taskRef = mDatabase.getReference(getString(R.string.tasks));
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        taskNameList = new ArrayList<>();

        View rootView = inflater.inflate(R.layout.task_recyclerview_fragment, container, false);
        rootView.setTag(TAG);

        mTaskListRecyclerView = (RecyclerView) rootView.findViewById(R.id.task_recyclerview);
        layoutManager = new LinearLayoutManager(getActivity());
        mTaskListRecyclerView.setLayoutManager(layoutManager);

        userRef.child(user.getUid()).child(getString(R.string.tasks)).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final String taskKey = dataSnapshot.getKey();
                final String taskStatus = dataSnapshot.getValue().toString();

                Log.d(TAG, "Task ID is: " + taskKey);

                /**
                 * order to prevent the app crashing when creating a new task. This is where the fragment is up to when it attaches
                 *to an activity before crashing
                 */
                if(isAdded()) {

                    if (taskStatus.equals(getString(R.string.open))) {

                        taskRef.child(taskKey).child(getString(R.string.taskName)).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                HashMap<String, String> taskMap = new HashMap<>();
                                String taskName = dataSnapshot.getValue().toString();
                                taskMap.put(taskKey, taskName);
                                taskNameList.add(taskMap);

                                taskAdapter = new TaskAdapter(getActivity(), taskNameList);
                                mTaskListRecyclerView.setAdapter(taskAdapter);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }
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


        return rootView;
    }

}
