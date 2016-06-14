package javis.wearsyncservice;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.MobileWearConfigClass;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Jeffrey Liu on 12/2/15.
 */
public class SendPhoneMessageIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "SendPhoneMessage";
    public static final String INPUT_EXTRA = "input extra";

    private GoogleApiClient mApiClient;


    public SendPhoneMessageIntentService() {
        super("SendPhoneMessageIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        initGoogleApiClient();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
        for (Node node : nodes.getNodes()) {
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mApiClient, node.getId(), MobileWearConfigClass.WATCH_TO_PHONE_MESSAGE_PATH, intent.getStringExtra(INPUT_EXTRA).getBytes()).await();
        }
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        mApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended: " + i);
    }

    @Override
    public void onDestroy() {
        if (mApiClient != null)
            mApiClient.disconnect();
        super.onDestroy();

    }
}