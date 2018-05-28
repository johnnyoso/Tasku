package com.example.android.tasku.JavaUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.example.android.tasku.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;

/**
 * Created by john.osorio on 13/05/2018.
 */

public class AvatarImagesUtils extends AsyncTask<Void, Void, Void> {

    private static final String TAG = AvatarImagesUtils.class.getSimpleName();

    Context context;
    String avatarUid;
    FirebaseStorage mStorage;
    StorageReference userImageRef;

    ImageView avatarImageView;
    ProgressBar progressBar;

    public AvatarImagesUtils(Context context, String avatarUid, ImageView avatarImageView, ProgressBar progressBar){

        this.context = context;
        this.avatarUid = avatarUid;
        this.avatarImageView = avatarImageView;
        this.progressBar = progressBar;
        mStorage = FirebaseStorage.getInstance();
        userImageRef = mStorage.getReference().child(context.getString(R.string.images) + avatarUid + context.getString(R.string.png));

    }

    @Override
    protected Void doInBackground(Void... voids) {

        Log.d(TAG, "DO IN BACKGROUND");

        userImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                try {

                    //Show the progress bar
                    progressBar.setVisibility(View.VISIBLE);
                    avatarImageView.setVisibility(View.GONE);

                    Glide.with(context.getApplicationContext())
                            .load(uri)
                            .asBitmap()
                            .centerCrop()
                            .error(R.drawable.assigned_user_icon)
                            .into(new BitmapImageViewTarget(avatarImageView) {

                                @Override
                                protected void setResource(Bitmap resource) {
                                    RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                                    circularBitmapDrawable.setCircular(true);
                                    avatarImageView.setImageDrawable(circularBitmapDrawable);
                                }
                            });

                } catch (Exception e) {

                        Log.d(TAG, "TASK CREATOR doesn't location doesn't exist");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "TASK CREATOR doesn't exist anymore");
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {

                //Hide the progress bar
                progressBar.setVisibility(View.GONE);
                avatarImageView.setVisibility(View.VISIBLE);
            }
        });

        return null;
    }

}
