const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const statsService = require('../services/statsService');

const router = express.Router();

router.use(authenticateToken);

router.get('/stats', async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const stats = await statsService.getDashboardStats(userId);

    const totalLogs = stats.notifications.total;
    const processedLogs = stats.notifications.byType
      .filter(n => n.type && n.type.startsWith('CHEQUE'))
      .reduce((sum, n) => sum + n.count, 0);
    const unprocessedLogs = totalLogs - processedLogs;

    res.json({
      totalLogCount: totalLogs,
      processedLogCount: processedLogs,
      unprocessedLogCount: unprocessedLogs,
      totalUpiReceived: stats.upi.total > 0 ? stats.upi.typeBreakdown.reduce((s, t) => s + t.totalAmount, 0) : 0,
      totalClearedAmount: stats.cheques.statusBreakdown.filter(s => s.status === 'CLEARED').reduce((s, x) => s + x.totalAmount, 0),
      totalReturnedAmount: stats.cheques.statusBreakdown.filter(s => s.status === 'RETURNED').reduce((s, x) => s + x.totalAmount, 0),
      totalUpiCount: stats.upi.total,
      totalClearedCount: stats.cheques.statusBreakdown.filter(s => s.status === 'CLEARED').reduce((s, x) => s + x.count, 0),
      totalReturnedCount: stats.cheques.statusBreakdown.filter(s => s.status === 'RETURNED').reduce((s, x) => s + x.count, 0),
      totalPresentedCount: stats.cheques.statusBreakdown.filter(s => s.status === 'PRESENTED').reduce((s, x) => s + x.count, 0)
    });
  } catch (error) {
    next(error);
  }
});

router.post('/migrate', async (req, res, next) => {
  try {
    res.json({
      message: 'Migration endpoint ready. Implement Firestore to MongoDB migration logic here.',
      status: 'placeholder',
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
