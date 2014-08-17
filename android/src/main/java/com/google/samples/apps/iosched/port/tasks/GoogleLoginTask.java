package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;

import com.google.android.gms.auth.GoogleAuthUtil;

import co.touchlab.android.threading.eventbus.EventBusExt;
import retrofit.RestAdapter;

/**
 * Created by kgalligan on 8/17/14.
 */
public class GoogleLoginTask extends AbstractLoginTask{
    public boolean success;

    private String email;
    private String name;
    private String imageURL;

    public final boolean firstLogin;

    public static final String SCOPE = "audience:server:client_id:654878069390-0rs83f4a457ggmlln2jnmedv1b808bkv.apps.googleusercontent.com";

    public GoogleLoginTask(String email, String name, String imageUrl, boolean firstLogin) {
        this.email = email;
        this.name = name;
        this.imageURL = imageUrl;
        this.firstLogin = firstLogin;
    }

    public void run(Context context)
    {
        try {
            String token = GoogleAuthUtil.getToken(context, email, SCOPE);
            RestAdapter restAdapter = DataHelper.makeRequestAdapter(context);
            GoogleLoginRequest loginRequest = restAdapter.create(GoogleLoginRequest.class);
            LoginResult loginResult = loginRequest.login(token, name);

            handleLoginResult(context, loginResult);
//        if (!TextUtils.isEmpty(imageURL))
//            CommandBusHelper.submitCommandSync(context, UploadAvatarCommand(imageURL));

            success = true;
        } catch (Exception e) {
            //TODO: log
        }

        EventBusExt.getDefault().post(this);
    }

}
