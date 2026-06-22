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

module.exports = router;
