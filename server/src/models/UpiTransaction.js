const mongoose = require('mongoose');

const upiTransactionSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    index: true
  },
  amount: {
    type: Number,
    required: true
  },
  transactionType: {
    type: String,
    required: true,
    enum: ['UPI_CREDIT', 'UPI_DEBIT', 'CREDIT', 'DEBIT']
  },
  accountSuffix: {
    type: String
  },
  referenceNumber: {
    type: String
  },
  debitedAccount: {
    type: String
  },
  transactionDate: {
    type: String
  },
  voiceAnnounced: {
    type: Boolean,
    default: false
  },
  timestamp: {
    type: Date,
    default: Date.now
  }
});

upiTransactionSchema.index({ userId: 1, timestamp: -1 });
upiTransactionSchema.index({ userId: 1, referenceNumber: 1 });

module.exports = mongoose.model('UpiTransaction', upiTransactionSchema);
