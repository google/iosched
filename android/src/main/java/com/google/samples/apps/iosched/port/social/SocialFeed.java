package com.google.samples.apps.iosched.port.social;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.TimeUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.samples.apps.iosched.ui.BaseActivity;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import co.touchlab.android.threading.eventbus.EventBusExt;
import co.touchlab.android.threading.tasks.TaskQueue;
import co.touchlab.droidconnyc.R;

public class SocialFeed extends BaseActivity
{
    private ListView socialList;

    public static void callMe(Activity a)
    {
        Intent i = new Intent(a, SocialFeed.class);
        a.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_feed);
        TaskQueue.execute(this, "network", new FetchSocialFeedTask());
        EventBusExt.getDefault().register(this);
        socialList = (ListView) findViewById(R.id.socialList);

    }

    public void onEventMainThread(FetchSocialFeedTask task)
    {
        if(task.socialEntries != null)
        {
            socialList.setAdapter(new SocialFeedAdapter(this, task.socialEntries));
            socialList.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    SocialEntry entry = ((SocialFeedAdapter) socialList.getAdapter()).getItem(position);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/" + entry.username + "/status/" + entry.sourceId));
                    startActivity(browserIntent);
                }
            });
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        EventBusExt.getDefault().unregister(this);
    }

    public class SocialFeedAdapter extends ArrayAdapter<SocialEntry>
    {
        private LayoutInflater layoutInflater;
        private DateFormat format = new SimpleDateFormat("MM/dd/yy");
        public SocialFeedAdapter(Context context, List<SocialEntry> objects)
        {
            super(context, android.R.layout.simple_list_item_1, objects);
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if(convertView == null)
            {
                convertView = layoutInflater.inflate(R.layout.list_item_social_entry, null);
            }

            ImageView profileImage = (ImageView) convertView.findViewById(R.id.profileImage);
            TextView socialDisplayName = (TextView) convertView.findViewById(R.id.socialDisplayName);
            TextView socialUsername = (TextView) convertView.findViewById(R.id.socialUsername);
            TextView socialTime = (TextView) convertView.findViewById(R.id.socialTime);
            TextView textVal = (TextView) convertView.findViewById(R.id.textVal);

            SocialEntry socialEntry = getItem(position);
            Picasso.with(SocialFeed.this).load(socialEntry.profileImage).into(profileImage);
            socialDisplayName.setText(socialEntry.screenName);
            socialUsername.setText("@"+ socialEntry.username);
            socialTime.setText(timeSince(socialEntry.createdAt));
            textVal.setText(socialEntry.textVal);

            return convertView;
        }

        private String timeSince(Date date)
        {
            long diff = System.currentTimeMillis() - date.getTime();
            if(diff < 0)
                return "";
            else if(diff < DateUtils.MINUTE_IN_MILLIS)
                return Integer.toString(((int)diff/1000)) + "s";
            else if(diff < DateUtils.HOUR_IN_MILLIS)
                return Integer.toString(((int)diff/(int)DateUtils.MINUTE_IN_MILLIS)) + "m";
            else if(diff < DateUtils.DAY_IN_MILLIS)
                return Integer.toString(((int)diff/(int)DateUtils.HOUR_IN_MILLIS)) + "h";

            else
            {
                long days = diff / DateUtils.DAY_IN_MILLIS;
                if(days < 10)
                    return Integer.toString((int)days) + "d";
                else
                    return format.format(date);
            }
        }
    }
}
