const UpiTransaction = require('../models/UpiTransaction');
const ChequeTransaction = require('../models/ChequeTransaction');
const NotificationLog = require('../models/NotificationLog');

const statsService = {
  async getNotificationTypeCounts(userId) {
    const result = await NotificationLog.aggregate([
      { $match: { userId } },
      { $group: { _id: '$notificationType', count: { $sum: 1 } } },
      { $sort: { count: -1 } }
    ]);
    return result.map(r => ({ type: r._id, count: r.count }));
  },

  async getUpiTransactionStats(userId) {
    const total = await UpiTransaction.countDocuments({ userId });

    const typeBreakdown = await UpiTransaction.aggregate([
      { $match: { userId } },
      { $group: { _id: '$transactionType', count: { $sum: 1 }, totalAmount: { $sum: '$amount' } } }
    ]);

    const recentTransactions = await UpiTransaction.find({ userId })
      .sort({ timestamp: -1 })
      .limit(10)
      .lean();

    return {
      total,
      typeBreakdown: typeBreakdown.map(r => ({
        type: r._id,
        count: r.count,
        totalAmount: r.totalAmount
      })),
      recentTransactions
    };
  },

  async getChequeTransactionStats(userId) {
    const total = await ChequeTransaction.countDocuments({ userId });

    const statusBreakdown = await ChequeTransaction.aggregate([
      { $match: { userId } },
      { $group: { _id: '$status', count: { $sum: 1 }, totalAmount: { $sum: '$amount' } } }
    ]);

    const recentCheques = await ChequeTransaction.find({ userId })
      .sort({ timestamp: -1 })
      .limit(10)
      .lean();

    return {
      total,
      statusBreakdown: statusBreakdown.map(r => ({
        status: r._id,
        count: r.count,
        totalAmount: r.totalAmount
      })),
      recentCheques
    };
  },

  async getDashboardStats(userId) {
    const [notificationStats, upiStats, chequeStats] = await Promise.all([
      this.getNotificationTypeCounts(userId),
      this.getUpiTransactionStats(userId),
      this.getChequeTransactionStats(userId)
    ]);

    return {
      notifications: {
        byType: notificationStats,
        total: notificationStats.reduce((sum, n) => sum + n.count, 0)
      },
      upi: upiStats,
      cheques: chequeStats,
      generatedAt: new Date().toISOString()
    };
  }
};

module.exports = statsService;
