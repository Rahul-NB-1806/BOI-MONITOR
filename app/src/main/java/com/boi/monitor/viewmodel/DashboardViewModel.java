package com.boi.monitor.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.boi.monitor.firebase.FirebaseDataModule;
import com.boi.monitor.model.ChequeTransaction;
import com.boi.monitor.model.DashboardStats;
import com.boi.monitor.model.UpiTransaction;

import java.util.List;

/**
 * DashboardViewModel
 *
 * Bridges FirebaseDataModule with the Dashboard UI.
 * Owns the Firestore listener lifecycle: starts on first observe, stops on cleared.
 */
public class DashboardViewModel extends ViewModel {

    private final FirebaseDataModule dataModule;

    public DashboardViewModel() {
        dataModule = FirebaseDataModule.getInstance();
        dataModule.startListening();
    }

    // ── Exposed LiveData ────────────────────────────────────────────────────────

    public LiveData<List<UpiTransaction>>    getUpiTransactions()    { return dataModule.getUpiTransactions(); }
    public LiveData<List<ChequeTransaction>> getChequeTransactions() { return dataModule.getChequeTransactions(); }
    public LiveData<DashboardStats>          getDashboardStats()     { return dataModule.getDashboardStats(); }
    public LiveData<String>                  getError()              { return dataModule.getError(); }

    // ── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();
        dataModule.stopListening();
    }
}
