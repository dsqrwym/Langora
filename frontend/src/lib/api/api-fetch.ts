
import {ApiError, ApiResponse, InvalidResponseError} from "@/lib/api/api-types";
import {ErrorCode} from "@/lib/api/api-error-codes";
import {DEFAULT_TIMEOUT_MS, SUCCESS_CODES} from "@/lib/api/api-config";

export type ApiFetch = <T>(path: string, init?: RequestInit) => Promise<T | null>;

/**
 * 认证挂钩，只在 admin 浏览器直连的 transport 上启用
 */
export type ApiFetchAuth = {
    beforeRequest?: (init: RequestInit) => RequestInit;
    recoverSession?: (failedInit: RequestInit) => Promise<boolean>
}

/**
 * 缓存策略。Next 16 未开 cacheComponents：fetch 默认不缓存，
 * 任何缓存都必须显式 force-cache（buildCacheOptions 负责翻译）。
 * "no-store"：admin 浏览器直连 + 带用户身份的服务端请求。永不过缓存 （浏览器侧 = 禁浏览器 HTTP 缓存；服务端侧 = 禁 Next 数据缓存）
 * revalidateSeconds：定时重验证。保留给"非 CMS"的共享数据，不作为公网默认
 * {tags, revalidateSeconds?}：CMS 公网默认。force-cache 永久缓存，只有后端数据变更后 webhook 调 revalidateTag 才失效；
 * 可选 revalidateSeconds 是 "webhook 丢失" 兜底（如 86400 = 1 天）
 */
export type FetchCache =
    | "no-store"
    | { revalidateSeconds: number }
    | { tags: string[]; revalidateSeconds?: number };

export type CreateApiFetchOptions = {
    baseUrl: string;
    cache: FetchCache;
    credentials?: RequestCredentials;
    timeoutMs?: number;
    auth?: ApiFetchAuth;
}

const buildCacheOptions = (cache: FetchCache): RequestInit => {
    if (cache === "no-store") return {cache: "no-store"};

    if ("tags" in cache) {
        return {
            cache: "force-cache", // 永久缓存必须显示 opt-in Next 16 默认不缓存
            next: {
                tags: cache.tags, // 用 tag 触发 Webhook
                // revalidate 兜底，无论有没有触发都是这个时间重新编译一次
                ...(cache.revalidateSeconds ? {revalidate: cache.revalidateSeconds} : {})
            }
        }
    }

    return {cache: "force-cache", next: {revalidate: cache.revalidateSeconds}};
};

/** 把 调用方取消信号 和 超时信号 合成一个：谁先触发谁生效 */
const buildSignal = (init: RequestInit, timeoutMs: number): AbortSignal => {
    const timeout = AbortSignal.timeout(timeoutMs);
    if (!init.signal) return timeout;
    return typeof AbortSignal.any === "function" ? AbortSignal.any([init.signal, timeout]) : timeout;
}

const isApiResponse = (body: unknown): boolean =>
    body !== null &&
    typeof body === "object" &&
    "status" in body &&
    "code" in body &&
    "message" in body &&
    "data" in body;

/**
 * 网络失败（fetch 抛 TypeError/TimeoutError/AbortError）不经过这里，原样上抛交给 safeApiCall。
 */
async function parseResponse<T>(response: Response): Promise<T | null> {
    // 204 No Content：成功但无 body（后端 logout 就是这种响应）
    if (response.status === 204) return null;

    let body: unknown;
    try {
        body = await response.json();
    } catch (cause) {
        throw new InvalidResponseError("Response is not valid JSON", cause);
    }

    if (!isApiResponse(body)) {
        throw new InvalidResponseError("Response does not match ApiResponse");
    }

    const {code, traceId, data, message} = body as ApiResponse<T>;

    // 双检：HTTP 状态 + 后端业务 code，两个都成功才算成功
    if (!response.ok || !SUCCESS_CODES.has(code)) {
        throw new ApiError(
            response.status,
            code,
            typeof traceId === "string" ? traceId : null,
            data,
            message,
        );
    }

    return data as T;
}

/** 只有"Access Token 失效"这一种 401 才触发恢复；登录失败等其他 401 不能递归刷新 */
const isAccessTokenExpired = (cause: unknown): boolean =>
    cause instanceof ApiError &&
    cause.status === 401 &&
    cause.code === ErrorCode.AUTH_ACCESS_TOKEN_INVALID;


export function createApiFetch(
    {
        baseUrl,
        cache,
        credentials = "same-origin",
        timeoutMs = DEFAULT_TIMEOUT_MS,
        auth
    }: CreateApiFetchOptions
): ApiFetch {
    return async function apiFetch<T>(path: string, init = {}) {
        const url = new URL(path, baseUrl);

        const request = async (current: RequestInit): Promise<T | null> => {
            const headers = new Headers(current.headers);
            if (!headers.has("Accept")) {
                headers.set("Accept", "application/json");
            }
            return parseResponse<T>(await fetch(url, {
                ...buildCacheOptions(cache),
                ...current,          // current 在后：endpoint 可整体覆盖 transport 缓存策略
                signal: buildSignal(current, timeoutMs),
                credentials,
                headers,
            }));
        }

        // 第一次尝试（beforeRequest 挂 Authorization 等）
        const attempt = auth?.beforeRequest ? auth.beforeRequest(init) : init;

        try {
            return await request(attempt);
        } catch (e) {
            // 不满足恢复条件即所有非 401、以及登录失败这种别的 401
            if (!isAccessTokenExpired(e) || !auth?.recoverSession) throw e;

            // 恢复失败就抛原错误
            const recovered = await auth.recoverSession(attempt);
            if (!recovered) throw e;

            // 恢复成功 用新 token 重建 init 重放一次
            const replay = auth.beforeRequest ? auth.beforeRequest(init) : init;
            return await request(replay);
        }
    }
}