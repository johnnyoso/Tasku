package com.example.android.tasku.JavaUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.example.android.tasku.R;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Created by john.osorio on 23/05/2018.
 */

public class InternetUtils {
    Context context;

    public InternetUtils(Context context){
        this.context = context;

    }

    public void checkInternet() {

        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network

        }
        else Toast.makeText(context, context.getString(R.string.network_issues), Toast.LENGTH_LONG).show();

    }

}
