const express = require('express');
const { body, validationResult } = require('express-validator');
const { authenticateToken } = require('../middleware/auth');
const { logService } = require('../services/dataService');

const router = express.Router();

router.use(authenticateToken);

router.get('/', async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const limit = parseInt(req.query.limit, 10) || 100;

    const logs = await logService.findAll(userId, limit);

    res.json(logs);
  } catch (error) {
    next(error);
  }
});

router.post('/', [
  body('notificationType').optional().isString(),
  body('rawTextStored').optional().isString()
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: 'Validation failed', details: errors.array() });
    }

    const userId = req.user.userId;
    const logData = {
      userId,
      notificationType: req.body.notificationType || null,
      processed: req.body.processed || false,
      processingError: req.body.processingError || null,
      rawTextStored: req.body.rawTextStored || null,
      packageName: req.body.packageName || null,
      timestamp: req.body.timestamp ? new Date(req.body.timestamp) : new Date()
    };

    const log = await logService.create(logData);

    res.status(201).json({
      message: 'Notification log saved',
      log
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
