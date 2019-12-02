package org.ubicomp.attentiontest;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


/**
 * @author Tilman Dingler
 * @see https://github.com/til-d/circog
 * @version 1.0
 * @since 05/2017
 */
public class MainActivity extends AppCompatActivity {

    private static final String	TAG	= MainActivity.class.getSimpleName();

    public static final int[] COMPLETE_TASKLIST = {PVTActivity.TASK_ID}; //{MOTActivity.TASK_ID};
    //public static final int[] COMPLETE_TASKLIST = {PVTActivity.TASK_ID, GoNoGoActivity.TASK_ID, MOTActivity.TASK_ID}; //{MOTActivity.TASK_ID};

    Button pvt,nogo,mot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(CircogPrefs.DEBUG_MODE) {
            Log.i(TAG, "+ onCreate()");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pvt = findViewById(R.id.btn_launch_pvt);
        nogo = findViewById(R.id.btn_launch_gonogo);
        mot = findViewById(R.id.btn_launch_mot);

        nogo.setEnabled(false);
        mot.setEnabled(false);

        pvt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPVT(findViewById(android.R.id.content));
            }
        });
        nogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchGoNoGo(findViewById(android.R.id.content));
            }
        });
        mot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchMOT(findViewById(android.R.id.content));
            }
        });
        TaskList.initTaskList(getApplicationContext());

        //make sure NotificationTriggerService is running
        startForegroundService(new Intent(getApplicationContext(), NotificationTriggerService.class));

    }

    @Override
    public void onResume() {
        super.onResume();

        if(CircogPrefs.DEBUG_MODE) {
            Log.i(TAG, "+ onResume()");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getApplicationContext().getPackageName();
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + packageName));
                getApplicationContext().startActivity(intent);
            }
        }

        Util.circogIsRunning(getApplicationContext(), true);
        //track whether app has been opened through a notification or explicit app launch
        boolean notifTriggered = Util.getBool(getApplicationContext(), CircogPrefs.NOTIF_CLICKED, false);
        LogManager.logAppLaunch(notifTriggered);
        Util.putBool(getApplicationContext(), CircogPrefs.NOTIF_CLICKED, false);
        NotificationTriggerService.removeNotification(getApplicationContext());

        // check that email exists
        String email = Util.getEmail(this);
        if (email.length() < 1) {
            //show popup
            final Intent intent = new Intent(this, EmailSurveyActivity.class);
            startActivity(intent);
        }
        TextView emailText = findViewById(R.id.participation_email_value);
        emailText.setText(email);

        emailText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(getApplicationContext(), EmailSurveyActivity.class);
                startActivity(intent);
            }
        });
        /*
        //check whether demographics have been recorded
        boolean provided = Util.getBool(getApplicationContext(), CircogPrefs.DEMOGRAPHICS_PROVIDED, false);

        //check whether consent has been given
        if (!Util.getBool(this, CircogPrefs.PREF_CONSENT_GIVEN, false)) {
            final Intent intent = new Intent(this, ConsentActivity.class);
            startActivity(intent);
            finish();
        } else {

            launchNextTask();

            if(!provided) {
                //launch demographics activity
                final Intent intent = new Intent(this, EnterDemographicsActivity.class);
                startActivity(intent);
                finish();
            }

        }
        */

    }

    private void launchNextTask() {
        // only use PVT
        launchPVT(findViewById(android.R.id.content));

        /*
        //launch task according to tasklist
        int task = TaskList.getCurrentTask(getApplicationContext());
        if(task==PVTActivity.TASK_ID) {
            launchPVT(findViewById(android.R.id.content));
        }
        else if(task==GoNoGoActivity.TASK_ID) {
            launchGoNoGo(findViewById(android.R.id.content));
        }
        else if(task==MOTActivity.TASK_ID) {
            launchMOT(findViewById(android.R.id.content));
        }


         */
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void launchPVT (final View view) {
        if(CircogPrefs.DEBUG_MODE) {
            Log.i(TAG, "launchPVT()");
        }
        Util.putString(getApplicationContext(), CircogPrefs.CURRENT_TASK, LogManager.KEY_PVT);
        final Intent intent = new Intent(this, PVTActivity.class);
        startActivity(intent);
//        finish();
    }

    public void launchGoNoGo(final View view) {
        if(CircogPrefs.DEBUG_MODE) {
            Log.i(TAG, "launchGoNoGo()");
        }
        Util.putString(getApplicationContext(), CircogPrefs.CURRENT_TASK, LogManager.KEY_GNG);
        final Intent intent = new Intent(this, GoNoGoActivity.class);
        startActivity(intent);
    }

    public void launchMOT(final View view) {
        if(CircogPrefs.DEBUG_MODE) {
            Log.i(TAG, "launchMOT()");
        }
        Util.putString(getApplicationContext(), CircogPrefs.CURRENT_TASK, LogManager.KEY_MOT);
        final Intent intent = new Intent(this, MOTActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Util.circogIsRunning(getApplicationContext(), false);
    }
}
