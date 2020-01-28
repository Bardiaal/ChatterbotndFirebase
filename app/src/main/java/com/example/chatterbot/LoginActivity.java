package com.example.chatterbot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    EditText numero;
    Button button;
    AlertDialog alertDialog, waitDialog;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;
    String verificationId;
    FirebaseDatabase database;
    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        FirebaseApp.initializeApp(this);

        userIsLoggedIn();

        alertDialog = builder().create();
        numero = findViewById(R.id.numeroET);
        button = findViewById(R.id.button);

        database = FirebaseDatabase.getInstance();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVerificationPhoneNumber();
            }
        });

        mCallBacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneCredentials(phoneAuthCredential);
                waitDialog.cancel();
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(LoginActivity.this, getResources().getString(R.string.failedVerification), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                verificationId = s;
            }
        };
    }

    public void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = null;
        try {
            credential = PhoneAuthProvider.getCredential(verificationId, code);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(LoginActivity.this, getResources().getString(R.string.cantVerify), Toast.LENGTH_LONG).show();
        }
        signInWithPhoneCredentials(credential);
    }

    private void signInWithPhoneCredentials(PhoneAuthCredential phoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    userIsLoggedIn();
                }
            }
        });
    }

    private void userIsLoggedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void startVerificationPhoneNumber() {
        try {
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    "+34" + numero.getText().toString(),
                    120,
                    TimeUnit.SECONDS,
                    this,
                    mCallBacks
            );
            alertDialog.show();
        } catch (Exception e) {
            Toast.makeText(LoginActivity.this, getResources().getString(R.string.numberError), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public AlertDialog.Builder builder() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.edittext_dialog, null);
        final TextInputEditText codeEditText = dialogView.findViewById(R.id.codeET);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.mess_title_dialog))
                .setMessage(getResources().getString(R.string.mess_number))
                .setView(dialogView)
                .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        waitDialog = waitDialog().create();
                        hideKeyboard(LoginActivity.this);
                        waitDialog.show();
                        verifyPhoneNumberWithCode(verificationId, codeEditText.getText().toString());
                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(LoginActivity.this, getResources().getString(R.string.cancelledVerNum), Toast.LENGTH_LONG).show();
                    }
                })
                .setCancelable(false);
        return builder;
    }

    public AlertDialog.Builder waitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.working))
                .setMessage(getResources().getString(R.string.waitPlease))
                .setCancelable(false);
        return builder;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
