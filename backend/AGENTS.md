# Repository Guidelines

## Project Structure & Module Organization

This repository contains the DeviceDNA sync backend, a TypeScript Cloudflare Worker built with Hono, Drizzle ORM, Neon Postgres, and Firebase token verification.

- `src/index.ts` creates the Hono app, health check, and route mounting.
- `src/routes/` contains API route handlers such as device sync and status checks.
- `src/middleware/` contains request middleware, including Firebase authentication.
- `src/db/` contains the Drizzle client and schema definitions.
- `drizzle/` stores generated SQL migrations and migration metadata; commit generated migration files.
- `wrangler.toml`, `.dev.vars.example`, and `drizzle.config.ts` hold Worker, local env, and database tooling configuration.

## Build, Test, and Development Commands

Run commands from the repository root.

- `npm install` installs dependencies from `package-lock.json`.
- `npm run dev` starts `wrangler dev` for local Worker testing.
- `npm run typecheck` runs `tsc --noEmit` with strict TypeScript settings.
- `npm run db:generate` generates Drizzle SQL migrations into `drizzle/`.
- `DATABASE_URL="postgresql://..." npm run db:migrate` applies migrations to Neon.
- `npm run deploy` deploys the Worker with Wrangler.

## Coding Style & Naming Conventions

Use TypeScript ES modules, strict types, and two-space indentation. Prefer named exports for shared modules, `camelCase` for variables/functions, and `PascalCase` for types and interfaces. Keep route files focused on HTTP behavior and put database schema changes in `src/db/schema.ts`. Use clear JSON error codes such as `android_id_required`.

No formatter or linter script is currently configured; before committing, run `npm run typecheck` and keep formatting consistent with existing files.

## Testing Guidelines

There is no dedicated test runner configured yet. For now, verify changes with `npm run typecheck`, `npm run dev`, and targeted `curl` checks against `/` and authenticated `/v1/*` endpoints. When adding tests, prefer TypeScript test files colocated near the behavior under test or in a future `test/` directory, and name them after the subject, for example `sync.test.ts`.

## Commit & Pull Request Guidelines

Recent history uses concise Conventional Commit-style subjects, especially `feat:` and `fix:`. Keep commits focused, for example `feat: add device status endpoint` or `fix: reject missing android id`.

Pull requests should include a short behavior summary, any database migration notes, required environment or secret changes, and manual verification steps. Link related issues when available. Include request/response examples for API changes.

## Security & Configuration Tips

Do not commit real secrets. Use `.dev.vars` for local values and Cloudflare secrets for deployed `DATABASE_URL`. Keep `.dev.vars.example` updated when adding required configuration. User identity must continue to come from verified Firebase claims, not request bodies.
