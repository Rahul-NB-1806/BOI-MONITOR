package com.boi.monitor.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Notification processing log stored in Firestore for audit purposes.
 * Maps to: notification_logs/{documentId}
 */
@IgnoreExtraProperties
public class NotificationLog {

    public static final String COLLECTION = "notification_logs";

    public static final String TYPE_CHEQUE_CLEARED    = "CHEQUE_CLEARED";
    public static final String TYPE_CHEQUE_RETURNED   = "CHEQUE_RETURNED";
    public static final String TYPE_CHEQUE_PRESENTED  = "CHEQUE_PRESENTED";
    public static final String TYPE_UPI_CREDIT        = "UPI_CREDIT";
    public static final String TYPE_UNRECOGNIZED      = "UNRECOGNIZED";

    @DocumentId
    private String documentId;

    private String packageName;
    private String notificationType;
    private boolean processed;
    private String processingError;

    @ServerTimestamp
    private Timestamp timestamp;

    public NotificationLog() {}

    public NotificationLog(String packageName, String notificationType,
                           boolean processed, String processingError) {
        this.packageName       = packageName;
        this.notificationType  = notificationType;
        this.processed         = processed;
        this.processingError   = processingError;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getDocumentId()        { return documentId; }
    public String getPackageName()       { return packageName; }
    public String getNotificationType()  { return notificationType; }
    public boolean isProcessed()         { return processed; }
    public String getProcessingError()   { return processingError; }
    public Timestamp getTimestamp()      { return timestamp; }

    public void setDocumentId(String id)          { this.documentId = id; }
    public void setPackageName(String name)       { this.packageName = name; }
    public void setNotificationType(String type)  { this.notificationType = type; }
    public void setProcessed(boolean p)           { this.processed = p; }
    public void setProcessingError(String err)    { this.processingError = err; }
    public void setTimestamp(Timestamp t)         { this.timestamp = t; }
}
