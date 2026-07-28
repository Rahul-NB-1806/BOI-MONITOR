const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const config = require('./config');
const errorHandler = require('./middleware/errorHandler');

const authRoutes = require('./routes/auth');
const upiRoutes = require('./routes/upi');
const chequeRoutes = require('./routes/cheques');
const logRoutes = require('./routes/logs');
const adminRoutes = require('./routes/admin');
const userRoutes = require('./routes/user');
const storageRoutes = require('./routes/storage');

const app = express();

app.use(helmet());
app.use(cors());
app.use(morgan('combined'));
app.use(express.json({ limit: '10mb' }));

app.get('/', (req, res) => {
  res.json({
    service: 'BOI Monitor API',
    version: '1.0.0',
    status: 'running',
    endpoints: {
      health: '/health',
      auth: '/api/auth/*',
      upi: '/api/upi',
      cheques: '/api/cheques',
      logs: '/api/logs',
      storage: '/api/storage/stats',
      admin: '/api/admin/*'
    },
    timestamp: new Date().toISOString()
  });
});

app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.use('/api/auth', authRoutes);
app.use('/api/upi', upiRoutes);
app.use('/api/cheques', chequeRoutes);
app.use('/api/logs', logRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/user', userRoutes);
app.use('/api/storage', storageRoutes);

app.use((req, res) => {
  res.status(404).json({ error: 'Route not found' });
});

app.use(errorHandler);

const connectWithRetry = async (retries = 5, delay = 5000) => {
  for (let i = 0; i < retries; i++) {
    try {
      await mongoose.connect(config.mongoUri, {
        serverSelectionTimeoutMS: 5000,
      });
      console.log('Connected to MongoDB Atlas');
      return;
    } catch (error) {
      console.error(`MongoDB connection attempt ${i + 1}/${retries} failed:`, error.message);
      if (i < retries - 1) {
        console.log(`Retrying in ${delay / 1000}s...`);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
  }
  console.error('All MongoDB connection attempts failed. Starting server without DB...');
};

mongoose.connection.on('disconnected', () => {
  console.warn('MongoDB disconnected. Will auto-reconnect...');
});

mongoose.connection.on('reconnected', () => {
  console.log('MongoDB reconnected');
});

mongoose.connection.on('error', (err) => {
  console.error('MongoDB connection error:', err.message);
});

const startServer = async () => {
  await connectWithRetry();

  app.listen(config.port, () => {
    console.log(`BOI Monitor server running on port ${config.port}`);
  });
};

startServer();

module.exports = app;
