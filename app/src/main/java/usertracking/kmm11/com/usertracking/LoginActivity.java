package usertracking.kmm11.com.usertracking;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;


public class LoginActivity extends AppCompatActivity {

    private EditText mUsernameView;
    private EditText mChannelView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);
        mChannelView = (EditText) findViewById(R.id.channel);
        Button mListenSignInButton = (Button) findViewById(R.id.sign_in_listenButton);
        mListenSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startListen();
            }
        });

        Button mShareSignInButton = (Button) findViewById(R.id.sign_in_ShareButton);
        mShareSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startShare();
            }
        });

    }
    private void startListen() {
        Constants.MODE = "listen";
        Constants.USERNAME = mUsernameView.getText().toString();
        Constants.CHANNEL_NAME = mChannelView.getText().toString();
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
        }
    private void startShare() {
        Constants.MODE = "share";
        Constants.USERNAME = mUsernameView.getText().toString();
        Constants.CHANNEL_NAME = mChannelView.getText().toString();
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }
}

