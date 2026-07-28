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
    const chequeData = {
      userId,
      chequeNumber: req.body.chequeNumber,
      amount: req.body.amount || null,
      status: req.body.status || null,
      availableBalance: req.body.availableBalance || null,
      transactionDate: req.body.transactionDate || null,
      favouringParty: req.body.favouringParty || null,
      timestamp: req.body.timestamp ? new Date(req.body.timestamp) : new Date()
    };

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
