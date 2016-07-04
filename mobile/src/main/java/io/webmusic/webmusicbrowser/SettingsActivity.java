package io.webmusic.webmusicbrowser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebBackForwardList;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements SettingsGeneralFragment.onCheckedChangedListener {

    private static final String TAG = "WebMusicBrowser";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ImageView backButton;

    //
    private static boolean isAcceptReceivingOSCMsg = false;

    //
    private static ArrayList<String> IPWhiteList = new ArrayList<>();

    //
    Intent intent;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // back arrow
        backButton = (ImageView) findViewById(R.id.settings_back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backToMainActivity();
            }
        });

        // tabs, pager
        toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        viewPager = (ViewPager) findViewById(R.id.settings_viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.settings_tab);
        tabLayout.setupWithViewPager(viewPager);
        //setupTabIcons();

        // receive value from activity
        Intent intent = getIntent();
        isAcceptReceivingOSCMsg = intent.getBooleanExtra("isAcceptReceivingOSCMsg", false);
        IPWhiteList = intent.getStringArrayListExtra("IPWhiteList");

        Logger.o("e", TAG, "[reveive] "+String.valueOf(isAcceptReceivingOSCMsg) + " [IPWhiteList] " + String.valueOf(IPWhiteList));

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void backToMainActivity() {
        intent = new Intent();
        intent.putExtra("isAcceptReceivingOSCMsg", isAcceptReceivingOSCMsg);
        intent.putExtra("IPWhiteList", IPWhiteList);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            backToMainActivity();
            return true;
        }
        return false;
    }

    @Override
    public boolean getAcceptStatus() { return isAcceptReceivingOSCMsg; }

    @Override
    public void updateAcceptStatus(boolean status) {
        isAcceptReceivingOSCMsg = status;
    }

    @Override
    public ArrayList getIPWhiteList() { return IPWhiteList; }

    @Override
    public void deleteOneIPWhiteList(String IP) { IPWhiteList.remove(IP);}

    private void setupViewPager(ViewPager viewPager) {
        // pass value to fragment
        Bundle bundle = new Bundle();
        bundle.putBoolean("isAcceptReceivingOSCMsg", isAcceptReceivingOSCMsg);
        SettingsGeneralFragment sgFragment = new SettingsGeneralFragment();
        sgFragment.setArguments(bundle);


        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(sgFragment, "General");
        adapter.addFragment(new SettingsBlemidiFragment(), "Bluetooth MIDI");
        viewPager.setAdapter(adapter);
    }

    private void setupTabIcons() {
        // TODO: need to fix as displaying icon next to title
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_settings_black_24dp);
        //tabLayout.getTabAt(1).setIcon(R.drawable.ic_bluetooth_black_24dp);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Settings Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://io.webmusic.webmusicbrowser/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Settings Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://io.webmusic.webmusicbrowser/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }


}
