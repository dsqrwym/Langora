import "server-only";
import {createSiteEndpoints, type SiteEndpoint} from "@/features/site/api/site-endpoint";
import type {SupportedLanguage} from "@/i18n/routing";
import {getMockSiteShell} from "@/features/site/api/site-mock";
import {createSiteRepository} from "@/features/site/api/site-repository";
import {cache} from "react";
import {publicApiFetch} from "@/lib/api/public-api-fetch";

const mockEndpoints: SiteEndpoint = {
    async getSiteShell(locale: SupportedLanguage) {
        return getMockSiteShell(locale);
    },
};
const apiEndpoints = createSiteEndpoints(publicApiFetch);

const activeEndpoints = process.env.SITE_DATA_SOURCE === "api" ? apiEndpoints : mockEndpoints;

export const siteRepository = createSiteRepository(activeEndpoints);
export const getCachedSiteShell = cache((lang: SupportedLanguage)=>{
    return siteRepository.getSiteShellOrFallback(lang);
})