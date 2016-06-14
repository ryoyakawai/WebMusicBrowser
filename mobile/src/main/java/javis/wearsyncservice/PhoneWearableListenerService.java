package javis.wearsyncservice;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.example.MobileWearConfigClass;

/**
 * Created by Jeffrey Liu on 12/2/15.
 * This service will keep listening to all the message coming from the watch
 */
public class PhoneWearableListenerService extends WearableListenerService {


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equalsIgnoreCase(MobileWearConfigClass.WATCH_TO_PHONE_MESSAGE_PATH)) {
            String receivedText = new String(messageEvent.getData());
            broadcastIntent(receivedText);
        } else {
            super.onMessageReceived(messageEvent);
        }
    }

    // broadcast a custom intent.
    public void broadcastIntent(String text) {
        Intent intent = new Intent();
        intent.setAction(Constant.MY_INTENT_FILTER);
        intent.putExtra(Constant.PHONE_TO_WATCH_TEXT, text);
        sendBroadcast(intent);
    }
}