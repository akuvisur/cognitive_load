package org.ubicomp.attentiontest;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class NotificationDelivered {

    public String email;
    public long timestamp;
    public String device_id;

    public NotificationDelivered(String device_id, String email, long timestamp) {
        this.device_id = device_id;
        this.email = email;
        this.timestamp = timestamp;
    }

}
