package io.webmusic.webmusicbrowser;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.illposed.osc.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

import javis.wearsyncservice.Constant;

public class MainActivity extends AppCompatActivity  {

    private static final String TAG="WebMusicBrowser";

    // for OSC
    private int listenPort = 10000;

    private OSCPortOut sender;
    private static OSCPortIn receiver;

    // UI
    // urlBar
    private RelativeLayout urlBar;
    private ImageView menuButton;

    // for Webview
    // http://qiita.com/sy_sft_/items/508870dfccfb237d72fd
    private AutoCompleteTextView urlText;
    private static WebView webView;
    private String defaultLocalURL="file:///android_asset/index.html";

    // for auto complete
    // http://systemout.net/2014/12/19/android-autocomplete-edittext-with-history/
    public static final String PREFS_NAME = "PingBusPrefs";
    public static final String PREFS_SEARCH_HISTORY = "SearchHistory";
    private SharedPreferences settings;
    private Set<String> history;

    // for check
    private static boolean isOSCAccessRequested = false;
    private boolean isExistOSCClient = false;
    private static boolean isExistOSCServer = false;
    private static boolean isRunningOSCServer = false;

    // for launching qr code reader
    public static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // hide progress bar
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.pageLoadingProgressBar);
        progressBar.setVisibility(View.GONE);


        // urlBar
        urlBar = (RelativeLayout) findViewById(R.id.urlBar);

        // menuButton
        // http://qiita.com/suzukihr/items/96b62d7ae758b09bcabc
        menuButton = (ImageView) findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(MainActivity.this, menuButton, Gravity.AXIS_PULL_BEFORE);

                // add icon
                try {
                    Field[] fields = popup.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        if ("mPopup".equals(field.getName())) {
                            field.setAccessible(true);
                            Object menuPopupHelper = field.get(popup);
                            Class<?> classPopupHelper = Class.forName(menuPopupHelper
                                    .getClass().getName());
                            Method setForceIcons = classPopupHelper.getMethod(
                                    "setForceShowIcon", boolean.class);
                            setForceIcons.invoke(menuPopupHelper, true);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                popup.getMenuInflater().inflate(R.menu.more_vert_menu, popup.getMenu());
                popup.show();

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.more_vert_settings_setting:
                                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                                startActivity(settings);
                                Toast.makeText(MainActivity.this, "You have selected AAA Menu", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.more_vert_settings_refresh:
                                refreshPageByUrl();
                                break;
                            case R.id.intent_qrcodereader:
                                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                                try{
                                    startActivityForResult(intent, REQUEST_CODE);
                                }catch (ActivityNotFoundException e){
                                    Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
                                    googlePlayIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.google.zxing.client.android"));
                                    startActivity(googlePlayIntent);    Toast.makeText(MainActivity.this, "Please install this app", Toast.LENGTH_LONG ).show();
                                }
                                break;
                        }
                        return true;
                    }
                });


            }
        });

        // webView
        urlText = (AutoCompleteTextView) findViewById(R.id.urlText);
        webView = (WebView) findViewById(R.id.webView);
        //webView.setWebViewClient(new runBrowser());
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // TODO Auto-generated method stub
                Log.i(TAG, "[WebView] Start Loading: "+url);
                super.onPageStarted(view, url, favicon);

                // display progress bar
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.pageLoadingProgressBar);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // TODO Auto-generated method stub
                Log.i(TAG, "[WebView] Finished Loading: "+url);
                super.onPageFinished(view, url);

                // display progress bar
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.pageLoadingProgressBar);
                progressBar.setVisibility(View.GONE);

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                urlText.setText(url);
                if(startsWithfile(url)==true) {
                    urlText = (AutoCompleteTextView) findViewById(R.id.urlText);
                    urlText.setText(null);
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {});


        // set default URL
        //urlText.setText(defaultURL);

        // enable JavaScript & add custom JavaScript API
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebOSCInterface(MainActivity.this), "osc");

        webView.loadUrl(defaultLocalURL);

        // for providing using devtool on webview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // Enter Key event on urlBar
        urlText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    refreshPageByUrl();
                    urlText.clearFocus();
//                    hideKeyboard(urlText);

                    // for autocomplete
                    addSearchInput(urlText.getText().toString());
                    return true;
                }
                return false;
            }
        });
        // Touch Event when one of the autocomplete item touched/selected
        urlText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                refreshPageByUrl();
            }
        });
        // auto complete
        settings = getSharedPreferences(PREFS_NAME, 0);
        history = settings.getStringSet(PREFS_SEARCH_HISTORY, new HashSet<String>());
        setAutoCompleteSource();

    } // onCreate


    // action after receive data from intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            String contents = data.getStringExtra("SCAN_RESULT");
            AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.urlText);
            String url = data.getStringExtra("SCAN_RESULT");
            textView.setText(url);
            webView.loadUrl(url);
        }
    }

    /// receiver for wear's data
    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(Constant.MY_INTENT_FILTER)) {
                //textView.setText(intent.getStringExtra(Constant.PHONE_TO_WATCH_TEXT));
                Log.i(TAG, intent.getStringExtra(Constant.PHONE_TO_WATCH_TEXT));
            }
        }
    };

    // for receiving data from wear
    private void unregisterReceiver() {
        try {
            if (mWifiScanReceiver != null) {
                unregisterReceiver(mWifiScanReceiver);
            }
        } catch (IllegalArgumentException e) {
            mWifiScanReceiver = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWifiScanReceiver, new IntentFilter(Constant.MY_INTENT_FILTER));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public boolean startsWithfile(String url) {
        return (url.startsWith( "file://" ));
    }

    public boolean startsWithhttphttps(String url) {
        return (url.startsWith( "http://" ) || url.startsWith( "https://" ));
    }


    // Key down event for back button on Android UI
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            WebBackForwardList list = webView.copyBackForwardList() ;
            int len = list.getCurrentIndex() ;
            String url;
            int cnt = 1 ;
            for( int i = 1 ; i < len ; i ++ ) {
                url = list.getItemAtIndex(list.getCurrentIndex()-i).getUrl() ;
//                if( url.startsWith( "http://" ) || url.startsWith( "https://" ) ) {
                if(startsWithhttphttps(url)) {
                    urlText.setText(url);
                    break ;
                }
                cnt ++ ;
            }
            webView.goBack();
            return true;
        }
        return false;
    }


    // hide kyeboard
    public void hideKeyboard(EditText v) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    // get IP Address
    private static String reveilIPAddress() throws IOException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while(interfaces.hasMoreElements()){
            NetworkInterface network = interfaces.nextElement();
            Enumeration<InetAddress> addresses = network.getInetAddresses();

            while(addresses.hasMoreElements()){
                String address = addresses.nextElement().getHostAddress();

                //127.0.0.1と0.0.0.0以外のアドレスが見つかったらそれを返す
                if(!"127.0.0.1".equals(address) && !"0.0.0.0".equals(address)){
                    return address;
                }
            }
        }
        return "127.0.0.1";
    }


    // move to input URL
    public void refreshPageByUrl() {
        String url = urlText.getText().toString();
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        if(startsWithfile(url)) {
            urlText.setText(null);
        } else if(url.isEmpty()==true) {
            url=defaultLocalURL;
        } else if(url.isEmpty()==false && startsWithhttphttps(url)==false) {
            url = "http://" + url;
            urlText.setText(url);
        } else {
            urlText.setText(url);
        }
        hideKeyboard(urlText);
        webView.loadUrl(url);
    }


    // for auto Complete
    // TODO: take care the URL which has "www"
    private void setAutoCompleteSource() {
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.urlText);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, history.toArray(new String[history.size()]));
        textView.setAdapter(adapter);
    }
    private void addSearchInput(String input) {
        input=input.replace("https://", "").replace("http://", "").replace("www.", "");
        if (!history.contains(input)) {
            history.add(input);
            setAutoCompleteSource();
        }
    }
    private void savePrefs() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(PREFS_SEARCH_HISTORY, history);

        editor.commit();
    }


    // OSC Client
    public void setOSCClient(String targetIP, int targetPort) {

        if(isExistOSCClient == false) {
            try {
                sender = new OSCPortOut(InetAddress.getByName(targetIP), targetPort);
                isExistOSCClient = true;
            } catch (SocketException e) {
                e.printStackTrace();
                Log.e(TAG, "[Error] SocketException occurred while creating client.");
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, "[Error] UnknownException occurred while creating client.");
            }
        } else {
            Log.i(TAG, "OSC Client already exist.");
        }
    }

    // OSC Receiver
    public static class OSCReceiver {
        public static void prepare(int listenPort, String addrPattern) {
            // OSC Server
            if(isExistOSCServer == false) {
                try {
                    receiver = new OSCPortIn(listenPort);
                    isExistOSCServer = true;
                } catch (SocketException e) {
                    e.printStackTrace();
                    Log.e(TAG, "[Error] while creating receiver.");
                }
                // TODO: add message directory and pass to injectParam()
                OSCListener listener = new OSCListener() {
                    @Override
                    public void acceptMessage(Date time, OSCMessage message, String senderAddr) {
                        injectParam(message.getAddress(), message.getArguments());
                        Log.i(TAG, "[Reveived] " + time + " :: " + message.getArguments() + " :: " + message.getAddress() + " :: " + senderAddr);
                    }
                };
                receiver.addListener(addrPattern, listener);
            }
            Log.i(TAG, "OSCReceiver prepared. [AddressPattern] " + addrPattern);
        }

        // http://stackoverflow.com/questions/22607657/webview-methods-on-same-thread-error
        public static void injectParam(final String addrPattern, final Object arguments) {
            webView.post(new Runnable(){
                @Override
                public void run() {
                    Gson gson = new Gson();
                    String json = gson.toJson(arguments);
                    // CustomEvent
                    // https://www.sitepoint.com/javascript-custom-events/
                    webView.loadUrl(
                            "javascript:(function() { " +
                                    "var event = new CustomEvent('onoscmessage', {" +
                                    "   detail: {" +
                                    "     addrPattern: '" + addrPattern + "'," +
                                    "     arguments: " + json + "," +
                                    "     time: new Date()," +
                                    "    }," +
                                    "    bubbles: true," +
                                    "    cancelable: true" +
                                    "});" +
                                    "document.dispatchEvent(event);" +
                            "})()");

                    Log.i(TAG,"[Inject Param]"+json);
                }
            });
        }

        public static void start() {
            if(isRunningOSCServer == false) {
                receiver.startListening();
                isRunningOSCServer = true;
                Log.i(TAG, "OSCReceiver has Start.");
            } else {
                Log.i(TAG, "OSCReceiver has already started Start.");
            }
        }

        public static void stop() {
            receiver.stopListening();
            isRunningOSCServer = false;
            Log.i(TAG, "Receiver Stop!!");
        }

    }

    private OSCMessage createOSCMessage(String OSCAddr, Object OSCArgument) {
        return new OSCMessage(OSCAddr, Arrays.asList(OSCArgument));
    }

    private void sendOSCMsg(OSCMessage msg) {
        try {
            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }
            sender.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Web OSC API Interface
    // http://www.buildinsider.net/mobile/bookhtml5hybrid/1101
    public class WebOSCInterface {

        Context mContext;
        StackTraceElement trace = Thread.currentThread().getStackTrace()[4];
        String targetIP;
        public int targetPort;
        public int serverPort;
        public String addrPattern;

        WebOSCInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public String getIPAddress() {
            String ipAddr = null;
            if(isOSCAccessRequested == true) {
                ipAddr="127.0.0.1";
                WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                WifiInfo w_info = wifiManager.getConnectionInfo();
                int ipAddrInt = w_info.getIpAddress();
                if (((((ipAddrInt >> 0) & 0xff) == 0) && (((ipAddrInt >> 8) & 0xff) == 0) &&
                        (((ipAddrInt >> 16) & 0xff) == 0) && (((ipAddrInt >> 24) & 0xff) == 0)) == false) {
                    ipAddr = ((ipAddrInt >> 0) & 0xFF) + "." + ((ipAddrInt >> 8) & 0xFF) + "." + ((ipAddrInt >> 16) & 0xFF) + "." + ((ipAddrInt >> 24) & 0xFF);
                }
            } else {
                Log.i("TAG", "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
            return ipAddr;
        }

        @JavascriptInterface
        public void requestOSCAccess() {
            if(isOSCAccessRequested == false) {
                isOSCAccessRequested = true;
            }
        }

        @JavascriptInterface
        public void setClient(String vTargetIP, int vTargetPort) {
            if(isOSCAccessRequested == true) {
                setOSCClient(vTargetIP, vTargetPort);
                vTargetIP = targetIP;
                vTargetPort = targetPort;
                Log.i("TAG", "Client is set on IP: " + vTargetIP + ", Port: "+ vTargetPort +".");
            } else {
                Log.i("TAG", "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
        }

        @JavascriptInterface
        public void send(String json) {
            if(isOSCAccessRequested == true) {
                if(isExistOSCClient==true) {
                    Log.i(TAG, json);
                    String addrPattern = null;
                    JSONObject params = null;
                    JSONArray arguments = null;

                    try {
                        params = new JSONObject(json);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        addrPattern = params.getString("addrPattern");
                        arguments = params.getJSONArray("arguments");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int length = arguments.length();
                    final Object[] OSCValues = new Object[length];

                    for (int i = 0; i < length; i++) {

                        String val = null;
                        try {
                            val = arguments.getString(i);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        OSCValues[i] = val;
                        try {
                            Integer.parseInt(val);
                            OSCValues[i] = Integer.parseInt(val);
                        } catch (NumberFormatException e) {
                            OSCValues[i] = val;
                        }
                    }
                    sendOSCMsg(new OSCMessage(addrPattern, Arrays.asList(OSCValues)));
                } else {
                    Log.i("TAG", "setClient() must be called to use " + trace.getMethodName() + " method.");
                }
            } else {
                Log.i("TAG", "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
        }

        @JavascriptInterface
        public void setServer(int vServerPort, String vAddrPattern) {
            if(isOSCAccessRequested == true) {
                OSCReceiver.prepare(vServerPort, vAddrPattern);
                serverPort = vServerPort;
                addrPattern = vAddrPattern;
            } else {
                Log.i("TAG", "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
        }

        @JavascriptInterface
        public void startServer() {
            if(isOSCAccessRequested == true) {
                if(isExistOSCServer == true) {
                    OSCReceiver.start();
                } else {
                    Log.i("TAG", "setServer() must be called to use " + trace.getMethodName() + " method.");
                }
            } else {
                Log.i("TAG", "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
        }

        @JavascriptInterface
        public void stopServer() {
            if(isOSCAccessRequested == true) {
                if(isExistOSCServer == true) {
                    OSCReceiver.stop();
                } else {
                    Log.i("TAG", "setServer() must be called to use " + trace.getMethodName() + " method.");
                }
            } else {
                Log.i("TAG", "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
        }
    }
}


