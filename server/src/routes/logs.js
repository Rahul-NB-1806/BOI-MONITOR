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
      timestamp: req.body.timestamp ? new Date(req.body.timestamp) : new Date()
    };
    if (req.body.notificationType !== undefined && req.body.notificationType !== null) logData.notificationType = req.body.notificationType;
    if (req.body.processed !== undefined && req.body.processed !== null) logData.processed = req.body.processed;
    if (req.body.processingError !== undefined && req.body.processingError !== null) logData.processingError = req.body.processingError;
    if (req.body.rawTextStored !== undefined && req.body.rawTextStored !== null) logData.rawTextStored = req.body.rawTextStored;
    if (req.body.packageName !== undefined && req.body.packageName !== null) logData.packageName = req.body.packageName;

    const log = await logService.create(logData);

    res.status(201).json({
      message: 'Notification log saved',
      log
    });
  } catch (error) {
    next(error);
  }
});

router.delete('/', async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const result = await logService.deleteAll(userId);
    res.json({ message: 'All notification logs deleted', deletedCount: result.deletedCount });
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

    const result = await logService.deleteOlderThan(userId, cutoffDate);
    res.json({ message: 'Old notification logs deleted', deletedCount: result.deletedCount });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
