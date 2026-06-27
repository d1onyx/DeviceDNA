import { defineConfig } from "drizzle-kit";

// DATABASE_URL is read from the environment during generate/migrate:
//   DATABASE_URL="postgresql://..." npm run db:migrate
export default defineConfig({
  schema: "./src/db/schema.ts",
  out: "./drizzle",
  dialect: "postgresql",
  dbCredentials: {
    url: process.env.DATABASE_URL!,
  },
});
