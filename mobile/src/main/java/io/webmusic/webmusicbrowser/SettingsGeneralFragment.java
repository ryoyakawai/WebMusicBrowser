package io.webmusic.webmusicbrowser;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by kawai on 5/30/16.
 */
public class SettingsGeneralFragment extends Fragment {

    public interface onCheckedChangedListener {
        boolean getAcceptStatus();
        void updateAcceptStatus(boolean status);
        ArrayList<String> getIPWhiteList();
        void deleteOneIPWhiteList(String IP);
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

        // get value from activity & set to UI
        Boolean receiveStatus = mOnCheckedChangedListener.getAcceptStatus();
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

        // display floating list of Accepted IP List
        setHasOptionsMenu(true);
        Button ipListViewButton = (Button) view.findViewById(R.id.button_accept_iplist_view);
        ipListViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().openContextMenu(view);
            }
        });
        registerForContextMenu(ipListViewButton);

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        ArrayList<String> IPList = mOnCheckedChangedListener.getIPWhiteList();
        Logger.o("e", TAG, "[Check] " + String.valueOf(IPList));

        if(IPList.size()>0) {
            getActivity().getMenuInflater().inflate(R.menu.menu_accepted_ip_list, menu);

            //Set Context of Context Menu
            menu.setHeaderTitle(getActivity().getString(R.string.permission_context_menu_delete_ip_title));
            //Menu.add(int groupId, int itemId, int order, CharSequence title)
            for (int i = 0; i < IPList.size(); i++) {
                menu.add(0, i, 0, IPList.get(i));
            }
        } else {
            Toast.makeText(getActivity(), getActivity().getString(R.string.permission_context_menu_delete_ip_is_zero), Toast.LENGTH_LONG ).show();
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ArrayList<String> IPList = mOnCheckedChangedListener.getIPWhiteList();
        Logger.o("e", TAG, "[BEFORE] " + String.valueOf(IPList));
        mOnCheckedChangedListener.deleteOneIPWhiteList(IPList.get(item.getItemId()));
        Logger.o("e", TAG, "[AFTER] " + String.valueOf(IPList));
        return true;
    }

}
