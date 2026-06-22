const mongoose = require('mongoose');

const notificationLogSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    index: true
  },
  notificationType: {
    type: String
  },
  processed: {
    type: Boolean,
    default: false
  },
  processingError: {
    type: String
  },
  rawTextStored: {
    type: String
  },
  packageName: {
    type: String
  },
  timestamp: {
    type: Date,
    default: Date.now
  }
});

notificationLogSchema.index({ userId: 1, timestamp: -1 });

module.exports = mongoose.model('NotificationLog', notificationLogSchema);
