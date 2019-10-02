package org.ubicomp.attentiontest;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;

@IgnoreExtraProperties
public class ReactionTestResult {

    public String email;
    public ArrayList<Long> measurements;
    public int numberOfTaps;
    public long startTasksTime;
    public long endTasksTime;
    public boolean taskCompleted;
    public int alertness;
    public boolean caffeinated;
    public boolean nicotine;
    public boolean food;
    public boolean alcohol;

    public ReactionTestResult() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public ReactionTestResult(String email, ArrayList<Long> measurements, int numberOfTaps, long startTasksTime,
                              long endTasksTime, boolean taskCompleted, int alertness, boolean caffeinated,
                              boolean nicotine, boolean food, boolean alcohol) {

        this.email = email;
        this.measurements = measurements;
        this.numberOfTaps = numberOfTaps;
        this.startTasksTime = startTasksTime;
        this.endTasksTime = endTasksTime;
        this.taskCompleted = taskCompleted;
        this.alertness = alertness;
        this.caffeinated = caffeinated;
        this.nicotine = nicotine;
        this.food = food;
        this.alcohol = alcohol;
    }

}
