package org.ubicomp.attentiontest;

import android.provider.Settings;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;

@IgnoreExtraProperties
public class ReactionTestResult {

    public int work;
    public String email;
    public String measurements;
    public int numberOfTaps;
    public int numberOfErrors;
    public long startTasksTime;
    public long endTasksTime;
    public boolean taskCompleted;
    public int alertness;
    public int focus;
    public int attention;
    public boolean caffeinated;
    public boolean nicotine;
    public boolean food;
    public boolean alcohol;
    public String device_id;

    public ReactionTestResult(String device_id, String email, String measurements, int numberOfTaps, int numberOfErrors, long startTasksTime,
                              long endTasksTime, boolean taskCompleted, int alertness, boolean caffeinated,
                              boolean nicotine, boolean food, boolean alcohol, int focus, int attention, int work) {

        this.device_id = device_id;
        this.email = email;
        this.measurements = measurements;
        this.numberOfTaps = numberOfTaps;
        this.numberOfErrors = numberOfErrors;
        this.startTasksTime = startTasksTime;
        this.endTasksTime = endTasksTime;
        this.taskCompleted = taskCompleted;
        this.alertness = alertness;
        this.caffeinated = caffeinated;
        this.nicotine = nicotine;
        this.food = food;
        this.alcohol = alcohol;

        this.focus = focus;
        this.attention = attention;
        this.work = work;
    }

}
