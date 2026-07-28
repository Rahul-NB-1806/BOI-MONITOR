package com.boi.monitor.model;

public class StorageStats {

    private CategoryStats upi;
    private CategoryStats cheques;
    private CategoryStats logs;
    private CategoryStats total;

    public CategoryStats getUpi() { return upi; }
    public CategoryStats getCheques() { return cheques; }
    public CategoryStats getLogs() { return logs; }
    public CategoryStats getTotal() { return total; }

    public static class CategoryStats {
        private int count;
        private double estimatedMB;

        public int getCount() { return count; }
        public double getEstimatedMB() { return estimatedMB; }
    }
}
