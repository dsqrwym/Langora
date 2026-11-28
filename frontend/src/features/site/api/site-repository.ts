import type {SiteShellData} from "@/features/site/site-types";
import type {SiteEndpoint} from "@/features/site/api/site-endpoint";
import type {SupportedLanguage} from "@/i18n/routing";
import {safeApiCall} from "@/lib/api/safe-api-call";
import {InvalidResponseError} from "@/lib/api/api-types";

const FALLBACK_SITE_SHELL: SiteShellData = {
    schoolName: "Langora School",
    showHeaderSchoolName: true,
    logo: {
        url: "/langora.svg",
        alt: "Logo",
        width: 1256,
        height: 1204
    },
    tagline: "Placeholder school tagline for layout and typography preview.",
    footerText: `© ${new Date().getFullYear()} Demo School · Demo placeholder copyright`,
    contact: {
        email: "contact@demo-school.example",
        phone: "+00 000 000 000",
        addressLine1: "Demo Street 00",
        addressLine2: "Suite 00 (Demo Building)",
        city: "Demo Location",
        stateProvince: "Demo Region",
        postalCode: "00000",
        countryCode: "XX",
        timezone: "UTC",
    }
}

export function createSiteRepository(endpoint: SiteEndpoint) {
    return {
        getSiteShell(lang: SupportedLanguage) {
            return safeApiCall(async () => {
                const data = await endpoint.getSiteShell(lang);
                if (!data) {
                    throw new InvalidResponseError("Site shell response has no data");
                }
                return data;
            });
        },
        async getSiteShellOrFallback(lang: SupportedLanguage): Promise<SiteShellData> {
            const result = await this.getSiteShell(lang);
            if (result.success) {
                return result.data;
            }
            console.error("[siteRepository.getSiteShellOrFallback]", result.error);
            return FALLBACK_SITE_SHELL;
        },
    }
}
