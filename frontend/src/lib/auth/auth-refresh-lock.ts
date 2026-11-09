import {requestAdminRefresh} from "@/lib/auth/auth-api.ts";
import {getApiFailure, isTerminalSessionCode} from "@/lib/api/errors.ts";
import {ErrorCode} from "@/lib/api/error-codes.ts";
import {AUTH_LOCK_NAME} from "@/lib/auth/auth-constant.ts";
import {useAuthStore} from "@/lib/auth/auth-store.ts";
import {publishLogout, publishSession, requestPeerSession, waitForVersion} from "@/lib/auth/auth-channel.ts";

/**
 * 409 等待同伴完成刷新的超时时间
 */
const PEER_WAIT_MS = 2_000;
/**
 * 冷启动向同伴探查 token 的间隔时间
 */
const PEER_PROBE_MS = 120;
/**
 * 单飞，所有并发 401 共享
 */
let inflight: Promise<string> | null = null;

/**
 * 跨标签互斥锁
 * @param operation
 */
export const withSessionLock = <T>(operation: () => Promise<T>): Promise<T> => {
    // 降级处理
    if (typeof navigator === "undefined" || !navigator.locks) {
        return operation();
    }
    return navigator.locks.request(
        AUTH_LOCK_NAME,
        {mode: "exclusive"},
        operation
    );
}

export const refreshInsideLock = async (): Promise<string> => {
    const state = useAuthStore.getState();
    const startAt = state.getVersion();

    try {
        const tokens = await requestAdminRefresh(state.getCsrfToken());
        publishSession(tokens.accessToken, tokens.csrfToken);
        return tokens.accessToken;
    } catch (e) {
        const failure = getApiFailure(e);

        if (failure.kind === "http" && failure.code === ErrorCode.AUTH_REFRESH_IN_PROGRESS) {
            const target = Math.max(startAt + 1, state.getSharedVersion());
            const peerToken = await waitForVersion(target, PEER_WAIT_MS);
            if (peerToken) return peerToken;
            throw e;
        }

        if (failure.kind === "http" && isTerminalSessionCode(failure.code)) {
            if (state.snapshot.status !== "anonymous") {
                publishLogout();
            }
        }

        throw e;
    }
}

export const ensureFreshToken = async (staleToken: string | null = null): Promise<string> => {
    // 已在 刷新中 直接共享
    if (inflight) return inflight;

    const startAt = useAuthStore.getState().getVersion();

    const task = withSessionLock(async () => {
        // 拿到锁之后先复查：排队期间 token 可能已经被换掉了
        const current = useAuthStore.getState().getAccessToken();
        if (current && current !== staleToken) {
            return current;
        }

        // 磁盘上的版本比进入时更新 -> 别的标签页刚改过会话
        const shared = useAuthStore.getState().getSharedVersion();
        if (shared > startAt) {
            const peerToken = await waitForVersion(shared, PEER_WAIT_MS);
            if (peerToken) return peerToken;
            throw new Error("Another tab changed the session. Please retry.");
        }

        // 新标签页，先问兄弟要现成的 token
        if (!current) {
            const peerToken = await requestPeerSession(PEER_PROBE_MS);
            if (peerToken) return peerToken;
        }


        return refreshInsideLock();
    });

    inflight = task;

    try {
        return await task;
    } finally {
        if (inflight === task) {
            inflight = null;
        }
    }
};