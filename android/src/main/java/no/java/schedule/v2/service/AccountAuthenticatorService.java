package no.java.schedule.v2.service;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;

/**
 * Created by kkho on 16.04.2016.
 */
public class AccountAuthenticatorService extends Service {

    private static final String DUMMY_ACCOUNT_NAME = "no.java.schedule.v2";
    private static final String DUMMY_ACCOUNT_TYPE = "no.java.schedule.v2";
    private static final String DUMMY_AUTH_TOKEN = "authtoken";


    @Override
    public IBinder onBind(final Intent pIntent) {
        return new AccountAuthenticator(this).getIBinder();
    }

    private class AccountAuthenticator extends AbstractAccountAuthenticator {


        public AccountAuthenticator(Context context) {
            super(context);
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response, String s) {
            return new Bundle();
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse, String s, String s1, String[] strings, Bundle bundle) throws NetworkErrorException {
            bundle.putString(KEY_ACCOUNT_NAME, DUMMY_ACCOUNT_NAME);
            bundle.putString(KEY_ACCOUNT_TYPE, DUMMY_ACCOUNT_TYPE);
            return bundle;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException {
            bundle.putBoolean(KEY_BOOLEAN_RESULT, true);
            return bundle;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
            bundle.putString(KEY_ACCOUNT_NAME, DUMMY_ACCOUNT_NAME);
            bundle.putString(KEY_ACCOUNT_TYPE, DUMMY_ACCOUNT_TYPE);
            bundle.putString(KEY_AUTHTOKEN, DUMMY_AUTH_TOKEN);
            return bundle;
        }

        @Override
        public String getAuthTokenLabel(String s) {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
            bundle.putString(KEY_ACCOUNT_NAME, DUMMY_ACCOUNT_NAME);
            bundle.putString(KEY_ACCOUNT_TYPE, DUMMY_ACCOUNT_TYPE);
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strings) throws NetworkErrorException {
            Bundle bundle = new Bundle();
            bundle.putBoolean(KEY_BOOLEAN_RESULT, true);
            return bundle;
        }
    }
}