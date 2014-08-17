package com.google.samples.apps.iosched.port.superbus;

import android.content.Context;

import co.touchlab.android.superbus.CheckedCommand;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;

/**
 * Created by kgalligan on 8/17/14.
 */
public class UploadAvatarCommand extends CheckedCommand
{
    private String imageURL;

    public UploadAvatarCommand(String imageURL) {
        this.imageURL = imageURL;
    }

    public UploadAvatarCommand() {
    }

    @Override
    public boolean handlePermanentError(Context context, PermanentException exception)
    {
        return false;
    }

    @Override
    public String logSummary() {
        return "imageURL: " + imageURL;
    }

    @Override
    public boolean same(Command command) {
        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException {
        byte[] body;

        /*if(imageURL.startsWith("http"))
        {
            BusHttpClient client = new BusHttpClient("");
            HttpResponse response = client.get(imageURL, null);
            client.checkAndThrowError();
            body = response.getBody();
        }
        else
        {
            FileInputStream inp = new FileInputStream(imageURL);
            body = IOUtils.toByteArray(inp);
                inp.close();
        }

        String uuid = AppPrefs.getInstance(context).getUserUuid();
        BusHttpClient postClient = new BusHttpClient(context.getString(R.string.base_url));
        postClient.addHeader("uuid", uuid);
        HttpResponse uploadResponse = postClient.post("dataTest/uploadAvatar", "image/jpeg", body);
        postClient.checkAndThrowError();

        String userResponseString = uploadResponse.getBodyAsString();
        if(userResponseString == null)
            throw new RuntimeException("No user response");
        Gson gson = new Gson();
        UserInfoResponse userInfoResponse = gson.fromJson(userResponseString, UserInfoResponse.class);
        AbstractFindUserTask.saveUserResponse(context, null, userInfoResponse);

        EventBusExt.getDefault().post(this);*/

        throw new UnsupportedOperationException("Not done");
    }
}
