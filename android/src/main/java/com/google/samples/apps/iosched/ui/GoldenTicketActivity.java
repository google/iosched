package com.google.samples.apps.iosched.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.port.tasks.AppPrefs;
import com.google.samples.apps.iosched.port.tasks.Ticket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
        findViewById(R.id.mainViewGroup).setDrawingCacheEnabled(true);

        ticketType = (TextView) findViewById(R.id.ticketType);
        ticketMessage = (TextView) findViewById(R.id.ticketMessage);
        ticketImage = (ImageView) findViewById(R.id.ticketImage);
        ticketShareMessage = (TextView) findViewById(R.id.ticketShareMessage);

        showTicket();
    }

    public Bitmap takeScreenshot() {
        View rootView = findViewById(R.id.mainViewGroup);
        Bitmap drawingCache = rootView.getDrawingCache();
        int width = drawingCache.getWidth();
        return width > 480 ? Bitmap.createScaledBitmap(drawingCache, width /2, drawingCache.getHeight()/2, false) : drawingCache;
    }

    public File saveBitmap(Bitmap bitmap)
    {
        File imagePath = new File(Environment.getExternalStorageDirectory() + "/screenshot_"+ System.currentTimeMillis() +".jpg");
        FileOutputStream fos;
        try
        {
            fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
        }
        catch (IOException e)
        {
            Log.e("GREC", e.getMessage(), e);
        }

        return imagePath;
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
                Bitmap screenshot = takeScreenshot();
                new ShareScreenshotTask().execute(screenshot);
            }
        });
    }

    //Ugh.  Wouldn't normally use the AsyncTask, but this is pretty hacky.
    class ShareScreenshotTask extends AsyncTask<Bitmap, Void, File>
    {
        @Override
        protected File doInBackground(Bitmap... params)
        {
            return saveBitmap(params[0]);
        }

        @Override
        protected void onPostExecute(File file)
        {
            shareTicket(file);
        }
    }

    private void shareTicket(File imageFile)
    {
        Ticket ticket = AppPrefs.getInstance(this).getTicket();
        String ticketTypeString = ticket.ticketType == Ticket.Type.Gold ? "Golden" : "Silver";
        String shareMessage = "Hey @droidconnyc, I have a " + ticketTypeString + " ticket! My code '" + ticket.ticketCode + "'. #androidnyc";

        Intent intent=new Intent(android.content.Intent.ACTION_SEND);
        Uri uri = Uri.fromFile(imageFile);
        intent.setType("*/*");

        intent.putExtra(Intent.EXTRA_SUBJECT, "Droidcon NYC!");
        intent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        startActivity(Intent.createChooser(intent, "Go!"));
    }

}
