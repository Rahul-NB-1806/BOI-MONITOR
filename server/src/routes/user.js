const express = require('express');
const { authenticateToken } = require('../middleware/auth');
const { userService } = require('../services/dataService');

const router = express.Router();

router.use(authenticateToken);

router.delete('/data', async (req, res, next) => {
  try {
    const userId = req.user.userId;

    const result = await userService.deleteAllData(userId);

    res.json({
      message: 'All user data deleted successfully',
      userId,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
