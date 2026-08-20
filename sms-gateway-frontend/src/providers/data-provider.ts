import type { DataProvider } from "@refinedev/core";
import { API_URL } from "./constants";

const isMock = import.meta.env.VITE_MOCK === "true";

async function getHeaders(): Promise<HeadersInit> {
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    Accept: "application/json",
  };
  if (!isMock) {
    const { userManager } = await import("./auth-provider");
    const user = await userManager.getUser();
    if (user?.access_token) {
      headers["Authorization"] = `Bearer ${user.access_token}`;
    }
  }
  return headers;
}

function resourceToPath(resource: string): string {
  const mapping: Record<string, string> = {
    plans: "/api/v1/admin/plans",
    users: "/api/v1/admin/users",
    "api-keys": "/api/v1/api-keys",
  };
  return mapping[resource] || `/${resource}`;
}

async function fetchApi(url: string, options?: RequestInit): Promise<Response> {
  const headers = await getHeaders();
  const response = await fetch(url, { ...options, headers: { ...headers, ...options?.headers } });
  if (response.status === 401) {
    throw { statusCode: 401, message: "Unauthorized" };
  }
  if (response.status === 403) {
    throw { statusCode: 403, message: "Forbidden" };
  }
  return response;
}

export const dataProvider: DataProvider = {
  getList: async ({ resource }) => {
    const path = resourceToPath(resource);
    const response = await fetchApi(`${API_URL}${path}`);
    const data = await response.json();
    // Backend returns array directly
    return { data, total: data.length };
  },

  getOne: async ({ resource, id }) => {
    const path = resourceToPath(resource);
    const response = await fetchApi(`${API_URL}${path}/${id}`);
    const data = await response.json();
    return { data };
  },

  create: async ({ resource, variables }) => {
    const path = resourceToPath(resource);
    const response = await fetchApi(`${API_URL}${path}`, {
      method: "POST",
      body: JSON.stringify(variables),
    });
    const data = await response.json();
    return { data };
  },

  update: async ({ resource, id, variables }) => {
    const path = resourceToPath(resource);
    const response = await fetchApi(`${API_URL}${path}/${id}`, {
      method: "PUT",
      body: JSON.stringify(variables),
    });
    const data = await response.json();
    return { data };
  },

  deleteOne: async ({ resource, id }) => {
    const path = resourceToPath(resource);
    const response = await fetchApi(`${API_URL}${path}/${id}`, {
      method: "DELETE",
    });
    // DELETE might return 204 with no body
    if (response.status === 204) {
      return { data: { id } as any };
    }
    const data = await response.json();
    return { data };
  },

  getApiUrl: () => API_URL,
};
