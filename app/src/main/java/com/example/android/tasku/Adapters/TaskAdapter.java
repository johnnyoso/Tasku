package com.example.android.tasku.Adapters;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.example.android.tasku.MyTaskActivity;
import com.example.android.tasku.R;
import com.example.android.tasku.TaskDescriptionActivity;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by john.osorio on 25/08/2017.
 */

public class TaskAdapter extends RecyclerView.Adapter <TaskAdapter.ViewHolder> {

    private static final String TAG = TaskAdapter.class.getSimpleName();
    private List<HashMap<String, String>> taskNameList;
    private Context context;
    private Boolean isTwoPane;

    public TaskAdapter (Context context, List<HashMap<String, String>> taskNameList) {
        this.taskNameList = taskNameList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        TaskAdapter.ViewHolder viewHolder = null;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_list_item, parent, false);
        viewHolder = new TaskAdapter.ViewHolder(view, taskNameList);

        viewHolder.setOnClickListener(new ViewHolder.ClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                HashMap<String, String> IntentTaskMap = taskNameList.get(position);
                Bundle taskDescriptionBundle = new Bundle();
                for(Map.Entry item : IntentTaskMap.entrySet()) {
                    String taskKey = item.getKey().toString();
                    String taskName = item.getValue().toString();
                    taskDescriptionBundle.putString(context.getString(R.string.task_key), taskKey);
                    taskDescriptionBundle.putString(context.getString(R.string.task_name), taskName);

                }

//                Log.d(TAG, "Task Key is: " + taskDescriptionBundle.get("taskKey") + " Task Name is: " + taskDescriptionBundle.get("taskName"));
                Intent sendToTaskDescriptionActivityIntent = new Intent(context, TaskDescriptionActivity.class);
                sendToTaskDescriptionActivityIntent.putExtras(taskDescriptionBundle);
                context.startActivity(sendToTaskDescriptionActivityIntent);
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        HashMap<String, String> taskMap = taskNameList.get(position);
        for(Map.Entry item : taskMap.entrySet()) {
//            String taskKey = item.getKey().toString();
            String taskName = item.getValue().toString();
            holder.taskName.setText(taskName);
        }


    }

    @Override
    public int getItemCount() {
        return taskNameList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView taskName;

        public ViewHolder (View view, List<HashMap<String, String>> taskNameList) {
            super(view);

            taskName = (TextView) view.findViewById(R.id.task_list_name);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onItemClick(v, getAdapterPosition());
                }
            });
        }

        private ViewHolder.ClickListener mClickListener;

        public interface ClickListener {
            void onItemClick(View view, int position);
        }

        public void setOnClickListener(ViewHolder.ClickListener clickListener) {
            mClickListener = clickListener;
        }
    }

    public void removeAt(int position) {
        taskNameList.remove(position);
        notifyItemChanged(position);
        notifyItemRangeChanged(position, taskNameList.size());
    }
}
