package com.live.broadcast.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.live.broadcast.plugin.livebroadcastplugin.R;

import org.json.JSONException;

@NativePlugin()
public class LiveBroadcastPlugin extends Plugin {
    static PluginCall response ;
    @PluginMethod()
    public void callLive(PluginCall call) {
        response = call;
        String streamName = call.getString("streamName");
        Intent intent = new Intent(getContext(),MainActivity.class);
        intent.putExtra("streamName",streamName);
        getContext().startActivity(intent);
    }

    public static void returnResponse(Object object,boolean isSuccess,Context context){
        JSObject jsObject = new JSObject();
        try {
            jsObject.putSafe("data",object);
            if(isSuccess) response.success(jsObject);
            else response.error(object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void showPopup(final Context context, final AlertDialog mDialog, final JSObject res )
    {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View inflatedView = layoutInflater.inflate(R.layout.live_pop_up, null, false);
        Button save = inflatedView.findViewById(R.id.save);
        Button dontSave = inflatedView.findViewById(R.id.dont_save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    res.putSafe("isSave",true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                returnResponse(res,true,context);
                mDialog.dismiss();
                ((Activity) context).finish();
            }
        });

        dontSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    res.putSafe("isSave",false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                returnResponse(res,true,context);
                mDialog.dismiss();
                ((Activity) context).finish();
            }
        });
        showMessageBox(false,inflatedView,mDialog);
    }

    static void showMessageBox(boolean cancelable, View view, AlertDialog mDialog) {
        if(null != mDialog) {
            try {
                if(!mDialog.isShowing()) {
                    mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    mDialog.setView(view);
                    mDialog.setCanceledOnTouchOutside(cancelable);
                    mDialog.setCancelable(cancelable);
                    mDialog.show();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
