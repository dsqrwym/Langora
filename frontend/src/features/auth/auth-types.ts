export type AuthStatus = "checking" | "authenticated" | "anonymous";

export type AuthUser = {
    userId: number;
    username: string;
    email: string;
    userTypes: string;
    roles: string[];
    sessionId: string;
}

export type AuthSnapshot = {
    status: AuthStatus;
    user: AuthUser | null;
}

/**
 * 跨标签共享会话数据信封
 */
export type SessionEnvelop = {
    /** 会话内容版本号：本地产生新会话时 +1，采纳远端消息时不 +1 */
    version: number;
    csrfToken: string | null;
}

export type AuthMessage =
    | { type: "SESSION"; accessToken: string; csrfToken: string; version: number }
    | { type: "LOGOUT"; version: number }
    | { type: "SESSION_REQUEST"; requestId: string }
    | {
    type: "SESSION_RESPONSE";
    requestId: string;
    accessToken: string;
    csrfToken: string;
    version: number;
};

export type AuthState = {
    snapshot: AuthSnapshot;
    accessToken: string | null;
    csrfToken: string | null;
    version: number;

    beginCheck: () => void;
    settleAnonymous: () => void;
    commitSession: (nextToken: string, nextCsrf: string) => number;
    commitLogout: () => number;
    adoptSession: (nextToken: string, nextCsrf: string, peerVersion: number) => void;
    adoptLogout: (peerVersion: number) => void;

    getAccessToken: () => string | null;
    getCsrfToken: () => string | null;
    getVersion: () => number;
    getSharedVersion: () => number;
}

export type LoginRequest = {
    email?: string;
    username?: string;
    password: string;
    deviceUuid: string;
    captchaToken?: string;
};

export type LoginResponse = {
    accessToken: string;
    csrfToken: string;
    userId: number;
    username: string;
    email: string;
    userTypes: string;
    roles: string[];
    sessionId: string;
};

export type RefreshTokenResponse = {
    accessToken: string;
    csrfToken: string;
};

/** GET /auth/captcha/config */
export type CaptchaConfigResponse = {
    enabled: boolean;
    provider: string;
    siteKey: string | null;
    action: string;
};