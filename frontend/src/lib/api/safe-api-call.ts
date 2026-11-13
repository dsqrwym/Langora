import {
    ApiError,
    ApiResult,
    AppError,
    InvalidResponseError,
    NetworkError,
    RequestTimeoutError,
    UnexpectedError
} from "@/lib/api/api-types";

const isAppError = (cause: unknown): cause is AppError => {
    return cause instanceof ApiError || cause instanceof NetworkError || cause instanceof RequestTimeoutError || cause instanceof InvalidResponseError || cause instanceof UnexpectedError;
};

const normalizeError = (cause: unknown): AppError => {
    if (isAppError(cause)) return cause;

    // 超时信号触发的是 TimeoutError
    if (cause instanceof DOMException && cause.name === "TimeoutError") {
        return new RequestTimeoutError(cause);
    }

    // fetch 网络失败抛出 TypeError
    if (cause instanceof TypeError) {
        return new NetworkError(cause);
    }

    return new UnexpectedError(cause);
}

export const safeApiCall = async <T>(request: () => Promise<T>): Promise<ApiResult<T>> => {
    try {
        return {success: true, data: await request()};
    } catch (e) {
        // 主动取消（CancellationException / AbortError）继续抛出，交给发起方自己处理。
        if (e instanceof DOMException && e.name === "AbortError") {
            throw e;
        }
        return {success: false, error: normalizeError(e)};
    }
};
