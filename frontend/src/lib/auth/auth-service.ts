import type {LoginRequest, LoginResponse} from "@/lib/auth/auth-types.ts";
import {ensureFreshToken, refreshInsideLock, withSessionLock} from "@/lib/auth/auth-refresh-lock.ts";
import {getOrCreateDeviceUuid} from "@/lib/device.ts";
import {requestAdminLogin, requestAdminLogout} from "@/lib/auth/auth-api.ts";
import {publishLogout, publishSession} from "@/lib/auth/auth-channel.ts";
import {useAuthStore} from "@/lib/auth/auth-store.ts";
import {getApiFailure, isTerminalSessionCode} from "@/lib/api/errors.ts";
import {ErrorCode} from "@/lib/api/error-codes.ts";

export const loginAdmin = (identifier: string, password: string, captchaToken?: string): Promise<LoginResponse> => withSessionLock(async () => {
    const trimmed = identifier.trim();

    const payload: LoginRequest = {
        password,
        deviceUuid: getOrCreateDeviceUuid(),
        ...(trimmed.includes("@") ? {email: trimmed} : {username: trimmed}),
        ...(captchaToken ? {captchaToken} : {})
    };

    const result = await requestAdminLogin(payload);
    publishSession(result.accessToken, result.csrfToken);
    return result;
});

export const logoutAdmin = (): Promise<void> => withSessionLock(async () => {
    const token = useAuthStore.getState().getAccessToken();

    if (!token) {
        publishLogout();
        return;
    }

    try {
        await requestAdminLogout(token);
    } catch (error) {
        const failure = getApiFailure(error);

        if (failure.kind === "http" && failure.code === ErrorCode.AUTH_ACCESS_TOKEN_INVALID) {
            // 已经在锁里，所以用 refreshInsideLock（不是 ensureFreshToken，那会死锁）
            const fresh = await refreshInsideLock();
            await requestAdminLogout(fresh);
        } else if (failure.kind === "http" && isTerminalSessionCode(failure.code)) {
            publishLogout();
            return;
        } else {
            throw error;
        }
    }

    publishLogout();
});

let bootstrapTask: Promise<void> | null = null;

/**
 * 启动引导：刷新页面后内存 token 必然为空，但 Cookie 还在，
 * 只有问一次后端才知道"我还登录着吗"。
 *
 * 模块级 Promise 去重：StrictMode 会把 effect 跑两遍，两次调用共享同一次网络请求。
 */
export const bootstrapSession = (): Promise<void> => {
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
            if (!useAuthStore.getState().getAccessToken()) {
                useAuthStore.getState().settleAnonymous();
            }
        }
    })().finally(() => {
        bootstrapTask = null; // 允许"重试"再次调用
    });

    return bootstrapTask;
};
