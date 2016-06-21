package io.webmusic.webmusicbrowser;

import android.util.Log;

/**
 * Created by kawai on 6/20/16.
 */
public class Logger {
    public static void o(String Level, String TAG, String msg) {
        StackTraceElement calledClass = Thread.currentThread().getStackTrace()[3];
        String logTag = "" + TAG + ": " + calledClass.getFileName() + ":"
                + calledClass.getLineNumber();
        switch(Level) {
            case "d":
                Log.d(logTag, msg);
                break;
            case "i":
                Log.i(logTag, msg);
                break;
            case "e":
                Log.e(logTag, msg);
                break;
        }
    }
}
