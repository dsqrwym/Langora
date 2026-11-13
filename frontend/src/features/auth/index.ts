import {useAuthStore} from "@/features/auth/session/auth-store";

export {bootstrapSession} from "@/features/auth/session/auth-session"
export {ensureFreshToken} from "@/features/auth/session/auth-refresh-lock";
export type {
    AuthSnapshot,
    AuthStatus,
    AuthUser,
} from "@/features/auth/auth-types";

/** 组件订阅认证状态用（selector 只取 snapshot，登录态变化才会重渲染） */
export const useAuth = () => useAuthStore((state) => state.snapshot);

/** 网络层同步读 token 用（拦截器里不能用 hook） */
export const getAccessToken = (): string | null => useAuthStore.getState().getAccessToken();