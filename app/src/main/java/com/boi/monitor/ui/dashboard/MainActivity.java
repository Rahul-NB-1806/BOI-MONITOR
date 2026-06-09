package com.boi.monitor.ui.dashboard;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.widget.Toast;

import com.boi.monitor.R;
import com.boi.monitor.firebase.FirebaseDataModule;
import com.boi.monitor.service.BOINotificationListenerService;
import com.boi.monitor.ui.admin.AdminLoginActivity;
import com.boi.monitor.util.PrefsManager;
import com.boi.monitor.ui.cheque.ChequeManagementFragment;
import com.boi.monitor.ui.upi.UpiTransactionFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity — shell activity hosting three fragments via BottomNavigationView.
 *
 * Tabs:
 *   1. Dashboard (DashboardFragment) — stats overview + recent items
 *   2. Cheques  (ChequeManagementFragment) — full cheque list with filter
 *   3. UPI      (UpiTransactionFragment) — full UPI history
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String FRAGMENT_TAG_DASHBOARD = "dashboard";
    private static final String FRAGMENT_TAG_CHEQUES = "cheques";
    private static final String FRAGMENT_TAG_UPI = "upi";

    private BottomNavigationView bottomNav;

    // Keep fragment references to avoid re-creating on tab switch
    private DashboardFragment        dashboardFragment;
    private ChequeManagementFragment chequeFragment;
    private UpiTransactionFragment   upiFragment;
    private Fragment                 activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!PrefsManager.getInstance(this).isOnboarded()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupFragments(savedInstanceState);
        setupBottomNav();
        checkNotificationPermission();
    }

    // ── Fragment Management ────────────────────────────────────────────────────

    private void setupFragments(Bundle savedState) {
        if (savedState == null) {
            dashboardFragment = new DashboardFragment();
            chequeFragment    = new ChequeManagementFragment();
            upiFragment       = new UpiTransactionFragment();

            getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, dashboardFragment, FRAGMENT_TAG_DASHBOARD)
                .add(R.id.fragment_container, chequeFragment,    FRAGMENT_TAG_CHEQUES).hide(chequeFragment)
                .add(R.id.fragment_container, upiFragment,       FRAGMENT_TAG_UPI).hide(upiFragment)
                .commit();
            activeFragment = dashboardFragment;
            return;
        }

        dashboardFragment = (DashboardFragment) getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG_DASHBOARD);
        chequeFragment = (ChequeManagementFragment) getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG_CHEQUES);
        upiFragment = (UpiTransactionFragment) getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG_UPI);

        if (dashboardFragment == null) dashboardFragment = new DashboardFragment();
        if (chequeFragment == null) chequeFragment = new ChequeManagementFragment();
        if (upiFragment == null) upiFragment = new UpiTransactionFragment();

        activeFragment = dashboardFragment;
        if (chequeFragment.isAdded() && !chequeFragment.isHidden()) activeFragment = chequeFragment;
        if (upiFragment.isAdded() && !upiFragment.isHidden()) activeFragment = upiFragment;

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        if (!dashboardFragment.isAdded()) tx.add(R.id.fragment_container, dashboardFragment, FRAGMENT_TAG_DASHBOARD);
        if (!chequeFragment.isAdded()) tx.add(R.id.fragment_container, chequeFragment, FRAGMENT_TAG_CHEQUES);
        if (!upiFragment.isAdded()) tx.add(R.id.fragment_container, upiFragment, FRAGMENT_TAG_UPI);
        tx.hide(dashboardFragment).hide(chequeFragment).hide(upiFragment).show(activeFragment).commitNow();
    }

    private void showFragment(Fragment target) {
        if (target == activeFragment) return;
        getSupportFragmentManager().beginTransaction()
            .hide(activeFragment)
            .show(target)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit();
        activeFragment = target;
    }

    // ── Bottom Navigation ──────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) { showFragment(dashboardFragment); return true; }
            if (id == R.id.nav_cheques)   { showFragment(chequeFragment);    return true; }
            if (id == R.id.nav_upi)       { showFragment(upiFragment);       return true; }
            return false;
        });

        if (activeFragment == chequeFragment) bottomNav.setSelectedItemId(R.id.nav_cheques);
        else if (activeFragment == upiFragment) bottomNav.setSelectedItemId(R.id.nav_upi);
        else bottomNav.setSelectedItemId(R.id.nav_dashboard);
    }

    // ── Options Menu ───────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_admin) {
            startActivity(new Intent(this, AdminLoginActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_notification_access) {
            openNotificationSettings();
            return true;
        }
        if (item.getItemId() == R.id.action_privacy) {
            showPrivacyDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Privacy ─────────────────────────────────────────────────────────────────

    private void showPrivacyDialog() {
        String[] options = {"View Privacy Policy", "Delete My Data", "Export My Data"};
        new AlertDialog.Builder(this)
                .setTitle("Privacy Settings")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showPrivacyPolicyDialog();
                            break;
                        case 1:
                            showDeleteDataConfirmation();
                            break;
                        case 2:
                            showExportDataDialog();
                            break;
                    }
                })
                .show();
    }

    private void showPrivacyPolicyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage("BOI Monitor collects and stores the following data locally and in Firestore:\n\n"
                        + "• UPI credit notifications (amount, account suffix, reference number)\n"
                        + "• Cheque transaction notifications (cheque number, amount, status)\n"
                        + "• Notification processing logs\n\n"
                        + "This data is used solely to display your transaction history within the app. "
                        + "No data is shared with third parties. All data is scoped to your anonymous user ID.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showDeleteDataConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete My Data")
                .setMessage("This will permanently delete all your transaction data and notification logs "
                        + "from Firestore. This action cannot be undone. Continue?")
                .setPositiveButton("Delete", (d, w) -> deleteUserData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserData() {
        FirebaseDataModule.getInstance().deleteAllUserData()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "All your data has been deleted", Toast.LENGTH_SHORT).show();
                    // restart listeners to reflect empty state
                    FirebaseDataModule.getInstance().restartListening();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showExportDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Export My Data")
                .setMessage("Your data is already accessible within the app. "
                        + "You can view all your UPI and cheque transactions on their respective tabs. "
                        + "To export, take a screenshot or copy the information you need. "
                        + "A full JSON export feature is planned for a future update.")
                .setPositiveButton("OK", null)
                .show();
    }

    // ── Notification Permission ────────────────────────────────────────────────

    private void checkNotificationPermission() {
        if (!isNotificationListenerEnabled()) {
            new AlertDialog.Builder(this)
                .setTitle("Notification Access Required")
                .setMessage("BOI Monitor needs notification access to track your " +
                            "Bank of India transactions in real time.")
                .setPositiveButton("Enable Now", (d, w) -> openNotificationSettings())
                .setNegativeButton("Later", null)
                .setCancelable(false)
                .show();
        }
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        if (TextUtils.isEmpty(flat)) return false;
        String component = new ComponentName(this,
                BOINotificationListenerService.class).flattenToString();
        return flat.contains(component);
    }

    private void openNotificationSettings() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }
}
