import { defineConfig } from "drizzle-kit";

// D1 is a SQLite database. drizzle-kit generates SQLite migrations from the schema;
// they are applied to D1 with `wrangler d1 migrations apply` (see package.json).
export default defineConfig({
  schema: "./src/db/schema.ts",
  out: "./drizzle",
  dialect: "sqlite",
  driver: "d1-http",
});
