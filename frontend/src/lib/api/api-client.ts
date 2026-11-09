import * as axios from "axios";
import type {InternalAxiosRequestConfig} from "axios";
import {API_BASE, API_TIMEOUT} from "@/lib/api/api-config.ts";
import {ensureFreshToken, getAccessToken} from "@/lib/auth/index.ts";
import {getApiFailure} from "@/lib/api/errors.ts";
import {ErrorCode} from "@/lib/api/error-codes.ts";

/**
 * 挂在 axios config 上的私有字段。
 * 下划线前缀 = "内部用，别在业务代码里读"。
 */
type TrackedConfig = InternalAxiosRequestConfig & {
    /** 已经重放过一次，防止 401 → 刷新 → 401 → 刷新 死循环 */
    _retried?: boolean;
    /** 这次请求实际携带的 token，用于判断"是不是已经被别人换掉了" */
    _tokenUsed?: string | null;
};

export const api = axios.create({
    baseURL: API_BASE,
    withCredentials: true, // 跨 origin 也要带 Cookie
    timeout: API_TIMEOUT,  // 必须设：否则请求挂住会让单飞 Promise 永不 settle
    headers: {Accept: "application/json"},
});

// 请求拦截：挂 token
api.interceptors.request.use((config) => {
    const tracked = config as TrackedConfig;
    const token = getAccessToken();

    tracked._tokenUsed = token;

    if (token) {
        config.headers.set("Authorization", `Bearer ${token}`);
    }

    return config;
});

// 响应拦截：401 → 刷新 → 重放一次
api.interceptors.response.use(
    (response) => response,
    async (error: unknown) => {
        if (!axios.isAxiosError(error)) throw error;

        const config = error.config as TrackedConfig | undefined;
        if (!config) throw error;

        const failure = getApiFailure(error);

        /**
         * 四重条件，缺一不可：
         *   1. 没重放过
         *   2. 是 HTTP 错误（网络错误不刷）
         *   3. 状态码 401
         *   4. **业务码是 AUTH_ACCESS_TOKEN_INVALID**
         */
        const shouldRefresh =
            !config._retried &&
            failure.kind === "http" &&
            failure.status === 401 &&
            failure.code === ErrorCode.AUTH_ACCESS_TOKEN_INVALID;

        if (!shouldRefresh) throw error;

        // 先打标记再 await：await 抛错时标记已经生效，不会重入
        config._retried = true;

        /**
         * 这里可能抛错。如果抛的是终止码，refresh 层已经 publishLogout，
         * store 变成 anonymous，<RequireAuth> 会自己把用户送到 /login。
         * 拦截器**不需要**知道路由的存在。
         */
        const token = await ensureFreshToken(config._tokenUsed ?? null);

        config.headers.set("Authorization", `Bearer ${token}`);
        return api.request(config);
    },
);