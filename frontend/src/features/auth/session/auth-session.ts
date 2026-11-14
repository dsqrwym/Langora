import {useAuthStore} from "@/features/auth/session/auth-store";
import {ensureFreshToken, refreshInsideLock, withSessionLock} from "@/features/auth/session/auth-refresh-lock";
import {useEffect} from "react";
import {ApiError, ApiResult} from "@/lib/api/api-types";
import {LoginResponse} from "@/features/auth/auth-types";
import {authRepository} from "@/features/auth/api/auth-client";
import {publishLogout, publishSession} from "@/features/auth/session/auth-channel";
import {ErrorCode, isTerminalSessionCode} from "@/lib/api/api-error-codes";
import {safeApiCall} from "@/lib/api/safe-api-call";

export const loginAdmin = (
    identifier: string,
    password: string,
    captchaToken?: string,
): Promise<ApiResult<LoginResponse>> => withSessionLock(async () => {
    const result = await authRepository.login(identifier, password, captchaToken);
    if (result.success) {
        publishSession(result.data.accessToken, result.data.csrfToken);
    }
    return result;
});

const isTerminalSessionError = (error: unknown): boolean =>
    error instanceof ApiError && isTerminalSessionCode(error.code);

/**
 * 只有服务端确认撤销，或服务端明确表示会话已终止，才广播本地退出。
 * 网络错误不能用“本地先退出”伪装成功，否则刷新页面可能被 Cookie 自动登录。
 */
export const logoutAdmin = (): Promise<ApiResult<null>> =>
    safeApiCall<null>(() => withSessionLock(async () => {
        let token = useAuthStore.getState().getAccessToken();

        // 内存没有 token，但 HttpOnly Refresh Cookie 可能仍然有效
        if (!token) {
            try {
                token = await refreshInsideLock();
            } catch (error) {
                if (isTerminalSessionError(error)) return null;
                throw error;
            }
        }

        const result = await authRepository.logout(token);
        if (result.success) {
            publishLogout();
            return null;
        }

        if (isTerminalSessionError(result.error)) {
            publishLogout();
            return null;
        }

        if (!(result.error instanceof ApiError)
            || result.error.code !== ErrorCode.AUTH_ACCESS_TOKEN_INVALID) {
            throw result.error;
        }

        // Access Token 失效时只刷新并重试一次，避免退出流程无限循环
        let freshToken: string;
        try {
            freshToken = await refreshInsideLock();
        } catch (error) {
            if (isTerminalSessionError(error)) return null;
            throw error;
        }

        const retried = await authRepository.logout(freshToken);
        if (retried.success || isTerminalSessionError(retried.error)) {
            publishLogout();
            return null;
        }

        throw retried.error;
    }));

let bootstrapTask: Promise<void> | null = null;
let settled = false;

/**
 * 启动引导：刷新页面后内存 token 必然为空，但 Cookie 还在，
 * 只有问一次后端才知道"我还登录着吗"。
 *
 * 模块级 Promise 去重：StrictMode 会把 effect 跑两遍，两次调用共享同一次网络请求。
 */
export const bootstrapSession = (): Promise<void> => {
    if (settled) return Promise.resolve();
    bootstrapTask ??= (async () => {
        //  顺序不能反：先判断有没有 token，再 beginCheck。
        // 反了（先 checking 再 return）会把状态永久卡在 checking → 无限 loading。
        if (useAuthStore.getState().getAccessToken()) return;

        useAuthStore.getState().beginCheck();

        try {
            await ensureFreshToken(null);
        } catch {
            // 失败可能是"没有 Cookie"（正常首次访问），也可能是网络断了。
            // 两者都进匿名态；但不广播 别的标签页可能好好的。
            if (useAuthStore.getState().getAccessToken() === null) {
                useAuthStore.getState().settleAnonymous();
            }
        }finally {
            settled = true;
        }
    })().finally(() => {
        bootstrapTask = null; // 允许"重试"再次调用
    });

    return bootstrapTask;
};

export function useSessionBootstrap(): void {
    useEffect(() => {
        void bootstrapSession();
    }, []);
}
