package com.example.chatterbot;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chatterbot.apibot.ChatterBot;
import com.example.chatterbot.apibot.ChatterBotFactory;
import com.example.chatterbot.apibot.ChatterBotSession;
import com.example.chatterbot.apibot.ChatterBotType;
import com.example.chatterbot.data.FirebaseObject;
import com.example.chatterbot.data.Message;
import com.example.chatterbot.view.MainViewModel;
import com.example.chatterbot.view.RecyclerViewAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
{
    private MainViewModel mainViewModel;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private final int REQ_CODE = 100;
    EditText etText;
    TextToSpeech t1;
    FirebaseDatabase database;
    DatabaseReference myRef;

    FirebaseObject incomingObj, outcomingObj;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("data").child("users").child(FirebaseAuth.getInstance().getUid());

        incomingObj = new FirebaseObject();
        outcomingObj = new FirebaseObject();
        init();
    }

    private void init() {
        //View Models
        mainViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerViewAdapter = mainViewModel.getRecyclerViewAdapter();

        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());


        etText = findViewById(R.id.etText);
        FloatingActionButton btSend = findViewById(R.id.btSend);
        FloatingActionButton mic = findViewById(R.id.floatingActionButton);

        btSend.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String text = etText.getText().toString();
                if(text.length() > 0 && !mainViewModel.isWaitingResponse()) {
                    addMessage(true, text);
                    Log.v("Texto in translated", text);
                    etText.setText("");
                    incomingObj.setMessage(text);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                    Date date = new Date();
                    incomingObj.setDate(dateFormat.format(date));
                    incomingObj.setOutcoming(false);

                    mainViewModel.setWaitingResponse(true);
                    mainViewModel.translate("es", text, "en");
                }
            }
        });


        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(new Locale("es", "ES"));
                }
            }
        });

        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
                try {
                    startActivityForResult(intent, REQ_CODE);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            "Sorry your device not supported",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mainViewModel.setOnTranslationResultListener(new MainViewModel.OnTranslationResult()
        {
            @Override
            public void OnTranslationResult(boolean ok, String text, String countryCode)
            {
                if(ok) {
                    if(mainViewModel.isWaitingBotTranslation()) {
                        addMessage(false, text);
                        Log.v("Texto out translated", text);
                        outcomingObj.setMessageTranslation(text);
                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                        Date date = new Date();
                        outcomingObj.setDate(dateFormat.format(date));
                        outcomingObj.setOutcoming(true);
                        myRef.child(myRef.push().getKey()).setValue(outcomingObj);
                        Log.v("OBJECT OUTCOMING", outcomingObj.toString());
                        String toSpeak = text;
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                        mainViewModel.setWaitingResponse(false);
                        mainViewModel.setWaitingBotTranslation(false);
                    }
                    else {
                        mainViewModel.setTranslateCountryCode(countryCode);
                        new BotChat().execute(text);
                        incomingObj.setMessageTranslation(text);
                        myRef.child(myRef.push().getKey()).setValue(incomingObj);
                        Log.v("OBJECT INCOMING", incomingObj.toString());
                        Log.v("Texto in", text);
                    }
                }
                else {
                    addMessage(false, "¡Error!");
                    mainViewModel.setWaitingResponse(false);
                    mainViewModel.setWaitingBotTranslation(false);
                }
            }
        });

        database = FirebaseDatabase.getInstance();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE: {
                if ((resultCode == RESULT_OK) && (null != data)) {
                    ArrayList result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    etText.setText(result.get(0).toString());
                }
                break;
            }
        }
    }

    private void addMessage(boolean outcoming, String text)
    {
        recyclerViewAdapter.addMessage(new Message(outcoming, text));
        recyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());

    }

    private class BotChat extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... strings)
        {
            return chat(strings[0]);
        }

        @Override
        protected void onPostExecute(String s)
        {
            mainViewModel.translate("en", s, "es");
            Log.v("Texto outcoming", s);
            outcomingObj.setMessage(s);
            mainViewModel.setWaitingBotTranslation(true);
        }
    }

    private String chat(String message) {
        String response = "";
        try {
            ChatterBotFactory factory = new ChatterBotFactory();
            ChatterBot bot = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
            ChatterBotSession botSession = bot.createSession();
            response = botSession.think(message);
        }
        catch(Exception e) {
            AlertDialog alertDialog;
            AlertDialog.Builder alertDialog_builder = new AlertDialog.Builder(this)
                    .setTitle("No hay conexión")
                    .setMessage("No se ha podido enviar la petición de respuesta")
                    .setPositiveButton("OK", null);
            alertDialog = alertDialog_builder.create();
            alertDialog.show();
        }
        return response;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_items, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case R.id.history:
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            case R.id.logout:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.askLogout)
                        .setMessage(R.string.askLogoutMessage)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                logOut();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void logOut() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}
