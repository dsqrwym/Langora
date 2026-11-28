export const PUBLIC_CACHE_TAGS = {
    ALL: "public",
    SITE: "site",
    SITE_LANG: (lang: string) => `${PUBLIC_CACHE_TAGS.SITE}:${lang}`,
    COURSES: "courses",
    NEWS: "news",
    ABOUT: "about",
    PAGES: "pages"
} as const;