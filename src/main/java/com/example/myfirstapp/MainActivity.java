package com.example.myfirstapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.myfirstapp.client.view.NonBlockingInterpreter;


public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public static final String EXTRA_INTER = "com.example.myfirstapp.MESSAGE";

    NonBlockingInterpreter nb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //nb.start();
        setContentView(R.layout.activity_main);
        TextView textView = (TextView)findViewById(R.id.textView3);
        System.out.println("view in create:" + textView);
        textView.setText("Hej");
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        //TextView textView = (TextView)findViewById(R.id.textView3);
        System.out.println("view in create:" + textView);
        //textView.setText("Hej");
        nb = new NonBlockingInterpreter(textView);
        System.out.println("IN MAIN Create");
        //setContentView(R.layout.activity_main);
    }
    @Override
    public void onResume() {
        super.onResume();
        setContentView(R.layout.activity_main);
    }

    /** Called when the user taps the send button */
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        //Intent intent = getIntent();
        EditText editText = (EditText) findViewById(R.id.editText);
        final String message = editText.getText().toString();
        System.out.println(message);
        new Thread(new Runnable() {
            @Override
            public void run() {
                nb.nextCall(message);
            }
        }).start();

        intent.putExtra(EXTRA_MESSAGE, message);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);

        //intent.putExtra(EXTRA_INTER, nb);
        startActivity(intent);


    }
}
