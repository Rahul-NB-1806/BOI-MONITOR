const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { body, validationResult } = require('express-validator');
const config = require('../config');
const { authenticateApiKey } = require('../middleware/auth');
const { userService } = require('../services/dataService');

const router = express.Router();

router.post('/register', [
  body('email').isEmail().withMessage('Valid email is required'),
  body('password').isLength({ min: 6 }).withMessage('Password must be at least 6 characters')
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: 'Validation failed', details: errors.array() });
    }

    const { email, password } = req.body;

    const existingUser = await userService.findByEmail(email);
    if (existingUser) {
      return res.status(409).json({ error: 'Email already registered' });
    }

    const hashedPassword = await bcrypt.hash(password, 12);

    const user = await userService.create({
      email,
      password: hashedPassword
    });

    const token = jwt.sign(
      { userId: user.userId, email, isAdmin: false },
      config.jwtSecret,
      { expiresIn: '30d' }
    );

    res.status(201).json({
      token,
      userId: user.userId,
      message: 'User registered successfully'
    });
  } catch (error) {
    next(error);
  }
});

router.post('/login', [
  body('email').isEmail().withMessage('Valid email is required'),
  body('password').notEmpty().withMessage('Password is required')
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: 'Validation failed', details: errors.array() });
    }

    const { email, password } = req.body;

    const user = await userService.findByEmail(email);
    if (!user) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const token = jwt.sign(
      { userId: user.userId, email: user.email, isAdmin: user.isAdmin || false },
      config.jwtSecret,
      { expiresIn: '30d' }
    );

    res.json({
      token,
      userId: user.userId,
      message: 'Login successful'
    });
  } catch (error) {
    next(error);
  }
});

router.post('/anonymous', authenticateApiKey, async (req, res, next) => {
  try {
    let { userId } = req.body;

    if (userId) {
      const existingUser = await userService.findById(userId);
      if (existingUser) {
        const token = jwt.sign(
          { userId, email: existingUser.email, isAnonymous: true },
          config.jwtSecret,
          { expiresIn: '30d' }
        );
        return res.json({ token, userId, message: 'Anonymous session renewed' });
      }
    }

    const deviceId = 'device_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    userId = `anon_${deviceId}`;

    await userService.create({
      userId,
      email: `${userId}@anonymous.boi`,
      password: await bcrypt.hash(deviceId, 12)
    });

    const token = jwt.sign(
      { userId, email: `${userId}@anonymous.boi`, isAnonymous: true },
      config.jwtSecret,
      { expiresIn: '30d' }
    );

    res.json({
      token,
      userId,
      message: 'Anonymous session created'
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
