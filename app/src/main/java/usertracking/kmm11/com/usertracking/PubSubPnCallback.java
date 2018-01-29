package usertracking.kmm11.com.usertracking;

import android.app.Activity;
import android.content.Context;
import android.media.MediaExtractor;
import android.util.Log;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonElement;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import org.json.JSONObject;

import java.util.List;


/**
 * Created by kmm11 on 16.01.2018.
 */

public class PubSubPnCallback extends SubscribeCallback {
    private static final String TAG = PubSubPnCallback.class.getName();
    private final Context context;
    public PubSubPnCallback(Context context) {
        this.context = context;
    }

    @Override
    public void status(PubNub pubnub, PNStatus status) {
    }

    @Override
    public void message(PubNub pubnub, PNMessageResult message) {
        try {
            JsonElement jsonElement = message.getMessage();
            JSONObject jsonObject = new JSONObject(jsonElement.toString());
            final double latitude = Double.parseDouble(jsonObject.getString("latitude"));
            final double longitude = Double.parseDouble(jsonObject.getString("longitude"));
            final String name = jsonObject.getString("name");
            ((MapsActivity)this.context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((MapsActivity) context).updateMap(latitude, longitude, true);
                }
            });

            Log.v("LISTEN", "listen done");
        } catch (Exception e) {
            e.printStackTrace();
            Log.v("LISTEN", "mistake");
        }
    }

    @Override
    public void presence(PubNub pubnub, PNPresenceEventResult presence) {
    }
}
