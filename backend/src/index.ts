import { Hono } from "hono";
import { accountRegistry } from "./middleware/account-registry";
import { firebaseAuth } from "./middleware/auth";
import { firebaseAccountExists } from "./middleware/firebase-account";
import { accountDeletionRoutes } from "./routes/account-deletion";
import { authRoutes } from "./routes/auth";
import { playRtdnRoutes } from "./routes/play-rtdn";
import { internalSubscriptionRoutes, subscriptionRoutes } from "./routes/subscriptions";
import { syncRoutes } from "./routes/sync";
import type { AppBindings } from "./types";

const app = new Hono<AppBindings>();
const v1Routes = new Hono<AppBindings>();

// Health-check (no auth)
app.get("/", (c) => c.json({ ok: true, service: "devicedna-sync" }));

// Public account-deletion page (no auth) — required by Google Play; put the URL in Play Console.
app.route("/account-deletion", accountDeletionRoutes);

// All /v1 routes require a valid Firebase ID token and a live Firebase account.
v1Routes.use("*", firebaseAuth);
v1Routes.use("*", firebaseAccountExists);
v1Routes.use("*", accountRegistry);
v1Routes.route("/", syncRoutes);
v1Routes.route("/", subscriptionRoutes);

app.route("/auth", authRoutes);
app.route("/v1", v1Routes);
app.route("/internal", internalSubscriptionRoutes);
// Google Play RTDN webhook (Pub/Sub push). Auth via ?token=PLAY_RTDN_VERIFICATION_TOKEN.
app.route("/play", playRtdnRoutes);

export default app;
