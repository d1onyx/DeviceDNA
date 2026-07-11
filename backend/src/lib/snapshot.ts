import { eq } from "drizzle-orm";
import type { SQLiteTable } from "drizzle-orm/sqlite-core";
import type { Db } from "../db/client";
import {
  appListInfo,
  apps,
  batteryInfo,
  cameraApertures,
  cameraFocalLengths,
  cameraSupportedModes,
  cameras,
  connectivityInfo,
  connectivityWifiStandards,
  cpuClusterCoreIndices,
  cpuClusters,
  cpuCores,
  cpuInfo,
  cpuInstructionSets,
  deviceInfo,
  deviceSupportedAbis,
  deviceSuspiciousRootPaths,
  displayHdrCapabilities,
  displayInfo,
  displaySupportedRefreshRates,
  fraudSignals,
  healthInsightActions,
  healthInsights,
  healthScore,
  networkActiveTransports,
  networkDns,
  networkInfo,
  ramInfo,
  sensors,
  storageInfo,
  systemInfo,
  thermalZones,
} from "../db/schema";

// ─────────────────────────────────────────────────────────────────────────────
// TypeScript mirror of the Kotlin `DeviceSnapshot` (shared/.../domain/model).
// The app posts this JSON to POST /v1/sync; here it is exploded into the
// normalized D1 tables. All fields are optional — a section may be missing.
// ─────────────────────────────────────────────────────────────────────────────

export interface DeviceSnapshot {
  device?: DeviceInfoDto | null;
  cpu?: CpuInfoDto | null;
  system?: SystemInfoDto | null;
  battery?: BatteryInfoDto | null;
  ram?: RamInfoDto | null;
  storage?: StorageInfoDto | null;
  network?: NetworkInfoDto | null;
  connectivity?: ConnectivityInfoDto | null;
  display?: DisplayInfoDto | null;
  camera?: CameraInfoDto | null;
  thermal?: ThermalInfoDto | null;
  sensors?: SensorInfoDto | null;
  apps?: AppListInfoDto | null;
  health?: HealthScoreDto | null;
}

interface DeviceInfoDto {
  name: string;
  model: string;
  manufacturer: string;
  brand: string;
  board: string;
  hardware: string;
  codename: string;
  buildFingerprint: string;
  androidId: string;
  supportedAbis?: string[];
  isRooted: boolean;
  bootloader: string;
  socName?: string;
  serialNumber?: string;
  isEmulator?: boolean;
  isDeveloperOptionsEnabled?: boolean;
  isAdbEnabled?: boolean;
  buildTags?: string;
  buildUser?: string;
  buildHost?: string;
  buildTimeMillis?: number;
  isTestKeysBuild?: boolean;
  isDebuggableBuild?: boolean;
  verifiedBootState?: string;
  bootVerifiedState?: string;
  vbMetaDeviceState?: string;
  flashLocked?: string;
  verityMode?: string;
  warrantyBit?: string;
  firstApiLevel?: number | null;
  suspiciousRootPaths?: string[];
}

interface CpuCoreDto {
  index: number;
  currentFrequencyKhz?: number | null;
  minFrequencyKhz: number;
  maxFrequencyKhz: number;
  isOnline: boolean;
}
interface CpuClusterDto {
  name: string;
  coreIndices?: number[];
  minFrequencyMhz: number;
  maxFrequencyMhz: number;
}
interface GpuInfoDto {
  renderer: string;
  vendor: string;
  version: string;
}
interface CpuInfoDto {
  chipsetName: string;
  architecture: string;
  coreCount: number;
  cores?: CpuCoreDto[];
  clusters?: CpuClusterDto[];
  governor: string;
  gpu: GpuInfoDto;
  temperatureCelsius?: number | null;
  usagePercent?: number | null;
  instructionSets?: string[];
  processCount?: number | null;
  minFreqMhz?: number;
  maxFreqMhz?: number;
}

interface SystemInfoDto {
  androidVersion: string;
  apiLevel: number;
  securityPatchLevel: string;
  buildNumber: string;
  kernelVersion: string;
  javaVm: string;
  openGlVersion: string;
  baseband: string;
  bootloader: string;
  language: string;
  timeZone: string;
  releaseName: string;
  uptimeMillis?: number;
  buildType?: string;
  runningProcessCount?: number;
  seLinuxStatus?: string;
  isEncrypted?: boolean;
  glEsVersion?: string;
  totalRamGb?: number;
  isAppDebuggable?: boolean;
  installerPackageName?: string | null;
  signingCertificateSha256?: string | null;
  appVersionName?: string;
  appVersionCode?: number;
  packageName?: string;
  isInstalledFromKnownStore?: boolean;
  isPowerSaveMode?: boolean;
}

interface BatteryInfoDto {
  levelPercent: number;
  status: string;
  health: string;
  source: string;
  technology: string;
  temperatureCelsius: number;
  voltageMv: number;
  currentMa?: number | null;
  capacityMah?: number | null;
  chargeCycles?: number | null;
  isPresent: boolean;
  estimatedWatts?: number | null;
  chargeTimeRemainingMs?: number | null;
  isPowerSaveMode?: boolean;
}

interface RamInfoDto {
  totalBytes: number;
  availableBytes: number;
  usedBytes: number;
  usedPercent: number;
  isLowMemory: boolean;
  cachedBytes?: number;
  thresholdBytes?: number;
}
interface StorageInfoDto {
  totalBytes: number;
  usedBytes: number;
  freeBytes: number;
  usedPercent: number;
  externalTotalBytes?: number;
  externalFreeBytes?: number;
}

interface NetworkInfoDto {
  connectionType: string;
  ssid?: string | null;
  localIpv4?: string | null;
  localIpv6?: string | null;
  gateway?: string | null;
  dns?: string[];
  subnetMask?: string | null;
  interfaceName?: string | null;
  linkSpeedMbps?: number | null;
  frequencyMhz?: number | null;
  channel?: number | null;
  wifiStandard?: string | null;
  securityType?: string | null;
  signalStrength?: number | null;
  macAddress?: string | null;
  rxBytesPerSec?: number | null;
  txBytesPerSec?: number | null;
  isMetered?: boolean;
  cellularOperator?: string | null;
  cellularGeneration?: string | null;
  isVpnActive?: boolean;
  isValidatedInternet?: boolean;
  isCaptivePortal?: boolean;
  activeTransports?: string[];
  privateDnsServerName?: string | null;
  httpProxyHost?: string | null;
  httpProxyPort?: number | null;
}

interface ConnectivityInfoDto {
  hasWifi: boolean;
  hasWifi5Ghz: boolean;
  hasWifi6Ghz: boolean;
  hasWifiDirect: boolean;
  wifiStandards?: string[];
  hasBluetooth: boolean;
  hasBluetoothLe: boolean;
  hasNfc: boolean;
  hasUwb: boolean;
  hasEsim: boolean;
  bluetoothVersion?: string | null;
}

interface DisplayInfoDto {
  widthPx: number;
  heightPx: number;
  densityDpi: number;
  densityBucket: string;
  fontScale: number;
  physicalSizeInches: number;
  refreshRateHz: number;
  supportedRefreshRates?: number[];
  hdrCapabilities?: string[];
  isHdr: boolean;
  isWideColorGamut: boolean;
  brightnessLevel: number;
  isAdaptiveBrightness: boolean;
  orientation: string;
  displayType: string;
}

interface CameraDetailsDto {
  id: string;
  facing: string;
  megapixels: number;
  resolutionWidth: number;
  resolutionHeight: number;
  focalLengths?: number[];
  hasFlash: boolean;
  hasOis: boolean;
  apertures?: number[];
  supportedModes?: string[];
  maxVideoWidth?: number;
  maxVideoHeight?: number;
  maxVideoFps?: number;
  maxSlowMoFps?: number;
  slowMoWidth?: number;
  slowMoHeight?: number;
  maxPhotoWidth?: number;
  maxPhotoHeight?: number;
  minExposureNanos?: number;
  maxExposureNanos?: number;
}
interface CameraInfoDto {
  cameras?: CameraDetailsDto[];
}

interface ThermalZoneDto {
  name: string;
  type: string;
  temperatureCelsius?: number | null;
}
interface ThermalInfoDto {
  zones?: ThermalZoneDto[];
}

interface SensorDetailsDto {
  name: string;
  vendor: string;
  type: number;
  typeName: string;
  version: number;
  powerMa: number;
  resolution: number;
  maxRange: number;
  isWakeUp: boolean;
  isDynamic: boolean;
}
interface SensorInfoDto {
  sensors?: SensorDetailsDto[];
}

interface AppDetailsDto {
  name: string;
  packageName: string;
  versionName: string;
  versionCode: number;
  isSystemApp: boolean;
  installedAt?: number | null;
  updatedAt?: number | null;
  targetSdkVersion: number;
}
interface AppListInfoDto {
  totalCount: number;
  userCount: number;
  systemCount: number;
  apps?: AppDetailsDto[];
}

interface RecommendedActionDto {
  label: string;
  description: string;
}
interface HealthInsightDto {
  id: string;
  title: string;
  summary: string;
  severity: string;
  confidence: number;
  actions?: RecommendedActionDto[];
}
interface FraudSignalDto {
  id: string;
  label: string;
  severity: string;
  evidence: string;
}
interface FraudRiskScoreDto {
  score?: number;
  level?: string;
  signals?: FraudSignalDto[];
}
interface HealthScoreDto {
  overall: number;
  battery: number;
  performance: number;
  storage: number;
  security: number;
  thermal: number;
  insights?: HealthInsightDto[];
  fraudRisk?: FraudRiskScoreDto;
}

// Every device_id-keyed table, used to wipe a device's previous snapshot before
// re-inserting. Grandchild rows (camera_*/cpu_cluster_core_indices/
// health_insight_actions) are removed by ON DELETE CASCADE from their parents.
const deviceScopedTables = [
  deviceInfo,
  deviceSupportedAbis,
  deviceSuspiciousRootPaths,
  cpuInfo,
  cpuCores,
  cpuClusters,
  cpuInstructionSets,
  systemInfo,
  batteryInfo,
  ramInfo,
  storageInfo,
  networkInfo,
  networkDns,
  networkActiveTransports,
  connectivityInfo,
  connectivityWifiStandards,
  displayInfo,
  displaySupportedRefreshRates,
  displayHdrCapabilities,
  thermalZones,
  sensors,
  cameras,
  appListInfo,
  apps,
  healthScore,
  healthInsights,
  fraudSignals,
] as const;

/**
 * Replaces the stored diagnostics snapshot for `deviceId` with the contents of
 * `snapshot`, exploded across the normalized tables. Old rows are deleted first
 * (atomic batch); the fresh rows are then inserted section by section.
 */
export async function writeSnapshot(db: Db, deviceId: string, snapshot: DeviceSnapshot): Promise<void> {
  const deletes = deviceScopedTables.map((table) => db.delete(table).where(eq(table.deviceId, deviceId)));
  await db.batch(deletes as unknown as [ReturnType<Db["delete"]>, ...ReturnType<Db["delete"]>[]]);

  await insertDeviceInfo(db, deviceId, snapshot.device);
  await insertCpu(db, deviceId, snapshot.cpu);
  await insertSystem(db, deviceId, snapshot.system);
  await insertBattery(db, deviceId, snapshot.battery);
  await insertRam(db, deviceId, snapshot.ram);
  await insertStorage(db, deviceId, snapshot.storage);
  await insertNetwork(db, deviceId, snapshot.network);
  await insertConnectivity(db, deviceId, snapshot.connectivity);
  await insertDisplay(db, deviceId, snapshot.display);
  await insertThermal(db, deviceId, snapshot.thermal);
  await insertSensors(db, deviceId, snapshot.sensors);
  await insertCameras(db, deviceId, snapshot.camera);
  await insertApps(db, deviceId, snapshot.apps);
  await insertHealth(db, deviceId, snapshot.health);
}

async function insertDeviceInfo(db: Db, deviceId: string, d?: DeviceInfoDto | null): Promise<void> {
  if (!d) return;
  await db.insert(deviceInfo).values({
    deviceId,
    name: d.name,
    model: d.model,
    manufacturer: d.manufacturer,
    brand: d.brand,
    board: d.board,
    hardware: d.hardware,
    codename: d.codename,
    buildFingerprint: d.buildFingerprint,
    androidId: d.androidId,
    isRooted: d.isRooted,
    bootloader: d.bootloader,
    socName: d.socName ?? "",
    serialNumber: d.serialNumber ?? "",
    isEmulator: d.isEmulator ?? false,
    isDeveloperOptionsEnabled: d.isDeveloperOptionsEnabled ?? false,
    isAdbEnabled: d.isAdbEnabled ?? false,
    buildTags: d.buildTags ?? "",
    buildUser: d.buildUser ?? "",
    buildHost: d.buildHost ?? "",
    buildTimeMillis: d.buildTimeMillis ?? 0,
    isTestKeysBuild: d.isTestKeysBuild ?? false,
    isDebuggableBuild: d.isDebuggableBuild ?? false,
    verifiedBootState: d.verifiedBootState ?? "",
    bootVerifiedState: d.bootVerifiedState ?? "",
    vbMetaDeviceState: d.vbMetaDeviceState ?? "",
    flashLocked: d.flashLocked ?? "",
    verityMode: d.verityMode ?? "",
    warrantyBit: d.warrantyBit ?? "",
    firstApiLevel: d.firstApiLevel ?? null,
  });
  await insertStringList(db, deviceSupportedAbis, deviceId, d.supportedAbis);
  await insertStringList(db, deviceSuspiciousRootPaths, deviceId, d.suspiciousRootPaths);
}

async function insertCpu(db: Db, deviceId: string, c?: CpuInfoDto | null): Promise<void> {
  if (!c) return;
  await db.insert(cpuInfo).values({
    deviceId,
    chipsetName: c.chipsetName,
    architecture: c.architecture,
    coreCount: c.coreCount,
    governor: c.governor,
    temperatureCelsius: c.temperatureCelsius ?? null,
    usagePercent: c.usagePercent ?? null,
    processCount: c.processCount ?? null,
    minFreqMhz: c.minFreqMhz ?? 0,
    maxFreqMhz: c.maxFreqMhz ?? 0,
    gpuRenderer: c.gpu.renderer,
    gpuVendor: c.gpu.vendor,
    gpuVersion: c.gpu.version,
  });
  await insertRows(
    db,
    cpuCores,
    (c.cores ?? []).map((core) => ({
      deviceId,
      coreIndex: core.index,
      currentFrequencyKhz: core.currentFrequencyKhz ?? null,
      minFrequencyKhz: core.minFrequencyKhz,
      maxFrequencyKhz: core.maxFrequencyKhz,
      isOnline: core.isOnline,
    })),
  );
  for (const cluster of c.clusters ?? []) {
    const [row] = await db
      .insert(cpuClusters)
      .values({
        deviceId,
        name: cluster.name,
        minFrequencyMhz: cluster.minFrequencyMhz,
        maxFrequencyMhz: cluster.maxFrequencyMhz,
      })
      .returning({ id: cpuClusters.id });
    await insertRows(
      db,
      cpuClusterCoreIndices,
      (cluster.coreIndices ?? []).map((coreIndex) => ({ clusterId: row.id, coreIndex })),
    );
  }
  await insertStringList(db, cpuInstructionSets, deviceId, c.instructionSets);
}

async function insertSystem(db: Db, deviceId: string, s?: SystemInfoDto | null): Promise<void> {
  if (!s) return;
  await db.insert(systemInfo).values({
    deviceId,
    androidVersion: s.androidVersion,
    apiLevel: s.apiLevel,
    securityPatchLevel: s.securityPatchLevel,
    buildNumber: s.buildNumber,
    kernelVersion: s.kernelVersion,
    javaVm: s.javaVm,
    openGlVersion: s.openGlVersion,
    baseband: s.baseband,
    bootloader: s.bootloader,
    language: s.language,
    timeZone: s.timeZone,
    releaseName: s.releaseName,
    uptimeMillis: s.uptimeMillis ?? 0,
    buildType: s.buildType ?? "",
    runningProcessCount: s.runningProcessCount ?? 0,
    seLinuxStatus: s.seLinuxStatus ?? "",
    isEncrypted: s.isEncrypted ?? true,
    glEsVersion: s.glEsVersion ?? "",
    totalRamGb: s.totalRamGb ?? 0,
    isAppDebuggable: s.isAppDebuggable ?? false,
    installerPackageName: s.installerPackageName ?? null,
    signingCertificateSha256: s.signingCertificateSha256 ?? null,
    appVersionName: s.appVersionName ?? "",
    appVersionCode: s.appVersionCode ?? 0,
    packageName: s.packageName ?? "",
    isInstalledFromKnownStore: s.isInstalledFromKnownStore ?? false,
    isPowerSaveMode: s.isPowerSaveMode ?? false,
  });
}

async function insertBattery(db: Db, deviceId: string, b?: BatteryInfoDto | null): Promise<void> {
  if (!b) return;
  await db.insert(batteryInfo).values({
    deviceId,
    levelPercent: b.levelPercent,
    status: b.status,
    health: b.health,
    source: b.source,
    technology: b.technology,
    temperatureCelsius: b.temperatureCelsius,
    voltageMv: b.voltageMv,
    currentMa: b.currentMa ?? null,
    capacityMah: b.capacityMah ?? null,
    chargeCycles: b.chargeCycles ?? null,
    isPresent: b.isPresent,
    estimatedWatts: b.estimatedWatts ?? null,
    chargeTimeRemainingMs: b.chargeTimeRemainingMs ?? null,
    isPowerSaveMode: b.isPowerSaveMode ?? false,
  });
}

async function insertRam(db: Db, deviceId: string, r?: RamInfoDto | null): Promise<void> {
  if (!r) return;
  await db.insert(ramInfo).values({
    deviceId,
    totalBytes: r.totalBytes,
    availableBytes: r.availableBytes,
    usedBytes: r.usedBytes,
    usedPercent: r.usedPercent,
    isLowMemory: r.isLowMemory,
    cachedBytes: r.cachedBytes ?? 0,
    thresholdBytes: r.thresholdBytes ?? 0,
  });
}

async function insertStorage(db: Db, deviceId: string, s?: StorageInfoDto | null): Promise<void> {
  if (!s) return;
  await db.insert(storageInfo).values({
    deviceId,
    totalBytes: s.totalBytes,
    usedBytes: s.usedBytes,
    freeBytes: s.freeBytes,
    usedPercent: s.usedPercent,
    externalTotalBytes: s.externalTotalBytes ?? 0,
    externalFreeBytes: s.externalFreeBytes ?? 0,
  });
}

async function insertNetwork(db: Db, deviceId: string, n?: NetworkInfoDto | null): Promise<void> {
  if (!n) return;
  await db.insert(networkInfo).values({
    deviceId,
    connectionType: n.connectionType,
    ssid: n.ssid ?? null,
    localIpv4: n.localIpv4 ?? null,
    localIpv6: n.localIpv6 ?? null,
    gateway: n.gateway ?? null,
    subnetMask: n.subnetMask ?? null,
    interfaceName: n.interfaceName ?? null,
    linkSpeedMbps: n.linkSpeedMbps ?? null,
    frequencyMhz: n.frequencyMhz ?? null,
    channel: n.channel ?? null,
    wifiStandard: n.wifiStandard ?? null,
    securityType: n.securityType ?? null,
    signalStrength: n.signalStrength ?? null,
    macAddress: n.macAddress ?? null,
    rxBytesPerSec: n.rxBytesPerSec ?? null,
    txBytesPerSec: n.txBytesPerSec ?? null,
    isMetered: n.isMetered ?? false,
    cellularOperator: n.cellularOperator ?? null,
    cellularGeneration: n.cellularGeneration ?? null,
    isVpnActive: n.isVpnActive ?? false,
    isValidatedInternet: n.isValidatedInternet ?? false,
    isCaptivePortal: n.isCaptivePortal ?? false,
    privateDnsServerName: n.privateDnsServerName ?? null,
    httpProxyHost: n.httpProxyHost ?? null,
    httpProxyPort: n.httpProxyPort ?? null,
  });
  await insertStringList(db, networkDns, deviceId, n.dns);
  await insertStringList(db, networkActiveTransports, deviceId, n.activeTransports);
}

async function insertConnectivity(db: Db, deviceId: string, c?: ConnectivityInfoDto | null): Promise<void> {
  if (!c) return;
  await db.insert(connectivityInfo).values({
    deviceId,
    hasWifi: c.hasWifi,
    hasWifi5Ghz: c.hasWifi5Ghz,
    hasWifi6Ghz: c.hasWifi6Ghz,
    hasWifiDirect: c.hasWifiDirect,
    hasBluetooth: c.hasBluetooth,
    hasBluetoothLe: c.hasBluetoothLe,
    hasNfc: c.hasNfc,
    hasUwb: c.hasUwb,
    hasEsim: c.hasEsim,
    bluetoothVersion: c.bluetoothVersion ?? null,
  });
  await insertStringList(db, connectivityWifiStandards, deviceId, c.wifiStandards);
}

async function insertDisplay(db: Db, deviceId: string, d?: DisplayInfoDto | null): Promise<void> {
  if (!d) return;
  await db.insert(displayInfo).values({
    deviceId,
    widthPx: d.widthPx,
    heightPx: d.heightPx,
    densityDpi: d.densityDpi,
    densityBucket: d.densityBucket,
    fontScale: d.fontScale,
    physicalSizeInches: d.physicalSizeInches,
    refreshRateHz: d.refreshRateHz,
    isHdr: d.isHdr,
    isWideColorGamut: d.isWideColorGamut,
    brightnessLevel: d.brightnessLevel,
    isAdaptiveBrightness: d.isAdaptiveBrightness,
    orientation: d.orientation,
    displayType: d.displayType,
  });
  await insertRows(
    db,
    displaySupportedRefreshRates,
    (d.supportedRefreshRates ?? []).map((value) => ({ deviceId, value })),
  );
  await insertStringList(db, displayHdrCapabilities, deviceId, d.hdrCapabilities);
}

async function insertThermal(db: Db, deviceId: string, t?: ThermalInfoDto | null): Promise<void> {
  await insertRows(
    db,
    thermalZones,
    (t?.zones ?? []).map((zone) => ({
      deviceId,
      name: zone.name,
      type: zone.type,
      temperatureCelsius: zone.temperatureCelsius ?? null,
    })),
  );
}

async function insertSensors(db: Db, deviceId: string, s?: SensorInfoDto | null): Promise<void> {
  await insertRows(
    db,
    sensors,
    (s?.sensors ?? []).map((sensor) => ({
      deviceId,
      name: sensor.name,
      vendor: sensor.vendor,
      type: sensor.type,
      typeName: sensor.typeName,
      version: sensor.version,
      powerMa: sensor.powerMa,
      resolution: sensor.resolution,
      maxRange: sensor.maxRange,
      isWakeUp: sensor.isWakeUp,
      isDynamic: sensor.isDynamic,
    })),
  );
}

async function insertCameras(db: Db, deviceId: string, c?: CameraInfoDto | null): Promise<void> {
  for (const cam of c?.cameras ?? []) {
    const [row] = await db
      .insert(cameras)
      .values({
        deviceId,
        cameraId: cam.id,
        facing: cam.facing,
        megapixels: cam.megapixels,
        resolutionWidth: cam.resolutionWidth,
        resolutionHeight: cam.resolutionHeight,
        hasFlash: cam.hasFlash,
        hasOis: cam.hasOis,
        maxVideoWidth: cam.maxVideoWidth ?? 0,
        maxVideoHeight: cam.maxVideoHeight ?? 0,
        maxVideoFps: cam.maxVideoFps ?? 0,
        maxSlowMoFps: cam.maxSlowMoFps ?? 0,
        slowMoWidth: cam.slowMoWidth ?? 0,
        slowMoHeight: cam.slowMoHeight ?? 0,
        maxPhotoWidth: cam.maxPhotoWidth ?? 0,
        maxPhotoHeight: cam.maxPhotoHeight ?? 0,
        minExposureNanos: cam.minExposureNanos ?? 0,
        maxExposureNanos: cam.maxExposureNanos ?? 0,
      })
      .returning({ id: cameras.id });
    await insertRows(db, cameraFocalLengths, (cam.focalLengths ?? []).map((value) => ({ cameraId: row.id, value })));
    await insertRows(db, cameraApertures, (cam.apertures ?? []).map((value) => ({ cameraId: row.id, value })));
    await insertRows(db, cameraSupportedModes, (cam.supportedModes ?? []).map((value) => ({ cameraId: row.id, value })));
  }
}

async function insertApps(db: Db, deviceId: string, a?: AppListInfoDto | null): Promise<void> {
  if (!a) return;
  await db.insert(appListInfo).values({
    deviceId,
    totalCount: a.totalCount,
    userCount: a.userCount,
    systemCount: a.systemCount,
  });
  await insertRows(
    db,
    apps,
    (a.apps ?? []).map((app) => ({
      deviceId,
      name: app.name,
      packageName: app.packageName,
      versionName: app.versionName,
      versionCode: app.versionCode,
      isSystemApp: app.isSystemApp,
      installedAt: app.installedAt ?? null,
      updatedAt: app.updatedAt ?? null,
      targetSdkVersion: app.targetSdkVersion,
    })),
  );
}

async function insertHealth(db: Db, deviceId: string, h?: HealthScoreDto | null): Promise<void> {
  if (!h) return;
  await db.insert(healthScore).values({
    deviceId,
    overall: h.overall,
    battery: h.battery,
    performance: h.performance,
    storage: h.storage,
    security: h.security,
    thermal: h.thermal,
    fraudScore: h.fraudRisk?.score ?? 0,
    fraudLevel: h.fraudRisk?.level ?? "Low",
  });
  for (const insight of h.insights ?? []) {
    const [row] = await db
      .insert(healthInsights)
      .values({
        deviceId,
        insightId: insight.id,
        title: insight.title,
        summary: insight.summary,
        severity: insight.severity,
        confidence: insight.confidence,
      })
      .returning({ id: healthInsights.id });
    await insertRows(
      db,
      healthInsightActions,
      (insight.actions ?? []).map((action) => ({ insightId: row.id, label: action.label, description: action.description })),
    );
  }
  await insertRows(
    db,
    fraudSignals,
    (h.fraudRisk?.signals ?? []).map((signal) => ({
      deviceId,
      signalId: signal.id,
      label: signal.label,
      severity: signal.severity,
      evidence: signal.evidence,
    })),
  );
}

// D1 caps bound parameters per statement at 100. A multi-row INSERT binds
// (columns × rows) parameters, so a device with hundreds of apps would blow the
// limit. Chunk every multi-row insert to stay comfortably under it.
const D1_MAX_BOUND_PARAMS = 90;

async function insertRows(db: Db, table: SQLiteTable, rows: Array<Record<string, unknown>>): Promise<void> {
  if (rows.length === 0) return;
  const columnsPerRow = Math.max(1, Object.keys(rows[0]).length);
  const rowsPerChunk = Math.max(1, Math.floor(D1_MAX_BOUND_PARAMS / columnsPerRow));
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const insert = (chunk: Array<Record<string, unknown>>) => db.insert(table).values(chunk as any);
  for (let i = 0; i < rows.length; i += rowsPerChunk) {
    await insert(rows.slice(i, i + rowsPerChunk));
  }
}

// Small helper for the many `List<String>` child tables, which are all
// structurally { id, device_id, value }.
async function insertStringList(
  db: Db,
  table: SQLiteTable,
  deviceId: string,
  values?: string[],
): Promise<void> {
  if (!values?.length) return;
  await insertRows(db, table, values.map((value) => ({ deviceId, value })));
}
