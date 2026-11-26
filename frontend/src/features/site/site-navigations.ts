//import type zhDJson from "@/i18n/messages/zh.d.json";

//export type SiteNavKey = keyof typeof zhDJson.site.nav;
export type SiteNavKey = "about" | "courses" | "home" | "news";

export type SiteNavItem = { key: SiteNavKey, path: string }
export const SITE_NAV_KEYS: SiteNavItem[] = [
    {key: "home", path: "/"},
    {key: "about", path: "/about"},
    {key: "courses", path: "/courses"},
    {key: "news", path: "/news"},
];

export const isSiteNavActive = (nav: SiteNavItem, currentPath: string) => {
    const cleanCurrent = currentPath.split(/[?#]/)[0].replace(/\/+$/, "") || "/";
    const cleanNav = nav.path.replace(/\/+$/, "") || "/";

    if (cleanNav === "/") {
        return cleanCurrent === "/";
    }

    return cleanCurrent === cleanNav || cleanCurrent.startsWith(`${cleanNav}/`);
}
