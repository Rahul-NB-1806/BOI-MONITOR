const AdminUser = require('../models/AdminUser');
const UpiTransaction = require('../models/UpiTransaction');
const ChequeTransaction = require('../models/ChequeTransaction');
const NotificationLog = require('../models/NotificationLog');

function generateUserId() {
  return 'user_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

const userService = {
  async findByEmail(email) {
    return await AdminUser.findOne({ email }).lean();
  },

  async findById(userId) {
    return await AdminUser.findOne({ userId }).lean();
  },

  async create(userData) {
    const user = new AdminUser({
      userId: userData.userId || generateUserId(),
      email: userData.email,
      password: userData.password
    });
    return await user.save();
  },

  async deleteByUserId(userId) {
    return await AdminUser.deleteOne({ userId });
  },

  async deleteAllData(userId) {
    const results = await Promise.all([
      UpiTransaction.deleteMany({}),
      ChequeTransaction.deleteMany({}),
      NotificationLog.deleteMany({})
    ]);
    return {
      upiDeleted: results[0].deletedCount,
      chequesDeleted: results[1].deletedCount,
      logsDeleted: results[2].deletedCount
    };
  }
};

const upiService = {
  async findAll(userId, limit = 100) {
    return await UpiTransaction.find({})
      .sort({ timestamp: -1 })
      .limit(limit)
      .lean();
  },

  async create(data) {
    const tx = new UpiTransaction(data);
    return await tx.save();
  },

  async deleteAll(userId) {
    return await UpiTransaction.deleteMany({});
  },

  async deleteOlderThan(userId, cutoffDate) {
    return await UpiTransaction.deleteMany({ timestamp: { $lt: cutoffDate } });
  },

  async count(userId) {
    return await UpiTransaction.countDocuments({});
  }
};

const chequeService = {
  async findAll(userId, limit = 100) {
    return await ChequeTransaction.find({})
      .sort({ timestamp: -1 })
      .limit(limit)
      .lean();
  },

  async upsert(data) {
    const { userId, chequeNumber } = data;
    const cleanNum = String(chequeNumber).trim().replace(/^0+(?!$)/, '');
    return await ChequeTransaction.findOneAndUpdate(
      { chequeNumber: cleanNum },
      { $set: { ...data, chequeNumber: cleanNum } },
      { upsert: true, new: true }
    ).lean();
  },

  async deleteAll(userId) {
    return await ChequeTransaction.deleteMany({});
  },

  async deleteOlderThan(userId, cutoffDate) {
    return await ChequeTransaction.deleteMany({ timestamp: { $lt: cutoffDate } });
  },

  async count(userId) {
    return await ChequeTransaction.countDocuments({});
  }
};

const logService = {
  async findAll(userId, limit = 100) {
    return await NotificationLog.find({})
      .sort({ timestamp: -1 })
      .limit(limit)
      .lean();
  },

  async create(data) {
    const log = new NotificationLog(data);
    return await log.save();
  },

  async deleteAll(userId) {
    return await NotificationLog.deleteMany({});
  },

  async deleteOlderThan(userId, cutoffDate) {
    return await NotificationLog.deleteMany({ timestamp: { $lt: cutoffDate } });
  },

  async count(userId) {
    return await NotificationLog.countDocuments({});
  }
};

module.exports = { userService, upiService, chequeService, logService };
