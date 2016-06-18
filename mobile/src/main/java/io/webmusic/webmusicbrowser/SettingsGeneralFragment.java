package io.webmusic.webmusicbrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

/**
 * Created by kawai on 5/30/16.
 */
public class SettingsGeneralFragment extends Fragment {

    public interface onCheckedChangedListener {
        void updateAcceptStatus(boolean status);
    }

    private static final String TAG="WebMusicBrowser";

    private onCheckedChangedListener mOnCheckedChangedListener;

    CompoundButton.OnCheckedChangeListener listener;

    public SettingsGeneralFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onCheckedChangedListener) {
            mOnCheckedChangedListener = (onCheckedChangedListener) context;
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mOnCheckedChangedListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_general, container, false);

        // get value from activity
        Boolean receiveStatus = getArguments().getBoolean("isAcceptReceivingOSCMsg");
        SwitchCompat bReceiveStatus = (SwitchCompat) view.findViewById(R.id.switch_accept_receive_osc);
        bReceiveStatus.setChecked(receiveStatus);

        // for Receive OSC Message
        SwitchCompat accept_receive_osc_message = (SwitchCompat) view.findViewById(R.id.switch_accept_receive_osc);
        accept_receive_osc_message.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean receiveStatus) {
                mOnCheckedChangedListener.updateAcceptStatus(receiveStatus);
            }
        });

        return view;
    }

}
