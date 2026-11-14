import {createApiFetch} from "@/lib/api/api-fetch";
import {ensureFreshToken, getAccessToken} from "@/features/auth";
import {API_BASE} from "@/lib/api/api-config";

export const adminApiFetch = createApiFetch({
    baseUrl: API_BASE,
    cache: "no-store",
    credentials: "include",
    auth: {
        beforeRequest(init) {
            const token = getAccessToken();
            if (!token) return init;
            const headers = new Headers(init.headers);
            headers.set("Authorization", `Bearer ${token}`);
            return {...init, headers};
        },
        async recoverSession(init) {
            try {
                const authorization = new Headers(init?.headers).get("Authorization");
                const match = authorization?.match(/^Bearer\s+(.+)$/i);
                const staleToken = match?.[1] ?? null;
                await ensureFreshToken(staleToken);
                return true;
            } catch {
                return false;
            }
        }
    }
});