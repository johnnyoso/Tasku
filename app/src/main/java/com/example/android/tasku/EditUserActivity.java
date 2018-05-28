package com.example.android.tasku;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.example.android.tasku.BitmapUtils.BitmapUtils;
import com.example.android.tasku.JavaUtils.PermissionUtils;
import com.example.android.tasku.JavaUtils.UserUtils;
import com.example.android.tasku.fcm.TaskuFirebaseInstanceIdService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;


/**
 * Created by john.osorio on 18/08/2017.
 */

//TODO 7: Read more about Firebase authentication and how it can add more details to user's info
public class EditUserActivity extends AppCompatActivity  {

    private static final String TAG = EditUserActivity.class.getSimpleName();
    private static String userChosenTask;
    private static final int REQUEST_CAMERA = 1;
    private static final int SELECT_FILE = 2;
    private static final int EDIT_IMAGE = 3;

    private BitmapUtils bitmapUtils;

    private FirebaseAuth myEditUserActivityAuth;
    private FirebaseUser user;


    FirebaseDatabase mDatabase;
    DatabaseReference myRef;
    DatabaseReference userRef;
    FirebaseStorage mStorage;
    StorageReference storageRef;
    StorageReference userImagesStorageRef;

//    @BindView(R.id.new_user_toolbar) Toolbar mToolbar;
    @BindView(R.id.first_name_display) LinearLayout mFirstNameDisplay;
    @BindView(R.id.edit_user_first_name) EditText mUserFirstName;
    @BindView(R.id.surname_display) LinearLayout mLastNameDisplay;
    @BindView(R.id.edit_user_last_name) EditText mUserLastName;
    @BindView(R.id.mobile_display) LinearLayout mMobileDisplay;
    @BindView(R.id.edit_user_mobile) EditText mUserMobile;
    @BindView(R.id.new_user_pic) ImageView mNewUserPic;
    @BindView(R.id.update_user_profile_button) Button mUpdateUserProfileButton;
    @BindView(R.id.edit_user_profile_button) Button mEditUserProfileButton;
    @BindView(R.id.cancel_update_button) Button mCancelUserUpdateButton;
    @BindView(R.id.add_user_pic_button) Button mAddUserPicButton;
    @BindView(R.id.email_display) LinearLayout mUserEmailDisplay;
    @BindView(R.id.edit_user_email_button) Button mEditUserEmailButton;
    @BindView(R.id.password_display) LinearLayout mUserPasswordDisplay;
    @BindView(R.id.edit_user_password_button) Button mEditUserPasswordButton;

    @BindView(R.id.user_first_name) TextView mUserFirstNameText;
    @BindView(R.id.user_surname)TextView mUserLastNameText;
    @BindView(R.id.user_mobile)TextView mUserMobileText;
    @BindView(R.id.user_email) TextView mUserEmailText;

    @BindView(R.id.my_toolbar_title) TextView myToolbarTitle;

    private String oldPassword;
    private String password1;
    private String password2;
    private String emailAddress;
    private String userFirstName;
    private String userLastName;
    private String userMobile;
    public String userID;
    private String oldUserFirstName;
    private String oldUserLastName;

    private Boolean isAuthenticated;

    private Bitmap bm;
    private byte[] bitmapByteArray;

    private AlertDialog changeEmailDialog;
    private AlertDialog changePasswordDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_user);
        ButterKnife.bind(this);

        myToolbarTitle.setText(getString(R.string.edit_user_profile_toolbar_title));

        //This should keep the profile pic and the rest of the info even if you change handset orientation
        if(savedInstanceState != null) {

            if(savedInstanceState.getBoolean("isEditing", false)) {
                editUserAccount();

                bitmapByteArray = savedInstanceState.getByteArray(getString(R.string.bitmap_byte_array));

                if(bitmapByteArray != null) {
                    Log.d(TAG, "SAVEDSTATE Bitmap is: " + bitmapByteArray);
                    bm = BitmapFactory.decodeByteArray(bitmapByteArray, 0, bitmapByteArray.length);
                    mNewUserPic.setImageBitmap(bm);
                }
                mUserFirstName.setText(savedInstanceState.getString(getString(R.string.firstName)));
                mUserLastName.setText(savedInstanceState.getString(getString(R.string.surName)));
                mUserMobile.setText(savedInstanceState.getString(getString(R.string.mobile)));

            }

            //For the change email dialog box
            if(savedInstanceState.getBoolean(getString(R.string.change_user_email_alert_shown), false)) {
                emailAddress = savedInstanceState.getString(getString(R.string.dialog_email));
                oldPassword = savedInstanceState.getString(getString(R.string.old_password));
                changeUserEmail(emailAddress, oldPassword);

            } else if (savedInstanceState.getBoolean(getString(R.string.change_user_password_alert_shown), false)) {
                oldPassword = savedInstanceState.getString(getString(R.string.old_password));
                password1 = savedInstanceState.getString(getString(R.string.new_password_1));
                password2 = savedInstanceState.getString(getString(R.string.new_password_2));
                changeUserPassword(oldPassword, password1, password2);
            }

        }

        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference(getString(R.string.users));

        bitmapUtils = new BitmapUtils();

//        setSupportActionBar(mToolbar);
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        myEditUserActivityAuth = FirebaseAuth.getInstance();
        user = myEditUserActivityAuth.getCurrentUser();

        userID = user.getUid();
        userImagesStorageRef = storageRef.child("images/" + userID + ".png");
        emailAddress = user.getEmail();
        oldPassword = null;
        password1 = null;
        password2 = null;
        isAuthenticated = false;

        getUserProfileData();

        //Button to change the user's email
        mEditUserEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeUserEmail(emailAddress, oldPassword);
            }
        });

        mEditUserPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeUserPassword(oldPassword, password1, password2);
            }
        });

        //Set the relevant views back to either GONE or VISIBLE when the user cancels update
        mCancelUserUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setViewsBackToOriginalState();
            }
        });

        mEditUserProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editUserAccount();
            }
        });
    }

    /**
     * This gets the user profile data from Firebase
     */
    private void getUserProfileData() {

        mDatabase = FirebaseDatabase.getInstance();

        myRef = mDatabase.getReference("users/" + userID);

        emailAddress = user.getEmail();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserUtils newUserUtils = dataSnapshot.getValue(UserUtils.class);
                mUserFirstNameText.setText(newUserUtils.getFirstName());
                mUserLastNameText.setText(newUserUtils.getLastName());
                mUserMobileText.setText(newUserUtils.getUserMobile());
                mUserEmailText.setText(newUserUtils.getEmailAddress());

                userImagesStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d(TAG, "USER PROFILE PIC URL IS: " + uri);


                        Glide.with(getApplicationContext()).load(uri).asBitmap().centerCrop().error(R.drawable.assigned_user_icon).into(new BitmapImageViewTarget(mNewUserPic) {
                            @Override
                            protected void setResource(Bitmap resource) {
                                RoundedBitmapDrawable circularBitmapDrawable =
                                        RoundedBitmapDrawableFactory.create(getResources(), resource);
                                circularBitmapDrawable.setCircular(true);
                                mNewUserPic.setImageDrawable(circularBitmapDrawable);
                            }
                        });
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    /**
     * This button will let the user edit their personal details
     *
     */
    public void editUserAccount(){

        //Firstly mark the current views as gone
        mFirstNameDisplay.setVisibility(GONE);
        mLastNameDisplay.setVisibility(GONE);
        mMobileDisplay.setVisibility(GONE);
        mEditUserProfileButton.setVisibility(GONE);

        //Then mark the edit texts as visible
        mUserFirstName.setVisibility(View.VISIBLE);
        mUserLastName.setVisibility(View.VISIBLE);
        mUserMobile.setVisibility(View.VISIBLE);
        mUpdateUserProfileButton.setVisibility(View.VISIBLE);
        mCancelUserUpdateButton.setVisibility(View.VISIBLE);
        mAddUserPicButton.setVisibility(View.VISIBLE);

        //Then mark the change email and passwords as gone
        mUserEmailDisplay.setVisibility(GONE);
        mEditUserEmailButton.setVisibility(GONE);
        mUserPasswordDisplay.setVisibility(GONE);
        mEditUserPasswordButton.setVisibility(GONE);

//        //Populate the user's first and last names
//        userRef.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                for(DataSnapshot snap: dataSnapshot.getChildren()) {
//                    if(snap.equals(getString(R.string.firstName))) {
//                        oldUserFirstName = snap.getValue().toString();
//                        mUserFirstName.setHint(oldUserFirstName);
//                    }
//                    if(snap.equals(getString(R.string.surName))) {
//                        oldUserLastName = snap.getValue().toString();
//                        mUserLastName.setHint(oldUserLastName);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
    }

    public void updateUserAccount(View view){

        //Get all the user input strings
        userFirstName = mUserFirstName.getText().toString();
        userLastName = mUserLastName.getText().toString();
        userMobile = mUserMobile.getText().toString();

        //Show notification if a mandatory entry field is empty. Let's make first and last name mandatory
        if(userFirstName.equals("") || userLastName.equals("")) {
            Toast.makeText(EditUserActivity.this, getString(R.string.fill_required_entries), Toast.LENGTH_SHORT).show();

            //Perhaps change the text color of the first and last name hints whatever's empty
            mUserFirstName.setHintTextColor(getResources().getColor(android.R.color.holo_red_light));
            mUserLastName.setHintTextColor(getResources().getColor(android.R.color.holo_red_light));

        } else {

            mDatabase = FirebaseDatabase.getInstance();
            myRef = mDatabase.getReference();
            String firebaseMessagingToken = FirebaseInstanceId.getInstance().getToken();


            myRef.child(getString(R.string.users)).child(userID).child("emailAddress").setValue(emailAddress);
            myRef.child(getString(R.string.users)).child(userID).child("firstName").setValue(userFirstName);
            myRef.child(getString(R.string.users)).child(userID).child("lastName").setValue(userLastName);
            myRef.child(getString(R.string.users)).child(userID).child("userMobile").setValue(userMobile);
            myRef.child(getString(R.string.users)).child(userID).child("userID").setValue(userID);
            myRef.child(getString(R.string.users)).child(userID).child("firebaseToken").setValue(firebaseMessagingToken);

            //Update user display name in Firebase as well
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(userFirstName + " " + userLastName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User profile updated.");
                            }
                        }
                    });

            //Show the newly edited user data
            getUserProfileData();

            //Upload the bitmap to Firebase
            if(bitmapByteArray != null) {
                uploadBitmapToFirebase(bitmapByteArray);
            }

            //Then make the relevant views visible or gone
            setViewsBackToOriginalState();
        }
    }

    private void setViewsBackToOriginalState(){
        //Firstly mark the current views as gone
        mFirstNameDisplay.setVisibility(View.VISIBLE);
        mLastNameDisplay.setVisibility(View.VISIBLE);
        mMobileDisplay.setVisibility(View.VISIBLE);
        mEditUserProfileButton.setVisibility(View.VISIBLE);
        mCancelUserUpdateButton.setVisibility(GONE);

        //Then mark the edit texts as visible
        mUserFirstName.setVisibility(GONE);
        mUserLastName.setVisibility(GONE);
        mUserMobile.setVisibility(GONE);
        mUpdateUserProfileButton.setVisibility(GONE);
        mAddUserPicButton.setVisibility(View.GONE);

        //Then mark the change email and passwords as visibile
        mUserEmailDisplay.setVisibility(View.VISIBLE);
        mEditUserEmailButton.setVisibility(View.VISIBLE);
        mUserPasswordDisplay.setVisibility(View.VISIBLE);
        mEditUserPasswordButton.setVisibility(View.VISIBLE);

    }

    /**
     * Button to change user email. Just show a dialog
     *
     */
    private void changeUserEmail(String userEmail, String confirmPassword) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_change_email, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Change Email");
        dialogBuilder.setMessage("Enter email below");

        final EditText changeUserEmailEditText = (EditText) dialogView.findViewById(R.id.change_user_email);
        final EditText confirmUserPasswordEditText = (EditText) dialogView.findViewById(R.id.confirm_password);

        changeUserEmailEditText.setText(userEmail);
        confirmUserPasswordEditText.setText(confirmPassword);

        //set the listener to null so the dialog won't close upon clicking the button
        dialogBuilder.setPositiveButton("Change Email", null);

        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                changeUserEmailEditText.setText("");
                confirmUserPasswordEditText.setText("");

                //Reset all String values for orientation changes
                emailAddress = "";
                oldPassword = "";
                dialog.cancel();
            }
        });

        changeEmailDialog = dialogBuilder.create();

        changeEmailDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String email = changeUserEmailEditText.getText().toString();
                        String confirmPassword = confirmUserPasswordEditText.getText().toString();

                        //Reauthenticate user by asking for password along with the new email
                        if(userReauthenticate(email, confirmPassword)) {

                            user.updateEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        Toast.makeText(EditUserActivity.this, getString(R.string.email_changed), Toast.LENGTH_SHORT).show();

                                        //Set the newly changed email address in the text view
                                        mUserEmailText.setText(emailAddress);

                                        changeEmailDialog.dismiss();
                                    } else {
                                        String errorMessage = task.getException().getMessage();
                                        Toast.makeText(EditUserActivity.this, getString(R.string.error) + errorMessage, Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
        changeEmailDialog.show();
    }

    /**
     * Change the user's password
     * @param userPassword1
     * @param userPassword2
     */
    private void changeUserPassword(String oldPassword, String userPassword1, String userPassword2){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_change_password, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Change Password");
        dialogBuilder.setMessage("Enter password below");

        final EditText changeUserOldPasswordEditText = (EditText) dialogView.findViewById(R.id.old_password);
        final EditText changeUserPassword1EditText = (EditText) dialogView.findViewById(R.id.new_password_1);
        final EditText changeUserPassword2EditText = (EditText) dialogView.findViewById(R.id.new_password_2);

        changeUserOldPasswordEditText.setText(oldPassword);
        changeUserPassword1EditText.setText(userPassword1);
        changeUserPassword2EditText.setText(userPassword2);

        //set the listener to null so the dialog won't close upon clicking the button
        dialogBuilder.setPositiveButton("Change Password", null);

        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                changeUserOldPasswordEditText.setText("");
                changeUserPassword1EditText.setText("");
                changeUserPassword2EditText.setText("");

                //Reset all String values for orientation changes
                password1 = null;
                password2 = null;
                dialog.cancel();
            }
        });

        changePasswordDialog = dialogBuilder.create();

        changePasswordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String oldPassword = changeUserOldPasswordEditText.getText().toString();
                        String password1 = changeUserPassword1EditText.getText().toString();
                        String password2 = changeUserPassword2EditText.getText().toString();

                        //TODO 14: Reauthenticate user by asking for password along with the new email
                        if(userReauthenticate(emailAddress, oldPassword)){
                            if(password1.length() >= 8 && password1.length() <= 20) {
                                if (password1.equals(password2)) {
                                    user.updatePassword(password1)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(EditUserActivity.this, getString(R.string.password_changed), Toast.LENGTH_SHORT).show();
                                                        changePasswordDialog.dismiss();

                                                    } else {
                                                        Toast.makeText(EditUserActivity.this, getString(R.string.error) + task.getException(), Toast.LENGTH_LONG).show();

                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(EditUserActivity.this, getString(R.string.password_not_match), Toast.LENGTH_SHORT).show();

                                }
                            } else {
                                Toast.makeText(EditUserActivity.this, getString(R.string.password_length), Toast.LENGTH_SHORT).show();

                            }
                        }
                    }
                });
            }
        });
        changePasswordDialog.show();
    }

    /**
     * This reauthenticates the user's credentials before doing sensitive tasks like changing password and email
     * @param email
     * @param password
     * @return
     */
    private Boolean userReauthenticate(String email, String password){

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful()) {
                    Toast.makeText(EditUserActivity.this, getString(R.string.change_success), Toast.LENGTH_SHORT).show();
                    isAuthenticated = true;
                } else {
                    Toast.makeText(EditUserActivity.this, getString(R.string.error) + task.getException(), Toast.LENGTH_LONG).show();
                    isAuthenticated = false;
                }
            }
        });

        return isAuthenticated;
    }

    public void setUserPhoto(View view){

        final CharSequence[] items = {getString(R.string.take_photo), getString(R.string.choose_from_library), getString(R.string.cancel)};
        AlertDialog.Builder builder = new AlertDialog.Builder(EditUserActivity.this);
        builder.setTitle(getString(R.string.add_photo));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                boolean result = PermissionUtils.checkPermission(EditUserActivity.this);
                if(items[item].equals(getString(R.string.take_photo))){
                    userChosenTask = getString(R.string.take_photo);
                    if(result){
                        cameraIntent();
                    }

                } else if(items[item].equals(getString(R.string.choose_from_library))){
                    userChosenTask = getString(R.string.choose_from_library);
                    if(result){
                        galleryIntent();
                    }

                } else if(items[item].equals(getString(R.string.cancel))){
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void cameraIntent(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), SELECT_FILE);
    }

    private void editImageIntent(Intent data) {
        Intent editIntent = new Intent(Intent.ACTION_EDIT);
        editIntent.setDataAndType(data.getData(), "image/*");
        editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(editIntent, getString(R.string.please_edit_your_profile_pic)), EDIT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == SELECT_FILE) {
                editImageIntent(data);

            } else if(requestCode == REQUEST_CAMERA){


            } else if(requestCode == EDIT_IMAGE){
                onSelectFromGalleryResult(data);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        bm = null;
        if(data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Crop it in a circular area
        bm = bitmapUtils.getCroppedBitmap(bm);

        //This sets up the user profile pic but doesn't save the image yet.
        mNewUserPic.setImageBitmap(bm);

        Log.d(TAG, "User profile image set!");

        //Convert the bitmap to a Byte Array
        bitmapByteArray = bitmapUtils.convertBitmapToByteArray(bm);

    }


    /**
     * This is where the chosen bitmap will be uploaded to Firebase
     * @param bitmapArray
     */
    private void uploadBitmapToFirebase(byte[] bitmapArray) {

        //TODO 9: Consider uploading the user profile pic under user profile in Firebase auth
        //Upload the bitmap byte array to Firebase
        UploadTask uploadTask = userImagesStorageRef.putBytes(bitmapArray);
        uploadTask.addOnFailureListener(new OnFailureListener() {

            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Image upload not successful");
                Toast.makeText(EditUserActivity.this, getString(R.string.image_upload_not_successful), Toast.LENGTH_LONG).show();

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                Uri downloadUri = taskSnapshot.getDownloadUrl();

                //updateUserPhotoUriFirebase(downloadUri);
                UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri)
                        .build();

                user.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        Log.d(TAG, "USER PROFILE PIC UPDATED");
                    }
                });

                Log.d(TAG, "Image Download URL is: " + downloadUri);

            }
        });

    }

    /**
     * This is supposedly where the app will ask for user permissions but the dialog is not appearing for some reason
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PermissionUtils.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChosenTask.equals(getString(R.string.take_photo)))
                        cameraIntent();
                    else if(userChosenTask.equals(getString(R.string.choose_from_library)))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        //Check if the user is currently in edit mode
        if(mUserFirstName.getVisibility() == View.VISIBLE) {
            outState.putBoolean("isEditing", true);
            outState.putByteArray(getString(R.string.bitmap_byte_array), bitmapByteArray);
            Log.d(TAG, "OUTSTATE Bitmap is: " + bitmapByteArray);
            outState.putString(getString(R.string.firstName), mUserFirstName.getText().toString());
            outState.putString(getString(R.string.surName), mUserLastName.getText().toString());
            outState.putString(getString(R.string.mobile), mUserMobile.getText().toString());
        }

        //For the change user email alert dialog
        if(changeEmailDialog != null && changeEmailDialog.isShowing()) {
            EditText changeEmailET = (EditText) changeEmailDialog.getWindow().findViewById(R.id.change_user_email);
            EditText confirmPasswordET = (EditText) changeEmailDialog.getWindow().findViewById(R.id.confirm_password);

            String userEmail = changeEmailET.getText().toString();
            String confirmPassword = confirmPasswordET.getText().toString();
            changeEmailDialog.dismiss();

            outState.putBoolean(getString(R.string.change_user_email_alert_shown), true);
            outState.putString(getString(R.string.dialog_email), userEmail);
            outState.putString(getString(R.string.old_password), confirmPassword);
        }

        //For the change user password alert dialog
        if(changePasswordDialog != null && changePasswordDialog.isShowing()) {
            EditText changeOldPasswordET = (EditText) changePasswordDialog.getWindow().findViewById(R.id.old_password);
            EditText changeNewPassword1ET = (EditText) changePasswordDialog.getWindow().findViewById(R.id.new_password_1);
            EditText changeNewPassword2ET = (EditText) changePasswordDialog.getWindow().findViewById(R.id.new_password_2);

            String oldPassword = changeOldPasswordET.getText().toString();
            String newPassword1 = changeNewPassword1ET.getText().toString();
            String newPassword2 = changeNewPassword2ET.getText().toString();

            outState.putBoolean(getString(R.string.change_user_password_alert_shown), true);
            outState.putString(getString(R.string.old_password), oldPassword);
            outState.putString(getString(R.string.new_password_1), newPassword1);
            outState.putString(getString(R.string.new_password_2), newPassword2);
        }
    }


}


