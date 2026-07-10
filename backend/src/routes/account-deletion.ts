import { Hono } from "hono";
import type { AppBindings } from "../types";

// Public, unauthenticated page for Google Play's account-deletion requirement: users must be able
// to request account + data deletion from the web, without needing the installed app. Put this
// URL (https://<worker>/account-deletion) in Play Console -> App content -> Data safety.
export const accountDeletionRoutes = new Hono<AppBindings>();

// Set this to the real support address before release (must match the Play Console contact email).
const CONTACT_EMAIL = "support@example.com";

const PAGE = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Delete your DeviceDNA account</title>
<style>
  :root { color-scheme: light dark; }
  body { margin: 0; font: 16px/1.6 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
         background: #f6f7f9; color: #1a1a1a; }
  main { max-width: 640px; margin: 0 auto; padding: 32px 20px 64px; }
  h1 { font-size: 1.6rem; margin: 0 0 4px; }
  h2 { font-size: 1.15rem; margin: 28px 0 8px; }
  .lead { color: #555; margin-top: 0; }
  ol, ul { padding-left: 22px; }
  li { margin: 6px 0; }
  code { background: rgba(127,127,127,.15); padding: 1px 5px; border-radius: 4px; }
  a { color: #2f6fed; }
  .card { background: #fff; border: 1px solid #e4e6eb; border-radius: 12px; padding: 4px 20px 16px; }
  footer { margin-top: 28px; font-size: .85rem; color: #888; }
  @media (prefers-color-scheme: dark) {
    body { background: #16181c; color: #e8e8e8; }
    .lead { color: #a8a8a8; }
    .card { background: #1f2227; border-color: #2c2f36; }
    footer { color: #7a7a7a; }
  }
</style>
</head>
<body>
<main>
  <h1>Delete your DeviceDNA account</h1>
  <p class="lead">Request deletion of your DeviceDNA account and all data associated with it.</p>

  <div class="card">
    <h2>In the app (fastest)</h2>
    <ol>
      <li>Open <strong>DeviceDNA</strong> and sign in.</li>
      <li>Go to <strong>Settings &rarr; Account</strong>.</li>
      <li>Tap <strong>Delete account</strong> and confirm.</li>
    </ol>
    <p>This permanently deletes your account immediately.</p>

    <h2>Without the app</h2>
    <p>If you have uninstalled DeviceDNA, email
      <a href="mailto:${CONTACT_EMAIL}?subject=DeviceDNA%20account%20deletion">${CONTACT_EMAIL}</a>
      from the address linked to your account. We delete the account and its data within 30 days.</p>
  </div>

  <h2>What is deleted</h2>
  <ul>
    <li>Your account record (Firebase user and profile fields).</li>
    <li>All synced device snapshots.</li>
    <li>Subscription records; any active Google Play subscription is canceled first.</li>
  </ul>
  <p>Some records may be retained only where required for fraud prevention, tax, or legal
     compliance, and are removed once no longer needed.</p>

  <footer>See the DeviceDNA Privacy Policy in the Google Play listing for full details.</footer>
</main>
</body>
</html>`;

accountDeletionRoutes.get("/", (c) => c.html(PAGE));
