const mongoose = require('mongoose');

const chequeTransactionSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    index: true
  },
  chequeNumber: {
    type: String,
    required: true
  },
  amount: {
    type: Number
  },
  status: {
    type: String,
    enum: ['PRESENTED', 'CLEARED', 'RETURNED']
  },
  availableBalance: {
    type: Number
  },
  transactionDate: {
    type: String
  },
  favouringParty: {
    type: String
  },
  timestamp: {
    type: Date,
    default: Date.now
  }
});

chequeTransactionSchema.index({ userId: 1, timestamp: -1 });
chequeTransactionSchema.index({ userId: 1, chequeNumber: 1 });

module.exports = mongoose.model('ChequeTransaction', chequeTransactionSchema);
