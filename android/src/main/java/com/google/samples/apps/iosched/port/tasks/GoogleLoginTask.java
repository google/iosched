package com.google.samples.apps.iosched.port.tasks;

import android.accounts.Account;
import android.content.Context;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.util.AccountUtils;

import co.touchlab.android.threading.eventbus.EventBusExt;
import co.touchlab.android.threading.tasks.TaskQueue;
import retrofit.RestAdapter;

/**
 * Created by kgalligan on 8/17/14.
 */
public class GoogleLoginTask extends TaskQueue.Task
{
    public boolean success;

    private String email;
    private String name;
    private String imageURL;

    public final boolean firstLogin;

    public static final String SCOPE = "audience:server:client_id:654878069390-0rs83f4a457ggmlln2jnmedv1b808bkv.apps.googleusercontent.com";

    public GoogleLoginTask(String email, String name, String imageUrl, boolean firstLogin)
    {
        this.email = email;
        this.name = name;
        this.imageURL = imageUrl;
        this.firstLogin = firstLogin;
    }

    public void run(Context context) throws Exception
    {
        String token = GoogleAuthUtil.getToken(context, email, SCOPE);
        RestAdapter restAdapter = DataHelper.makeRequestAdapter(context);
        GoogleLoginRequest loginRequest = restAdapter.create(GoogleLoginRequest.class);
        LoginResult loginResult = loginRequest.login(token, name);

        handleLoginResult(context, loginResult, email);
//        if (!TextUtils.isEmpty(imageURL))
//            CommandBusHelper.submitCommandSync(context, UploadAvatarCommand(imageURL));

        success = true;

        EventBusExt.getDefault().post(this);
    }

    @Override
    protected boolean handleError(Exception e)
    {
        //TODO: do something
        return true;
    }

    void handleLoginResult(Context context, LoginResult loginResult, String accountName)
    {
        AppPrefs appPrefs = AppPrefs.getInstance(context);
        appPrefs.setUserUuid(loginResult.uuid);
        appPrefs.setUserId(loginResult.userId);

        AccountUtils.setGcmKey(context, accountName, loginResult.gcmKey);

        UserAuthHelper.processLoginResonse(context, loginResult);

        Account activeAccount = AccountUtils.getActiveAccount(context);
        if(activeAccount != null)
            SyncHelper.requestManualSync(activeAccount);
    }
}
