package com.example.android.tasku.fcm;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.example.android.tasku.FriendsActivity;
import com.example.android.tasku.MyTaskActivity;
import com.example.android.tasku.NewTaskActivity;
import com.example.android.tasku.R;
import com.example.android.tasku.SignInActivity;
import com.example.android.tasku.TaskListWidgetProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by john.osorio on 20/09/2017.
 */

public class TaskuFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = TaskuFirebaseMessagingService.class.getSimpleName();
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;
    private String notificationTitle = null;
    private String notificationBody = null;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging
     */
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference();


        Log.d(TAG, "From: " + remoteMessage.getFrom());


        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {

            if(remoteMessage.getData().get(getString(R.string.notification_type)).equals(getString(R.string.new_task))){

                Log.d(TAG, "Message data payload: " + remoteMessage.getData());
                String taskKey = remoteMessage.getData().get(getString(R.string.taskKey));
                String taskCreatorUid = remoteMessage.getData().get(getString(R.string.taskCreator));
                final String taskStatus = remoteMessage.getData().get(getString(R.string.taskStatus));
                Log.d(TAG, "MESSAGE FROM Task Creator: " + taskCreatorUid + " with Task Key: " + taskKey + " Task Status: " + taskStatus);

                //Update the widget
                TaskListWidgetProvider.sendRefreshBroadcast(getApplicationContext());

//            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                //scheduleJob();
//            } else {
                // Handle message within 10 seconds
                //handleNow();
//            }

                mRef.child(getString(R.string.tasks)).child(taskKey).child(getString(R.string.taskAssignee)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot snap : dataSnapshot.getChildren()) {
                            String assigneeUid = snap.getKey().toString();
                            if(user.getUid().equals(assigneeUid)) {
                                Log.d(TAG, "This user is a TASK ASSIGNEE!!");
                                // Check if message contains a notification payload.
                                if (remoteMessage.getNotification() != null) {
                                    Log.d(TAG, "MESSAGE Notification Body: " + remoteMessage.getNotification().getBody());

                                    notificationTitle = remoteMessage.getNotification().getTitle();
                                    notificationBody = remoteMessage.getNotification().getBody();

                                    //Check if the task status is open, then send the notification
                                    if(taskStatus.equals("open")) {
                                        sendNotification(notificationTitle, notificationBody, getString(R.string.new_task_activity));


                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }else if(remoteMessage.getData().get(getString(R.string.notification_type)).equals(getString(R.string.new_friend_request))){

                Log.d(TAG, "Message data payload: " + remoteMessage.getData());
                final String currentUserRecipientUid = remoteMessage.getData().get(getString(R.string.current_uid));

                //If the current user is also the recipient of the friend request
                if(user.getUid().equals(currentUserRecipientUid)) {

                    notificationTitle = remoteMessage.getNotification().getTitle();
                    notificationBody = remoteMessage.getNotification().getBody();

                    sendNotification(notificationTitle, notificationBody, getString(R.string.new_friend_request_activity));

                }
            }
        }


        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with FCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options\

    }


    /**
     *
     * @param notificationTitle
     * @param notificationBody
     * @param activityDestination
     */
    private void sendNotification(String notificationTitle, String notificationBody, String activityDestination) {

        Intent intent = null;
        if(activityDestination.equals(getString(R.string.new_task_activity))){
            intent = new Intent(this, MyTaskActivity.class);

        } else if(activityDestination.equals(getString(R.string.new_friend_request_activity))) {
            intent = new Intent(this, FriendsActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setAutoCancel(true)   //Automatically delete the notification
                .setSmallIcon(R.drawable.tasku) //Notification icon
                .setContentIntent(pendingIntent)
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setSound(defaultSoundUri);


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());

    }
}
