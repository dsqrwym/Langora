import {API_TIMEOUT} from "@/lib/api/api-config.ts";
import * as axios from "axios";
import type {ApiResponse} from "@/lib/api/types.ts";
import {
    AUTH_ADMIN_LOGIN_ENDPOINT, AUTH_ADMIN_LOGOUT_ENDPOINT,
    AUTH_ADMIN_REFRESH_ENDPOINT,
    AUTH_BASE_ENDPOINT,
    CSRF_HEADER
} from "@/lib/auth/auth-constant.ts";
import type {LoginRequest, LoginResponse, RefreshTokenResponse} from "@/lib/auth/auth-types.ts";


export const authHttp = axios.create({
    baseURL: AUTH_BASE_ENDPOINT,
    withCredentials: true,
    timeout: API_TIMEOUT,
    headers: {
        Accept: 'application/json',
    }
});

const requireOk = <T>(response: ApiResponse<T>): T => {
    if (response.code !== "OK" || response.data === null) {
        throw new Error(`Unexpected API response code: ${response.code}`);
    }
    return response.data;
};

export const requestAdminLogin = async (payload: LoginRequest): Promise<LoginResponse> => {
    const response = await authHttp.post<ApiResponse<LoginResponse>>(AUTH_ADMIN_LOGIN_ENDPOINT, payload);
    return requireOk(response.data);
};

export const requestAdminRefresh = async (csrfToken: string | null): Promise<RefreshTokenResponse> => {
    const response = await authHttp.post<ApiResponse<RefreshTokenResponse>>(
        AUTH_ADMIN_REFRESH_ENDPOINT,
        undefined,
        {
            headers: csrfToken ? {[CSRF_HEADER]: csrfToken} : {}
        }
    );

    return requireOk(response.data);
};

export const requestAdminLogout = async (accessToken: string): Promise<void> => {
    await authHttp.post(AUTH_ADMIN_LOGOUT_ENDPOINT, undefined, {
        headers: {Authorization: `Bearer ${accessToken}`},
    })
}
