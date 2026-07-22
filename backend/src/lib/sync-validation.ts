import type { DeviceSnapshot } from "./snapshot";

export interface ValidSyncBody {
  deviceId: string;
  deviceName?: string;
  manufacturer?: string;
  model?: string;
  osVersion?: string;
  appVersion?: string;
  snapshotHash?: string;
  snapshot?: DeviceSnapshot;
}

export type SyncBodyValidation =
  | { ok: true; value: ValidSyncBody }
  | { ok: false; error: string };

const MAX_SHORT_TEXT = 256;
const MAX_FIELD_TEXT = 4_096;
const MAX_TOTAL_VALUES = 100_000;
const MAX_DEPTH = 12;

const collectionLimits: Record<string, number> = {
  "snapshot.device.supportedAbis": 32,
  "snapshot.device.suspiciousRootPaths": 64,
  "snapshot.cpu.cores": 256,
  "snapshot.cpu.clusters": 64,
  "snapshot.cpu.clusters[].coreIndices": 256,
  "snapshot.cpu.instructionSets": 64,
  "snapshot.network.dns": 32,
  "snapshot.network.activeTransports": 16,
  "snapshot.connectivity.wifiStandards": 32,
  "snapshot.display.supportedRefreshRates": 64,
  "snapshot.display.hdrCapabilities": 32,
  "snapshot.camera.cameras": 32,
  "snapshot.camera.cameras[].focalLengths": 64,
  "snapshot.camera.cameras[].apertures": 64,
  "snapshot.camera.cameras[].supportedModes": 128,
  "snapshot.thermal.zones": 256,
  "snapshot.sensors.sensors": 1_024,
  "snapshot.apps.apps": 5_000,
  "snapshot.health.insights": 128,
  "snapshot.health.insights[].actions": 128,
  "snapshot.health.fraudRisk.signals": 128,
};

/** Rejects malformed or pathologically large sync payloads before any existing snapshot is erased. */
export function validateSyncBody(input: unknown): SyncBodyValidation {
  if (!isRecord(input)) return { ok: false, error: "invalid_request_body" };

  const deviceId = shortString(input.deviceId) ?? shortString(input.androidId);
  if (!deviceId || !/^[A-Za-z0-9._:-]+$/.test(deviceId)) {
    return { ok: false, error: "invalid_device_id" };
  }

  for (const key of ["deviceName", "manufacturer", "model", "osVersion", "appVersion", "snapshotHash"] as const) {
    const value = input[key];
    if (value !== undefined && value !== null && shortString(value) === null) {
      return { ok: false, error: `invalid_${key}` };
    }
  }

  if (input.snapshot !== undefined && input.snapshot !== null && !isRecord(input.snapshot)) {
    return { ok: false, error: "invalid_snapshot" };
  }

  const budget = { values: 0 };
  const structuralError = validateValue(input, "", 0, budget);
  if (structuralError) return { ok: false, error: structuralError };

  const value = { ...input, deviceId } as Record<string, unknown>;
  for (const key of ["deviceName", "manufacturer", "model", "osVersion", "appVersion", "snapshotHash"] as const) {
    if (typeof value[key] === "string") value[key] = value[key].trim();
  }
  return { ok: true, value: value as unknown as ValidSyncBody };
}

function validateValue(value: unknown, path: string, depth: number, budget: { values: number }): string | null {
  budget.values += 1;
  if (budget.values > MAX_TOTAL_VALUES) return "sync_payload_too_complex";
  if (depth > MAX_DEPTH) return "sync_payload_too_deep";

  if (value === null || value === undefined) return null;
  if (collectionLimits[path] !== undefined && !Array.isArray(value)) return "invalid_sync_collection";
  if (typeof value === "boolean") return null;
  if (typeof value === "number") return Number.isFinite(value) ? null : "invalid_number";
  if (typeof value === "string") return value.length <= MAX_FIELD_TEXT ? null : "sync_field_too_long";

  if (Array.isArray(value)) {
    const limit = collectionLimits[path] ?? 256;
    if (value.length > limit) return "sync_collection_too_large";
    for (const item of value) {
      const error = validateValue(item, `${path}[]`, depth + 1, budget);
      if (error) return error;
    }
    return null;
  }

  if (!isRecord(value)) return "invalid_sync_value";
  for (const [key, child] of Object.entries(value)) {
    if (key.length > 128 || key === "__proto__" || key === "constructor" || key === "prototype") {
      return "invalid_sync_key";
    }
    const childPath = path ? `${path}.${key}` : key;
    const error = validateValue(child, childPath, depth + 1, budget);
    if (error) return error;
  }
  return null;
}

function shortString(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed.length >= 1 && trimmed.length <= MAX_SHORT_TEXT ? trimmed : null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
