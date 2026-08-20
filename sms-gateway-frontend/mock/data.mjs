let nextPlanId = 4;
let nextApiKeyId = 4;

export const plans = [
  {
    id: 1,
    code: "free",
    name: "Free Tier",
    quotaType: "PER_DAY",
    quotaLimit: 10,
    minDelaySeconds: 60,
    maxRecipientsPerRequest: 1,
    isActive: true,
    createdAt: "2025-01-15T10:00:00Z",
    updatedAt: null,
  },
  {
    id: 2,
    code: "basic",
    name: "Basic Plan",
    quotaType: "PER_MONTH",
    quotaLimit: 500,
    minDelaySeconds: 10,
    maxRecipientsPerRequest: 5,
    isActive: true,
    createdAt: "2025-01-15T10:00:00Z",
    updatedAt: "2025-02-01T12:00:00Z",
  },
  {
    id: 3,
    code: "premium",
    name: "Premium Plan",
    quotaType: "UNLIMITED",
    quotaLimit: null,
    minDelaySeconds: null,
    maxRecipientsPerRequest: null,
    isActive: true,
    createdAt: "2025-01-15T10:00:00Z",
    updatedAt: null,
  },
];

export const users = [
  {
    id: 1,
    externalProviderId: "auth-uuid-001",
    username: "andrei",
    enabled: true,
    planCode: "premium",
    createdAt: "2025-01-20T08:00:00Z",
    updatedAt: "2025-02-10T14:00:00Z",
  },
  {
    id: 2,
    externalProviderId: "auth-uuid-002",
    username: "maria",
    enabled: true,
    planCode: "basic",
    createdAt: "2025-02-01T09:00:00Z",
    updatedAt: null,
  },
  {
    id: 3,
    externalProviderId: "auth-uuid-003",
    username: "ion",
    enabled: false,
    planCode: null,
    createdAt: "2025-03-01T11:00:00Z",
    updatedAt: "2025-03-05T16:00:00Z",
  },
];

export const apiKeys = [
  {
    id: 1,
    name: "Production Key",
    prefix: "sk-prod-abcd",
    enabled: true,
    expiresAt: "2026-01-01T00:00:00Z",
    lastUsedAt: "2025-03-10T15:30:00Z",
    createdAt: "2025-01-20T08:30:00Z",
  },
  {
    id: 2,
    name: "Testing Key",
    prefix: "sk-test-efgh",
    enabled: true,
    expiresAt: null,
    lastUsedAt: "2025-03-08T10:00:00Z",
    createdAt: "2025-02-15T12:00:00Z",
  },
  {
    id: 3,
    name: "Old Key",
    prefix: "sk-old-ijkl",
    enabled: false,
    expiresAt: "2025-06-01T00:00:00Z",
    lastUsedAt: "2025-01-30T09:00:00Z",
    createdAt: "2025-01-01T00:00:00Z",
  },
];

export function getNextPlanId() {
  return nextPlanId++;
}

export function getNextApiKeyId() {
  return nextApiKeyId++;
}
