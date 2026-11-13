import {useAuthStore} from "@/features/auth/session/auth-store";
import {ensureFreshToken, refreshInsideLock, withSessionLock} from "@/features/auth/session/auth-refresh-lock";
import {useEffect} from "react";
import {ApiError, ApiResult} from "@/lib/api/api-types";
import {LoginResponse} from "@/features/auth/auth-types";
import {authRepository} from "@/features/auth/api/auth-client";
import {publishLogout, publishSession} from "@/features/auth/session/auth-channel";
import {ErrorCode, isTerminalSessionCode} from "@/lib/api/api-error-codes";

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

export const logoutAdmin = (): Promise<ApiResult<null>> => withSessionLock(async () => {
    const token = useAuthStore.getState().getAccessToken();

    if (!token) {
        publishLogout();
        return {success: true, data: null};
    }

    const result = await authRepository.logout(token);
    if (result.success) {
        publishLogout();
        return result;
    }

    if (result.error instanceof ApiError) {
        if (result.error.code === ErrorCode.AUTH_ACCESS_TOKEN_INVALID) {
            try {
                const fresh = await refreshInsideLock();
                await authRepository.logout(fresh);
            } catch {
                // 刷新也失败了：会话已经不可用，直接清本地
            }
            publishLogout();
            return {success: true, data: null};
        }
        if (isTerminalSessionCode(result.error.code)) {
            publishLogout();
            return {success: true, data: null};
        }
    }

    return result;
});

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
