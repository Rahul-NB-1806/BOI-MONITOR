package com.boi.monitor.model;

public class DashboardStats {

    private long totalUpiReceived;
    private long totalClearedAmount;
    private long totalReturnedAmount;
    private long totalProcessingAmount;
    private int totalPresentedCount;
    private int totalClearedCount;
    private int totalReturnedCount;
    private int totalUpiCount;
    private int totalLogCount;
    private int processedLogCount;
    private int unprocessedLogCount;

    public DashboardStats() {}

    public long getTotalUpiReceived()     { return totalUpiReceived; }
    public void setTotalUpiReceived(long v){ this.totalUpiReceived = v; }

    public long getTotalClearedAmount()   { return totalClearedAmount; }
    public void setTotalClearedAmount(long v){ this.totalClearedAmount = v; }

    public long getTotalReturnedAmount()  { return totalReturnedAmount; }
    public void setTotalReturnedAmount(long v){ this.totalReturnedAmount = v; }

    public long getTotalProcessingAmount() { return totalProcessingAmount; }
    public void setTotalProcessingAmount(long v) { this.totalProcessingAmount = v; }

    public int getTotalPresentedCount()     { return totalPresentedCount; }
    public void setTotalPresentedCount(int v){ this.totalPresentedCount = v; }

    public int getTotalClearedCount()       { return totalClearedCount; }
    public void setTotalClearedCount(int v) { this.totalClearedCount = v; }

    public int getTotalReturnedCount()      { return totalReturnedCount; }
    public void setTotalReturnedCount(int v){ this.totalReturnedCount = v; }

    public int getTotalUpiCount()           { return totalUpiCount; }
    public void setTotalUpiCount(int v)     { this.totalUpiCount = v; }

    public int getTotalLogCount()           { return totalLogCount; }
    public void setTotalLogCount(int v)     { this.totalLogCount = v; }

    public int getProcessedLogCount()       { return processedLogCount; }
    public void setProcessedLogCount(int v) { this.processedLogCount = v; }

    public int getUnprocessedLogCount()     { return unprocessedLogCount; }
    public void setUnprocessedLogCount(int v){ this.unprocessedLogCount = v; }
}
