package com.dhchoi.crowdsourcingapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dhchoi.crowdsourcingapp.R;
import com.dhchoi.crowdsourcingapp.activities.MainActivity;
import com.dhchoi.crowdsourcingapp.activities.TaskCreateActivity;
import com.dhchoi.crowdsourcingapp.activities.TaskInfoActivity;
import com.dhchoi.crowdsourcingapp.task.Task;
import com.dhchoi.crowdsourcingapp.task.TaskManager;
import com.dhchoi.crowdsourcingapp.user.UserManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserInfoFragment extends Fragment implements MainActivity.OnTasksUpdatedListener {

    public static final String NAME = "MY INFO";
    private final int COLOR_ON = 0xffffffff;
    private final int COLOR_OFF = 0x22000000;

    // task related
    private List<Task> mCreatedTasks = new ArrayList<>();
    private List<Task> mCompletedTasks = new ArrayList<>();

    private ArrayAdapter<Task> mCreatedTaskListAdapter;
    private ArrayAdapter<Task> mCompletedTaskListAdapter;

    private TextView mUserId;
    private TextView mUserBalance;
    private TextView mCreatedTasksNotice;
    private TextView mCompletedTasksNotice;
    private ListView mListCreatedTasks;
    private ListView mListCompletedTasks;
    private TextView mNumCreatedTasks;
    private TextView mNumCompletedTasks;
    private LinearLayout mNumCreatedTasksTitle;
    private LinearLayout mNumCompletedTasksTitle;
    private SwipeRefreshLayout mSwipeRefresh;

    private static final int TIME_OFFSET = 1000 * 3600 * 4;

    public UserInfoFragment() {
    }

    public static UserInfoFragment newInstance() {
        return new UserInfoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_user_info, container, false);

        final FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), TaskCreateActivity.class);
                startActivity(intent);
            }
        });

        final LinearLayout createdTasksContainer = (LinearLayout) rootView.findViewById(R.id.created_tasks_container);
        final LinearLayout completedTasksContainer = (LinearLayout) rootView.findViewById(R.id.completed_tasks_container);

        // setup created-tasks related lists
        mListCreatedTasks = (ListView) rootView.findViewById(R.id.list_created_tasks);
        mListCreatedTasks.setAdapter(mCreatedTaskListAdapter = new CreatedTaskListAdapter(getActivity(), mCreatedTasks));
        mListCreatedTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), TaskInfoActivity.class);
                intent.putExtra("taskId", ((Task) mListCreatedTasks.getAdapter().getItem(position)).getId());
                startActivity(intent);
            }
        });
        mNumCreatedTasksTitle = (LinearLayout) rootView.findViewById(R.id.num_created_tasks_title_layout);
        mNumCreatedTasksTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (createdTasksContainer.getVisibility() == View.GONE) {
                    // enable task created
                    createdTasksContainer.setVisibility(View.VISIBLE);
                    mNumCreatedTasksTitle.setBackgroundColor(COLOR_ON);
                    // disable task completed
                    completedTasksContainer.setVisibility(View.GONE);
                    mNumCompletedTasksTitle.setBackgroundColor(COLOR_OFF);
                }
            }
        });

        // setup completed-tasks related lists
        mListCompletedTasks = (ListView) rootView.findViewById(R.id.list_completed_tasks);
        mListCompletedTasks.setAdapter(mCompletedTaskListAdapter = new CompletedTaskListAdapter(getActivity(), mCompletedTasks));
        mListCompletedTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getActivity(), "Task Clicked", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getActivity(), TaskInfoActivity.class);
                intent.putExtra("taskId", ((Task)mListCompletedTasks.getAdapter().getItem(position)).getId());
                startActivity(intent);
            }
        });
        mNumCompletedTasksTitle = (LinearLayout) rootView.findViewById(R.id.num_completed_tasks_title_layout);
        mNumCompletedTasksTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (completedTasksContainer.getVisibility() == View.GONE) {
                    // disable task created
                    createdTasksContainer.setVisibility(View.GONE);
                    mNumCreatedTasksTitle.setBackgroundColor(COLOR_OFF);
                    // enable task completed
                    completedTasksContainer.setVisibility(View.VISIBLE);
                    mNumCompletedTasksTitle.setBackgroundColor(COLOR_ON);
                }
            }
        });

        // update views
        mNumCreatedTasks = (TextView) rootView.findViewById(R.id.num_created_tasks);
        mNumCompletedTasks = (TextView) rootView.findViewById(R.id.num_completed_tasks);
        mCreatedTasksNotice = (TextView) rootView.findViewById(R.id.created_tasks_notice);
        mCompletedTasksNotice = (TextView) rootView.findViewById(R.id.completed_tasks_notice);

        String userId = UserManager.getUserId(getActivity());
        mUserId = (TextView) rootView.findViewById(R.id.user_id);
        mUserId.setText(userId);

        mUserBalance = (TextView) rootView.findViewById(R.id.available_balance);

        // swipe refresh layout
        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.layout_swipe_refresh);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        // update the two layouts
                        TaskManager.syncTasks(getActivity(), ((MainActivity)getActivity()).getGoogleApiClient());
                        UserManager.syncUser(getActivity());
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        mSwipeRefresh.setRefreshing(false);
                        Snackbar.make(getView(), "Sync success!", Snackbar.LENGTH_LONG).show();
                    }
                }.execute();
            }
        });

        updateNoticeTextViews();

        return rootView;
    }

    private void updateNoticeTextViews() {
        mCreatedTasksNotice.setVisibility(mCreatedTaskListAdapter.getCount() > 0 ? TextView.GONE : TextView.VISIBLE);
        mCompletedTasksNotice.setVisibility(mCompletedTaskListAdapter.getCount() > 0 ? TextView.GONE : TextView.VISIBLE);
    }

    @SuppressWarnings("All")
    public void updateUserTextViews() {
        mUserBalance.setText(String.format("%.1f", UserManager.getUserBalance(getActivity())));
    }

    private void fetchTasks() {
        // fetch tasks
        mCreatedTasks.clear();
        mCreatedTasks.addAll(TaskManager.getAllOwnedTasks(getActivity()));
        mCreatedTaskListAdapter.notifyDataSetChanged();
        mNumCreatedTasks.setText(String.valueOf(mCreatedTasks.size()));
        if (mCreatedTaskListAdapter.getCount() > 0)
            mCreatedTasksNotice.setVisibility(View.GONE);
        else
            mCreatedTasksNotice.setVisibility(View.VISIBLE);

        mCompletedTasks.clear();
        mCompletedTasks.addAll(TaskManager.getAllUnownedCompletedTasks(getActivity()));
        mCompletedTaskListAdapter.notifyDataSetChanged();
        mNumCompletedTasks.setText(String.valueOf(mCompletedTasks.size()));
        if (mCompletedTaskListAdapter.getCount() > 0)
            mCompletedTasksNotice.setVisibility(View.GONE);
        else
            mCompletedTasksNotice.setVisibility(View.VISIBLE);

        if (mSwipeRefresh.isRefreshing())
            mSwipeRefresh.setRefreshing(false);
    }

    @Override
    public void onTasksActivationUpdated(List<Task> activatedTasks, List<Task> inactivatedTasks) {
        fetchTasks();
    }

    class CreatedTaskListAdapter extends ArrayAdapter<Task> {

        public CreatedTaskListAdapter(Context context, List<Task> tasks) {
            super(context, android.R.layout.simple_list_item_1, tasks);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Task task = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_task_created, parent, false);
            }

            ((TextView) convertView.findViewById(R.id.num_submitted_response)).setText(task.getName());
            final View finalConvertView = convertView;
            new AsyncTask<String, Void, JSONArray>() {
                @Override
                protected JSONArray doInBackground(String... params) {
                    return TaskManager.getTaskResponses(params[0]);
                }

                @Override
                protected void onPostExecute(JSONArray jsonArray) {
                    try {
                        if (jsonArray.length() > 0) {
                            ((TextView)finalConvertView.findViewById(R.id.num_answers)).setText(
                                    String.valueOf(jsonArray.length()));

                            // "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
                            String dateString = jsonArray.getJSONObject(jsonArray.length() - 1)
                                    .get("createdAt").toString();
                            dateString = dateString.substring(0, dateString.length() - 1).replace('T', ' ');
                            long last = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                                    .parse(dateString).getTime() - TIME_OFFSET;
                            // TODO: figure out why this weird time difference is happening
                            long now = new Date().getTime();

                            ((TextView)finalConvertView.findViewById(R.id.last_answer_time)).setText(
                                    String.valueOf((now - last) / (1000 * 60)) + " minutes ago");
                        } else
                            ((TextView)finalConvertView.findViewById(R.id.last_answer_time)).setText("N/A");
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                }
            }.execute(task.getId());

            return convertView;
        }
    }

    class CompletedTaskListAdapter extends ArrayAdapter<Task> {

        public CompletedTaskListAdapter(Context context, List<Task> tasks) {
            super(context, 0, tasks);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Task task = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_task_completed, parent, false);
            }

            ((TextView) convertView.findViewById(R.id.num_submitted_response)).setText(task.getName());
            ((TextView) convertView.findViewById(R.id.task_location)).setText(task.getLocation().getName());
            ((TextView) convertView.findViewById(R.id.task_cost)).setText("$" + task.getCost());

            return convertView;
        }
    }

}
