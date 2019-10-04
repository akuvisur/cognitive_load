package org.ubicomp.attentiontest;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class NotificationDelivered {

    public String email;
    public long timestamp;

    public NotificationDelivered(String email, long timestamp) {
        this.email = email;
        this.timestamp = timestamp;
    }

}
