import {jwtDecode} from "jwt-decode";
import {AuthUser} from "@/features/auth/auth-types";

export const decodeFromAccessToken = (accessToken: string) => {
    return jwtDecode<AuthUser>(accessToken);
}