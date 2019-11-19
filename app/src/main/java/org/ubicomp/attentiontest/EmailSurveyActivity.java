package org.ubicomp.attentiontest;

import android.app.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EmailSurveyActivity extends Activity {
    EditText email;
    Button submit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_survey);

        email = findViewById(R.id.participation_email_input);
        submit = findViewById(R.id.survey_send_email);

        email.setText(Util.getEmail(getApplicationContext()));

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (email.getText().length() > 2) {
                    Util.setEmail(getApplicationContext(), email.getText().toString());
                    finish();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Please fill in your participant ID before submitting", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
