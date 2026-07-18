import { Hono } from "hono";
import privacyPolicyMarkdown from "../../../PRIVACY_POLICY.md";
import { renderPolicyMarkdown } from "../lib/markdown";
import type { AppBindings } from "../types";

export const privacyPolicyRoutes = new Hono<AppBindings>();

const content = renderPolicyMarkdown(privacyPolicyMarkdown);
const page = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="index,follow">
<title>DeviceDNA Privacy Policy</title>
<style>
  :root { color-scheme: light dark; }
  * { box-sizing: border-box; }
  body { margin: 0; font: 16px/1.65 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
         background: #f5f7fa; color: #172033; }
  main { width: min(860px, 100%); margin: 0 auto; padding: 40px 22px 80px; }
  article { background: #fff; border: 1px solid #e2e7ef; border-radius: 18px; padding: 28px 34px; }
  h1 { margin-top: 0; font-size: clamp(1.8rem, 4vw, 2.5rem); }
  h2 { margin-top: 48px; padding-top: 18px; border-top: 1px solid #e2e7ef; }
  h3 { margin-top: 30px; }
  p, li { color: #39445a; }
  li { margin: 7px 0; }
  a { color: #2563eb; text-underline-offset: 3px; }
  code { padding: 2px 6px; border-radius: 5px; background: rgba(127,127,127,.14); }
  footer { margin-top: 24px; text-align: center; color: #718096; font-size: .875rem; }
  @media (max-width: 600px) { main { padding: 16px 10px 48px; } article { padding: 22px 18px; } }
  @media (prefers-color-scheme: dark) {
    body { background: #11151d; color: #edf2f7; }
    article { background: #181e29; border-color: #2b3445; }
    h2 { border-color: #2b3445; }
    p, li { color: #c5cedd; }
    a { color: #7da7ff; }
  }
</style>
</head>
<body>
<main>
  <article>${content}</article>
  <footer>DeviceDNA · Privacy Policy</footer>
</main>
</body>
</html>`;

privacyPolicyRoutes.get("/", (c) => {
  c.header("Cache-Control", "public, max-age=3600");
  c.header("X-Content-Type-Options", "nosniff");
  c.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'");
  return c.html(page);
});
