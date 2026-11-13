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
        async recoverSession() {
            try {
                await ensureFreshToken();
                return true;
            } catch {
                return false;
            }
        }
    }
});