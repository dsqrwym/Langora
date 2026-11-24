import {ApiError, type AppError, InvalidResponseError, NetworkError, RequestTimeoutError} from "@/lib/api/api-types";
import {ErrorCode} from "@/lib/api/api-error-codes";
import type {useTranslations} from "next-intl";

export type CommonErrorTranslator = ReturnType<typeof useTranslations<"common.errors">>;

/**
 * 获取通用错误字符串 如果为空则不是通用错误
 */
export const getCommonErrorString = (error: AppError, t: CommonErrorTranslator): string => {
    if (error instanceof NetworkError) return t("network");
    if (error instanceof RequestTimeoutError) return t("timeout");
    if (error instanceof InvalidResponseError) return t("invalidResponse");

    if (error instanceof ApiError) {
        switch (error.code) {
            case ErrorCode.RATE_LIMITED:
                return t("rateLimited");
            case ErrorCode.ACCESS_DENIED:
                return t("accessDenied");
            case ErrorCode.AUTH_ACCOUNT_DISABLED:
                return t("accountDisabled");

            case ErrorCode.AUTH_ACCESS_TOKEN_INVALID:
            case ErrorCode.AUTH_REFRESH_TOKEN_MISSING:
            case ErrorCode.AUTH_REFRESH_TOKEN_INVALID:
            case ErrorCode.AUTH_SESSION_REVOKED:
            case ErrorCode.AUTH_SESSION_EXPIRED:
            case ErrorCode.AUTH_REFRESH_REUSED:
            case ErrorCode.AUTH_CSRF_INVALID:
                return t("sessionExpired");

            // 前后端契约不一致
            case ErrorCode.VALIDATION_FAILED:
            case ErrorCode.MALFORMED_REQUEST:
            case ErrorCode.UNSUPPORTED_MEDIA_TYPE:
            case ErrorCode.METHOD_NOT_ALLOWED:
            case ErrorCode.NOT_ACCEPTABLE:
                return t("validationFailed");

            case ErrorCode.INTERNAL_ERROR:
                return t("server");

            default:
                return t("requestFailed");
        }
    }

    return t("unexpected");
};