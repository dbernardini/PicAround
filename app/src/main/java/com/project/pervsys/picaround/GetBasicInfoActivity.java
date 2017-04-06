package com.project.pervsys.picaround;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.domain.User;
import com.project.pervsys.picaround.utility.Config;

public class GetBasicInfoActivity extends AppCompatActivity {
    private static final int MIN_AGE = 6;
    private final static int MAX_AGE = 95;
    private final static String TAG = "GetBasicInfoActivity";
    private final static String USERS = "users";
    private final static String USERNAME = "username";
    private final static String AGE = "age";
    private GoogleApiClient mGoogleApiClient;
    private User newUser;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private String age;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_basic_info);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onBackPressed(){
        prepareLogOut();
    }


    public void onClick(View w){
        EditText ageField = (EditText) findViewById(R.id.age);
        age = ageField.getText().toString();
        EditText usernameField = (EditText) findViewById(R.id.username);
        String username = usernameField.getText().toString();
        if (checkAge(age)){
            //newUser.setAge(Integer.parseInt(age));
            if (!checkUsername(username)) {
                usernameField.setHint(R.string.username);
                usernameField.setText("");
            }
        }
        else{
            ageField.setHint(R.string.age);
            ageField.setText("");
        }
    }

    private boolean checkAge(String age){
        if (age.equals("")) {
            Toast.makeText(this, R.string.age_missing, Toast.LENGTH_SHORT).show();
            return false;
        }
        int a = Integer.parseInt(age);
        if (a < MIN_AGE || a > MAX_AGE){
            Toast.makeText(this, R.string.age_not_in_range, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean checkUsername(final String username){
        if (username.equals("")){
            Toast.makeText(this, R.string.username_missing, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (username.contains(" ")){
            Toast.makeText(this, R.string.username_with_spaces,Toast.LENGTH_SHORT).show();
            return false;
        }
        //query to database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child(USERS).orderByChild(USERNAME).equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // username already used
                            Toast.makeText(getApplicationContext(),
                                    R.string.username_unavailable,
                                    Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "Username unavailable");
                        } else {
                            //username not used
                            Log.i(TAG, "Username ok");
                            Intent i = getIntent();
                            i.putExtra(USERNAME, username);
                            i.putExtra(AGE, age);
                            setResult(RESULT_OK, i);
                            Log.i(TAG, "Data sent to LoginActivity");
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
        return true;
    }

    private void prepareLogOut(){
        mGoogleApiClient = ApplicationClass.getGoogleApiClient();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
        AlertDialog.Builder dialog = new AlertDialog.Builder(GetBasicInfoActivity.this)
                .setTitle(R.string.exit)
                .setMessage(R.string.registration_exit)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        logOut();
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                });
        dialog.show();
    }

    private void logOut() {

        // logout Facebook
        if (Profile.getCurrentProfile() != null) {
            LoginManager.getInstance().logOut();
            Log.i(TAG, "Logout from Facebook");
            getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit()
                    .putString(Config.LOG_PREF_INFO, Config.NOT_LOGGED).apply();
            setResult(RESULT_CANCELED);
            finish();
        }
        else{
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Logout from Google");
                            getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit()
                                    .putString(Config.LOG_PREF_INFO, Config.NOT_LOGGED).apply();
                            ApplicationClass.setGoogleApiClient(null);
                            setResult(RESULT_CANCELED);
                            finish();
                        } else
                            Log.e(TAG, "Error during the Google logout");
                    }
                });
        }
    }
}