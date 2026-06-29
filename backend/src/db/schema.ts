import { pgTable, text, timestamp, jsonb, uuid, unique, index } from "drizzle-orm/pg-core";

// Firebase user. One user can have many devices.
export const users = pgTable("users", {
  firebaseUid: text("firebase_uid").primaryKey(),
  email: text("email"),
  displayName: text("display_name"),
  photoUrl: text("photo_url"),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow().notNull(),
  updatedAt: timestamp("updated_at", { withTimezone: true }).defaultNow().notNull(),
});

// One row per user's device. The full snapshot is stored in JSONB.
export const devices = pgTable(
  "devices",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    userUid: text("user_uid")
      .notNull()
      .references(() => users.firebaseUid, { onDelete: "cascade" }),
    androidId: text("android_id").notNull(),
    // Denormalized key fields for fast querying/browsing
    deviceName: text("device_name"),
    manufacturer: text("manufacturer"),
    model: text("model"),
    osVersion: text("os_version"),
    appVersion: text("app_version"),
    // Full diagnostics snapshot
    snapshot: jsonb("snapshot"),
    snapshotHash: text("snapshot_hash"),
    lastSyncedAt: timestamp("last_synced_at", { withTimezone: true }).defaultNow().notNull(),
    createdAt: timestamp("created_at", { withTimezone: true }).defaultNow().notNull(),
  },
  (t) => ({
    // One device (android_id) per user
    uqUserAndroid: unique("uq_user_android").on(t.userUid, t.androidId),
    userIdx: index("idx_devices_user").on(t.userUid),
  }),
);

// One row per user subscription entitlement. The app reads this from Neon, while
// writes should come from a trusted internal flow such as purchase verification.
export const userSubscriptions = pgTable(
  "user_subscriptions",
  {
    userUid: text("user_uid")
      .primaryKey()
      .references(() => users.firebaseUid, { onDelete: "cascade" }),
    status: text("status").notNull().default("inactive"),
    provider: text("provider").notNull().default("manual"),
    productId: text("product_id"),
    originalTransactionId: text("original_transaction_id"),
    latestTransactionId: text("latest_transaction_id"),
    latestPurchaseToken: text("latest_purchase_token"),
    expiresAt: timestamp("expires_at", { withTimezone: true }),
    createdAt: timestamp("created_at", { withTimezone: true }).defaultNow().notNull(),
    updatedAt: timestamp("updated_at", { withTimezone: true }).defaultNow().notNull(),
  },
  (t) => ({
    providerIdx: index("idx_user_subscriptions_provider").on(t.provider),
    expiresIdx: index("idx_user_subscriptions_expires_at").on(t.expiresAt),
  }),
);

export type UserRow = typeof users.$inferSelect;
export type DeviceRow = typeof devices.$inferSelect;
export type UserSubscriptionRow = typeof userSubscriptions.$inferSelect;
