import * as axios from "axios";
import type {ApiResponse} from "@/lib/api/types.ts";
import {ErrorCode} from "./error-codes";

export type ApiFailure = {
    kind: "http";
    status: number;
    code: string;
    message: string;
    traceId: string;
    data: unknown;
} | {
    kind: "network";
    message: string;
} | {
    kind: "unexpected";
    message: string;
};

export function getApiFailure(error: unknown): ApiFailure {
    if (axios.isAxiosError(error)) {
        if (error.response) {
            const body = error.response.data as Partial<ApiResponse<unknown>> | undefined;

            return {
                kind: "http",
                status: error.response.status,
                code: typeof body?.code === "string" ? body.code : "UNKNOWN",
                message: typeof body?.message === "string" ? body.message : "Request failed",
                traceId: typeof body?.traceId === "string" ? body.traceId : "UNKNOWN",
                data: body?.data ?? null,
            }
        }

        return {
            kind: "network",
            message: error.message,
        }
    }

    return {
        kind: "unexpected",
        message: error instanceof Error ? error.message : "Unknown error",
    }
}

const terminalSessionCodes = new Set<string>([
    ErrorCode.AUTH_REFRESH_TOKEN_MISSING,
    ErrorCode.AUTH_REFRESH_TOKEN_INVALID,
    ErrorCode.AUTH_SESSION_REVOKED,
    ErrorCode.AUTH_SESSION_EXPIRED,
    ErrorCode.AUTH_REFRESH_REUSED,
    ErrorCode.AUTH_CSRF_INVALID,
    ErrorCode.AUTH_ACCOUNT_DISABLED,
]);

export function isTerminalSessionCode(code: string): boolean {
    return terminalSessionCodes.has(code);
}