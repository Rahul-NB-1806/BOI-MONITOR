#!/usr/bin/env node
require('dotenv').config();

const admin = require('firebase-admin');
const mongoose = require('mongoose');

const DRY_RUN = process.env.DRY_RUN === 'true';
const BATCH_SIZE = parseInt(process.env.BATCH_SIZE, 10) || 100;

// ── Mongoose Schemas ──────────────────────────────────────────────────────────

const upiTransactionSchema = new mongoose.Schema({
  userId: { type: String, required: true, index: true },
  amount: { type: Number, required: true },
  transactionType: { type: String, required: true, enum: ['CREDIT', 'DEBIT'] },
  accountSuffix: { type: String },
  referenceNumber: { type: String },
  debitedAccount: { type: String },
  transactionDate: { type: String },
  voiceAnnounced: { type: Boolean, default: false },
  timestamp: { type: Date, default: Date.now }
});

const chequeTransactionSchema = new mongoose.Schema({
  userId: { type: String, required: true, index: true },
  chequeNumber: { type: String, required: true },
  amount: { type: Number },
  status: { type: String, enum: ['PRESENTED', 'CLEARED', 'RETURNED'] },
  availableBalance: { type: Number },
  transactionDate: { type: String },
  favouringParty: { type: String },
  timestamp: { type: Date, default: Date.now }
});

const notificationLogSchema = new mongoose.Schema({
  userId: { type: String, required: true, index: true },
  notificationType: { type: String },
  processed: { type: Boolean, default: false },
  processingError: { type: String },
  rawTextStored: { type: String },
  packageName: { type: String },
  timestamp: { type: Date, default: Date.now }
});

const adminUserSchema = new mongoose.Schema({
  userId: { type: String, required: true, unique: true },
  email: { type: String, required: true },
  password: { type: String, required: true },
  createdAt: { type: Date, default: Date.now }
});

const UpiTransaction = mongoose.model('UpiTransaction', upiTransactionSchema);
const ChequeTransaction = mongoose.model('ChequeTransaction', chequeTransactionSchema);
const NotificationLog = mongoose.model('NotificationLog', notificationLogSchema);
const AdminUser = mongoose.model('AdminUser', adminUserSchema);

// ── Helpers ───────────────────────────────────────────────────────────────────

function firestoreTimestampToDate(ts) {
  if (!ts) return new Date();
  if (ts.toDate) return ts.toDate();
  if (ts._seconds) return new Date(ts._seconds * 1000 + (ts._nanoseconds || 0) / 1e6);
  if (ts instanceof Date) return ts;
  if (typeof ts === 'number') return new Date(ts);
  return new Date(ts);
}

function convertTimestamps(doc) {
  const converted = { ...doc };
  for (const [key, value] of Object.entries(converted)) {
    if (value && typeof value === 'object' && (value.toDate || value._seconds !== undefined)) {
      converted[key] = firestoreTimestampToDate(value);
    }
  }
  return converted;
}

function stripUndefined(doc) {
  const clean = {};
  for (const [key, value] of Object.entries(doc)) {
    if (value !== undefined) clean[key] = value;
  }
  return clean;
}

async function insertBatch(Model, docs, label) {
  if (docs.length === 0) return { inserted: 0, errors: 0 };

  let inserted = 0;
  let errors = 0;

  for (let i = 0; i < docs.length; i += BATCH_SIZE) {
    const batch = docs.slice(i, i + BATCH_SIZE);
    if (DRY_RUN) {
      inserted += batch.length;
      continue;
    }
    try {
      const result = await Model.insertMany(batch, { ordered: false });
      inserted += result.length;
    } catch (err) {
      if (err.writeErrors) {
        inserted += err.writeErrors.length;
        errors += batch.length - err.writeErrors.length;
        const failedIds = err.writeErrors.map((e) => e.op?._id || 'unknown');
        console.error(`  [${label}] ${batch.length - err.writeErrors.length} inserted, ${err.writeErrors.length} failed. Failed IDs: ${failedIds.slice(0, 5).join(', ')}${failedIds.length > 5 ? '...' : ''}`);
      } else {
        errors += batch.length;
        console.error(`  [${label}] Batch error at index ${i}:`, err.message);
      }
    }
  }

  return { inserted, errors };
}

// ── Migration Functions ───────────────────────────────────────────────────────

async function migrateUpiTransactions(db) {
  console.log('\n--- UPI Transactions ---');
  const usersSnap = await db.collection('users').get();
  if (usersSnap.empty) {
    console.log('No users found. Skipping UPI transactions.');
    return { inserted: 0, errors: 0, skipped: true };
  }

  const allDocs = [];

  for (const userDoc of usersSnap.docs) {
    const uid = userDoc.id;
    const txnsSnap = await db.collection(`users/${uid}/upi_transactions`).get();
    if (txnsSnap.empty) continue;

    for (const txnDoc of txnsSnap.docs) {
      const data = convertTimestamps(txnDoc.data());
      data.userId = uid;
      allDocs.push(stripUndefined(data));
    }
  }

  console.log(`Found ${allDocs.length} UPI transaction documents across ${usersSnap.size} users`);
  if (allDocs.length === 0) return { inserted: 0, errors: 0, skipped: true };

  const result = await insertBatch(UpiTransaction, allDocs, 'upi');
  console.log(`${DRY_RUN ? '[DRY RUN] Would insert' : 'Inserted'} ${result.inserted} UPI transactions${result.errors ? `, ${result.errors} errors` : ''}`);
  return result;
}

async function migrateChequeTransactions(db) {
  console.log('\n--- Cheque Transactions ---');
  const usersSnap = await db.collection('users').get();
  if (usersSnap.empty) {
    console.log('No users found. Skipping cheque transactions.');
    return { inserted: 0, errors: 0, skipped: true };
  }

  const allDocs = [];

  for (const userDoc of usersSnap.docs) {
    const uid = userDoc.id;
    const txnsSnap = await db.collection(`users/${uid}/cheque_transactions`).get();
    if (txnsSnap.empty) continue;

    for (const txnDoc of txnsSnap.docs) {
      const data = convertTimestamps(txnDoc.data());
      data.userId = uid;
      allDocs.push(stripUndefined(data));
    }
  }

  console.log(`Found ${allDocs.length} cheque transaction documents across ${usersSnap.size} users`);
  if (allDocs.length === 0) return { inserted: 0, errors: 0, skipped: true };

  const result = await insertBatch(ChequeTransaction, allDocs, 'cheque');
  console.log(`${DRY_RUN ? '[DRY RUN] Would insert' : 'Inserted'} ${result.inserted} cheque transactions${result.errors ? `, ${result.errors} errors` : ''}`);
  return result;
}

async function migrateNotificationLogs(db) {
  console.log('\n--- Notification Logs ---');
  const usersSnap = await db.collection('users').get();
  if (usersSnap.empty) {
    console.log('No users found. Skipping notification logs.');
    return { inserted: 0, errors: 0, skipped: true };
  }

  const allDocs = [];

  for (const userDoc of usersSnap.docs) {
    const uid = userDoc.id;
    const logsSnap = await db.collection(`users/${uid}/notification_logs`).get();
    if (logsSnap.empty) continue;

    for (const logDoc of logsSnap.docs) {
      const data = convertTimestamps(logDoc.data());
      data.userId = uid;
      allDocs.push(stripUndefined(data));
    }
  }

  console.log(`Found ${allDocs.length} notification log documents across ${usersSnap.size} users`);
  if (allDocs.length === 0) return { inserted: 0, errors: 0, skipped: true };

  const result = await insertBatch(NotificationLog, allDocs, 'notif');
  console.log(`${DRY_RUN ? '[DRY RUN] Would insert' : 'Inserted'} ${result.inserted} notification logs${result.errors ? `, ${result.errors} errors` : ''}`);
  return result;
}

async function migrateAdminUsers(db) {
  console.log('\n--- Admin Users ---');
  const adminsSnap = await db.collection('admin_users').get();
  if (adminsSnap.empty) {
    console.log('No admin users found. Skipping.');
    return { inserted: 0, errors: 0, skipped: true };
  }

  const allDocs = [];
  for (const adminDoc of adminsSnap.docs) {
    const data = convertTimestamps(adminDoc.data());
    data.userId = adminDoc.id;
    allDocs.push(stripUndefined(data));
  }

  console.log(`Found ${allDocs.length} admin user documents`);
  if (allDocs.length === 0) return { inserted: 0, errors: 0, skipped: true };

  const result = await insertBatch(AdminUser, allDocs, 'admin');
  console.log(`${DRY_RUN ? '[DRY RUN] Would insert' : 'Inserted'} ${result.inserted} admin users${result.errors ? `, ${result.errors} errors` : ''}`);
  return result;
}

// ── Main ──────────────────────────────────────────────────────────────────────

async function main() {
  console.log('=== Firestore → MongoDB Migration ===');
  console.log(`Mode: ${DRY_RUN ? 'DRY RUN (no writes)' : 'LIVE'}`);
  console.log(`Batch size: ${BATCH_SIZE}\n`);

  // Validate env
  if (!process.env.MONGODB_URI) {
    console.error('ERROR: MONGODB_URI is not set. Copy .env.example to .env and fill in your credentials.');
    process.exit(1);
  }
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    console.error('ERROR: GOOGLE_APPLICATION_CREDENTIALS is not set. Copy .env.example to .env and fill in your credentials.');
    process.exit(1);
  }

  // Initialize Firebase Admin
  const app = admin.initializeApp({
    credential: admin.credential.applicationDefault(),
  });
  const db = admin.firestore(app);

  // Connect to MongoDB
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to MongoDB Atlas');

  const results = {
    upi: { inserted: 0, errors: 0 },
    cheque: { inserted: 0, errors: 0 },
    notification: { inserted: 0, errors: 0 },
    admin: { inserted: 0, errors: 0 },
  };

  try {
    results.upi = await migrateUpiTransactions(db);
    results.cheque = await migrateChequeTransactions(db);
    results.notification = await migrateNotificationLogs(db);
    results.admin = await migrateAdminUsers(db);
  } catch (err) {
    console.error('\nFatal error during migration:', err.message);
  } finally {
    await mongoose.disconnect();
    await app.delete();
    console.log('\nConnections closed.');
  }

  // Summary
  console.log('\n=== Migration Summary ===');
  const collections = [
    ['UPI Transactions', results.upi],
    ['Cheque Transactions', results.cheque],
    ['Notification Logs', results.notification],
    ['Admin Users', results.admin],
  ];

  let totalInserted = 0;
  let totalErrors = 0;
  for (const [name, r] of collections) {
    const skipped = r.skipped ? ' (skipped)' : '';
    console.log(`  ${name}: ${r.inserted} migrated, ${r.errors} errors${skipped}`);
    totalInserted += r.inserted;
    totalErrors += r.errors;
  }
  console.log(`\n  Total: ${totalInserted} migrated, ${totalErrors} errors`);
  console.log(DRY_RUN ? '\n[DRY RUN] No data was written. Run without DRY_RUN=true to perform the actual migration.' : '\nMigration complete.');
}

main().catch((err) => {
  console.error('Unhandled error:', err);
  process.exit(1);
});
