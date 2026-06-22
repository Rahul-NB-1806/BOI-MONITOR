require('dotenv').config();

const config = {
  mongoUri: process.env.MONGODB_URI || 'mongodb://localhost:27017/boi-monitor',
  jwtSecret: process.env.JWT_SECRET || 'default-dev-secret-change-in-production',
  apiKey: process.env.API_KEY || 'dev-api-key-change-this',
  port: parseInt(process.env.PORT, 10) || 3000
};

module.exports = config;
