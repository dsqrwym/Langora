import {createApiFetch} from "@/lib/api/api-fetch";
import {API_BASE} from "@/lib/api/api-config";
import {createAdminAuthRepository} from "@/features/auth/api/auth-repository";
import {createAdminAuthEndpoints} from "@/features/auth/api/auth-endpoint";

/**
 * 不挂 Authorization 钩子, 不挂 401 恢复钩子
 */
const authApiFetch = createApiFetch({
    baseUrl: API_BASE,
    cache: "no-store",
    credentials: "include"
});

export const authRepository = createAdminAuthRepository(createAdminAuthEndpoints(authApiFetch));