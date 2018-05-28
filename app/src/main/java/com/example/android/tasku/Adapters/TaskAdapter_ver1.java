package com.example.android.tasku.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.tasku.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by john.osorio on 11/05/2018.
 */

public class TaskAdapter_ver1 extends RecyclerView.Adapter<TaskAdapter_ver1.TaskAdapter_ver1ViewHolder> {

    private static final String TAG = TaskAdapter_ver1.class.getSimpleName();

    private List<HashMap<String, String>> taskDataList;
    private Context context;

    private final TaskAdapater_ver1OnClickHandler mClickHandler;

    public interface TaskAdapater_ver1OnClickHandler {

        void onClick(String[] taskDataArray, int adapterPosition);
    }

    public TaskAdapter_ver1(TaskAdapater_ver1OnClickHandler clickHandler) {

        mClickHandler = clickHandler;
    }

    public class TaskAdapter_ver1ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.task_list_name) TextView taskName;

        public TaskAdapter_ver1ViewHolder (View view) {
            super(view);

            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Context context = v.getContext();
            int adapterPosition = getAdapterPosition();

            HashMap<String, String> taskMap = taskDataList.get(adapterPosition);
            for(Map.Entry item : taskMap.entrySet()) {
                String taskKey = item.getKey().toString();
                String taskName = item.getValue().toString();

                String[] taskDataArray = {taskKey, taskName};
                mClickHandler.onClick(taskDataArray, adapterPosition);
            }
        }
    }

    @Override
    public TaskAdapter_ver1.TaskAdapter_ver1ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.task_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        return new TaskAdapter_ver1ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TaskAdapter_ver1.TaskAdapter_ver1ViewHolder holder, int position) {

        HashMap<String, String> taskMap = taskDataList.get(position);
        for(Map.Entry item : taskMap.entrySet()) {
//            String taskKey = item.getKey().toString();
            String taskName = item.getValue().toString();

            holder.taskName.setText(taskName);

        }
    }

    @Override
    public int getItemCount() {
        if(taskDataList.size() == 0) {
            return 0;
        }
        return taskDataList.size();
    }

    public void setTaskDataList(List<HashMap<String, String>> taskDataList) {
        this.taskDataList = taskDataList;
        notifyDataSetChanged();
    }

    public void updateDataSet(){
        notifyDataSetChanged();
    }
}
