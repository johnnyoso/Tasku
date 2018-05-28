package com.example.android.tasku.fjd;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.android.tasku.MyTaskActivity;
import com.example.android.tasku.R;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;


/**
 * Created by john.osorio on 27/04/2018.
 */

public class ScheduledJobService extends JobService {

    private static final String TAG = ScheduledJobService.class.getSimpleName();

    @Override
    public boolean onStartJob(final JobParameters params) {

        Bundle taskBundle = params.getExtras();
        final String taskKey = taskBundle.getString(getString(R.string.taskKey));
        final String taskName = taskBundle.getString(getString(R.string.taskName));

//        mDatabase = FirebaseDatabase.getInstance();
//        userRef = mDatabase.getReference(getString(R.string.users));
//        taskRef = mDatabase.getReference(getString(R.string.tasks));
//        mAuth = FirebaseAuth.getInstance();
//        mUser = mAuth.getCurrentUser();
//
//        //get all open tasks
//        userRef.child(mUser.getUid()).child(getString(R.string.tasks)).child(taskKey).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//
//                String taskStatus = dataSnapshot.getValue().toString();
//                Log.d(TAG, "TASK STATUS IS: " + taskStatus);
//
//                if(taskStatus.equals(getString(R.string.open))) {
//
//                    taskRef.child(taskKey).child(getString(R.string.taskName)).addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(DataSnapshot dataSnapshot) {
//                            taskName = dataSnapshot.getValue().toString();
//                            Log.d(TAG, "Open Task is: " + taskName);
//
//                        }
//
//                        @Override
//                        public void onCancelled(DatabaseError databaseError) {
//
//                        }
//                    });
//
//                }
//
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });

        //Offloading work to a new thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Job you want to run
                taskReminder(params, taskKey, taskName);
            }
        }).start();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public void taskReminder(final JobParameters parameters, final String taskKey, final String taskName) {

        try{
            Log.d(TAG, "completeJob: " + "jobStarted");
            Log.d(TAG, "TASK REMINDER KEY IS: " + taskKey);
            Log.d(TAG, "TASK NAME IS: " + taskName);

            sendNotification(taskName);

            Thread.sleep(2000);

            Log.d(TAG, "completeJob: " + "jobFinished");

        } catch (InterruptedException e){
            e.printStackTrace();

        } finally {

            jobFinished(parameters, true);
        }
    }

    public void sendNotification(String notificationBody) {
        Intent intent = null;

        intent = new Intent(this, MyTaskActivity.class);


        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setAutoCancel(true)   //Automatically delete the notification
                .setSmallIcon(R.drawable.tasku) //Notification icon
                .setContentIntent(pendingIntent)
                .setContentTitle("Reminder")
                .setContentText(notificationBody)
                .setSound(defaultSoundUri);


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }
}
