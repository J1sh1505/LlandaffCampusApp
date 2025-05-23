package com.example.llandaffcampusapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";
    private static final String USERS_COLLECTION = "users";
    private static final String SETTINGS_COLLECTION = "settings";
    
    //firebase components
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private GoogleSignInClient mGoogleSignInClient;
    
    //ui components
    private SignInButton signInButton;
    private TextView skipLoginTextView;
    private ProgressBar progressBar;
    
    //shared Preferences
    private SharedPreferences preferences;
    private static final String PREF_NAME = "LlandaffCampusSettings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_TEXT_SIZE = "text_size";
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    
    // Activity Result Launcher for Google Sign-In
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Sign in result code: " + result.getResultCode());
                
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d(TAG, "Google sign in succeeded, account: " + account.getEmail());
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(LoginActivity.this, 
                                "Google Sign-In failed. Please try again.", 
                                Toast.LENGTH_SHORT).show();
                        showSignInButton();
                    }
                } else {
                    Log.d(TAG, "Sign in canceled or failed: result code = " + result.getResultCode());
                    showSignInButton();
                }
            });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        
        // Initialize UI components
        signInButton = findViewById(R.id.sign_in_button);
        skipLoginTextView = findViewById(R.id.skip_login);
        progressBar = findViewById(R.id.progress_bar);
        
        // Get shared preferences
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Configure Google Sign-In - without web client ID as a fallback
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();
        
        try {
            // Try to use the default_web_client_id from strings.xml if it exists
            String webClientId = getString(R.string.default_web_client_id);
            if (webClientId != null && !webClientId.isEmpty()) {
                gsoBuilder.requestIdToken(webClientId);
                Log.d(TAG, "Using web client ID from strings.xml: " + webClientId);
            } else {
                Log.w(TAG, "default_web_client_id is empty or not defined");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting default_web_client_id", e);
        }
        
        GoogleSignInOptions gso = gsoBuilder.build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Set up click listeners
        signInButton.setOnClickListener(v -> signIn());
        skipLoginTextView.setOnClickListener(v -> skipLogin());
        
        // Check if user is already logged in
        if (preferences.getBoolean(PREF_IS_LOGGED_IN, false)) {
            proceedToMainActivity();
        }
        
        // Debug: Check if we have the SHA-1 cert fingerprint correct
        try {
            Log.d(TAG, "Signing config: " + getPackageManager().getPackageInfo(getPackageName(), 
                    android.content.pm.PackageManager.GET_SIGNATURES).signatures[0].toCharsString());
        } catch (Exception e) {
            Log.e(TAG, "Error getting signature", e);
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in with Google
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Log.d(TAG, "User already signed in with Google: " + account.getEmail());
        }
    }
    
    private void signIn() {
        showProgressBar();
        // Clear previous sign-in state
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        });
    }
    
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogle: " + account.getId());
        
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Log.d(TAG, "Got Firebase user: " + user.getEmail());
                                saveUserToPreferences(user.getUid());
                                checkAndSyncUserSettings(user.getUid());
                            } else {
                                Log.w(TAG, "User is null after successful login");
                                showSignInButton();
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, 
                                    "Authentication failed. Please try again.", 
                                    Toast.LENGTH_SHORT).show();
                            showSignInButton();
                        }
                    }
                });
    }
    
    private void saveUserToPreferences(String userId) {
        preferences.edit()
                .putString(PREF_USER_ID, userId)
                .putBoolean(PREF_IS_LOGGED_IN, true)
                .apply();
    }

    void checkAndSyncUserSettings(String userId) {
        DocumentReference userSettingsRef = mFirestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document("user_preferences");

        userSettingsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // User has settings in Firestore, sync to local preferences
                    String language = document.getString(PREF_LANGUAGE);
                    String textSize = document.getString(PREF_TEXT_SIZE);

                    if (language != null) {
                        preferences.edit().putString(PREF_LANGUAGE, language).apply();
                    }

                    if (textSize != null) {
                        preferences.edit().putString(PREF_TEXT_SIZE, textSize).apply();
                    }
                } else {
                    // No settings in Firestore, upload current preferences
                    syncLocalSettingsToFirestore(userId);
                }

                // Proceed to main activity after syncing
                proceedToMainActivity();
            } else {
                Log.w(TAG, "Error getting user settings", task.getException());
                proceedToMainActivity(); // Proceed anyway to not block the user
            }
        });
    }
    
    private void syncLocalSettingsToFirestore(String userId) {
        String language = preferences.getString(PREF_LANGUAGE, "en");
        String textSize = preferences.getString(PREF_TEXT_SIZE, "normal");
        
        Map<String, Object> userSettings = new HashMap<>();
        userSettings.put(PREF_LANGUAGE, language);
        userSettings.put(PREF_TEXT_SIZE, textSize);
        
        mFirestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document("user_preferences")
                .set(userSettings)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User settings synced to Firestore"))
                .addOnFailureListener(e -> Log.w(TAG, "Error syncing user settings", e));
    }
    
    private void skipLogin() {
        preferences.edit().putBoolean(PREF_IS_LOGGED_IN, false).apply();
        proceedToMainActivity();
    }
    
    private void proceedToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        signInButton.setVisibility(View.GONE);
        skipLoginTextView.setVisibility(View.GONE);
    }
    
    private void showSignInButton() {
        progressBar.setVisibility(View.GONE);
        signInButton.setVisibility(View.VISIBLE);
        skipLoginTextView.setVisibility(View.VISIBLE);
    }
}