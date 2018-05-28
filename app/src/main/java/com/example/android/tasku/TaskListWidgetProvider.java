package com.example.android.tasku;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Created by john.osorio on 10/04/2018.
 */

public class TaskListWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for(int appWidgetId : appWidgetIds) {

//            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tasklist_layout);
//            Intent intent = new Intent(context, TaskListWidgetRemoteViewsService.class);
//            views.setRemoteAdapter(R.id.widget_tasklist_view, intent);
//            appWidgetManager.updateAppWidget(appWidgetId, views);

            updateAppWidget(context, appWidgetManager, appWidgetId);
            Toast.makeText(context, context.getString(R.string.widget_has_been_updated), Toast.LENGTH_SHORT).show();

        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tasklist_layout);
//            Create an intent with the AppWidgetManager
            Intent intentUpdate = new Intent(context, TaskListWidgetRemoteViewsService.class);

            views.setRemoteAdapter(R.id.widget_tasklist_view, intentUpdate);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

            //Update the current widget instance only
            int[] idArray = new int[]{appWidgetId};
            intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray);

            //Wrap the intent as a Pending Intent
            PendingIntent pendingUpdate = PendingIntent.getBroadcast(context, appWidgetId, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT);

            //Send the pending intent when user presses the update button
            views.setOnClickPendingIntent(R.id.update_widget_button, pendingUpdate);


    }

    public static void sendRefreshBroadcast(Context context) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setComponent(new ComponentName(context, TaskListWidgetProvider.class));
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            // refresh all your widgets
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, TaskListWidgetProvider.class);
            mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widget_tasklist_view);
        }
        super.onReceive(context, intent);
    }
}
