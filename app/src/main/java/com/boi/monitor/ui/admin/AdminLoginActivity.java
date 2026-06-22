package com.boi.monitor.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.boi.monitor.R;
import com.boi.monitor.databinding.ActivityAdminLoginBinding;
import com.boi.monitor.network.AuthManager;

public class AdminLoginActivity extends AppCompatActivity {

    private static final String TAG = "AdminLogin";

    private ActivityAdminLoginBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Access");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        authManager = AuthManager.getInstance(this);

        if (authManager.isLoggedIn() && !authManager.isAnonymous()) {
            goToAdminPanel();
            return;
        }

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String pass  = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            binding.etPassword.setError("Required");
            return;
        }

        setLoading(true);

        authManager.login(email, pass, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String userId, String token) {
                setLoading(false);
                goToAdminPanel();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(AdminLoginActivity.this,
                        "Auth failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToAdminPanel() {
        startActivity(new Intent(this, AdminPanelActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
