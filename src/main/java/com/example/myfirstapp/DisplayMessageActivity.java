package com.example.myfirstapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.myfirstapp.client.view.NonBlockingInterpreter;


public class DisplayMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);

        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        TextView textView = findViewById(R.id.textView);
        textView.setText(message);

    }
    public void sendMessage2(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        //Intent intent = getIntent();
        //EditText editText = (EditText) findViewById(R.id.editText);
        //String message = editText.getText().toString();

        //intent.putExtra(EXTRA_INTER, nb);
        this.onBackPressed();
        //startActivity(intent);


    }
}
