package org.ubicomp.attentiontest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationTriggerService extends Service implements CircogPrefs {

    private static final String TAG = NotificationTriggerService.class.getSimpleName();

    private static final boolean DISABLE_NOTIFICATIONS = false;
    private static final boolean MAKE_NOISE = true;
    public static final String NOTIFICATION_TAG = "TASK";
    public static final int    NOTIFICATION_ID  = 0;
    public static final int ONGOING_NOTIFICATION_ID = 1;
    private final String channel_id = "circog_reminder";

    private static NotificationTriggerService instance;
    private Timer showNotifTimer;
    private Timer cancelNotifTimer;

    public static boolean notificationIsShown;
    public static boolean notificationIsScheduled;

    // Hours in which the experience sampling may be active
    private static final int START_HOUR = 7;
    private static final int END_HOUR = 23;

    private static final int MIN_DELAY = 10; //seconds
    private static final int MAX_DELAY = 5 * 60; //seconds

    private static final int MIN_TIME_SINCE_LAST_NOTIFICATION = 45; //20; // in minutes

    protected static final int		MIN_MS							= 1000 * 60; //milliseconds per minute
    protected static final int 		SEC_MS 							= 1000;	//milliseconds per second
    protected static final int      EVERY_HOUR                      = 60 * 60 * SEC_MS;
    protected static final int      EVERY_MINUTE                    = 60 * SEC_MS;

    private static final int		CANCEL_NOTIFICATION_DELAY_MS	= MIN_MS * 5; //5

    public NotificationTriggerService() {
        instance = this;
    }

    public static NotificationTriggerService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if(DEBUG_MODE) {
            Log.i(TAG, "onBind()");
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(DEBUG_MODE) {
            Log.i(TAG, "onCreate()");
        }

        FirebaseApp.initializeApp(getApplicationContext());

        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(this, channel_id)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.circog_notificon)
                        .setContentIntent(pendingIntent)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
        notificationIsScheduled = false;
        notificationIsShown = false;
        scheduleNotification();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenMonitor, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(DEBUG_MODE) {
            Log.i(TAG, "onStartCommand()");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        //startService(new Intent(this, NotificationTriggerService.class));

    }

    /**
     * Schedules a notification ahead of time
     */
    public boolean notificationAllowed() {

        if (DISABLE_NOTIFICATIONS) {
            if(DEBUG_MODE) {
                Log.i(TAG, "notification aborted - disable_esm_notifs == TRUE");
            }
            return false;
        }
        // we dont require consent for this application (given for all in signing)
        /*
        if (!Util.getBool(getApplicationContext(), PREF_CONSENT_GIVEN, false)) {
            if(DEBUG_MODE) {
                Log.i(TAG, "notification aborted - still waiting for consent");
            }
            return false;
        }
         */

        if (!isTimingAllowed()) {
            if(DEBUG_MODE) {
                Log.i(TAG, "notification aborted - not allowed at this time -- IGNORED");
            }
            return false;
        }

        if (!isMinTimeElapsed()) {
            if(DEBUG_MODE) {
                Log.i(TAG, "notification aborted - not enough time elapsed");
            }
            return false;
        }

        if (applicationIsOpen()) {
            if(DEBUG_MODE) {
                Log.i(TAG, "notification aborted - application already in foreground");
            }
            return false;
        }

        // no limit for daily tasks
        /*
        if (dailyTasksCompleted()) {
            if(DEBUG_MODE) {
                Log.i(TAG, "notification aborted - daily tasks completed: " + MAX_DAILY_TASKS);
            }
            return false;
        }
        */
        return true;
    }

    private void scheduleNotification() {

        //TODO: spread this more across day?
        int delayMs;
        // between two and three hours?
        int randomDelay = Util.randInt(60, SEC_MS * 60 * 60); //0 - 60mins
        if(DEBUG_MODE) {
            delayMs = EVERY_MINUTE/2;
        } else {
            delayMs = EVERY_HOUR + randomDelay;
        }

        if(CircogPrefs.DEBUG_MODE) {
            Log.i(TAG, "scheduling notification - due in " + Util.format((double) delayMs / MIN_MS) + " min");
        }
        Log.i(TAG, "scheduling notification - due in " + Util.format((double) delayMs / MIN_MS) + " min");
        if (showNotifTimer != null) {
            showNotifTimer.cancel();
            showNotifTimer.purge();
        }

        showNotifTimer = new Timer();
        if (!notificationIsScheduled) {
            showNotifTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    notificationIsScheduled = false;
                    onNotificationTimerFired();
                }

            }, delayMs);
        }

        notificationIsScheduled = true;
    }

    private void onNotificationTimerFired() {
        if(DEBUG_MODE) {
            Log.i(TAG, "onNotificationTimerFired()");
        }

        if(notificationAllowed()) {

            notificationIsShown = true;
            Util.putLong(getApplicationContext(), LAST_NOTIFICATION_POSTED_MS, System.currentTimeMillis());
            triggerNotification();
        }

        scheduleNotification();

    }

    public static void removeNotification(Context context) {
        NotificationManager notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancelAll();
        notificationIsShown = false;
        Util.putBool(context, NOTIF_POSTED, false);
    }

    /**
     *
     * makes sure that notifications are only triggered during awake times
     *
     * @return
     */
    protected static boolean isTimingAllowed() {
        if(DEBUG_MODE) {
            Log.i(TAG, "-isTimingAllowed");
            return true;
        }

        Calendar cal = Calendar.getInstance();
        Date now = new Date(System.currentTimeMillis());
        cal.setTime(now);
        int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);

        // Log.i(TAG, "hour of the day is " + hourOfDay);

        if (hourOfDay >= START_HOUR && hourOfDay <= (END_HOUR - 1)) { return true; }

        return false;
    }

    /**
     *
     * @returns true when both screen is turned on and application is in foreground
     */
    protected boolean applicationIsOpen() {
        if(CircogPrefs.DEBUG_MODE) {
            Log.i(TAG, "applicationIsOpen()");
        }

        return false;
    }

    public void triggerNotification() {
        if(DEBUG_MODE) {
            Log.i(TAG, "triggerNotification");
        }

        //update PREF_LAST_NOTIFICATION_POSTED
        Util.putLong(getApplicationContext(), NOTIF_POSTED_MILLIS, System.currentTimeMillis());
        Util.putBool(getApplicationContext(), NOTIF_POSTED, true);

        showNotification();
    }

    private void showNotification() {

        if(DEBUG_MODE) {
            Log.i(TAG, "showNotification");
        }
        notificationIsShown = false;

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
//                .setContentIntent(pendingIntentMain)
                .setSmallIcon(R.drawable.circog_notificon)
                .setContentTitle(getApplicationContext().getString(R.string.notif_title))
                .setContentText(getApplicationContext().getString(R.string.notif_content))
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_HIGH);

        // Intent to open the MainActivity
        Intent intentMain = new Intent(getApplicationContext(), PVTActivity.class);
        intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Util.putBool(getApplicationContext(), CircogPrefs.NOTIF_CLICKED, true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intentMain);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);

        // Sound/vibrate?
        if(MAKE_NOISE) {
            builder.setDefaults(Notification.DEFAULT_ALL);
        }

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, builder.build());


        NotificationDelivered n = new NotificationDelivered(Util.getEmail(getApplicationContext()), System.currentTimeMillis());

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("notification_delivery").child(String.valueOf(System.currentTimeMillis())).setValue(n)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "notification delivery upload success");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "notifiation delivery upload failed");
                    }
                });
        mDatabase.child("notification_delivery").push();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    public boolean isMinTimeElapsed() {
        if(DEBUG_MODE) {
            return true;
        }
        Date now = new Date();
        //Date minMinutesAgo = new Date(now.getTime() - (MIN_TIME_SINCE_LAST_NOTIFICATION * StudyManager.ONE_MINUTE_IN_MILLIS));
        long minMinutesAgo = now.getTime() - (MIN_TIME_SINCE_LAST_NOTIFICATION * MIN_MS);
        Date lastScheduled = Util.getDateFromTimestamp(Util.getLong(getApplicationContext(), LAST_NOTIFICATION_POSTED_MS, minMinutesAgo)); //Util.getDateFromString("Mon Sep 14 13:08:43 MESZ 2015"); //
        long minutesPassed = ((now.getTime()/MIN_MS) - (lastScheduled.getTime()/MIN_MS));
        boolean notificationTimeoutPassed = (minutesPassed >= MIN_TIME_SINCE_LAST_NOTIFICATION);

        if(DEBUG_MODE) {
            Log.i(TAG, "isMinTimeElapsed: " + notificationTimeoutPassed + " (last notification posted " + minutesPassed + " Minutes ago. Required: " + MIN_TIME_SINCE_LAST_NOTIFICATION + " minutes)");
        }
        return notificationTimeoutPassed;
    }

    /**
     *
     * @returns boolean: whether tasks were all completed for today (as defined as MAX_DAILY_TASKS)
     */
    private boolean dailyTasksCompleted() {
        int dailyTaskCount = TaskList.getDailyTaskCount(getApplicationContext());
        return dailyTaskCount>=MAX_DAILY_TASKS;
    }


    // screen sensing
    private final ScreenMonitor screenMonitor = new ScreenMonitor();

    private final int INITIAL_NOTIFICATION_DELAY = 1000*60*20;
    private final int PHONE_USAGE_NOTIFICATION_DELAY = 1000*60*5;

    public class ScreenMonitor extends BroadcastReceiver {
        private boolean SCREEN_ON = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "Screen ON");
                SCREEN_ON = true;
                // this can trigger during the night but wont actually show a notification
                // schedule the first notification 20-40 mins after 'waking up'

                // initialise the schedule if its the first screen even of the day
                if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) > 7) {
                    Log.d(TAG, "Starting a new notification schedule");
                    scheduleNotification();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(INITIAL_NOTIFICATION_DELAY + Util.randInt(0, SEC_MS * 60 * 20));
                                onNotificationTimerFired();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }


                // always show a notification if user spends more than five minutes on the phone
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(PHONE_USAGE_NOTIFICATION_DELAY + Util.randInt(0, SEC_MS * 60 * 10)); //5 - 15min random delay);
                            Log.d(TAG, "Showing a notification after 5 minutes of use!");
                            if(SCREEN_ON) showNotification();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_OFF)) {
                Log.d(TAG, "screen OFF");
                SCREEN_ON = false;
            }
        }
    }
}