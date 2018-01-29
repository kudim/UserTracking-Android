package usertracking.kmm11.com.usertracking;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * Created by kmm11 on 16.01.2018.
 */

public class Message {
    private final double latitude;
    private final double longitude;
    private final String name;
    public Message(@JsonProperty("latitude") double latitude, @JsonProperty("longitude") double longitude, @JsonProperty("name") String name){
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    public double getLatitude(){
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public String getName() { return this.name; }
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Message.class)
                .add("sender", latitude)
                .add("message", longitude)
                .add("name", name)
                .toString();
    }
}
