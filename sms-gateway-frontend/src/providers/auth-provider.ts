import type { AuthProvider } from "@refinedev/core";
import { UserManager, WebStorageStateStore, type User } from "oidc-client-ts";
import { oidcConfig } from "./constants";

const ADMIN_ROLE = "sms_gateway_admin";
const USER_ROLE = "sms_gateway_user";

export const userManager = new UserManager({
  ...oidcConfig,
  scope: "openid profile email",
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  automaticSilentRenew: true,
});

function parseRoles(user: User | null): string[] {
  const groups = (user?.profile as any)?.groups;
  return Array.isArray(groups) ? groups : [];
}

export function isAdmin(roles: string[]): boolean {
  return roles.includes(ADMIN_ROLE);
}

export function isUser(roles: string[]): boolean {
  return roles.includes(USER_ROLE) || roles.includes(ADMIN_ROLE);
}

export const authProvider: AuthProvider = {
  login: async () => {
    await userManager.signinRedirect();
    return { success: true };
  },

  logout: async () => {
    await userManager.signoutRedirect();
    return { success: true, redirectTo: "/" };
  },

  check: async () => {
    const user = await userManager.getUser();
    if (user && !user.expired) {
      return { authenticated: true };
    }
    return { authenticated: false, redirectTo: "/login" };
  },

  getPermissions: async () => {
    const user = await userManager.getUser();
    return parseRoles(user);
  },

  getIdentity: async () => {
    const user = await userManager.getUser();
    if (!user) return null;
    const roles = parseRoles(user);
    return {
      id: user.profile.sub,
      name: user.profile.preferred_username || user.profile.name || user.profile.sub,
      email: user.profile.email,
      avatar: undefined,
      roles,
      isAdmin: isAdmin(roles),
    };
  },

  onError: async (error) => {
    if (error?.statusCode === 401) {
      return { logout: true, redirectTo: "/login" };
    }
    return { error };
  },
};
