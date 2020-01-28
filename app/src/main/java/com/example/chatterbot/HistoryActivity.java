package com.example.chatterbot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.chatterbot.data.FirebaseObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    TextView mensajes;
    List<FirebaseObject> firebaseObjects;
    DatabaseReference reference;
    AlertDialog alertDialog;
    String allChats = "";
    TextToSpeech t1;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mensajes = findViewById(R.id.mensajesTV);
        firebaseObjects = new ArrayList<>();
        button = findViewById(R.id.button2);

        alertDialog = waitDialog().create();
        alertDialog.show();

        reference = FirebaseDatabase.getInstance().getReference("data").child("users").child(FirebaseAuth.getInstance().getUid());
        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    firebaseObjects.add(postSnapshot.getValue(FirebaseObject.class));
                    Log.v("OBJECT dataSnapshot", postSnapshot.getValue(FirebaseObject.class).toString());
                }
                for (int i = 0; i < firebaseObjects.size(); i++) {
                    if (firebaseObjects.get(i).isOutcoming()) {
                        allChats += "Bot " + firebaseObjects.get(i).getDate() + " -> " + firebaseObjects.get(i).getMessageTranslation() + "\n";
                    } else {
                        allChats += "You " + firebaseObjects.get(i).getDate() + " -> " + firebaseObjects.get(i).getMessage() + "\n";
                    }
                }
                mensajes.setText(allChats);
                alertDialog.cancel();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.v("OBJECT dataSnapshot", "Failed to retrieve data");
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR) {
                            t1.setLanguage(new Locale("es", "ES"));
                        }
                    }
                });
                if (t1.isSpeaking()) {
                    t1.stop();
                } else {
                    t1.speak(allChats, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });

    }

    public AlertDialog.Builder waitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.working))
                .setMessage(getResources().getString(R.string.preparingData))
                .setCancelable(false);
        return builder;
    }



}
