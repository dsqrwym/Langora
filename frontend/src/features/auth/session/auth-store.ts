import {create} from "zustand";
import {readEnvelop, writeEnvelope} from "./auth-storage";
import {AuthState, AuthUser} from "@/features/auth/auth-types";
import {decodeFromAccessToken, isAccessTokenUsable} from "@/features/auth/auth-jwt";

// 初始化 accessToken 一定是 null, version 和 csrfToken 从本地存储中读取
// 模块在 SSR 阶段也会被求值，readEnvelope 有 canUseWindow 守卫 → 服务端恒为空信封
const boot = readEnvelop();

const toUser = (token: string | null): AuthUser | null => {
    if (!token) return null;
    try {
        return decodeFromAccessToken(token);
    } catch {
        return null;
    }
}

export const useAuthStore = create<AuthState>((set, get) => ({
    snapshot: {status: "checking", user: null},
    accessToken: null,
    csrfToken: boot.csrfToken,
    version: boot.version,

    /**
     * Begin checking session status
     */
    beginCheck: () => {
        set({snapshot: {status: "checking", user: get().snapshot.user}});
    },
    /**
     * Settle anonymous status, 本次没有会话
     */
    settleAnonymous: () => {
        set({
            accessToken: null,
            snapshot: {status: 'anonymous', user: null}
        });
    },
    /**
     * Commit session, 储存会话
     */
    commitSession: (nextToken, nextCsrf) => {
        // max(内存, 磁盘) + 1：必须严格大于磁盘上任何已知版本
        const version = Math.max(get().version, readEnvelop().version) + 1;

        set({
            version,
            accessToken: nextToken,
            csrfToken: nextCsrf,
            snapshot: {status: 'authenticated', user: toUser(nextToken)}
        });

        writeEnvelope({version, csrfToken: nextCsrf});
        return version;
    },
    /**
     * Commit logout, 登出
     */
    commitLogout: () => {
        const version = Math.max(get().version, readEnvelop().version) + 1;

        set({
            version,
            accessToken: null,
            csrfToken: null,
            snapshot: {status: 'anonymous', user: null}
        });

        writeEnvelope({version, csrfToken: null});
        return version;
    },
    /**
     * Adopt session, 检查并储存会话
     */
    adoptSession: (nextToken, nextCsrf, peerVersion) => {
        // 同伴可能空闲到 token 过期，不能把过期会话当成有效会话采纳
        if (!isAccessTokenUsable(nextToken)) return;
        if (peerVersion < get().version) return;
        if (peerVersion < readEnvelop().version) return;

        set({
            version: peerVersion,
            accessToken: nextToken,
            csrfToken: nextCsrf,
            snapshot: {status: 'authenticated', user: toUser(nextToken)}
        });

        writeEnvelope({version: peerVersion, csrfToken: nextCsrf});
    },
    /**
     * Adopt logout, 检查并登出
     */
    adoptLogout: (peerVersion) => {
        if (peerVersion < get().version) return;
        if (peerVersion < readEnvelop().version) return;

        set({
            version: peerVersion,
            accessToken: null,
            csrfToken: null,
            snapshot: {status: 'anonymous', user: null}
        });

        writeEnvelope({version: peerVersion, csrfToken: null});
    },
    getAccessToken: () => get().accessToken,
    getCsrfToken: () => get().csrfToken,
    getVersion: () => get().version,
    getSharedVersion: () => readEnvelop().version,
}));