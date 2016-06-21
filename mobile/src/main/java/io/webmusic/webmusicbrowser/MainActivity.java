package io.webmusic.webmusicbrowser;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Receiving OSC msg change from App Settings
    public static boolean isAcceptReceivingOSCMsg = false;

    // for check
    private static boolean isOSCAccessRequested = false;
    private boolean isExistOSCClient = false;
    private static boolean isExistOSCServer = false;
    private static boolean isRunningOSCServer = false;

    // for launching qr code reader
    public static final int REQUEST_CODE_QRCODE = 1000;
    public static final int REQUEST_CODE_SETTINGS = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // hide progress bar
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.pageLoadingProgressBar);
        progressBar.setVisibility(View.GONE);

        // for dialog
        //builder = new AlertDialog.Builder(this);

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
                                openSettingsActivity();
                                break;
                            case R.id.more_vert_settings_refresh:
                                refreshPageByUrl();
                                break;
                            case R.id.intent_qrcodereader:
                                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                                try{
                                    startActivityForResult(intent, REQUEST_CODE_QRCODE);
                                }catch (ActivityNotFoundException e){
                                    Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
                                    googlePlayIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.google.zxing.client.android"));
                                    startActivity(googlePlayIntent);
                                    Toast.makeText(MainActivity.this, "Please install this app", Toast.LENGTH_LONG ).show();
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
                Logger.o("i", TAG, "[WebView] Start Loading: "+url);
                super.onPageStarted(view, url, favicon);

                // display progress bar
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.pageLoadingProgressBar);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // TODO Auto-generated method stub
                Logger.o("i", TAG, "[WebView] Finished Loading: "+url);
                super.onPageFinished(view, url);

                // display progress bar
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.pageLoadingProgressBar);
                progressBar.setVisibility(View.GONE);

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                urlText.setText(url);
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


    public void openSettingsActivity() {
        Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
        settings.putExtra("isAcceptReceivingOSCMsg", isAcceptReceivingOSCMsg);
        int requestCode = REQUEST_CODE_SETTINGS;
        startActivityForResult(settings, requestCode);
    }

    // action after receive data from intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // from QR Code Reader
        if( requestCode == REQUEST_CODE_QRCODE && resultCode == Activity.RESULT_OK){
            String contents = data.getStringExtra("SCAN_RESULT");
            AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.urlText);
            String url = data.getStringExtra("SCAN_RESULT");
            textView.setText(url);
            webView.loadUrl(url);
        }
        // from Settings Activity
        if(requestCode == REQUEST_CODE_SETTINGS) {
            Boolean status = data.getBooleanExtra("isAcceptReceivingOSCMsg", false);
            isAcceptReceivingOSCMsg = status;
        }
    }

    /// receiver for wear's data
    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(Constant.MY_INTENT_FILTER)) {
                //textView.setText(intent.getStringExtra(Constant.PHONE_TO_WATCH_TEXT));
                Logger.o("i", TAG, "[onReceive()] "+ intent.getStringExtra(Constant.PHONE_TO_WATCH_TEXT));
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
        try {
            sender.close();
            Logger.o("i", TAG, "sender closed");
        } catch(Exception e) {
            Logger.o("i", TAG, "ERROR: sender");
        }
        try {
            receiver.close();
            Logger.o("i", TAG, "receiver closed");
        } catch(Exception e) {
            Logger.o("i", TAG, "ERROR: receiver");
        }
        isOSCAccessRequested = false;
        isExistOSCClient = false;
        isExistOSCServer = false;
        isRunningOSCServer = false;
        //webView.removeJavascriptInterface("osc");
        //webView.addJavascriptInterface(new WebOSCInterface(MainActivity.this), "osc");
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

    // Display permission display
    public void displayPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.permission_title)
            .setMessage(R.string.permission_body_01)
            .setPositiveButton(R.string.permission_button_right, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openSettingsActivity();
                }
            })
            .setNegativeButton(R.string.permission_button_left, null)
            .show();

    }

    // OSC Client
    public void setOSCClient(String targetIP, int targetPort) {

        if(isExistOSCClient == false) {
            try {
                sender = new OSCPortOut(InetAddress.getByName(targetIP), targetPort);
                isExistOSCClient = true;
            } catch (SocketException e) {
                e.printStackTrace();
                Logger.o("e", TAG, "[Error] SocketException occurred while creating client.");
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Logger.o("e", TAG, "[Error] UnknownException occurred while creating client.");
            }
        } else {
            Logger.o("i", TAG, "OSC Client already exist.");
        }
    }

    public jsOSCReceiver OSCReceiver = new jsOSCReceiver();
    // OSC Receiver
    public class jsOSCReceiver {
        public Boolean prepare(final int listenPort, final String addrPattern) {
            Boolean status=false;
            status=false;
            Logger.o("e", TAG, isAcceptReceivingOSCMsg +" :: "+isExistOSCServer + " :: ");
            if(isAcceptReceivingOSCMsg==false) {
                displayPermissionDialog();
            } else {
                // OSC Server
                if (isExistOSCServer == false) {
                    try {
                        receiver = new OSCPortIn(listenPort);
                        isExistOSCServer = true;
                        Logger.o("e", TAG, isAcceptReceivingOSCMsg +" :: "+isExistOSCServer + " :: ");
                    } catch (SocketException e) {
                        e.printStackTrace();
                        Logger.o("e", TAG, "[Error] while creating receiver.");
                    }
                    // TODO: add message directory and pass to injectParam()
                    webView.post(new Runnable() {
                        @Override
                        public void run() {
                            //displayPermissionDialog();
                            OSCListener listener = new OSCListener() {
                                @Override
                                public void acceptMessage(Date time, OSCMessage message, String senderAddr) {
                                    if(isAcceptReceivingOSCMsg==true) {
                                        Gson gson = new Gson();
                                        String Arguments = gson.toJson(message.getArguments());
                                        Map<String, String> params = new HashMap<String, String>();
                                        params.put("addrPattern", message.getAddress());
                                        params.put("arguments", Arguments);
                                        injectParam("onoscmessage", params);
                                        Logger.o("i", TAG, "[Reveived] " + time + " :: " + message.getArguments() + " :: " + message.getAddress() + " :: " + senderAddr);
                                    } else {
                                        webView.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                displayPermissionDialog();
                                            }
                                        });
                                    }
                                }
                            };
                            receiver.addListener(addrPattern, listener);

                        }
                    });

                }
                Logger.o("i", TAG, "OSCReceiver prepared. [AddressPattern] " + addrPattern);
                status=true;
            }

            return status;

        }

        // http://stackoverflow.com/questions/22607657/webview-methods-on-same-thread-error
        public void injectParam(final String type, final Map params) {
            webView.post(new Runnable(){
                @Override
                public void run() {
                    String eventName = null;
                    String detail = null;

                    switch(type) {
                    case "onoscmessage":
                        eventName = "onoscmessage";
                        detail =
                            "   detail: {" +
                            "     addrPattern: '" + params.get("addrPattern") + "'," +
                            "     arguments: " + params.get("arguments") + "," +
                            "     time: new Date()," +
                            "    }";
                        break;
                    }
                    if(eventName!=null && detail!=null) {
                        // CustomEvent
                        // https://www.sitepoint.com/javascript-custom-events/
                        webView.loadUrl(
                                        "javascript:(function() { " +
                                        "var event = new CustomEvent('"+eventName+"', {" +
                                        detail + "," +
                                        "    bubbles: true," +
                                        "    cancelable: true" +
                                        "});" +
                                        "document.dispatchEvent(event);" +
                                        "})()");
                    } else {
                        Logger.o("e", TAG, "[fetal:injectParam] eventName and/or detail are not specified.");
                    }
                }
            });
        }

        public boolean start() {
            Boolean result=false;
            if(isAcceptReceivingOSCMsg==false) {
                displayPermissionDialog();
                result=false;
            } else if(isRunningOSCServer==false) {
                receiver.startListening();
                isRunningOSCServer = true;
                Logger.o("i", TAG, "OSCReceiver starting.");
                result=true;
            } else {
                Logger.o("i", TAG, "OSCReceiver has already started.");
            }
            return result;
        }

        public void stop() {
            receiver.stopListening();
            isRunningOSCServer = false;
            Logger.o("i", TAG, "Receiver Stop!!");
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
                Logger.o("i", TAG, "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
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
        public void setClient(String TargetIP, int TargetPort) {
            Logger.o("e", TAG, String.valueOf(isOSCAccessRequested));
            if(isOSCAccessRequested == true) {
                setOSCClient(TargetIP, TargetPort);
                Logger.o("i", TAG, "Client is set on IP: " + TargetIP + ", Port: "+ TargetPort +".");
            } else {
                Logger.o("i", TAG, "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
        }

        @JavascriptInterface
        public void send(String json) {
            if(isOSCAccessRequested == true) {
                if(isExistOSCClient==true) {
                    Logger.o("i", TAG, "send() " + json);
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
                    Logger.o("i", TAG, "setClient() must be called to use " + trace.getMethodName() + " method.");
                }
            } else {
                Logger.o("i", TAG, "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
        }

        @JavascriptInterface
        public String setServer(int ServerPort, String AddrPattern) {
            Boolean status = false;
            if(isOSCAccessRequested == true) {
                if(OSCReceiver.prepare(ServerPort, AddrPattern)==true) {
                    status=true;
                } else {
                    status=false;
                }
            } else {
                Logger.o("i", TAG, "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
                status=false;
            }
            return String.valueOf(status);
        }

        @JavascriptInterface
        public String startServer() {
            Boolean status=false;
            if(isOSCAccessRequested == true) {
                if(isExistOSCServer == true) {
                    status=OSCReceiver.start();
                } else {
                    Logger.o("i", TAG, "setServer() must be called to use " + trace.getMethodName() + " method.");
                }
            } else {
                Logger.o("i", TAG, "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
            return String.valueOf(status);
        }

        @JavascriptInterface
        public void stopServer() {
            if(isOSCAccessRequested == true) {
                if(isExistOSCServer == true) {
                    OSCReceiver.stop();
                } else {
                    Logger.o("i", TAG, "setServer() must be called to use " + trace.getMethodName() + " method.");
                }
            } else {
                Logger.o("i", TAG, "requestOSCAccess() must be called to use " + trace.getMethodName() + " method.");
            }
        }
    }
}


