import Link from "next/link";
import {notFound} from "next/navigation";
import {LANGUAGE_LABELS, SUPPORTED_LANGUAGES, isSupportedLanguage} from "@/i18n/languages";
import {buildLocalizedPath} from "@/i18n/paths";

type HomePageProps = {
    params: Promise<{lang: string}>;
};

export default async function HomePage({params}: HomePageProps) {
    const {lang} = await params;

    if (!isSupportedLanguage(lang)) {
        notFound();
    }

    return (
        <main className="mx-auto flex min-h-svh w-full max-w-3xl flex-col justify-center gap-10 px-6 py-16">
            <header className="space-y-3">
                <p className="font-mono text-[10px] tracking-[0.3em] text-muted-foreground uppercase">
                    Langora
                </p>
                <h1 className="text-4xl font-semibold tracking-tight text-balance sm:text-5xl">
                    Next.js migration scaffold
                </h1>
                <p className="max-w-xl text-sm leading-relaxed text-muted-foreground">
                    公网页面走 RSC / SSG，admin 子树保持客户端 SPA 语义。
                    当前是骨架页，用于验证路由、i18n 与主题是否正确。
                </p>
            </header>

            <section className="space-y-3">
                <h2 className="font-mono text-[10px] tracking-[0.2em] text-muted-foreground uppercase">
                    Language
                </h2>
                <div className="flex flex-wrap gap-2">
                    {SUPPORTED_LANGUAGES.map((language) => (
                        <Link
                            key={language}
                            href={buildLocalizedPath(language, "/")}
                            aria-current={language === lang ? "page" : undefined}
                            className="rounded-full border border-border px-3.5 py-1.5 text-xs font-medium transition-colors hover:bg-muted aria-[current=page]:bg-primary aria-[current=page]:text-primary-foreground"
                        >
                            {LANGUAGE_LABELS[language]}
                        </Link>
                    ))}
                </div>
            </section>

            <nav className="flex flex-wrap items-center gap-3 text-xs">
                <Link
                    href={buildLocalizedPath(lang, "/admin")}
                    className="rounded-md border border-border px-3 py-1.5 font-medium transition-colors hover:bg-muted"
                >
                    Admin
                </Link>
                <Link
                    href={buildLocalizedPath(lang, "/missing-page")}
                    className="rounded-md border border-border px-3 py-1.5 font-medium transition-colors hover:bg-muted"
                >
                    404 demo
                </Link>
            </nav>
        </main>
    );
}
