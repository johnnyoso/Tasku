package com.example.android.tasku.fjd;

import android.content.Context;
import android.os.Bundle;

import com.example.android.tasku.R;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

/**
 * Created by john.osorio on 27/04/2018.
 */

public class JobDispatcherUtils {
    Context context;
    String taskKey;
    String taskName;
    int taskReminderFrequency;

    public JobDispatcherUtils (Context context, String taskKey, String taskName, int taskReminderFrequency) {
        this.context = context;
        this.taskKey = taskKey;
        this.taskName = taskName;
        this.taskReminderFrequency = taskReminderFrequency;
    }

    /**
     * This is used to remind user of pending open tasks
     * Perhaps call this when a new task has been created or re-set to open
     *
     */
    public void scheduleJob() {

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        Job job = createJob(dispatcher);
        dispatcher.mustSchedule(job);
    }

    /**
     * Set the parameters of the job the dispatcher will do
     * @param dispatcher
     * @return
     */
    public Job createJob(FirebaseJobDispatcher dispatcher) {

        Bundle taskBundle = new Bundle();
        taskBundle.putString(context.getString(R.string.taskKey), taskKey);
        taskBundle.putString(context.getString(R.string.taskName), taskName);

        Job job = dispatcher.newJobBuilder()
                //persist the task across boots
                .setLifetime(Lifetime.FOREVER)
                //.setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                //call this service when the criteria are met.
                .setService(ScheduledJobService.class)
                //unique id of the task --> this should be the newly created / re-opened task
                .setTag(taskKey)
                //don't overwrite an existing job with the same tag
                .setReplaceCurrent(false)
                // We are mentioning that the job is periodic.
                .setRecurring(true)
                // Run between 30 - 60 seconds from now.
                .setTrigger(Trigger.executionWindow(taskReminderFrequency, taskReminderFrequency))
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                //.setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                //Run this job only when the network is available.
                .setConstraints(Constraint.ON_ANY_NETWORK, Constraint.DEVICE_CHARGING)
                .setExtras(taskBundle)
                .build();

        return job;
    }

    /**
     * Cancel the task reminder job
     * Perhaps do this when the task has been set to closed or deleted
     *
     */
    public void cancelJob(String taskKey){

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        //Cancel all the jobs for this package
        dispatcher.cancelAll();

        // Cancel the job for this tag
//        dispatcher.cancel(taskKey);

    }

    /**
     * Update the task reminder job
     * @param dispatcher
     * @return
     */
    public Job updateJob(FirebaseJobDispatcher dispatcher) {
        Job newJob = dispatcher.newJobBuilder()
                //update if any task with the given tag exists.
                .setReplaceCurrent(true)
                //Integrate the job you want to start.
                .setService(ScheduledJobService.class)
                .setTag(taskKey)
                // Run between 30 - 60 seconds from now.
                .setTrigger(Trigger.executionWindow(30, 60))
                .build();
        return newJob;
    }
}
