package com.wkuxr.sunsketcher;

import android.app.Application;
import android.content.Context;

//App class that just statically holds a singleton for the current activity Context
public class App extends Application {
    private static Context context;
    public static Context getContext(){
        return context;
    }
    public static void setContext(Context newContext){
        context = newContext;
    }
}
