export const API_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const oidcConfig = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,

  // PKCE
  response_type: "code",

  redirect_uri: `${window.location.origin}/auth/callback`,
  post_logout_redirect_uri: `${window.location.origin}/`,
};
