import {jwtDecode} from "jwt-decode";
import type {AuthUser} from "@/lib/auth/auth-types.ts";

export const decodeFromAccessToken = (accessToken: string) => {
    return jwtDecode<AuthUser>(accessToken);
}