const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { upiService, chequeService, logService } = require('../services/dataService');

const router = express.Router();

router.use(authenticateToken);

const AVG_DOC_SIZE = { upi: 250, cheques: 300, logs: 200 };

router.get('/stats', async (req, res, next) => {
  try {
    const userId = req.user.userId;

    const [upiCount, chequeCount, logCount] = await Promise.all([
      upiService.count(userId),
      chequeService.count(userId),
      logService.count(userId)
    ]);

    const upiMB = parseFloat(((upiCount * AVG_DOC_SIZE.upi) / (1024 * 1024)).toFixed(1));
    const chequeMB = parseFloat(((chequeCount * AVG_DOC_SIZE.cheques) / (1024 * 1024)).toFixed(1));
    const logMB = parseFloat(((logCount * AVG_DOC_SIZE.logs) / (1024 * 1024)).toFixed(1));
    const totalMB = parseFloat((upiMB + chequeMB + logMB).toFixed(1));
    const totalCount = upiCount + chequeCount + logCount;

    res.json({
      upi: { count: upiCount, estimatedMB: upiMB },
      cheques: { count: chequeCount, estimatedMB: chequeMB },
      logs: { count: logCount, estimatedMB: logMB },
      total: { count: totalCount, estimatedMB: totalMB }
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
