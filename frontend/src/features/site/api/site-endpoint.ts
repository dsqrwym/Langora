import type {ApiFetch} from "@/lib/api/api-fetch";
import type {SupportedLanguage} from "@/i18n/routing";
import type {SiteShellData} from "@/features/site/site-types";
import {PUBLIC_CACHE_TAGS} from "@/lib/api/api-constants";

export function createSiteEndpoints(apiFetch: ApiFetch) {
    return {
        getSiteShell(lang: SupportedLanguage) {
            const param = new URLSearchParams({lang});
            return apiFetch<SiteShellData>(`public/site?${param.toString()}`, {
                next: {
                    tags: [PUBLIC_CACHE_TAGS.SITE, PUBLIC_CACHE_TAGS.SITE_LANG(lang)]
                }
            })
        }
    }
}

export type SiteEndpoint = ReturnType<typeof createSiteEndpoints>;
