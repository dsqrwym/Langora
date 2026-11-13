export type ApiResponse<T> = {
    status: number;
    code: string;
    message: string;
    traceId: string | null;
    data: T | null;
}

/** Represents an API error with http status code not 2xx. */
export class ApiError extends Error {
    readonly kind = "api";

    constructor(
        readonly status: number,
        readonly code: string,
        readonly traceId: string | null,
        readonly data: unknown,
        message: string,
    ) {
        super(message);
        this.name = "ApiError";
    }
}

/** Represents a network error that prevents the request from being sent. */
export class NetworkError extends Error {
    readonly kind = "network";

    constructor(cause: unknown) {
        super("Network request failed", {cause});
        this.name = "NetworkError";
    }
}

export class RequestTimeoutError extends Error {
    readonly kind = "timeout";

    constructor(cause?: unknown) {
        super("Request timed out", {cause});
        this.name = "RequestTimeoutError";
    }
}

/** Represents an invalid API response. */
export class InvalidResponseError extends Error {
    readonly kind = "invalid-response";

    constructor(message: string, cause?: unknown) {
        super(message, {cause});
        this.name = "InvalidResponseError";
    }
}

export class UnexpectedError extends Error {
    readonly kind = "unexpected";

    constructor(cause: unknown) {
        super("Unexpected error", {cause});
        this.name = "UnexpectedError";
    }
}

export type AppError = ApiError | NetworkError | RequestTimeoutError | InvalidResponseError | UnexpectedError;
export type ApiResult<T> = { success: true, data: T } | { success: false, error: AppError };
