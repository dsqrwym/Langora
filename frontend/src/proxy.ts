import createMiddleware from "next-intl/middleware";
import {LANGUAGE_COOKIE_NAME, routing, SUPPORTED_LANGUAGES} from "@/i18n/routing";
import {hasLocale} from "next-intl";
import {type NextRequest, NextResponse} from "next/server";


const handleI18nRouting = createMiddleware(routing);

/**
 * Next 16 用 proxy 取代 middleware 约定。
 */
export function proxy(request: NextRequest) {
    // priority: url > cookie > accept-language > default
    const {pathname} = request.nextUrl;
    // domain.com/en/some/path => pathname => /en/some/path => split => ["", "en", "some", "path"]
    const segments = pathname.split("/");
    const currentLang = segments[1]?.toLocaleLowerCase() ?? "";

    if (hasLocale(SUPPORTED_LANGUAGES, currentLang)) return handleI18nRouting(request);

    // without language in url
    const cookieLanguage = request.cookies.get(LANGUAGE_COOKIE_NAME)?.value;
    if (hasLocale(SUPPORTED_LANGUAGES, cookieLanguage)) {
        const url = request.nextUrl.clone();
        url.pathname = `/${cookieLanguage}${pathname === "/" ? "" : pathname}`;
        // 用 301 会被浏览器永久缓存，用户之后切换语言再也回不来。
        return NextResponse.redirect(url, 307);
    }
    // next-intl will handle the Accept-Language > default language 307
    return handleI18nRouting(request);
}

export const config = {
    // 必须使用纯静态字符串字面量，不能引用变量或模板插值；新增语言时需同步更新此处
    matcher: ["/((?!api|_vercel|_next|en(?:/|$)|es(?:/|$)|zh(?:/|$)|.*\\..*).*)"],
};