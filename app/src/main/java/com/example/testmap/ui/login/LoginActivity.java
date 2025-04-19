package com.example.testmap.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.testmap.MapFragment;
import com.example.testmap.R;
import com.example.testmap.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;

    private boolean isLoginMode = true;
    private Button loginButton;
    private Button signupButton;
    private TextView toggleTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        loginButton = binding.login;
        signupButton = findViewById(R.id.signup);
        toggleTextView = findViewById(R.id.toggleLoginSignup);
        final ProgressBar loadingProgressBar = binding.loading;

        signupButton.setVisibility(View.GONE);

        toggleTextView.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            if (isLoginMode) {
                loginButton.setVisibility(View.VISIBLE);
                signupButton.setVisibility(View.GONE);
                toggleTextView.setText("Don't have an account? Sign Up");
            } else {
                loginButton.setVisibility(View.GONE);
                signupButton.setVisibility(View.VISIBLE);
                toggleTextView.setText("Already have an account? Log In");
            }
        });

        signupButton.setOnClickListener(v -> {
            String email = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.signUp(email, password);
            }
        });

        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null) return;
            loginButton.setEnabled(loginFormState.isDataValid());
            if (loginFormState.getUsernameError() != null) {
                usernameEditText.setError(getString(loginFormState.getUsernameError()));
            }
            if (loginFormState.getPasswordError() != null) {
                passwordEditText.setError(getString(loginFormState.getPasswordError()));
            }
        });

        loginViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) return;
            loadingProgressBar.setVisibility(View.GONE);
            if (loginResult.getError() != null) {
                showLoginFailed(loginResult.getError());
            }
            if (loginResult.getSuccess() != null) {
                updateUiWithUser(loginResult.getSuccess());
                redirectToMap();
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(
                        usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };

        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);

        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginViewModel.login(
                        usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
            return false;
        });

        loginButton.setOnClickListener(v -> {
            loadingProgressBar.setVisibility(View.VISIBLE);
            loginViewModel.login(
                    usernameEditText.getText().toString(),
                    passwordEditText.getText().toString());
        });
    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private void redirectToMap() {
        Intent intent = new Intent(this, MapFragment.class); // Use activity if Map is an Activity
        startActivity(intent);
        finish(); // close login screen
    }
}
