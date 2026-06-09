package com.boi.monitor.model;

/**
 * Aggregated dashboard statistics shown on the main screen.
 */
public class DashboardStats {

    private long totalUpiReceived;
    private long totalClearedAmount;
    private long totalReturnedAmount;
    private long totalProcessingAmount;
    private int    totalPresentedCount;
    private int    totalClearedCount;
    private int    totalReturnedCount;
    private int    totalUpiCount;

    public DashboardStats() {
        // defaults all zero
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

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
}
