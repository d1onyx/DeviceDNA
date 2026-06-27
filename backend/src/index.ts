import { Hono } from "hono";
import { syncRoutes } from "./routes/sync";
import type { AppBindings } from "./types";

const app = new Hono<AppBindings>();

// Health-check (no auth)
app.get("/", (c) => c.json({ ok: true, service: "devicedna-sync" }));

app.route("/v1", syncRoutes);

export default app;
