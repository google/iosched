package com.google.samples.apps.iosched.port;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.samples.apps.iosched.port.tasks.FindUserByCodeTask;
import com.google.samples.apps.iosched.port.tasks.FollowUserTask;

import co.touchlab.android.threading.eventbus.EventBusExt;
import co.touchlab.android.threading.tasks.TaskQueue;
import co.touchlab.android.threading.tasks.TaskQueueActual;
import co.touchlab.droidconandroid.network.dao.UserAccount;
import co.touchlab.droidconandroid.network.dao.UserInfoResponse;
import co.touchlab.droidconnyc.R;

public class FindUserActivity extends Activity {

    private EditText userCode;
    private TextView nameText;
    private Button followUser;
    private UserInfoResponse userInfoResponse;

    public static void callMe(Activity activity)
    {
        Intent intent = new Intent(activity, FindUserActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_user);
        userCode = (EditText) findViewById(R.id.userCode);
        nameText = (TextView) findViewById(R.id.name);
        followUser = (Button) findViewById(R.id.followUser);

        findViewById(R.id.search).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                TaskQueue.execute(FindUserActivity.this, new FindUserByCodeTask(userCode.getText().toString()));
            }
        });

        followUser.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UserAccount user = userInfoResponse.getUser();
                TaskQueue.execute(FindUserActivity.this, new FollowUserTask(user.getId(), !user.getFollowing()));
                TaskQueue.execute(FindUserActivity.this, new FindUserByCodeTask(user.getUserCode()));
            }
        });

        EventBusExt.getDefault().register(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        EventBusExt.getDefault().unregister(this);
    }

    public void onEventMainThread(FindUserByCodeTask task)
    {
        userInfoResponse = task.getUserInfoResponse();
        boolean userNotFound = userInfoResponse == null;

        findViewById(R.id.childResults).setVisibility(userNotFound?View.GONE:View.VISIBLE);

        if(userNotFound)
        {
            Toast.makeText(this, "No result", Toast.LENGTH_LONG).show();
        }
        else
        {
            nameText.setText(task.getUserInfoResponse().getUser().getName());
            followUser.setText(userInfoResponse.getUser().getFollowing() ? "Unfollow" : "Follow");
        }
    }
}
