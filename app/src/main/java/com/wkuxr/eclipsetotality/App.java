package com.wkuxr.eclipsetotality;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    private static Context context;
    public static Context getContext(){
        return context;
    }
    public static void setContext(Context newContext){
        context = newContext;
    }
}
