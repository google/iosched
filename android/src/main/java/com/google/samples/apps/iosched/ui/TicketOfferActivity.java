package com.google.samples.apps.iosched.ui;

import com.google.samples.apps.iosched.port.superbus.EmailCodeCommand;
import com.google.samples.apps.iosched.ui.util.SystemUiHider;
import com.google.samples.apps.iosched.util.AccountUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import co.touchlab.android.superbus.appsupport.CommandBusHelper;
import co.touchlab.droidconnyc.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class TicketOfferActivity extends Activity {
    public static void callMe(Activity a)
    {
        Intent i = new Intent(a, TicketOfferActivity.class);
        a.startActivity(i);
    }

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ticket_offer);

        Button emailCode = (Button) findViewById(R.id.emailMe);
        emailCode.setText(getString(R.string.popup_email_code) + " to " + AccountUtils.getActiveAccount(this).name);
        emailCode.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CommandBusHelper.submitCommandAsync(TicketOfferActivity.this, new EmailCodeCommand());
                finish();
            }
        });

        findViewById(R.id.justClose).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        findViewById(R.id.registerNow).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
                String url = "https://www.squadup.com/events/droidconnyc";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
    }
}
