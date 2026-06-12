package com.blossomcraft.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.blossomcraft.core.BlossomCraft;
import com.blossomcraft.core.model.AuthResponse;

/**
 * Login / registration screen. Restores an existing session on launch and,
 * if valid, jumps straight to {@link MainActivity}. Otherwise it offers
 * email/password login & registration plus Google sign-in.
 */
public class AuthActivity extends AppCompatActivity {

    private BlossomCraft bc;
    private boolean registerMode = false;

    private EditText nameField;
    private EditText emailField;
    private EditText passwordField;
    private Button submit;
    private TextView toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        bc = BlossomApp.core(this);

        nameField = findViewById(R.id.field_name);
        emailField = findViewById(R.id.field_email);
        passwordField = findViewById(R.id.field_password);
        submit = findViewById(R.id.btn_submit);
        toggle = findViewById(R.id.link_toggle);
        Button google = findViewById(R.id.btn_google);

        submit.setOnClickListener(v -> onSubmit());
        toggle.setOnClickListener(v -> toggleMode());
        google.setOnClickListener(v -> startGoogleSignIn());

        applyMode();
        tryRestoreSession();
    }

    private void tryRestoreSession() {
        if (bc.api().tokenStore().getToken() == null) {
            return;
        }
        Async.run(bc::restoreSession, user -> {
            if (user != null) {
                goToMain();
            }
        }, err -> { /* stay on auth screen */ });
    }

    private void toggleMode() {
        registerMode = !registerMode;
        applyMode();
    }

    private void applyMode() {
        nameField.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        submit.setText(registerMode ? R.string.register : R.string.login);
        toggle.setText(registerMode ? R.string.have_account : R.string.no_account);
    }

    private void onSubmit() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString();
        submit.setEnabled(false);

        Async.OnSuccess<AuthResponse> onSuccess = res -> {
            submit.setEnabled(true);
            if (res != null && res.user != null) {
                bc.setSession(res.user);
                goToMain();
            } else {
                toast(res != null && res.error != null ? res.error : getString(R.string.login_failed));
            }
        };
        Async.OnError onError = err -> {
            submit.setEnabled(true);
            toast(Async.message(err));
        };

        if (registerMode) {
            String name = nameField.getText().toString().trim();
            Async.run(() -> bc.auth().register(name, email, password), onSuccess, onError);
        } else {
            Async.run(() -> bc.auth().login(email, password), onSuccess, onError);
        }
    }

    /**
     * Launches the native Google Sign-In flow. The resulting access token is sent
     * to google_auth.php (same exchange the website performs). Configure the OAuth
     * client id in google-services / the developer console — see the README.
     */
    private void startGoogleSignIn() {
        Toast.makeText(this, R.string.google_hint, Toast.LENGTH_LONG).show();
        // Integration point: obtain a GoogleSignInAccount, then:
        //   Async.run(() -> bc.auth().googleAuth("login", accessToken), onSuccess, onError);
        // The README documents wiring play-services-auth to fetch the token.
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
