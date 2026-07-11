import { drizzle } from "drizzle-orm/d1";
import * as schema from "./schema";

// Cloudflare D1 (SQLite) driver. The D1Database binding comes from wrangler.toml
// ([[d1_databases]]) and is exposed to the Worker as c.env.DB.
export function getDb(d1: D1Database) {
  return drizzle(d1, { schema });
}

export type Db = ReturnType<typeof getDb>;
