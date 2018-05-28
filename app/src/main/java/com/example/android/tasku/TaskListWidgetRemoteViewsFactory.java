package com.example.android.tasku;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by john.osorio on 8/04/2018.
 */

public class TaskListWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = TaskListWidgetRemoteViewsFactory.class.getSimpleName();

    private Context context;
    private List<String> taskList = new ArrayList<>();
    private int appWidgetId;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;

    public TaskListWidgetRemoteViewsFactory (Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference();

        getCurrentUserTasks();

        //This is where you grab the current user's tasks
//        for(int i = 0; i < 10; i++) {
//            String content = "TaskList: " + i;
//            taskList.add(content);
//        }
    }

    /**
     * Gets all the current user's tasks and puts them in the list
     */
    private void getCurrentUserTasks() {

        //If there is a user logged in then proceed
        if(user != null) {

            mRef.child(context.getString(R.string.users)).child(user.getUid()).child(context.getString(R.string.tasks)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snap : dataSnapshot.getChildren()) {
                        String taskUid = snap.getKey();
                        String taskStatus = snap.getValue().toString();

                        //Only get the open tasks
                        if(taskStatus.equals(context.getString(R.string.open))) {

                            //Next is to get the task name from the task database using the key
                            mRef.child(context.getString(R.string.tasks)).child(taskUid).child(context.getString(R.string.taskName)).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    String taskName = dataSnapshot.getValue().toString();
                                    taskList.add(taskName);
                                    Log.d(TAG, "Database task is: " + taskName);

                                    // refresh all your widgets
                                    AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                                    ComponentName cn = new ComponentName(context, TaskListWidgetProvider.class);
                                    mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widget_tasklist_view);
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
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return taskList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_task_list_item);



        if(taskList.get(position) != null) {
            rv.setTextViewText(R.id.widget_task_list_item_text_view, taskList.get(position));
        }
//        Log.d(TAG, "Remote View Task is: " + taskList.get(position));
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
