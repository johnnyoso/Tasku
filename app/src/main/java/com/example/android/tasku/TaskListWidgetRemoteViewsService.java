package com.example.android.tasku;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by john.osorio on 10/04/2018.
 */

public class TaskListWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TaskListWidgetRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}
