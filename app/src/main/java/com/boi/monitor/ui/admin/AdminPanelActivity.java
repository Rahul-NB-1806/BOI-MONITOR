package com.boi.monitor.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.boi.monitor.R;
import com.boi.monitor.databinding.ActivityAdminPanelBinding;
import com.boi.monitor.firebase.FirebaseAuthManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * AdminPanelActivity
 *
 * Authenticated admin section showing notification processing stats and app settings.
 * Accessible only after PIN + Firebase auth.
 */
public class AdminPanelActivity extends AppCompatActivity {

    private ActivityAdminPanelBinding binding;
    private FirebaseFirestore          db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminPanelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Panel");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        loadStats();
        binding.btnRefresh.setOnClickListener(v -> loadStats());
    }

    private void loadStats() {
        // Notification logs count across all users (admin privilege)
        db.collectionGroup("notification_logs").get()
          .addOnSuccessListener(qs -> {
              int total     = qs.size();
              int processed = 0;
              for (QueryDocumentSnapshot doc : qs) {
                  Boolean p = doc.getBoolean("processed");
                  if (Boolean.TRUE.equals(p)) processed++;
              }
              binding.tvLogCount.setText("Total logs: " + total);
              binding.tvProcessedCount.setText("Processed: " + processed);
              binding.tvUnprocessedCount.setText("Unprocessed: " + (total - processed));
          })
          .addOnFailureListener(e ->
              Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // App version / info
        binding.tvAppVersion.setText("BOI Monitor v1.0.0 — Production Prototype");
        binding.tvAdminUser.setText("Logged in as: " +
            (FirebaseAuth.getInstance().getCurrentUser() != null
             ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
             : "Unknown"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Sign out of admin panel?")
                .setPositiveButton("Sign Out", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    // Re-establish anonymous auth for notification monitoring
                    FirebaseAuthManager.getInstance().signInAnonymously(new FirebaseAuthManager.AuthCallback() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            Log.i("AdminPanel", "Anonymous auth re-established: " + user.getUid());
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e("AdminPanel", "Anonymous re-auth failed: " + errorMessage);
                        }
                    });
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
