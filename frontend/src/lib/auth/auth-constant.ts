import {API_BASE} from "@/lib/api/api-config.ts";

/** Auth localStorage envelope key */
export const AUTH_ENVELOPE_KEY = "langora.auth.envelope";
/** Auth BroadcastChannel */
export const AUTH_CHANNEL_NAME = "langora:auth";
/** Auth Web Locks */
export const AUTH_LOCK_NAME = "langora:auth-session";

export const CSRF_HEADER = "X-XSRF-TOKEN";
export const AUTH_BASE_ENDPOINT = `${API_BASE}/auth`;
export const AUTH_ADMIN_REFRESH_ENDPOINT = `${AUTH_BASE_ENDPOINT}/admin/refresh`;
export const AUTH_ADMIN_LOGIN_ENDPOINT = `${AUTH_BASE_ENDPOINT}/admin/login`;
export const AUTH_ADMIN_LOGOUT_ENDPOINT = `${AUTH_BASE_ENDPOINT}/admin/logout`;
