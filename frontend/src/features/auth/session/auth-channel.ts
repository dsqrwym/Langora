import {AuthMessage} from "@/features/auth/auth-types";
import {useAuthStore} from "@/features/auth/session/auth-store";


/** Auth BroadcastChannel */
export const AUTH_CHANNEL_NAME = "langora:auth";

/**
 * All pages with same origin will use this channel
 * SSR 阶段 BroadcastChannel 不存在，直接置 null 监听只在浏览器生效
 */
const authChannel = typeof BroadcastChannel === "undefined" ? null : new BroadcastChannel(AUTH_CHANNEL_NAME);

/**
 * Waiters for session request
 */
const waiters = new Map<string, (token: string | null) => void>();

/**
 * Check if the message is a valid auth message
 * @param value
 */
const isAuthMessage = (value: unknown): value is AuthMessage => {
    if (typeof value !== "object" || value === null) return false;
    const message = value as Record<string, unknown>;

    switch (message.type) {
        case "SESSION":
            return (
                typeof message.accessToken === "string" &&
                typeof message.csrfToken === "string" &&
                typeof message.version === "number"
            );
        case "SESSION_RESPONSE":
            return (
                typeof message.requestId === "string" &&
                typeof message.accessToken === "string" &&
                typeof message.csrfToken === "string" &&
                typeof message.version === "number"
            );
        case "LOGOUT":
            return typeof message.version === "number";
        case  "SESSION_REQUEST":
            return typeof message.requestId === "string";
        default:
            return false;
    }
};

/**
 *
 * @param message
 */
const handle = (message: AuthMessage) => {
    switch (message.type) {
        case "SESSION": {
            useAuthStore.getState().adoptSession(message.accessToken, message.csrfToken, message.version);
            return;
        }
        case "LOGOUT": {
            useAuthStore.getState().adoptLogout(message.version);
            return;
        }
        case "SESSION_REQUEST": {
            // 没有时直接不管
            const {getAccessToken, getCsrfToken, getVersion} = useAuthStore.getState();
            const accessToken = getAccessToken();
            const csrfToken = getCsrfToken();
            if (accessToken && csrfToken) {
                authChannel?.postMessage({
                    type: "SESSION_RESPONSE",
                    requestId: message.requestId,
                    accessToken,
                    csrfToken,
                    version: getVersion()
                }satisfies AuthMessage);
            }
            return;
        }
        case "SESSION_RESPONSE": {
            useAuthStore.getState().adoptSession(message.accessToken, message.csrfToken, message.version);
            const waiter = waiters.get(message.requestId);
            if (waiter) {
                waiters.delete(message.requestId);
                waiter(useAuthStore.getState().getAccessToken());
            }
            return;
        }
    }
};

/**
 * Handle auth messages
 */
authChannel?.addEventListener("message", (event: MessageEvent<unknown>) => {
    if (isAuthMessage(event.data)) handle(event.data);
});

/**
 * Publish session to other pages
 * @param accessToken
 * @param csrfToken
 */
export const publishSession = (accessToken: string, csrfToken: string) => {
    const version = useAuthStore.getState().commitSession(accessToken, csrfToken);
    authChannel?.postMessage({
        type: "SESSION",
        accessToken,
        csrfToken,
        version
    } satisfies AuthMessage);
};

/**
 * Publish logout to other pages
 */
export const publishLogout = () => {
    const version = useAuthStore.getState().commitLogout();
    authChannel?.postMessage({
        type: "LOGOUT",
        version
    } satisfies AuthMessage);
};

/**
 * Request session from other pages, 冷启动探查
 * @returns Promise<string | null>
 * @param timeoutMs 等待超时时间
 */
export const requestPeerSession = async (timeoutMs: number): Promise<string | null> => {
    const current = useAuthStore.getState().getAccessToken();
    if (current) return Promise.resolve(current);
    if (!authChannel) return Promise.resolve(null);

    const requestId = crypto.randomUUID();

    return new Promise<string | null>((resolve) => {
        const timer = setTimeout(() => {
            waiters.delete(requestId);
            resolve(null);
        }, timeoutMs);

        waiters.set(requestId, (token) => {
            clearTimeout(timer);
            resolve(token);
        });
        // 请求 token
        authChannel.postMessage({type: "SESSION_REQUEST", requestId} satisfies AuthMessage);
    });
};

export const waitForVersion = (minVersion: number, timeoutMs: number): Promise<string | null> => {
    const store = useAuthStore.getState();
    if (store.getVersion() >= minVersion) return Promise.resolve(store.getAccessToken());

    return new Promise<string | null>((resolve) => {
        let settled = false;
        let unsubscribe = () => {
        };
        let timer: NodeJS.Timeout;

        const finish = (token: string | null) => {
            if (settled) return;
            settled = true;
            unsubscribe();
            clearTimeout(timer);
            resolve(token);
        }

        unsubscribe = useAuthStore.subscribe(() => {
            const state = useAuthStore.getState();
            if (state.getVersion() >= minVersion) finish(state.getAccessToken());
        })

        timer = setTimeout(() => finish(null), timeoutMs);

        const current = useAuthStore.getState();
        // 双重检查：在 subscribe 和上一行之间，可能刚好发生过一次变更
        if (current.getVersion() >= minVersion) finish(current.getAccessToken());
    });
}
