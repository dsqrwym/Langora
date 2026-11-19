import {LoginForm} from "@/features/auth/components/login-form"
import {UserRoundShield} from "lucide-react"
import {getDictionary, type SupportedLanguage} from "@/i18n";
import {RedirectIfAuthed} from "@/features/auth/components/redirect-if-authed";
import {
    buildLoginPhotos,
    buildLoginPhotoScript,
    LOGIN_PHOTO_ID,
    LOGIN_QUOTE_BODY_ROLE
} from "@/features/auth/login-photo-script";
import {InlineScript} from "@/components/common/inline-script";
import {type Metadata} from "next";

export async function generateMetadata({params}: PageProps<'/[lang]/admin/login'>): Promise<Metadata> {
    const {lang} = await params;
    const dictionary = await getDictionary(lang as SupportedLanguage);

    return {title: dictionary.auth.login};
}

export default async function AdminLoginPage({params}: PageProps<'/[lang]/admin/login'>) {
    const {lang} = await params;
    const dictionary = await getDictionary(lang as SupportedLanguage);
    const photos = buildLoginPhotos(dictionary);
    // Only quoted photos get a card; the first is the SSR baseline. The inline script swaps visibility to the randomly picked photo before first paint.
    /* 有文案的照片才渲染引文卡片；首帧基准取第一张，脚本执行后切换到随机选中的那张 */
    const quoted = photos.filter((photo) => photo.quote);
    const fallback = quoted[0] ?? photos[0];

    return (
        <div className="grid min-h-svh lg:grid-cols-2">
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
                <div
                    className="pointer-events-none absolute inset-0 bg-linear-to-t from-black/45 via-black/10 to-transparent"/>
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
            <div
                className="pointer-events-none absolute inset-y-0 right-0 w-32 bg-linear-to-l from-background via-background/60 to-transparent"
            />
            <div className="flex flex-col gap-4 p-6 md:p-10">
                <div className="flex justify-center gap-2 md:justify-start">
                    <a href="../admin" className="flex items-center gap-2 font-medium">
                        <div
                            className="flex size-6 items-center justify-center rounded-md bg-primary text-primary-foreground">
                            <UserRoundShield className="size-4"/>
                        </div>
                        Langora Admin
                    </a>
                </div>
                <div className="flex flex-1 items-center justify-center">
                    <div className="w-full max-w-xs">
                        <RedirectIfAuthed>
                            <LoginForm dictionary={dictionary}/>
                        </RedirectIfAuthed>
                    </div>
                </div>
            </div>
        </div>
    )
}
