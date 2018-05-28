package com.example.android.tasku;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.tasku.Adapters.FriendRequestsSentAdapter;
import com.example.android.tasku.Fragments.FriendListRecyclerViewFragment;
import com.example.android.tasku.Fragments.FriendRequestRecyclerViewFragment;
import com.example.android.tasku.Fragments.FriendRequestsSentRecyclerViewFragment;
import com.example.android.tasku.JavaUtils.AvatarImagesUtils;
import com.example.android.tasku.JavaUtils.UserUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by john.osorio on 12/02/2018.
 */

public class FriendsActivity extends AppCompatActivity {

    private static final String TAG = FriendsActivity.class.getSimpleName();

    @BindView(R.id.friend_request_tab) TextView friendRequestTab;
    @BindView(R.id.friend_list_tab) TextView friendListTab;
    @BindView(R.id.request_sent_tab) TextView friendRequestsSentTab;
    @BindView(R.id.add_friends_fab) FloatingActionButton addFriendFab;
    @BindView(R.id.my_toolbar_title) TextView myToolbarTitle;

    @BindView(R.id.friend_requests_background) FrameLayout friendRequestBackground;
    @BindView(R.id.sent_invites_background) FrameLayout sentInvitesBackground;
    @BindView(R.id.friend_list_background) FrameLayout friendListBackground;

    private String friendEmail;
    private String friendUserId;
    private Boolean isFriend;

    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference myRef;

    private AlertDialog searchFriendEmailDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_page);

        ButterKnife.bind(this);
        myToolbarTitle.setText(getString(R.string.friend_list_toolbar_title));

        if(savedInstanceState != null){
            if(savedInstanceState.getBoolean(getString(R.string.search_friend_dialog_alert_shown), false)) {
                friendEmail = savedInstanceState.getString(getString(R.string.search_email));
                searchFriends(friendEmail);
            }
        }

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        myRef = mDatabase.getReference(getString(R.string.users));

        isFriend = false;

        //Show all your friends first before anything else
        showFriendListFragment();
        friendRequestBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));
        sentInvitesBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));
        friendListBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));


        addFriendFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                searchFriends(friendEmail);
            }
        });

        friendRequestTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                friendRequestBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                sentInvitesBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));
                friendListBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));

                showFriendRequestFragment();
            }
        });

        friendListTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                friendRequestBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));
                sentInvitesBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));
                friendListBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));

                showFriendListFragment();
            }
        });

        friendRequestsSentTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                friendRequestBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));
                sentInvitesBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                friendListBackground.setBackgroundColor(getResources().getColor(R.color.colorWhite));

                showFriendRequestSentFragment();
            }
        });

    }

    private void showFriendListFragment() {

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        FriendListRecyclerViewFragment fragment = new FriendListRecyclerViewFragment();
        transaction.replace(R.id.fragment_friend_list, fragment);
        transaction.commit();

    }

    private void showFriendRequestFragment() {

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        FriendRequestRecyclerViewFragment fragment = new FriendRequestRecyclerViewFragment();
        transaction.replace(R.id.fragment_friend_list, fragment);
        transaction.commit();
    }

    private void showFriendRequestSentFragment() {
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        FriendRequestsSentRecyclerViewFragment fragment = new FriendRequestsSentRecyclerViewFragment();
        transaction.replace(R.id.fragment_friend_list, fragment);
        transaction.commit();
    }

    private void searchFriends(String email) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_search_friend, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle(getString(R.string.dialog_friend_title));
        dialogBuilder.setMessage(getString(R.string.dialog_friend_message));

        final EditText searchFriendEmailEditText = (EditText) dialogView.findViewById(R.id.search_email);
        final Button addFriendsButton = (Button) dialogView.findViewById(R.id.add_friends_button);
        final TextView friendEmailTextView = (TextView) dialogView.findViewById(R.id.friend_email);
        final ImageView friendPic = (ImageView) dialogView.findViewById(R.id.friend_pic);
        final LinearLayout searchResult = (LinearLayout) dialogView.findViewById(R.id.search_result);
        final ProgressBar progressBar = (ProgressBar) dialogView.findViewById(R.id.search_friend_progress_bar);

        searchFriendEmailEditText.setText(email);

        //set the listener to null so the dialog won't close upon clicking the button
        dialogBuilder.setPositiveButton(getString(R.string.dialog_friend_pos_button), null);

        dialogBuilder.setNegativeButton(getString(R.string.dialog_friend_neg_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                searchFriendEmailEditText.setText("");

                //Reset all String values for orientation changes
                friendEmail = "";
                dialog.cancel();
            }
        });

        searchFriendEmailDialog = dialogBuilder.create();

        searchFriendEmailDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String email = searchFriendEmailEditText.getText().toString();

                        myRef.orderByChild(getString(R.string.emailAddress)).addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                UserUtils userUtils = dataSnapshot.getValue(UserUtils.class);
                                //Log.d(TAG, "Email address is: " + userUtils.getEmailAddress());
                                if(email.equals(userUtils.getEmailAddress())){
                                    addFriendsButton.setVisibility(View.VISIBLE);
                                    searchResult.setVisibility(View.VISIBLE);
                                    friendEmailTextView.setText(userUtils.getEmailAddress());
                                    String friendUid = userUtils.getUserID();

                                    //Show friend pic
                                    AvatarImagesUtils avatarImagesUtils = new AvatarImagesUtils(FriendsActivity.this, friendUid, friendPic, progressBar);
                                    avatarImagesUtils.execute();

                                    friendUserId = userUtils.getUserID();
                                    Log.d(TAG, "FRIEND USER ID" + friendUserId);
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
                });
            }
        });
        searchFriendEmailDialog.show();
    }

    public void addFriend(View view){

        Log.d(TAG, friendUserId + " " + user.getUid());

        //Check that you are not inviting yourself or an already invited friend or an already existing friend in the list
        checkFriendTree(friendUserId);

        searchFriendEmailDialog.dismiss();

    }

    private void checkFriendTree(final String checkFriendUid) {

        myRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            boolean dontSendRequest = false;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    Log.d(TAG, "Snap key is: " + snap.getKey());
                    if(snap.getKey().equals(getString(R.string.friends))) {

                        for(DataSnapshot friendSnap : snap.getChildren()) {
                            String friendSnapId = friendSnap.getKey();
                            if(friendSnapId.equals(checkFriendUid)) {
                                Toast.makeText(FriendsActivity.this, getString(R.string.toast_message_already_friend), Toast.LENGTH_SHORT).show();
                                dontSendRequest = true;
                            }
                        }

                    }if (snap.getKey().equals(getString(R.string.friend_requests))) {

                        for(DataSnapshot friendRequestSnap : snap.getChildren()) {
                            String friendRequestSnapId = friendRequestSnap.getKey();
                            if(friendRequestSnapId.equals(checkFriendUid)) {
                                Toast.makeText(FriendsActivity.this, getString(R.string.toast_message_already_received_request), Toast.LENGTH_SHORT).show();
                                dontSendRequest = true;
                            }
                        }

                    }if (snap.getKey().equals(getString(R.string.friend_requests_sent))) {

                        for(DataSnapshot friendRequestSentSnap : snap.getChildren()) {
                            String friendRequestSentSnapId = friendRequestSentSnap.getKey();
                            if(friendRequestSentSnapId.equals(checkFriendUid)) {
                                Toast.makeText(FriendsActivity.this, getString(R.string.toast_message_already_sent_request), Toast.LENGTH_SHORT).show();
                                dontSendRequest = true;
                            }
                        }

                    }
                }
                if(!dontSendRequest) {
                    //Add the userID of the invited friend under the current User's "friend" tree
                    myRef.child(user.getUid()).child(getString(R.string.friend_requests_sent)).child(friendUserId).setValue(isFriend);

                    //Add the current user's id under the invitee's "friend" tree so both have each other's User Id
                    myRef.child(friendUserId).child(getString(R.string.friend_requests)).child(user.getUid()).setValue(isFriend);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(searchFriendEmailDialog != null && searchFriendEmailDialog.isShowing()){
            EditText emailET = (EditText) searchFriendEmailDialog.getWindow().findViewById(R.id.search_email);
            String email = emailET.getText().toString();
            searchFriendEmailDialog.dismiss();
            outState.putBoolean(getString(R.string.search_friend_dialog_alert_shown), true);
            outState.putString(getString(R.string.search_email), email);
        }
    }
}
