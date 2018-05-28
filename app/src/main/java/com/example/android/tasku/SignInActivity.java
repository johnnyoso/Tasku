package com.example.android.tasku;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.tasku.JavaUtils.InternetUtils;
import com.example.android.tasku.JavaUtils.SignInUtils;
import com.example.android.tasku.JavaUtils.UserUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = SignInActivity.class.getSimpleName();

    @BindView(R.id.user_email_login) EditText mUserEmail;
    @BindView(R.id.password) EditText mPassword;
    @BindView(R.id.my_toolbar) Toolbar myToolbar;
    @BindView(R.id.createAccount) Button mCreateAccountButton;
    @BindView(R.id.forgot_password) TextView mForgotPassword;
    @BindView(R.id.my_toolbar_title) TextView myToolbarTitle;
    @BindView(R.id.sign_in_progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.tasku_logo)
    ImageView taskuLogo;

    private String mUsername;
    private String mUserFirstName;
    private String mUserLastName;
    private String mPassword1;
    private String mPassword2;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseDatabase mDatabase;
    private DatabaseReference myRef;
    private UserProfileChangeRequest userProfileChangeRequest;

    private AlertDialog newUserDialog;
    private AlertDialog forgotPasswordDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        //Check for internet connection
        InternetUtils internetUtils = new InternetUtils(this);
        internetUtils.checkInternet();

        if(savedInstanceState != null ){

            if (savedInstanceState.getBoolean(getString(R.string.new_user_dialog_alert_shown), false)) {
                mUsername = savedInstanceState.getString(getString(R.string.dialog_username));
                mUserFirstName = savedInstanceState.getString(getString(R.string.dialog_user_first_name));
                mUserLastName = savedInstanceState.getString(getString(R.string.dialog_user_last_name));
                mPassword1 = savedInstanceState.getString(getString(R.string.dialog_password_1));
                mPassword2 = savedInstanceState.getString(getString(R.string.dialog_password_2));
                showNewUserDialog(mUsername, mUserFirstName, mUserLastName, mPassword1, mPassword2);

            } else if(savedInstanceState.getBoolean(getString(R.string.forgot_password_dialog_alert_shown), false)){
                mUsername = savedInstanceState.getString(getString(R.string.dialog_username));
                showForgotPasswordDialog(mUsername);
            }
        }

        ButterKnife.bind(this);
        setSupportActionBar(myToolbar);
        myToolbarTitle.setText(getString(R.string.sign_in_toolbar_title));

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        myRef = mDatabase.getReference();

        user = mAuth.getCurrentUser();


        FirebaseMessaging.getInstance().subscribeToTopic(getString(R.string.new_task_notifications));
        FirebaseMessaging.getInstance().subscribeToTopic(getString(R.string.new_friend_request_notifications));


        //Temporary only so I don't have to keep logging in
        if(user != null) {
            Intent intent = new Intent(this, MyTaskActivity.class);
            startActivity(intent);
        }

        //TODO 3: Check if the checkbox is ticked or not and save the user's email (data persist)

        mCreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewUserDialog(mUsername, mUserFirstName, mUserLastName, mPassword1, mPassword2);
            }
        });

        mForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog(mUsername);
            }
        });
    }

    /**
     * Sign in existing users
     * @param view
     */
    public void signInExistingUser(View view) {

        //Get the user's credentials from Edit Text
        String username = mUserEmail.getText().toString();
        String password = mPassword.getText().toString();

        //Check if it's a valid email address
        if(Patterns.EMAIL_ADDRESS.matcher(username).matches()) {

            //Check if the password is not empty
            if(!password.equals("")) {

                SignInUtils signInUtils = new SignInUtils(this, mAuth, username, password, progressBar, taskuLogo);
                signInUtils.execute();

                mPassword.setText("");
            }else {
                Toast.makeText(SignInActivity.this, getString(R.string.please_enter_your_password), Toast.LENGTH_SHORT).show();
            }


        }else {
            Toast.makeText(SignInActivity.this, getString(R.string.please_enter_a_valid_email_address), Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * This is so that after logging out of the app and returning to the login page, if the user presses the back button
     * it won't go back to the previous page
     */
    @Override
    public void onBackPressed() {
        //Do nothing here
    }


    /**
     * Show the dialog where the user can create an account
     * @param username
     * @param password1
     * @param password2
     */
    private void showNewUserDialog(String username, String firstname, String lastname, String password1, String password2) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_new_user, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle(getString(R.string.create_a_new_account));
        dialogBuilder.setMessage(getString(R.string.enter_details_below));

        //This is for data persistence when changing orientations
        final EditText newUsernameEmailEditText = (EditText) dialogView.findViewById(R.id.new_username_email);
        final EditText newUserFirstNameEditText = (EditText) dialogView.findViewById(R.id.new_user_first_name);
        final EditText newUserLastNameEditText = (EditText) dialogView.findViewById(R.id.new_user_last_name);
        final EditText newPasswordEditText1 = (EditText) dialogView.findViewById(R.id.new_password_1);
        final EditText newPasswordEditText2 = (EditText) dialogView.findViewById(R.id.new_password_2);

        final TextView cancelNewUserAccount = (TextView) dialogView.findViewById(R.id.cancel_new_user_account);
        final TextView confirmNewUserAccount = (TextView) dialogView.findViewById(R.id.confirm_new_user_account);

        newUsernameEmailEditText.setText(username);
        newUserFirstNameEditText.setText(firstname);
        newUserLastNameEditText.setText(lastname);
        newPasswordEditText1.setText(password1);
        newPasswordEditText2.setText(password2);

        cancelNewUserAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                newUsernameEmailEditText.setText("");
                newUserFirstNameEditText.setText("");
                newUserLastNameEditText.setText("");
                newPasswordEditText1.setText("");
                newPasswordEditText2.setText("");

                //Reset all String values for orientation changes
                mUsername = "";
                mUserFirstName = "";
                mUserLastName = "";
                mPassword1 = "";
                mPassword2 = "";

                newUserDialog.cancel();
            }
        });

        newUserDialog= dialogBuilder.create();

        //We set the click listener for the positive button this way so the dialog doesn't disappear when the positive button is clicked
        newUserDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
//                Button button = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                confirmNewUserAccount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        final String newUsernameEmail = newUsernameEmailEditText.getText().toString();
                        final String newUserFirstName = newUserFirstNameEditText.getText().toString();
                        final String newUserLastName = newUserLastNameEditText.getText().toString();

                        //Password should be limited to between 8 - 20 characters long
                        String newPassword1 = newPasswordEditText1.getText().toString();
                        String newPassword2 = newPasswordEditText2.getText().toString();
                        Log.d(TAG, newUsernameEmail.isEmpty() + " " + newUserFirstName.isEmpty() + " " + newUserLastName + " " + newPassword1 + " " + newPassword2);

                        if(!newUsernameEmail.isEmpty() || !newUserFirstName.isEmpty() || !newUserLastName.isEmpty() || !newPassword1.isEmpty() || !newPassword2.isEmpty()) {

                            if (Patterns.EMAIL_ADDRESS.matcher(newUsernameEmail).matches()) {

                                if (!newPassword1.equals("") || !newPassword2.equals("")) {

                                    //Password must be of the correct length
                                    if (newPassword1.length() >= 8 && newPassword1.length() <= 20) {

                                        if (newPassword1.equals(newPassword2)) {

                                            mAuth.createUserWithEmailAndPassword(newUsernameEmail, newPassword1)
                                                    .addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                                            if (task.isSuccessful()) {
                                                                // Sign in success, update UI with the signed-in user's information
                                                                Log.d(TAG, "createUserWithEmail:success");
                                                                user = mAuth.getCurrentUser();
                                                                newUserDialog.dismiss();

                                                                //Upload the user's email to Firebase Database
                                                                UserUtils newUserUtils = new UserUtils(newUsernameEmail, newUserFirstName, newUserLastName);

                                                                //Update the user's display name in Firebase
                                                                updateUserDisplayNameFirebase(newUserFirstName + " " + newUserLastName);

                                                                if (user != null) {
                                                                    String userID = user.getUid();
                                                                    myRef.child(getString(R.string.users)).child(userID).setValue(newUserUtils);
                                                                    myRef.child(getString(R.string.users)).child(userID).child(getString(R.string.user_id)).setValue(userID);

                                                                    //Send a verification email to user
                                                                    sendVerificationEmail(user);

                                                                    Intent intent = new Intent(SignInActivity.this, MyTaskActivity.class);
                                                                    startActivity(intent);

                                                                } else {
                                                                    Toast.makeText(SignInActivity.this, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show();
                                                                }


                                                            } else {
                                                                // If sign in fails, display a message to the user.
                                                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                                                Toast.makeText(SignInActivity.this, getString(R.string.authentication_failed) + task.getException(), Toast.LENGTH_LONG).show();

                                                            }
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(SignInActivity.this, getString(R.string.password_doesnt_match), Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(SignInActivity.this, getString(R.string.password_length), Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(SignInActivity.this, getString(R.string.please_enter_a_password), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(SignInActivity.this, getString(R.string.please_enter_a_valid_email_address), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SignInActivity.this, getString(R.string.please_fill_in_all_the_entries), Toast.LENGTH_SHORT).show();
                            newUsernameEmailEditText.setHintTextColor(getResources().getColor(android.R.color.holo_red_light));
                            newUserFirstNameEditText.setHintTextColor(getResources().getColor(android.R.color.holo_red_light));
                            newUserLastNameEditText.setHintTextColor(getResources().getColor(android.R.color.holo_red_light));
                            newPasswordEditText1.setHintTextColor(getResources().getColor(android.R.color.holo_red_light));
                            newPasswordEditText2.setHintTextColor(getResources().getColor(android.R.color.holo_red_light));
                        }
                    }
                });
            }
        });

        newUserDialog.show();
    }

    /**
     * Use this to send a reset password email to the registered user
     * @param username
     */
    private void showForgotPasswordDialog(String username){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_forgot_password, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle(getString(R.string.forgot_password));
        dialogBuilder.setMessage(getString(R.string.enter_email_below));

        final EditText forgotPasswordEmailEditText = (EditText) dialogView.findViewById(R.id.forgot_password_email);

        forgotPasswordEmailEditText.setText(username);

        //set the listener to null so the dialog won't close upon clicking the button
        dialogBuilder.setPositiveButton(getString(R.string.reset_password), null);

        dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                forgotPasswordEmailEditText.setText("");

                //Reset all String values for orientation changes
                mUsername = "";
                dialog.cancel();
            }
        });

        forgotPasswordDialog = dialogBuilder.create();

        forgotPasswordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String email = forgotPasswordEmailEditText.getText().toString();
                        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(SignInActivity.this, getString(R.string.email_sent), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SignInActivity.this, getString(R.string.this_email_is_not_yet_registered), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        forgotPasswordDialog.dismiss();
                    }
                });
            }
        });
        forgotPasswordDialog.show();
    }

    /**
     * This will send a verification email to the user
     * @param user
     */
    private void sendVerificationEmail(FirebaseUser user){
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email sent.");
                            Toast.makeText(SignInActivity.this, getString(R.string.verification_email_sent), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(newUserDialog != null && newUserDialog.isShowing()){
            EditText usernameET = (EditText) newUserDialog.getWindow().findViewById(R.id.new_username_email);
            EditText firstnameET = (EditText) newUserDialog.getWindow().findViewById(R.id.new_user_first_name);
            EditText lastNameET = (EditText) newUserDialog.getWindow().findViewById(R.id.new_user_last_name);
            EditText password1ET = (EditText) newUserDialog.getWindow().findViewById(R.id.new_password_1);
            EditText password2ET = (EditText) newUserDialog.getWindow().findViewById(R.id.new_password_2);

            //Get the values in the Edit Texts then close the dialog window
            String username = usernameET.getText().toString();
            String firstname = firstnameET.getText().toString();
            String lastname = lastNameET.getText().toString();
            String password1 = password1ET.getText().toString();
            String password2 = password2ET.getText().toString();
            newUserDialog.dismiss();

            outState.putBoolean(getString(R.string.new_user_dialog_alert_shown), true);
            outState.putString(getString(R.string.dialog_username), username);
            outState.putString(getString(R.string.dialog_user_first_name), firstname);
            outState.putString(getString(R.string.dialog_user_last_name), lastname);
            outState.putString(getString(R.string.dialog_password_1), password1);
            outState.putString(getString(R.string.dialog_password_2), password2);
        }

        if(forgotPasswordDialog != null && forgotPasswordDialog.isShowing()){
            EditText userEmailET = (EditText) forgotPasswordDialog.getWindow().findViewById(R.id.forgot_password_email);

            String userEmail = userEmailET.getText().toString();
            forgotPasswordDialog.dismiss();

            outState.putBoolean(getString(R.string.forgot_password_dialog_alert_shown), true);
            outState.putString(getString(R.string.dialog_username), userEmail);
        }
    }

    private void updateUserDisplayNameFirebase(String displayName){

        userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        user = mAuth.getCurrentUser();
        user.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    Log.d(TAG, "User display name Firebase updated");

                } else {
                    String errorMessage = task.getException().toString();
                    Log.d(TAG, "Display name Firebase upload error: " + errorMessage);
                }
            }
        });
    }
}