import type { AccessControlProvider } from "@refinedev/core";
import { isAdmin, isUser } from "./auth-provider";

type GetPermissionsFn = () => Promise<string[]>;

let _getPermissions: GetPermissionsFn | null = null;

export function initAccessControl(getPermissions: GetPermissionsFn) {
  _getPermissions = getPermissions;
}

export const accessControlProvider: AccessControlProvider = {
  can: async ({ resource }) => {
    const roles: string[] = _getPermissions ? await _getPermissions() : [];

    if (resource === "plans" || resource === "users") {
      if (isAdmin(roles)) {
        return { can: true };
      }
      return { can: false, reason: "Admin access required" };
    }

    if (resource === "api-keys" || resource === "sms" || resource === "portability") {
      if (isUser(roles)) {
        return { can: true };
      }
      return { can: false, reason: "User access required" };
    }

    return { can: true };
  },
};
