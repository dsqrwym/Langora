import {getRequestConfig} from "next-intl/server";
import {hasLocale} from "next-intl";
import {routing} from "@/i18n/routing";
import {lang} from "next/root-params";

export default getRequestConfig(async ({locale: explicitLocale}) => {
    const requestedLang = explicitLocale ?? await lang();
    const locale = hasLocale(routing.locales, requestedLang) ? requestedLang : routing.defaultLocale;

    return {
        locale,
        messages: (await import(`@/i18n/messages/${locale}.json`)).default,
    }
});