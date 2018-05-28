package com.example.android.tasku.JavaUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.android.tasku.MyTaskActivity;
import com.example.android.tasku.R;
import com.example.android.tasku.SignInActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by john.osorio on 21/05/2018.
 */

public class SignInUtils extends AsyncTask<Void, Void, Void> {

    private static final String TAG = SignInUtils.class.getSimpleName();

    private Context context;
    private FirebaseAuth mAuth;
    private String username;
    private String password;
    private ProgressBar progressBar;
    private ImageView taskuLogo;

    public SignInUtils(Context context, FirebaseAuth mAuth, String username, String password, ProgressBar progressBar, ImageView taskuLogo) {
        this.context = context;
        this.mAuth = mAuth;
        this.username = username;
        this.password = password;
        this.progressBar = progressBar;
        this.taskuLogo = taskuLogo;
    }

    @Override
    protected void onPreExecute() {
//        super.onPreExecute();

        progressBar.setAlpha(1);
        taskuLogo.setAlpha(0.5f);
    }

    @Override
    protected Void doInBackground(Void... voids) {

        //See if we can autheticate the user with Firebase Auth
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener((Activity)context, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");

                            //Head to Task Activity
                            Intent intent = new Intent(context, MyTaskActivity.class);
                            context.startActivity(intent);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(context, context.getString(R.string.authentication_failed) + errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
//        super.onPostExecute(aVoid);
            progressBar.setAlpha(1);
            taskuLogo.setAlpha(0.5f);

//            turnOffProgressBar();
    }

    public void turnOffProgressBar() {
        progressBar.setAlpha(0);
        taskuLogo.setAlpha(1);
    }
}
