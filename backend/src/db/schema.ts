import { sqliteTable, text, integer, real, index, uniqueIndex } from "drizzle-orm/sqlite-core";

// ─────────────────────────────────────────────────────────────────────────────
// Cloudflare D1 (SQLite) schema.
//
// Timestamps are stored as INTEGER epoch-milliseconds (drizzle `timestamp_ms`),
// booleans as INTEGER 0/1. The full device diagnostics snapshot is fully
// normalized: every section is its own table and every list its own child table,
// all joined to `devices.id` by foreign key with ON DELETE CASCADE. D1 enforces
// foreign keys, so deleting a user cascades to its devices and then to every
// snapshot row.
// ─────────────────────────────────────────────────────────────────────────────

const createdAt = () =>
  integer("created_at", { mode: "timestamp_ms" }).$defaultFn(() => new Date()).notNull();
const updatedAt = () =>
  integer("updated_at", { mode: "timestamp_ms" }).$defaultFn(() => new Date()).notNull();

// ── Account & subscription ───────────────────────────────────────────────────

// Firebase user. One user can have many devices.
export const users = sqliteTable("users", {
  firebaseUid: text("firebase_uid").primaryKey(),
  email: text("email"),
  displayName: text("display_name"),
  photoUrl: text("photo_url"),
  createdAt: createdAt(),
  updatedAt: updatedAt(),
});

// One row per user subscription entitlement. Writes come from a trusted internal
// flow such as purchase verification.
export const userSubscriptions = sqliteTable(
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
    expiresAt: integer("expires_at", { mode: "timestamp_ms" }),
    createdAt: createdAt(),
    updatedAt: updatedAt(),
  },
  (t) => ({
    providerIdx: index("idx_user_subscriptions_provider").on(t.provider),
    expiresIdx: index("idx_user_subscriptions_expires_at").on(t.expiresAt),
  }),
);

// ── Device ───────────────────────────────────────────────────────────────────

// One row per user's device. The full snapshot lives in the normalized tables
// below, each keyed by this row's `id`.
export const devices = sqliteTable(
  "devices",
  {
    id: text("id").primaryKey().$defaultFn(() => crypto.randomUUID()),
    userUid: text("user_uid")
      .notNull()
      .references(() => users.firebaseUid, { onDelete: "cascade" }),
    androidId: text("android_id").notNull(),
    // Denormalized key fields for fast browsing without joining every section.
    deviceName: text("device_name"),
    manufacturer: text("manufacturer"),
    model: text("model"),
    osVersion: text("os_version"),
    appVersion: text("app_version"),
    snapshotHash: text("snapshot_hash"),
    lastSyncedAt: integer("last_synced_at", { mode: "timestamp_ms" })
      .$defaultFn(() => new Date())
      .notNull(),
    createdAt: createdAt(),
  },
  (t) => ({
    uqUserAndroid: uniqueIndex("uq_user_android").on(t.userUid, t.androidId),
    userIdx: index("idx_devices_user").on(t.userUid),
  }),
);

// Shared FK column → devices.id, cascading on delete.
const deviceIdFk = () =>
  text("device_id")
    .notNull()
    .references(() => devices.id, { onDelete: "cascade" });

// ── DeviceInfo (1:1) ─────────────────────────────────────────────────────────
export const deviceInfo = sqliteTable("device_info", {
  deviceId: deviceIdFk().primaryKey(),
  name: text("name").notNull(),
  model: text("model").notNull(),
  manufacturer: text("manufacturer").notNull(),
  brand: text("brand").notNull(),
  board: text("board").notNull(),
  hardware: text("hardware").notNull(),
  codename: text("codename").notNull(),
  buildFingerprint: text("build_fingerprint").notNull(),
  androidId: text("android_id").notNull(),
  isRooted: integer("is_rooted", { mode: "boolean" }).notNull(),
  bootloader: text("bootloader").notNull(),
  socName: text("soc_name").notNull().default(""),
  serialNumber: text("serial_number").notNull().default(""),
  isEmulator: integer("is_emulator", { mode: "boolean" }).notNull().default(false),
  isDeveloperOptionsEnabled: integer("is_developer_options_enabled", { mode: "boolean" })
    .notNull()
    .default(false),
  isAdbEnabled: integer("is_adb_enabled", { mode: "boolean" }).notNull().default(false),
  buildTags: text("build_tags").notNull().default(""),
  buildUser: text("build_user").notNull().default(""),
  buildHost: text("build_host").notNull().default(""),
  buildTimeMillis: integer("build_time_millis").notNull().default(0),
  isTestKeysBuild: integer("is_test_keys_build", { mode: "boolean" }).notNull().default(false),
  isDebuggableBuild: integer("is_debuggable_build", { mode: "boolean" }).notNull().default(false),
  verifiedBootState: text("verified_boot_state").notNull().default(""),
  bootVerifiedState: text("boot_verified_state").notNull().default(""),
  vbMetaDeviceState: text("vb_meta_device_state").notNull().default(""),
  flashLocked: text("flash_locked").notNull().default(""),
  verityMode: text("verity_mode").notNull().default(""),
  warrantyBit: text("warranty_bit").notNull().default(""),
  firstApiLevel: integer("first_api_level"),
});

export const deviceSupportedAbis = sqliteTable(
  "device_supported_abis",
  { id: integer("id").primaryKey({ autoIncrement: true }), deviceId: deviceIdFk(), value: text("value").notNull() },
  (t) => ({ deviceIdx: index("idx_device_supported_abis_device").on(t.deviceId) }),
);

export const deviceSuspiciousRootPaths = sqliteTable(
  "device_suspicious_root_paths",
  { id: integer("id").primaryKey({ autoIncrement: true }), deviceId: deviceIdFk(), value: text("value").notNull() },
  (t) => ({ deviceIdx: index("idx_device_suspicious_root_paths_device").on(t.deviceId) }),
);

// ── CpuInfo (1:1) + children ─────────────────────────────────────────────────
export const cpuInfo = sqliteTable("cpu_info", {
  deviceId: deviceIdFk().primaryKey(),
  chipsetName: text("chipset_name").notNull(),
  architecture: text("architecture").notNull(),
  coreCount: integer("core_count").notNull(),
  governor: text("governor").notNull(),
  temperatureCelsius: real("temperature_celsius"),
  usagePercent: real("usage_percent"),
  processCount: integer("process_count"),
  minFreqMhz: integer("min_freq_mhz").notNull().default(0),
  maxFreqMhz: integer("max_freq_mhz").notNull().default(0),
  gpuRenderer: text("gpu_renderer").notNull(),
  gpuVendor: text("gpu_vendor").notNull(),
  gpuVersion: text("gpu_version").notNull(),
});

export const cpuCores = sqliteTable(
  "cpu_cores",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    deviceId: deviceIdFk(),
    coreIndex: integer("core_index").notNull(),
    currentFrequencyKhz: integer("current_frequency_khz"),
    minFrequencyKhz: integer("min_frequency_khz").notNull(),
    maxFrequencyKhz: integer("max_frequency_khz").notNull(),
    isOnline: integer("is_online", { mode: "boolean" }).notNull(),
  },
  (t) => ({ deviceIdx: index("idx_cpu_cores_device").on(t.deviceId) }),
);

export const cpuClusters = sqliteTable(
  "cpu_clusters",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    deviceId: deviceIdFk(),
    name: text("name").notNull(),
    minFrequencyMhz: integer("min_frequency_mhz").notNull(),
    maxFrequencyMhz: integer("max_frequency_mhz").notNull(),
  },
  (t) => ({ deviceIdx: index("idx_cpu_clusters_device").on(t.deviceId) }),
);

export const cpuClusterCoreIndices = sqliteTable(
  "cpu_cluster_core_indices",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    clusterId: integer("cluster_id")
      .notNull()
      .references(() => cpuClusters.id, { onDelete: "cascade" }),
    coreIndex: integer("core_index").notNull(),
  },
  (t) => ({ clusterIdx: index("idx_cpu_cluster_core_indices_cluster").on(t.clusterId) }),
);

export const cpuInstructionSets = sqliteTable(
  "cpu_instruction_sets",
  { id: integer("id").primaryKey({ autoIncrement: true }), deviceId: deviceIdFk(), value: text("value").notNull() },
  (t) => ({ deviceIdx: index("idx_cpu_instruction_sets_device").on(t.deviceId) }),
);

// ── SystemInfo (1:1) ─────────────────────────────────────────────────────────
export const systemInfo = sqliteTable("system_info", {
  deviceId: deviceIdFk().primaryKey(),
  androidVersion: text("android_version").notNull(),
  apiLevel: integer("api_level").notNull(),
  securityPatchLevel: text("security_patch_level").notNull(),
  buildNumber: text("build_number").notNull(),
  kernelVersion: text("kernel_version").notNull(),
  javaVm: text("java_vm").notNull(),
  openGlVersion: text("open_gl_version").notNull(),
  baseband: text("baseband").notNull(),
  bootloader: text("bootloader").notNull(),
  language: text("language").notNull(),
  timeZone: text("time_zone").notNull(),
  releaseName: text("release_name").notNull(),
  uptimeMillis: integer("uptime_millis").notNull().default(0),
  buildType: text("build_type").notNull().default(""),
  runningProcessCount: integer("running_process_count").notNull().default(0),
  seLinuxStatus: text("se_linux_status").notNull().default(""),
  isEncrypted: integer("is_encrypted", { mode: "boolean" }).notNull().default(true),
  glEsVersion: text("gl_es_version").notNull().default(""),
  totalRamGb: real("total_ram_gb").notNull().default(0),
  isAppDebuggable: integer("is_app_debuggable", { mode: "boolean" }).notNull().default(false),
  installerPackageName: text("installer_package_name"),
  signingCertificateSha256: text("signing_certificate_sha256"),
  appVersionName: text("app_version_name").notNull().default(""),
  appVersionCode: integer("app_version_code").notNull().default(0),
  packageName: text("package_name").notNull().default(""),
  isInstalledFromKnownStore: integer("is_installed_from_known_store", { mode: "boolean" })
    .notNull()
    .default(false),
  isPowerSaveMode: integer("is_power_save_mode", { mode: "boolean" }).notNull().default(false),
});

// ── BatteryInfo (1:1) ────────────────────────────────────────────────────────
export const batteryInfo = sqliteTable("battery_info", {
  deviceId: deviceIdFk().primaryKey(),
  levelPercent: integer("level_percent").notNull(),
  status: text("status").notNull(),
  health: text("health").notNull(),
  source: text("source").notNull(),
  technology: text("technology").notNull(),
  temperatureCelsius: real("temperature_celsius").notNull(),
  voltageMv: integer("voltage_mv").notNull(),
  currentMa: integer("current_ma"),
  capacityMah: integer("capacity_mah"),
  chargeCycles: integer("charge_cycles"),
  isPresent: integer("is_present", { mode: "boolean" }).notNull(),
  estimatedWatts: real("estimated_watts"),
  chargeTimeRemainingMs: integer("charge_time_remaining_ms"),
  isPowerSaveMode: integer("is_power_save_mode", { mode: "boolean" }).notNull().default(false),
});

// ── RamInfo / StorageInfo (1:1) ──────────────────────────────────────────────
export const ramInfo = sqliteTable("ram_info", {
  deviceId: deviceIdFk().primaryKey(),
  totalBytes: integer("total_bytes").notNull(),
  availableBytes: integer("available_bytes").notNull(),
  usedBytes: integer("used_bytes").notNull(),
  usedPercent: real("used_percent").notNull(),
  isLowMemory: integer("is_low_memory", { mode: "boolean" }).notNull(),
  cachedBytes: integer("cached_bytes").notNull().default(0),
  thresholdBytes: integer("threshold_bytes").notNull().default(0),
});

export const storageInfo = sqliteTable("storage_info", {
  deviceId: deviceIdFk().primaryKey(),
  totalBytes: integer("total_bytes").notNull(),
  usedBytes: integer("used_bytes").notNull(),
  freeBytes: integer("free_bytes").notNull(),
  usedPercent: real("used_percent").notNull(),
  externalTotalBytes: integer("external_total_bytes").notNull().default(0),
  externalFreeBytes: integer("external_free_bytes").notNull().default(0),
});

// ── NetworkInfo (1:1) + children ─────────────────────────────────────────────
export const networkInfo = sqliteTable("network_info", {
  deviceId: deviceIdFk().primaryKey(),
  connectionType: text("connection_type").notNull(),
  ssid: text("ssid"),
  localIpv4: text("local_ipv4"),
  localIpv6: text("local_ipv6"),
  gateway: text("gateway"),
  subnetMask: text("subnet_mask"),
  interfaceName: text("interface_name"),
  linkSpeedMbps: integer("link_speed_mbps"),
  frequencyMhz: integer("frequency_mhz"),
  channel: integer("channel"),
  wifiStandard: text("wifi_standard"),
  securityType: text("security_type"),
  signalStrength: integer("signal_strength"),
  macAddress: text("mac_address"),
  rxBytesPerSec: integer("rx_bytes_per_sec"),
  txBytesPerSec: integer("tx_bytes_per_sec"),
  isMetered: integer("is_metered", { mode: "boolean" }).notNull().default(false),
  cellularOperator: text("cellular_operator"),
  cellularGeneration: text("cellular_generation"),
  isVpnActive: integer("is_vpn_active", { mode: "boolean" }).notNull().default(false),
  isValidatedInternet: integer("is_validated_internet", { mode: "boolean" }).notNull().default(false),
  isCaptivePortal: integer("is_captive_portal", { mode: "boolean" }).notNull().default(false),
  privateDnsServerName: text("private_dns_server_name"),
  httpProxyHost: text("http_proxy_host"),
  httpProxyPort: integer("http_proxy_port"),
});

export const networkDns = sqliteTable(
  "network_dns",
  { id: integer("id").primaryKey({ autoIncrement: true }), deviceId: deviceIdFk(), value: text("value").notNull() },
  (t) => ({ deviceIdx: index("idx_network_dns_device").on(t.deviceId) }),
);

export const networkActiveTransports = sqliteTable(
  "network_active_transports",
  { id: integer("id").primaryKey({ autoIncrement: true }), deviceId: deviceIdFk(), value: text("value").notNull() },
  (t) => ({ deviceIdx: index("idx_network_active_transports_device").on(t.deviceId) }),
);

// ── ConnectivityInfo (1:1) + children ────────────────────────────────────────
export const connectivityInfo = sqliteTable("connectivity_info", {
  deviceId: deviceIdFk().primaryKey(),
  hasWifi: integer("has_wifi", { mode: "boolean" }).notNull(),
  hasWifi5Ghz: integer("has_wifi_5ghz", { mode: "boolean" }).notNull(),
  hasWifi6Ghz: integer("has_wifi_6ghz", { mode: "boolean" }).notNull(),
  hasWifiDirect: integer("has_wifi_direct", { mode: "boolean" }).notNull(),
  hasBluetooth: integer("has_bluetooth", { mode: "boolean" }).notNull(),
  hasBluetoothLe: integer("has_bluetooth_le", { mode: "boolean" }).notNull(),
  hasNfc: integer("has_nfc", { mode: "boolean" }).notNull(),
  hasUwb: integer("has_uwb", { mode: "boolean" }).notNull(),
  hasEsim: integer("has_esim", { mode: "boolean" }).notNull(),
  bluetoothVersion: text("bluetooth_version"),
});

export const connectivityWifiStandards = sqliteTable(
  "connectivity_wifi_standards",
  { id: integer("id").primaryKey({ autoIncrement: true }), deviceId: deviceIdFk(), value: text("value").notNull() },
  (t) => ({ deviceIdx: index("idx_connectivity_wifi_standards_device").on(t.deviceId) }),
);

// ── DisplayInfo (1:1) + children ─────────────────────────────────────────────
export const displayInfo = sqliteTable("display_info", {
  deviceId: deviceIdFk().primaryKey(),
  widthPx: integer("width_px").notNull(),
  heightPx: integer("height_px").notNull(),
  densityDpi: integer("density_dpi").notNull(),
  densityBucket: text("density_bucket").notNull(),
  fontScale: real("font_scale").notNull(),
  physicalSizeInches: real("physical_size_inches").notNull(),
  refreshRateHz: real("refresh_rate_hz").notNull(),
  isHdr: integer("is_hdr", { mode: "boolean" }).notNull(),
  isWideColorGamut: integer("is_wide_color_gamut", { mode: "boolean" }).notNull(),
  brightnessLevel: real("brightness_level").notNull(),
  isAdaptiveBrightness: integer("is_adaptive_brightness", { mode: "boolean" }).notNull(),
  orientation: text("orientation").notNull(),
  displayType: text("display_type").notNull(),
});

export const displaySupportedRefreshRates = sqliteTable(
  "display_supported_refresh_rates",
  { id: integer("id").primaryKey({ autoIncrement: true }), deviceId: deviceIdFk(), value: real("value").notNull() },
  (t) => ({ deviceIdx: index("idx_display_supported_refresh_rates_device").on(t.deviceId) }),
);

export const displayHdrCapabilities = sqliteTable(
  "display_hdr_capabilities",
  { id: integer("id").primaryKey({ autoIncrement: true }), deviceId: deviceIdFk(), value: text("value").notNull() },
  (t) => ({ deviceIdx: index("idx_display_hdr_capabilities_device").on(t.deviceId) }),
);

// ── ThermalInfo (list) ───────────────────────────────────────────────────────
export const thermalZones = sqliteTable(
  "thermal_zones",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    deviceId: deviceIdFk(),
    name: text("name").notNull(),
    type: text("type").notNull(),
    temperatureCelsius: real("temperature_celsius"),
  },
  (t) => ({ deviceIdx: index("idx_thermal_zones_device").on(t.deviceId) }),
);

// ── SensorInfo (list) ────────────────────────────────────────────────────────
export const sensors = sqliteTable(
  "sensors",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    deviceId: deviceIdFk(),
    name: text("name").notNull(),
    vendor: text("vendor").notNull(),
    type: integer("type").notNull(),
    typeName: text("type_name").notNull(),
    version: integer("version").notNull(),
    powerMa: real("power_ma").notNull(),
    resolution: real("resolution").notNull(),
    maxRange: real("max_range").notNull(),
    isWakeUp: integer("is_wake_up", { mode: "boolean" }).notNull(),
    isDynamic: integer("is_dynamic", { mode: "boolean" }).notNull(),
  },
  (t) => ({ deviceIdx: index("idx_sensors_device").on(t.deviceId) }),
);

// ── CameraInfo (list) + children ─────────────────────────────────────────────
export const cameras = sqliteTable(
  "cameras",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    deviceId: deviceIdFk(),
    cameraId: text("camera_id").notNull(),
    facing: text("facing").notNull(),
    megapixels: real("megapixels").notNull(),
    resolutionWidth: integer("resolution_width").notNull(),
    resolutionHeight: integer("resolution_height").notNull(),
    hasFlash: integer("has_flash", { mode: "boolean" }).notNull(),
    hasOis: integer("has_ois", { mode: "boolean" }).notNull(),
    maxVideoWidth: integer("max_video_width").notNull().default(0),
    maxVideoHeight: integer("max_video_height").notNull().default(0),
    maxVideoFps: integer("max_video_fps").notNull().default(0),
    maxSlowMoFps: integer("max_slow_mo_fps").notNull().default(0),
    slowMoWidth: integer("slow_mo_width").notNull().default(0),
    slowMoHeight: integer("slow_mo_height").notNull().default(0),
    maxPhotoWidth: integer("max_photo_width").notNull().default(0),
    maxPhotoHeight: integer("max_photo_height").notNull().default(0),
    minExposureNanos: integer("min_exposure_nanos").notNull().default(0),
    maxExposureNanos: integer("max_exposure_nanos").notNull().default(0),
  },
  (t) => ({ deviceIdx: index("idx_cameras_device").on(t.deviceId) }),
);

export const cameraFocalLengths = sqliteTable(
  "camera_focal_lengths",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    cameraId: integer("camera_row_id")
      .notNull()
      .references(() => cameras.id, { onDelete: "cascade" }),
    value: real("value").notNull(),
  },
  (t) => ({ cameraIdx: index("idx_camera_focal_lengths_camera").on(t.cameraId) }),
);

export const cameraApertures = sqliteTable(
  "camera_apertures",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    cameraId: integer("camera_row_id")
      .notNull()
      .references(() => cameras.id, { onDelete: "cascade" }),
    value: real("value").notNull(),
  },
  (t) => ({ cameraIdx: index("idx_camera_apertures_camera").on(t.cameraId) }),
);

export const cameraSupportedModes = sqliteTable(
  "camera_supported_modes",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    cameraId: integer("camera_row_id")
      .notNull()
      .references(() => cameras.id, { onDelete: "cascade" }),
    value: text("value").notNull(),
  },
  (t) => ({ cameraIdx: index("idx_camera_supported_modes_camera").on(t.cameraId) }),
);

// ── AppListInfo (1:1 counts) + apps (list) ───────────────────────────────────
export const appListInfo = sqliteTable("app_list_info", {
  deviceId: deviceIdFk().primaryKey(),
  totalCount: integer("total_count").notNull(),
  userCount: integer("user_count").notNull(),
  systemCount: integer("system_count").notNull(),
});

export const apps = sqliteTable(
  "apps",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    deviceId: deviceIdFk(),
    name: text("name").notNull(),
    packageName: text("package_name").notNull(),
    versionName: text("version_name").notNull(),
    versionCode: integer("version_code").notNull(),
    isSystemApp: integer("is_system_app", { mode: "boolean" }).notNull(),
    installedAt: integer("installed_at"),
    updatedAt: integer("updated_at"),
    targetSdkVersion: integer("target_sdk_version").notNull(),
  },
  (t) => ({ deviceIdx: index("idx_apps_device").on(t.deviceId) }),
);

// ── HealthScore (1:1) + insights/actions/fraud (lists) ───────────────────────
export const healthScore = sqliteTable("health_score", {
  deviceId: deviceIdFk().primaryKey(),
  overall: integer("overall").notNull(),
  battery: integer("battery").notNull(),
  performance: integer("performance").notNull(),
  storage: integer("storage").notNull(),
  security: integer("security").notNull(),
  thermal: integer("thermal").notNull(),
  fraudScore: integer("fraud_score").notNull().default(0),
  fraudLevel: text("fraud_level").notNull().default("Low"),
});

export const healthInsights = sqliteTable(
  "health_insights",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    deviceId: deviceIdFk(),
    insightId: text("insight_id").notNull(),
    title: text("title").notNull(),
    summary: text("summary").notNull(),
    severity: text("severity").notNull(),
    confidence: real("confidence").notNull(),
  },
  (t) => ({ deviceIdx: index("idx_health_insights_device").on(t.deviceId) }),
);

export const healthInsightActions = sqliteTable(
  "health_insight_actions",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    insightId: integer("insight_row_id")
      .notNull()
      .references(() => healthInsights.id, { onDelete: "cascade" }),
    label: text("label").notNull(),
    description: text("description").notNull(),
  },
  (t) => ({ insightIdx: index("idx_health_insight_actions_insight").on(t.insightId) }),
);

export const fraudSignals = sqliteTable(
  "fraud_signals",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    deviceId: deviceIdFk(),
    signalId: text("signal_id").notNull(),
    label: text("label").notNull(),
    severity: text("severity").notNull(),
    evidence: text("evidence").notNull(),
  },
  (t) => ({ deviceIdx: index("idx_fraud_signals_device").on(t.deviceId) }),
);

export type UserRow = typeof users.$inferSelect;
export type DeviceRow = typeof devices.$inferSelect;
export type UserSubscriptionRow = typeof userSubscriptions.$inferSelect;
