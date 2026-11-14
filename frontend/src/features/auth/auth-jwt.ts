import {jwtDecode} from "jwt-decode";
import {AuthUser} from "@/features/auth/auth-types";

type AccessTokenPayload = AuthUser & {
    exp?: number;
    iat?: number;
};

/** 提前认为过期，避免请求发出途中刚好越过 exp */
const EXPIRY_SKEW_MS = 5_000;

export const decodeFromAccessToken = (accessToken: string) => {
    return jwtDecode<AccessTokenPayload>(accessToken);
}

/**
 * 只用于判断内存/同伴提供的 token 是否值得复用。
 */
export const isAccessTokenUsable = (
    token: string | null,
    now = Date.now()
): boolean => {
    if (!token) return false;

    try {
        const {exp} = decodeFromAccessToken(token);
        return (
            typeof exp === "number" &&
            exp * 1000 > now + EXPIRY_SKEW_MS
        );
    } catch {
        return false;
    }
}