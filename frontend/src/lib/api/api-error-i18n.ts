import {ApiError, AppError, InvalidResponseError, NetworkError, RequestTimeoutError} from "@/lib/api/api-types";
import {ErrorCode} from "@/lib/api/api-error-codes";
import {Dictionary} from "@/i18n";

/**
 * 获取通用错误字符串 如果为空则不是通用错误
 * @param error
 * @param dictionary
 */
export const getCommonErrorString = (error: AppError, dictionary: Dictionary): string => {
    const errors = dictionary.common.errors;

    if (error instanceof NetworkError) return errors.network;
    if (error instanceof RequestTimeoutError) return errors.timeout;
    if (error instanceof InvalidResponseError) return errors.invalidResponse;

    if (error instanceof ApiError) {
        switch (error.code) {
            case ErrorCode.RATE_LIMITED:
                return errors.rateLimited;
            case ErrorCode.ACCESS_DENIED:
                return errors.accessDenied;
            case ErrorCode.AUTH_ACCOUNT_DISABLED:
                return errors.accountDisabled;

            case ErrorCode.AUTH_ACCESS_TOKEN_INVALID:
            case ErrorCode.AUTH_REFRESH_TOKEN_MISSING:
            case ErrorCode.AUTH_REFRESH_TOKEN_INVALID:
            case ErrorCode.AUTH_SESSION_REVOKED:
            case ErrorCode.AUTH_SESSION_EXPIRED:
            case ErrorCode.AUTH_REFRESH_REUSED:
            case ErrorCode.AUTH_CSRF_INVALID:
                return errors.sessionExpired;

            // 前后端契约不一致
            case ErrorCode.VALIDATION_FAILED:
            case ErrorCode.MALFORMED_REQUEST:
            case ErrorCode.UNSUPPORTED_MEDIA_TYPE:
            case ErrorCode.METHOD_NOT_ALLOWED:
            case ErrorCode.NOT_ACCEPTABLE:
                return errors.validationFailed;

            case ErrorCode.INTERNAL_ERROR:
                return errors.server;

            default:
                return errors.requestFailed;
        }
    }

    return errors.unexpected;
};