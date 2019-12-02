package org.ubicomp.attentiontest;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static android.content.ContentValues.TAG;

public class PostTaskSurveyActivity extends Activity {

        private Button buttonSubmit;
        private RadioGroup radioAttention;
        private RadioGroup radioFocus;
        private CardView errorMessage;
        private CardView studyCompleted;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.post_test_survey);

            Util.circogIsRunning(getApplicationContext(), true);

            //check whether choice is made and log it
            buttonSubmit = (Button) findViewById(R.id.survey_send);
            radioAttention = (RadioGroup) findViewById(R.id.survey_focus);
            radioFocus = (RadioGroup) findViewById(R.id.survey_yn);

            errorMessage = (CardView) findViewById(R.id.error_message);
            studyCompleted = (CardView) findViewById(R.id.cardview_study_completed);

            buttonSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int attention = Util.getRating(radioAttention);
                    // 0 = no 1 = yes
                    int focus = Util.getRating(radioFocus);

                    if(attention==-1 || focus==-1) {
                        errorMessage.setVisibility(View.VISIBLE);
                    } else {
                        // get previous results
                        int alertness = Util.getInt(getApplicationContext(), CircogPrefs.LEVEL_ALERTNESS, -1);
                        boolean caffeinated = Util.getBool(getApplicationContext(), CircogPrefs.CAFFEINATED, false);
                        boolean nicotine = Util.getBool(getApplicationContext(), CircogPrefs.NICOTINE, false);
                        boolean food = Util.getBool(getApplicationContext(), CircogPrefs.FOOD, false);
                        boolean alcohol = Util.getBool(getApplicationContext(), CircogPrefs.ALCOHOL, false);
                        boolean taskCompleted = Util.getBool(getApplicationContext(), "taskCompleted", false);
                        String measurements = Util.getString(getApplicationContext(), "measurements", "");
                        int numberOfTaps= Util.getInt(getApplicationContext(), "falsePositives", -1);
                        int numberOfErrors = Util.getInt(getApplicationContext(), "numErrors", -1);
                        long startTasksTime = Util.getLong(getApplicationContext(), "testStart", 0);
                        long endTasksTime = Util.getLong(getApplicationContext(), "testEnd", 0);
                        // store results
                        Log.d(TAG, "Starting result storing");
                        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

                        ReactionTestResult result = new ReactionTestResult(Settings.Secure.getString(getApplicationContext().getContentResolver(),
                                Settings.Secure.ANDROID_ID), Util.getEmail(getApplicationContext()), measurements, numberOfTaps, numberOfErrors, startTasksTime, endTasksTime, taskCompleted, alertness, caffeinated, nicotine, food, alcohol, focus, attention);
                        mDatabase.child("reaction_tests").child(String.valueOf(System.currentTimeMillis())).setValue(result)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "result upload success");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "result upload failed");
                                    }
                                });
                        mDatabase.child("reaction_tests").push();

                        // mark when the last task was done
                        Util.storeLastTask(getApplicationContext());

                        finish();
                    }

                }
            });

            //check if studycompleted
            boolean completed = Util.studyCompleted(getApplicationContext());
            if(completed) {
                studyCompleted.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            Util.circogIsRunning(getApplicationContext(), false);
        }


}
