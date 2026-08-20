import type { AuthProvider } from "@refinedev/core";

const MOCK_USER = {
  id: "mock-user-001",
  name: "dev-admin",
  email: "admin@localhost",
  roles: ["sms_gateway_admin", "sms_gateway_user"],
  isAdmin: true,
};

const STORAGE_KEY = "mock_auth_logged_in";

export const mockAuthProvider: AuthProvider = {
  login: async () => {
    localStorage.setItem(STORAGE_KEY, "true");
    return { success: true, redirectTo: "/" };
  },

  logout: async () => {
    localStorage.removeItem(STORAGE_KEY);
    return { success: true, redirectTo: "/login" };
  },

  check: async () => {
    if (localStorage.getItem(STORAGE_KEY) === "true") {
      return { authenticated: true };
    }
    return { authenticated: false, redirectTo: "/login" };
  },

  getPermissions: async () => {
    return MOCK_USER.roles;
  },

  getIdentity: async () => {
    if (localStorage.getItem(STORAGE_KEY) !== "true") return null;
    return MOCK_USER;
  },

  onError: async (error) => {
    if (error?.statusCode === 401) {
      return { logout: true, redirectTo: "/login" };
    }
    return { error };
  },
};
