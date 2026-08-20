import express from "express";
import cors from "cors";
import crypto from "crypto";
import { plans, users, apiKeys, getNextPlanId, getNextApiKeyId } from "./data.mjs";

const app = express();
app.use(cors());
app.use(express.json());

// --- Plans CRUD ---
app.get("/api/v1/admin/plans", (_req, res) => {
  res.json(plans);
});

app.get("/api/v1/admin/plans/:id", (req, res) => {
  const plan = plans.find((p) => p.id === Number(req.params.id));
  if (!plan) return res.status(404).json({ error: "Not found" });
  res.json(plan);
});

app.post("/api/v1/admin/plans", (req, res) => {
  const plan = {
    id: getNextPlanId(),
    ...req.body,
    createdAt: new Date().toISOString(),
    updatedAt: null,
  };
  plans.push(plan);
  res.status(201).json(plan);
});

app.put("/api/v1/admin/plans/:id", (req, res) => {
  const idx = plans.findIndex((p) => p.id === Number(req.params.id));
  if (idx === -1) return res.status(404).json({ error: "Not found" });
  plans[idx] = {
    ...plans[idx],
    ...req.body,
    updatedAt: new Date().toISOString(),
  };
  res.json(plans[idx]);
});

app.delete("/api/v1/admin/plans/:id", (req, res) => {
  const idx = plans.findIndex((p) => p.id === Number(req.params.id));
  if (idx === -1) return res.status(404).json({ error: "Not found" });
  plans[idx].isActive = false;
  res.status(204).send();
});

// --- Users ---
app.get("/api/v1/admin/users", (_req, res) => {
  res.json(users);
});

app.get("/api/v1/admin/users/:id", (req, res) => {
  const user = users.find((u) => u.id === Number(req.params.id));
  if (!user) return res.status(404).json({ error: "Not found" });
  res.json(user);
});

app.put("/api/v1/admin/users/:id/plan", (req, res) => {
  const user = users.find((u) => u.id === Number(req.params.id));
  if (!user) return res.status(404).json({ error: "Not found" });
  const plan = plans.find((p) => p.code === req.body.planCode);
  if (!plan) return res.status(400).json({ error: `Plan '${req.body.planCode}' not found` });
  user.planCode = plan.code;
  user.updatedAt = new Date().toISOString();
  res.json(user);
});

app.delete("/api/v1/admin/users/:id/plan", (req, res) => {
  const user = users.find((u) => u.id === Number(req.params.id));
  if (!user) return res.status(404).json({ error: "Not found" });
  user.planCode = null;
  user.updatedAt = new Date().toISOString();
  res.json(user);
});

app.put("/api/v1/admin/users/:id/enabled", (req, res) => {
  const user = users.find((u) => u.id === Number(req.params.id));
  if (!user) return res.status(404).json({ error: "Not found" });
  user.enabled = req.body.enabled;
  user.updatedAt = new Date().toISOString();
  res.json(user);
});

// --- API Keys ---
app.get("/api/v1/api-keys", (_req, res) => {
  res.json(apiKeys);
});

app.post("/api/v1/api-keys", (req, res) => {
  const rawKey = `sk-${crypto.randomBytes(24).toString("hex")}`;
  const apiKey = {
    id: getNextApiKeyId(),
    name: req.body.name,
    key: rawKey,
    prefix: rawKey.substring(0, 12),
    enabled: true,
    expiresAt: req.body.expiresAt || null,
    lastUsedAt: null,
    createdAt: new Date().toISOString(),
  };
  apiKeys.push({ ...apiKey, key: undefined });
  res.json(apiKey);
});

app.delete("/api/v1/api-keys/:id", (req, res) => {
  const idx = apiKeys.findIndex((k) => k.id === Number(req.params.id));
  if (idx === -1) return res.status(404).json({ error: "Not found" });
  apiKeys[idx].enabled = false;
  res.status(204).send();
});

// --- SMS ---
app.post("/api/v1/sms", (req, res) => {
  const { phoneNumber, message } = req.body;
  console.log(`[MOCK SMS] To: ${phoneNumber} | Message: ${message}`);
  res.send("SMS sent successfully");
});

app.post("/api/v1/sms/otp", (req, res) => {
  const { phoneNumber, otpCode } = req.body;
  console.log(`[MOCK OTP] To: ${phoneNumber} | Code: ${otpCode}`);
  res.send("OTP SMS sent successfully");
});

const PORT = 3001;
app.listen(PORT, () => {
  console.log(`Mock API server running at http://localhost:${PORT}`);
  console.log("Available endpoints:");
  console.log("  GET/POST       /api/v1/admin/plans");
  console.log("  GET/PUT/DELETE  /api/v1/admin/plans/:id");
  console.log("  GET            /api/v1/admin/users");
  console.log("  PUT            /api/v1/admin/users/:id/plan");
  console.log("  DELETE         /api/v1/admin/users/:id/plan");
  console.log("  PUT            /api/v1/admin/users/:id/enabled");
  console.log("  GET/POST       /api/v1/api-keys");
  console.log("  DELETE         /api/v1/api-keys/:id");
  console.log("  POST           /api/v1/sms");
  console.log("  POST           /api/v1/sms/otp");
});
