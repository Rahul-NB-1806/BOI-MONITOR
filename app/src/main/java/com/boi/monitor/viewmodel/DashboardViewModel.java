package com.boi.monitor.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.boi.monitor.model.ChequeTransaction;
import com.boi.monitor.model.DashboardStats;
import com.boi.monitor.model.UpiTransaction;
import com.boi.monitor.network.ApiDataModule;

import java.util.List;

public class DashboardViewModel extends ViewModel {

    private final ApiDataModule dataModule;

    public DashboardViewModel() {
        dataModule = ApiDataModule.getInstance();
        dataModule.refreshAll();
    }

    public LiveData<List<UpiTransaction>>    getUpiTransactions()    { return dataModule.getUpiTransactions(); }
    public LiveData<List<ChequeTransaction>> getChequeTransactions() { return dataModule.getChequeTransactions(); }
    public LiveData<DashboardStats>          getDashboardStats()     { return dataModule.getDashboardStats(); }
    public LiveData<String>                  getError()              { return dataModule.getError(); }
}
