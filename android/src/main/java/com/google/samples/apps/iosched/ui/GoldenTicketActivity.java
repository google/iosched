package com.google.samples.apps.iosched.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.port.tasks.AppPrefs;
import com.google.samples.apps.iosched.port.tasks.Ticket;

import java.io.IOException;
import java.io.InputStream;

import co.touchlab.droidconnyc.R;

public class GoldenTicketActivity extends Activity {

    private TextView ticketType;
    private TextView ticketMessage;
    private ImageView ticketImage;
    private TextView ticketShareMessage;

    public static void callMe(Activity a)
    {
        Intent i = new Intent(a, GoldenTicketActivity.class);
        a.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_golden_ticket);

        ticketType = (TextView) findViewById(R.id.ticketType);
        ticketMessage = (TextView) findViewById(R.id.ticketMessage);
        ticketImage = (ImageView) findViewById(R.id.ticketImage);
        ticketShareMessage = (TextView) findViewById(R.id.ticketShareMessage);

        showTicket();
    }

    private void showTicket()
    {
        AppPrefs appPrefs = AppPrefs.getInstance(this);
        Ticket ticket = appPrefs.getTicket();
        boolean gold = ticket.ticketType == Ticket.Type.Gold;

        ticketType.setText(ticket.ticketType.name());
        ticketMessage.setText(ticket.message);

        try
        {

            // get input stream
            InputStream ims = getAssets().open(gold ? "kid.png" : "cat.jpg");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            ticketImage.setImageDrawable(d);
        }
        catch (IOException e)
        {
            //Nope
        }

        findViewById(R.id.mainViewGroup).setBackgroundColor(getResources().getColor(gold ? R.color.ticket_gold : R.color.ticket_silver));

        ticketShareMessage.setText("Share code '"+ ticket.ticketCode +"' to get your "+ (gold ? "free Pass" : "Discount") +"!");
        ticketShareMessage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                shareTicket();
            }
        });
    }

    private void shareTicket()
    {
        Ticket ticket = AppPrefs.getInstance(this).getTicket();
        String ticketTypeString = ticket.ticketType == Ticket.Type.Gold ? "Golden" : "Silver";
        String shareMessage = "Hey @droidconnyc, I have a " + ticketTypeString + " ticket! My code '" + ticket.ticketCode + "'. #androidnyc";

        Intent intent=new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        intent.putExtra(Intent.EXTRA_SUBJECT, "Droidcon NYC!");
        intent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        startActivity(Intent.createChooser(intent, "Go!"));
    }

}
