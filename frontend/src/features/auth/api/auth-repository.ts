import {AuthEndpoints} from "@/features/auth/api/auth-endpoint";
import {safeApiCall} from "@/lib/api/safe-api-call";
import {getOrCreateDeviceUuid} from "@/lib/device";
import {InvalidResponseError} from "@/lib/api/api-types";

export const createAdminAuthRepository = (endpoints: AuthEndpoints) => {
    return {
        login(identifier: string, password: string, captchaToken?: string) {
            return safeApiCall(async () => {
                const data = await endpoints.login({
                    password,
                    deviceUuid: getOrCreateDeviceUuid(),
                    ...(identifier.includes("@")
                        ? {email: identifier}
                        : {username: identifier}),
                    ...(captchaToken ? {captchaToken} : {}),
                });

                if (!data) {
                    throw new InvalidResponseError("Login response has no data");
                }

                return data;
            });
        },

        refresh(csrfToken: string | null) {
            return safeApiCall(async () => {
                const data = await endpoints.refresh(csrfToken);
                if (!data) {
                    throw new InvalidResponseError("Refresh response has no data");
                }
                return data;
            });
        },
        logout(accessToken: string) {
            return safeApiCall(() => endpoints.logout(accessToken));
        },
        captchaConfig() {
            return safeApiCall(async () => {
                const data = await endpoints.captchaConfig();
                if (!data) {
                    throw new InvalidResponseError("Captcha config response has no data");
                }
                return data;
            });
        },
    }
}

export type AuthRepository = ReturnType<typeof createAdminAuthRepository>;
