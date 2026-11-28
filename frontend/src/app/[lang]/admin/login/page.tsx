import {LoginForm} from "@/features/auth/components/login-form"
import {UserRoundShield} from "lucide-react"
import {RedirectIfAuthed} from "@/features/auth/components/redirect-if-authed";
import {
    buildLoginPhotos,
    buildLoginPhotoScript,
    LOGIN_PHOTO_ID,
    LOGIN_QUOTE_BODY_ROLE
} from "@/features/auth/login-photo-script";
import {InlineScript} from "@/components/common/inline-script";
import {type Metadata} from "next";
import {getMessages, getTranslations} from "next-intl/server";
import {Link} from "@/i18n/navigation";

export async function generateMetadata(): Promise<Metadata> {
    const t = await getTranslations("auth");

    return {title: t("login")};
}

export default async function AdminLoginPage() {
    const messages = await getMessages();
    const photos = buildLoginPhotos(messages.auth.loginQuotes);
    // Only quoted photos get a card; the first is the SSR baseline. The inline script swaps visibility to the randomly picked photo before first paint.
    /* 有文案的照片才渲染引文卡片；首帧基准取第一张，脚本执行后切换到随机选中的那张 */
    const quoted = photos.filter((photo) => photo.quote);
    const fallback = quoted[0] ?? photos[0];

    return (
        <div className="grid min-h-svh flex-1 lg:grid-cols-2">
            <aside>
                <figure className="relative hidden min-h-svh overflow-hidden bg-muted lg:block">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                        id={LOGIN_PHOTO_ID}
                        src={fallback?.src}
                        alt={fallback?.alt ?? ""}
                        fetchPriority="high" // "In most cases, you should use loading="eager" or fetchPriority="high" instead of preload." from next document
                        suppressHydrationWarning
                        className="absolute inset-0 size-full object-cover dark:brightness-[0.55] dark:saturate-[0.9]"
                    />
                    {/* 轻度压暗：玻璃板本身已经可读，这层只让照片下半部不刺眼 */}
                    <span
                        aria-hidden="true"
                        className="pointer-events-none absolute inset-0 bg-linear-to-t from-black/45 via-black/10 to-transparent"/>
                    <span
                        aria-hidden="true"
                        className="pointer-events-none absolute inset-y-0 right-0 w-2 bg-linear-to-l from-background via-background/80 to-transparent"
                    />
                    <figcaption
                        className="absolute inset-x-6 bottom-6"
                    >
                        {/* 引文卡片全部用 JSX 正常渲染，服务端与客户端结构一致；首帧脚本只切换 hidden 属性（属性差异可被 suppress），不增删节点 */}
                        {quoted.map((photo, index) => (
                            photo.quote && (
                                <blockquote
                                    key={photo.id}
                                    hidden={index !== 0}
                                    data-role={LOGIN_QUOTE_BODY_ROLE}
                                    suppressHydrationWarning
                                    className="max-w-md w-fit rounded-xl border border-white/15 bg-black/30 p-5 text-white backdrop-blur-xs">
                                    <p className="font-serif text-xl leading-relaxed tracking-[0.06em] text-balance">
                                        {photo.quote.original}
                                    </p>
                                    {photo.quote.translation && (
                                        <p className="mt-2 text-sm leading-relaxed text-white/70">{photo.quote.translation}</p>
                                    )}
                                    <cite className="mt-3 block text-xs not-italic tracking-wide text-white/50">
                                        {photo.quote.source}
                                    </cite>
                                </blockquote>
                            )
                        ))}
                    </figcaption>
                    <InlineScript id="login-photo-script" html={buildLoginPhotoScript(photos)}/>
                </figure>
            </aside>
            <main className="flex flex-col gap-4 p-6 md:p-10">
                <header className="flex justify-center gap-2 md:justify-start">
                    <Link href="/admin" className="flex items-center gap-2 font-medium">
                        <div
                            className="flex size-6 items-center justify-center rounded-md bg-primary text-primary-foreground">
                            <UserRoundShield className="size-4"/>
                        </div>
                        Langora Admin
                    </Link>
                </header>
                <section className="flex flex-1 items-center justify-center">
                    <div className="w-full max-w-xs">
                        <RedirectIfAuthed>
                            <LoginForm/>
                        </RedirectIfAuthed>
                    </div>
                </section>
            </main>
        </div>
    )
}
