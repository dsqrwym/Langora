import {ApiFetch} from "@/lib/api/api-fetch";
import {CaptchaConfigResponse, LoginRequest, LoginResponse, RefreshTokenResponse} from "@/features/auth/auth-types";

export const CSRF_HEADER = "X-XSRF-TOKEN";

export function createAdminAuthEndpoints(apiFetch: ApiFetch) {
    return {
        login(payload: LoginRequest) {
            return apiFetch<LoginResponse>("auth/admin/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });
        },
        refresh(csrfToken: string | null) {
            return apiFetch<RefreshTokenResponse>("auth/admin/refresh", {
                method: "POST",
                ...(csrfToken ? {headers: {[CSRF_HEADER]: csrfToken}} : {}),
            });
        },
        logout(accessToken: string) {
            return apiFetch<null>("auth/admin/logout", {
                method: "POST",
                headers: {"Authorization": `Bearer ${accessToken}`}
            });
        },
        captchaConfig() {
            return apiFetch<CaptchaConfigResponse>("auth/captcha/config");
        },
    }
}

export type AuthEndpoints = ReturnType<typeof createAdminAuthEndpoints>;
