import {useAuthStore} from "@/lib/auth/auth-store.ts";

export {bootstrapSession, loginAdmin, logoutAdmin} from "@/lib/auth/auth-service.ts";
export {ensureFreshToken} from "@/lib/auth/auth-refresh-lock.ts";
export type {
    AuthSnapshot,
    AuthStatus,
    AuthUser,
    LoginRequest,
    LoginResponse,
} from "@/lib/auth/auth-types.ts";

/** 组件订阅认证状态用（selector 只取 snapshot，登录态变化才会重渲染） */
export const useAuth = () => useAuthStore((state) => state.snapshot);

/** 网络层同步读 token 用（拦截器里不能用 hook） */
export const getAccessToken = (): string | null => useAuthStore.getState().getAccessToken();