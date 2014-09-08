package com.google.samples.apps.iosched.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by kgalligan on 9/8/14.
 */
public class IOUtils
{
    public static String toString(InputStream in) throws IOException
    {
        InputStreamReader inputStreamReader = new InputStreamReader(in);
        char[] buff = new char[1024];
        int read;
        StringBuilder stringBuilder = new StringBuilder();
        while ((read = inputStreamReader.read(buff)) > -1)
        {
            stringBuilder.append(buff, 0, read);
        }

        return stringBuilder.toString();
    }
}
