package com.dhchoi.crowdsourcingapp.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.dhchoi.crowdsourcingapp.Constants;
import com.dhchoi.crowdsourcingapp.LocationAgent;
import com.dhchoi.crowdsourcingapp.R;
import com.dhchoi.crowdsourcingapp.fragments.CrowdActivityFragment;
import com.dhchoi.crowdsourcingapp.fragments.TaskAvailableFragment;
import com.dhchoi.crowdsourcingapp.fragments.UserInfoFragment;
import com.dhchoi.crowdsourcingapp.task.Task;
import com.dhchoi.crowdsourcingapp.task.TaskManager;
import com.dhchoi.crowdsourcingapp.user.UserManager;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseGoogleApiActivity implements TaskManager.OnTasksUpdatedListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ProgressBar mSyncProgressBar;
    private ViewPager mViewPager; // The {@link ViewPager} that will host the section contents.

    // task related
    private List<OnTasksUpdatedListener> onTasksUpdatedListeners = new ArrayList<OnTasksUpdatedListener>();
    private List<Task> mActiveTasks = new ArrayList<>();
    private List<Task> mInactiveTasks = new ArrayList<>();
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "Broadcast Received");

            ArrayList<String> activatedTaskIds = intent.getStringArrayListExtra(LocationAgent.ACTIVATED_TASK_ID_KEY);
            ArrayList<String> inactivatedTaskIds = intent.getStringArrayListExtra(LocationAgent.INACTIVATED_TASK_ID_KEY);

            Log.d(Constants.TAG, "Activated: " + activatedTaskIds.toString());
            Log.d(Constants.TAG, "Inactivated: " + inactivatedTaskIds.toString());

            mActiveTasks.clear();
            for (String id : activatedTaskIds) {
                Task task = TaskManager.getTaskById(MainActivity.this, id);
                mActiveTasks.add(task);
            }

            mInactiveTasks.clear();
            for (String id : inactivatedTaskIds) {
                Task task = TaskManager.getTaskById(MainActivity.this, id);
                mInactiveTasks.add(task);
            }

//            String[] activatedTaskIds = intent.getStringArrayExtra(GeofenceTransitionsIntentService.ACTIVATED_TASK_ID_KEY);
//            Log.d(Constants.TAG, "activatedTaskIds: " + Arrays.toString(activatedTaskIds));
//            for (String activatedTaskId : intent.getStringArrayExtra(GeofenceTransitionsIntentService.ACTIVATED_TASK_ID_KEY)) {
//                for (int i = 0; i < mInactiveTasks.size(); i++) {
//                    Task inactiveTask = mInactiveTasks.get(i);
//                    if (inactiveTask.getId().equals(activatedTaskId)) {
//                        mInactiveTasks.remove(inactiveTask);
//                        mActiveTasks.add(inactiveTask);
//                    }
//                }
//            }
//
//            String[] inactivatedTaskIds = intent.getStringArrayExtra(GeofenceTransitionsIntentService.INACTIVATED_TASK_ID_KEY);
//            Log.d(Constants.TAG, "inactivatedTaskIds: " + Arrays.toString(inactivatedTaskIds));
//            for (String inactivatedTaskId : inactivatedTaskIds) {
//                for (int i = 0; i < mActiveTasks.size(); i++) {
//                    Task activeTask = mActiveTasks.get(i);
//                    if (activeTask.getId().equals(inactivatedTaskId)) {
//                        mActiveTasks.remove(activeTask);
//                        mInactiveTasks.add(activeTask);
//                    }
//                }
//            }

            triggerOnTasksUpdatedEvent();
        }
    };

    private LocationAgent.LocationChangeListener LocationListener;

    private TaskAvailableFragment mTaskAvailableFragment = TaskAvailableFragment.newInstance();
    private CrowdActivityFragment mCrowdActivityFragment = CrowdActivityFragment.newInstance();
    private UserInfoFragment mUserInfoFragment = UserInfoFragment.newInstance();

    {
        onTasksUpdatedListeners.add(mTaskAvailableFragment.getTaskAvailableListFragment());
        onTasksUpdatedListeners.add(mTaskAvailableFragment.getTaskAvailableMapFragment());
        onTasksUpdatedListeners.add(mUserInfoFragment);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.main_activity_container);
        if (mViewPager == null)
            Log.e(Constants.TAG, "ViewPager is null");
        else
            mViewPager.setAdapter(mSectionsPagerAdapter);

        // ProgressBar to show sync status
        mSyncProgressBar = (ProgressBar) findViewById(R.id.sync_progress_bar);

        // Set up the Tab Layout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout == null)
            Log.e(Constants.TAG, "TabLayout is null");
        else
            tabLayout.setupWithViewPager(mViewPager);

        // Register to receive messages.
//        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(GeofenceTransitionsIntentService.GEOFENCE_TRANSITION_BROADCAST));
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(LocationAgent.LOCATION_AGENT_BROADCAST));

        LocationListener = new LocationAgent.LocationChangeListener() {
            @Override
            public void onLocationChanged(Location location) {
                super.onLocationChanged(location);  // print log

                Intent intent = new Intent(MainActivity.this, LocationAgent.class);
                String latLngStr = new Gson().toJson(new LatLng(location.getLatitude(), location.getLongitude()));
                intent.setData(Uri.parse(latLngStr));
                startService(intent);
                Log.d(Constants.TAG, "Intent Sent from MainActivity");
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on the Home/Up button,
        // so long as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
//        } else if (id == R.id.action_check_current_location) {
//            startActivity(new Intent(this, CurrentLocationActivity.class));
//            return true;
        } else if (id == R.id.action_logout) {
            // reset or remove all data
            UserManager.reset(this);
            TaskManager.reset(this, getGoogleApiClient());

            // go back to login page
            startActivity(new Intent(this, CheckLoginActivity.class));

            // unregister location listener
            LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleApiClient(), LocationListener);

            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() > 0) {      // back to first page
            mViewPager.setCurrentItem(0, true);
            return;
        }

        if (mTaskAvailableFragment.isMapShown) {
            mTaskAvailableFragment.swapFragments();
            return;
        }

        super.onBackPressed();
    }

    @SuppressWarnings("All")
    @Override
    public void onConnected(Bundle bundle) {
        super.onConnected(bundle);

        // sync tasks with server
        mSyncProgressBar.setVisibility(ProgressBar.VISIBLE);
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return TaskManager.syncTasks(MainActivity.this, getGoogleApiClient());
            }

            @Override
            protected void onPostExecute(Boolean syncSuccess) {
                mSyncProgressBar.setVisibility(ProgressBar.GONE);
                Fragment currentFragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());
                View currentFragmentView = currentFragment.getView().findViewById(R.id.fragment_content);

                if (syncSuccess) {
                    Snackbar.make(currentFragmentView, "Sync success!", Snackbar.LENGTH_LONG).show();

                    // broadcast tasks to listeners
                    List<Task> allIncompleteTasks = TaskManager.getAllUnownedIncompleteTasks(MainActivity.this);

                    // TODO: add to geo fences
                    LocationAgent.addGeofences(allIncompleteTasks);

                    mActiveTasks = new ArrayList<>();
                    mInactiveTasks = new ArrayList<>();
                    for (Task t : allIncompleteTasks) {
                        if (t.isActivated()) {
                            mActiveTasks.add(t);
                        } else {
                            mInactiveTasks.add(t);
                        }
                    }

                    triggerOnTasksUpdatedEvent();
                } else {
                    Snackbar.make(currentFragmentView, "Failed to sync with server.", Snackbar.LENGTH_LONG).show();
                }
            }
        }.execute();

        mTaskAvailableFragment.getTaskAvailableMapFragment().updateCurrentLocation(this);

        LocationServices.FusedLocationApi.requestLocationUpdates(
                getGoogleApiClient(),
                LocationRequest.create()
                        .setInterval(5000)
                        .setFastestInterval(1000)
                        .setSmallestDisplacement(0.0001f)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                LocationListener);
    }

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onTasksCreatedByOthers(List<Task> createdTasksByOthers) {

    }

    @Override
    public void onTasksCreatedByUser(List<Task> createdTasksByUser) {

    }

    @Override
    public void onTasksDeleted(List<String> deletedTaskIds) {

    }

    // TODO: temp
    @Override
    public void onTasksUpdated(String taskId) {
        Task task = TaskManager.getTaskById(this, taskId);
        boolean isActive = task.isActivated();

        for (int i = 0; i < mActiveTasks.size(); i++) {
            if (mActiveTasks.get(i).getId().equals(taskId)) {
                mActiveTasks.set(i, TaskManager.getTaskById(this, taskId));
                if (!isActive) {
                    mActiveTasks.remove(i);
                    mInactiveTasks.add(task);
                }
                return;
            }
        }

        for (int i = 0; i < mInactiveTasks.size(); i++) {
            if (mInactiveTasks.get(i).getId().equals(taskId)) {
                mInactiveTasks.set(i, TaskManager.getTaskById(this, taskId));
                if (isActive) {
                    mInactiveTasks.remove(i);
                    mActiveTasks.add(task);
                }
            }
        }

        // update the list and map displays
        for (OnTasksUpdatedListener listener : onTasksUpdatedListeners) {
            listener.onTasksActivationUpdated(mActiveTasks, mInactiveTasks);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        List<FragmentWrapper> fragmentWrappers = new ArrayList<FragmentWrapper>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            fragmentWrappers.add(new FragmentWrapper(TaskAvailableFragment.NAME, mTaskAvailableFragment));
            // fragmentWrappers.add(new FragmentWrapper(CrowdActivityFragment.NAME, mCrowdActivityFragment));
            fragmentWrappers.add(new FragmentWrapper(UserInfoFragment.NAME, mUserInfoFragment));
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentWrappers.get(position).fragment;
        }

        @Override
        public int getCount() {
            return fragmentWrappers.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentWrappers.get(position).fragmentName;
        }

        class FragmentWrapper {
            String fragmentName;
            Fragment fragment;

            FragmentWrapper(String fragmentName, Fragment fragment) {
                this.fragmentName = fragmentName;
                this.fragment = fragment;
            }
        }
    }

    /**
     *
     */
    private void triggerOnTasksUpdatedEvent() {
        // remove geofence location tracking
        List<String> completedTaskIds = new ArrayList<>();

        // ignore completed tasks
        for (Task t : mActiveTasks) {
            if (t.isCompleted()) {
                mActiveTasks.remove(t);
                completedTaskIds.add(t.getId());
            }
        }
        for (Task t : mInactiveTasks) {
            if (t.isCompleted()) {
                mInactiveTasks.remove(t);
                completedTaskIds.add(t.getId());
            }
        }

        if (completedTaskIds.size() > 0)
            LocationServices.GeofencingApi.removeGeofences(getGoogleApiClient(), completedTaskIds);

        for (OnTasksUpdatedListener onTasksUpdatedListener : onTasksUpdatedListeners) {
            onTasksUpdatedListener.onTasksActivationUpdated(mActiveTasks, mInactiveTasks);
        }
    }

    /**
     * Interface for listening to near real-time TasksUpdated events.
     * Activated tasks are tasks that were activated by the geofence.
     * Inactivated tasks are tasks that were inactivated by the geofence.
     */
    public interface OnTasksUpdatedListener {
        void onTasksActivationUpdated(List<Task> activatedTasks, List<Task> inactivatedTasks);
    }
}
