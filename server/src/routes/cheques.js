const express = require('express');
const { body, validationResult } = require('express-validator');
const { authenticateToken } = require('../middleware/auth');
const { chequeService } = require('../services/dataService');

const router = express.Router();

router.use(authenticateToken);

router.get('/', async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const limit = parseInt(req.query.limit, 10) || 100;

    const cheques = await chequeService.findAll(userId, limit);

    res.json(cheques);
  } catch (error) {
    next(error);
  }
});

router.post('/', [
  body('chequeNumber').notEmpty().withMessage('Cheque number is required')
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: 'Validation failed', details: errors.array() });
    }

    const userId = req.user.userId;
    // Only include fields that are actually present (avoid null overwrites)
    const chequeData = {
      userId,
      chequeNumber: req.body.chequeNumber,
      timestamp: req.body.timestamp ? new Date(req.body.timestamp) : new Date()
    };
    if (req.body.amount !== undefined && req.body.amount !== null) chequeData.amount = req.body.amount;
    if (req.body.status !== undefined && req.body.status !== null) chequeData.status = req.body.status;
    if (req.body.availableBalance !== undefined && req.body.availableBalance !== null) chequeData.availableBalance = req.body.availableBalance;
    if (req.body.transactionDate !== undefined && req.body.transactionDate !== null) chequeData.transactionDate = req.body.transactionDate;
    if (req.body.favouringParty !== undefined && req.body.favouringParty !== null) chequeData.favouringParty = req.body.favouringParty;

    const cheque = await chequeService.upsert(chequeData);

    res.status(201).json({
      message: 'Cheque transaction saved',
      cheque
    });
  } catch (error) {
    next(error);
  }
});

router.delete('/', async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const result = await chequeService.deleteAll(userId);
    res.json({ message: 'All cheque transactions deleted', deletedCount: result.deletedCount });
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

    const result = await chequeService.deleteOlderThan(userId, cutoffDate);
    res.json({ message: 'Old cheque transactions deleted', deletedCount: result.deletedCount });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
