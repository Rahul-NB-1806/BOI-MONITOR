const express = require('express');
const { body, validationResult } = require('express-validator');
const { authenticateToken } = require('../middleware/auth');
const { upiService } = require('../services/dataService');

const router = express.Router();

router.use(authenticateToken);

router.get('/', async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const limit = parseInt(req.query.limit, 10) || 100;

    const transactions = await upiService.findAll(userId, limit);

    res.json(transactions);
  } catch (error) {
    next(error);
  }
});

router.post('/', [
  body('amount').isNumeric().withMessage('Amount must be a number'),
  body('transactionType').notEmpty().withMessage('Transaction type is required')
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: 'Validation failed', details: errors.array() });
    }

    const userId = req.user.userId;
    const transactionData = {
      userId,
      amount: req.body.amount,
      transactionType: req.body.transactionType,
      timestamp: req.body.timestamp ? new Date(req.body.timestamp) : new Date()
    };
    if (req.body.accountSuffix !== undefined && req.body.accountSuffix !== null) transactionData.accountSuffix = req.body.accountSuffix;
    if (req.body.referenceNumber !== undefined && req.body.referenceNumber !== null) transactionData.referenceNumber = req.body.referenceNumber;
    if (req.body.debitedAccount !== undefined && req.body.debitedAccount !== null) transactionData.debitedAccount = req.body.debitedAccount;
    if (req.body.transactionDate !== undefined && req.body.transactionDate !== null) transactionData.transactionDate = req.body.transactionDate;
    if (req.body.voiceAnnounced !== undefined && req.body.voiceAnnounced !== null) transactionData.voiceAnnounced = req.body.voiceAnnounced;

    const transaction = await upiService.create(transactionData);

    res.status(201).json({
      message: 'UPI transaction saved',
      transaction
    });
  } catch (error) {
    next(error);
  }
});

router.delete('/', async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const result = await upiService.deleteAll(userId);
    res.json({ message: 'All UPI transactions deleted', deletedCount: result.deletedCount });
  } catch (error) {
    next(error);
  }
});

router.delete('/older-than', async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const { days, date } = req.body;

    let cutoffDate;
    if (date) {
      cutoffDate = new Date(date);
    } else if (days) {
      cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - parseInt(days, 10));
    } else {
      return res.status(400).json({ error: 'Provide either days or date' });
    }

    if (isNaN(cutoffDate.getTime())) {
      return res.status(400).json({ error: 'Invalid date' });
    }

    const result = await upiService.deleteOlderThan(userId, cutoffDate);
    res.json({ message: 'Old UPI transactions deleted', deletedCount: result.deletedCount });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
