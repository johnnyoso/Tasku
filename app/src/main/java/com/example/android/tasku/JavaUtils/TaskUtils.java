package com.example.android.tasku.JavaUtils;

/**
 * Created by john.osorio on 25/08/2017.
 */

public class TaskUtils  {

    public String taskKey;
    public String taskName;
    public String taskDescription;
    public String taskCategory;
    public String taskCreator;
    public String taskDueDate;
    public String taskDueTime;
    public String taskStatus;

    public TaskUtils(){};

    public TaskUtils(String taskName){
        this.taskName = taskName;
    }

    public TaskUtils(String taskKey, String taskName, String taskDescription, String taskCreator, String taskDueDate, String taskDueTime, String taskStatus) {

        this.taskKey = taskKey;
        this.taskName = taskName;
        this.taskDescription = taskDescription;
        this.taskCreator = taskCreator;
        this.taskDueDate = taskDueDate;
        this.taskDueTime = taskDueTime;
        this.taskStatus = taskStatus;
    }

    public String getTaskKey() { return taskKey; }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskDescription(){ return taskDescription; }

    public String getTaskCreator() { return taskCreator; }

    public String getTaskDueDate() { return taskDueDate; }

    public String getTaskDueTime() { return taskDueDate; }

    public String getTaskStatus() { return taskStatus; }
}
