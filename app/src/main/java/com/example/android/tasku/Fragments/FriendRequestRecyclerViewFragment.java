package com.example.android.tasku.Fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.tasku.Adapters.FriendListAdapter;
import com.example.android.tasku.Adapters.FriendRequestAdapter;
import com.example.android.tasku.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by john.osorio on 19/02/2018.
 */

public class FriendRequestRecyclerViewFragment extends Fragment {

    private static final String TAG = FriendRequestRecyclerViewFragment.class.getSimpleName();

    private Boolean isFriend;

    private FriendRequestAdapter friendRequestAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<HashMap<String, String>> friendList;


    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference myRef;

    private RecyclerView mFriendRequestRecyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO: Initialise the dataset here

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        friendList = new ArrayList<HashMap<String, String>>();

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        myRef = mDatabase.getReference(getString(R.string.users));

        isFriend = false;

        View rootView = inflater.inflate(R.layout.friend_recyclerview_fragment, container, false);
        rootView.setTag(TAG);

        mFriendRequestRecyclerView = (RecyclerView) rootView.findViewById(R.id.friend_recyclerview);
        mLayoutManager  =new LinearLayoutManager(getActivity());
        mFriendRequestRecyclerView.setLayoutManager(mLayoutManager);


        myRef.child(user.getUid()).child(getString(R.string.friend_requests)).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final String friendUserId = dataSnapshot.getKey();
                Boolean friendUserStatus = (Boolean) dataSnapshot.getValue();

                //Make sure that the friend user status is FALSE which means invite is pending approval
                if(friendUserStatus == false) {
                    myRef.child(friendUserId).child(getString(R.string.emailAddress)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String friendEmailAdd = (String) dataSnapshot.getValue();

                            Log.d(TAG, "Friend email: " + friendEmailAdd);

                            HashMap<String, String> friendMap = new HashMap<>();
                            friendMap.put(friendUserId, friendEmailAdd);
                            friendList.add(friendMap);
                            friendRequestAdapter = new FriendRequestAdapter(getContext(), friendList);
                            mFriendRequestRecyclerView.setAdapter(friendRequestAdapter);
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

                Log.d(TAG,"CHILD REMOVED");
                friendRequestAdapter.notifyDataSetChanged();

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
