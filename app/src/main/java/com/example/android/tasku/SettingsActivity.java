package com.example.android.tasku;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.tasku.fjd.JobDispatcherUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by john.osorio on 10/02/2018.
 */

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    @BindView(R.id.friends_button) Button friendsButton;
    @BindView(R.id.my_toolbar_title) TextView myToolbarTitle;

    private AlertDialog confirmAccountDeletionDialog;

    final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    final FirebaseUser user = mAuth.getCurrentUser();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_page);

        ButterKnife.bind(this);
        myToolbarTitle.setText(getString(R.string.settings_toolbar_title));

        friendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, FriendsActivity.class);
                startActivity(intent);
            }
        });

    }

    public void editUserProfile(View view) {
        Intent intent = new Intent(this, EditUserActivity.class);
        startActivity(intent);
    }

    public void deleteAccount(View view) {


        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference taskRef = mDatabase.getReference(getString(R.string.tasks));
        final DatabaseReference userRef = mDatabase.getReference(getString(R.string.users));
        FirebaseStorage mStorage = FirebaseStorage.getInstance();
        final StorageReference storageRef = mStorage.getReference();
        final StorageReference userImageRef = storageRef.child("images/" + user.getUid() + ".png");

        //TODO: You need to reauthenticate user before deleting their account!

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_delete_account_confirm, null);
        builder.setView(dialogView);
        builder.setTitle("Please confirm details below");
        builder.setMessage("Re-enter username and password");

        final EditText userEmailET = (EditText) dialogView.findViewById(R.id.confirm_user_email);
        final EditText userPasswordET = (EditText) dialogView.findViewById(R.id.confirm_user_password);

        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        confirmAccountDeletionDialog = builder.create();

        confirmAccountDeletionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String userEmail = userEmailET.getText().toString();
                        String userPassword = userPasswordET.getText().toString();

                        AuthCredential credential = EmailAuthProvider.getCredential(userEmail, userPassword);

                        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if(task.isSuccessful()) {
                                    Log.d(TAG, "User is authenticated!");

                                    //Delete the user from the assignee portion of the tasks
                                    deleteUserFromTaskAssigneesList(taskRef, userRef);

                                    //Delete the user's image profile from storage
                                    deleteUserProfileImage(userImageRef);

                                    //Delete this user from his/her friend's friend list
                                    deleteFromUsersFriendsFriendList(userRef);

                                    //Delete this user from his/her sent request friend's request received list --> this is so confusing
                                    deleteFromUsersFriendsFriendRequestList(userRef);

                                    //Delete the user from his/her received request friend's sent requents list
                                    deleteFromUsersFriendsFriendRequestSentList(userRef);

                                    //Delete the user from the firebase database
                                    userRef.child(user.getUid()).removeValue();

                                    //Finally delete the user from Firebase Auth
                                    deleteUserFromFirebaseAuth();

                                } else {
                                    Toast.makeText(SettingsActivity.this, getString(R.string.error) + task.getException(), Toast.LENGTH_LONG).show();
                                    Log.d(TAG, "User is NOT authenticated!");

                                }
                            }
                        });

                    }
                });
            }
        });

        confirmAccountDeletionDialog.show();
    }


    /**
     *
     * @param taskRef
     * @param userRef
     */
    private void deleteUserFromTaskAssigneesList(final DatabaseReference taskRef, final DatabaseReference userRef) {

        //Check all the tasks the user has
        userRef.child(user.getUid()).child(getString(R.string.tasks)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()) {

                    for(DataSnapshot snap : dataSnapshot.getChildren()) {
                        final String taskKey = snap.getKey();

                        //Then delete the user's ID within that task in the task database
                        taskRef.child(taskKey).child(getString(R.string.taskAssignee)).child(user.getUid()).removeValue();


                        //Then check if that task has no more children, if none then just delete it
                        taskRef.child(taskKey).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.hasChild(getString(R.string.taskAssignee))) {

                                    //also remove the job dispatcher notification
                                    JobDispatcherUtils jobDispatcherUtils = new JobDispatcherUtils(SettingsActivity.this, taskKey, "", 0);
                                    jobDispatcherUtils.cancelJob(taskKey);

                                    Log.d(TAG, "Task ID: " + taskKey + " has no more assignees");
                                    taskRef.child(taskKey).removeValue();


                                } else {
                                    long children = dataSnapshot.getChildrenCount();
                                    Log.d(TAG, "TASK ID HAS: " + children + " CHILDREN");
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Delete the user's image in Firebase storage
     * @param userImageRef
     */
    private void deleteUserProfileImage(StorageReference userImageRef){
        //Then delete the user's image in the storage
        userImageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "User Photo successfully deleted");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "User Photo not deleted: " + e.getMessage());
            }
        });
    }
    /**
     * Deletes the user from his/her friend's friend list
     * @param userRef
     */
    private void deleteFromUsersFriendsFriendList(final DatabaseReference userRef) {
        userRef.child(user.getUid()).child(getString(R.string.friends)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()) {
                    for(DataSnapshot snap : dataSnapshot.getChildren()) {
                        String friendUserKey = snap.getKey();

                        //Delete the user from his/her friend's friend list
                        userRef.child(friendUserKey).child(getString(R.string.friends)).child(user.getUid()).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Delete the user from his/her sent requested friend's received requests list
     * @param userRef
     */
    private void deleteFromUsersFriendsFriendRequestList(final DatabaseReference userRef) {
        userRef.child(user.getUid()).child(getString(R.string.friend_requests_sent)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()) {
                    for(DataSnapshot snap : dataSnapshot.getChildren()) {
                        String friendUserKey = snap.getKey();

                        //Delete the user from his/her sent requested friend's received requests list
                        userRef.child(friendUserKey).child(getString(R.string.friend_requests)).child(user.getUid()).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Delete the user from his/her received request friend's sent requents list
     * @param userRef
     */
    private void deleteFromUsersFriendsFriendRequestSentList(final DatabaseReference userRef) {
        userRef.child(user.getUid()).child(getString(R.string.friend_requests)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()) {
                    for(DataSnapshot snap : dataSnapshot.getChildren()) {
                        String friendUserKey = snap.getKey();

                        //Delete the user from his/her received request friend's sent requents list
                        userRef.child(friendUserKey).child(getString(R.string.friend_requests_sent)).child(user.getUid()).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Delete the user's authentication credentials from Firebase
     */
    private void deleteUserFromFirebaseAuth(){

        //Now finally delete the user from Firebase Authentication
        user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(SettingsActivity.this, getString(R.string.account_deleted), Toast.LENGTH_SHORT);

                //Then go back to the sign in page and dismiss the dialog
                confirmAccountDeletionDialog.dismiss();
                Intent goBackToSignInPageIntent = new Intent(SettingsActivity.this, SignInActivity.class);
                startActivity(goBackToSignInPageIntent);
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SettingsActivity.this, getString(R.string.account_not_deleted) + e.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }
}

