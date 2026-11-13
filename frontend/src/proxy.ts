import {type NextRequest, NextResponse} from "next/server";
import {
    DEFAULT_LANGUAGE,
    isSupportedLanguage,
    LANGUAGE_COOKIE_NAME,
    pickLanguageFromAcceptLanguage,
} from "@/i18n/languages";

/**
 * Next 16 用 proxy 取代 middleware 约定。
 */
export function proxy(request: NextRequest) {
    const {pathname} = request.nextUrl;
    // domain.com/en/some/path => pathname => /en/some/path => split => ["", "en", "some", "path"]
    const segments = pathname.split("/");
    const currentLang = segments[1] ?? "";

    if (isSupportedLanguage(currentLang)) return NextResponse.next();

    // without language

    // priority: cookie > accept-language > default
    const cookieLanguage = request.cookies.get(LANGUAGE_COOKIE_NAME)?.value;
    const language = isSupportedLanguage(cookieLanguage)
        ? cookieLanguage
        : pickLanguageFromAcceptLanguage(request.headers.get("accept-language")) ?? DEFAULT_LANGUAGE;

    const url = request.nextUrl.clone();
    url.pathname = `/${language}${pathname === "/" ? "" : pathname}`;

    // 用 301 会被浏览器永久缓存，用户之后切换语言再也回不来。
    return NextResponse.redirect(url, 307);
}

export const config = {
    // 排除下一层静态资源与带扩展名的文件避免对静态资源进行多语言拦截
    matcher: ["/((?!en(?:/|$)|es(?:/|$)|zh(?:/|$)|_next|.*\\..*).*)"],
};
