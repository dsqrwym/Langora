"use client";

import {useCallback, useState} from "react";
import {EllipsisVertical, Menu} from "lucide-react";
import {cn} from "cn";
import {useTranslations} from "next-intl";
import {Link, usePathname} from "@/i18n/navigation";
import {Button} from "@/components/ui/button";
import {Separator} from "@/components/ui/separator";
import {navigationMenuTriggerStyle} from "@/components/ui/navigation-menu";
import {Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle, SheetTrigger,} from "@/components/ui/sheet";
import ThemeSwitcher from "@/components/common/theme-switcher";
import LanguageSwitcher from "@/components/common/language-switcher";
import {SiteLogoMark} from "@/components/site/site-logo-mark";
import type {SiteMediaImage} from "@/features/site/site-types";
import {isSiteNavActive, SITE_NAV_KEYS} from "@/features/site/site-navigations";

type SiteMobileNavProps = {
    schoolName: string;
    showHeaderSchoolName: boolean;
    logo: SiteMediaImage | null;
};

export function SiteMobileNav(
    {
        schoolName,
        showHeaderSchoolName,
        logo,
    }: SiteMobileNavProps
) {
    const tNav = useTranslations("site.nav");
    const tMenu = useTranslations("site.menu");
    const currentPath = usePathname() || "/";
    const [menuOpen, setMenuOpen] = useState(false);
    const [failedLogoUrl, setFailedLogoUrl] = useState<string | null>(null);

    const logoUrl = logo?.url?.trim() || null;
    const isLogoOnly = !showHeaderSchoolName && Boolean(logoUrl) && failedLogoUrl !== logoUrl;

    const handleCustomLogoError = useCallback((url: string) => {
        setFailedLogoUrl(url);
    }, []);

    return (
        <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
            <SheetTrigger
                render={
                    <Button
                        variant="ghost"
                        size="icon-lg"
                        className="relative md:hidden"
                        aria-label={menuOpen ? tMenu("close") : tMenu("open")}
                    />
                }
            >
                <Menu
                    aria-hidden="true"
                    className={cn(
                        "absolute inset-0 m-auto transition-all duration-300",
                        menuOpen
                            ? "rotate-90 scale-0 opacity-0"
                            : "rotate-0 scale-100 opacity-100",
                    )}
                />
                <EllipsisVertical
                    aria-hidden="true"
                    className={cn(
                        "absolute inset-0 m-auto transition-all duration-300",
                        menuOpen
                            ? "rotate-0 scale-100 opacity-100"
                            : "-rotate-90 scale-0 opacity-0",
                    )}
                />
            </SheetTrigger>

            <SheetContent
                side="left"
                className="w-[min(85vw,20rem)] **:data-[slot=sheet-close]:size-8 [&_[data-slot=sheet-close]_svg]:size-4!"
                showCloseButton={false}
            >
                <SheetHeader className="min-w-0">
                    <SheetTitle className="sr-only">{tMenu("mobile")}</SheetTitle>

                    <Link
                        href="/"
                        aria-label={isLogoOnly ? schoolName : undefined}
                        onClick={() => setMenuOpen(false)}
                        className="inline-flex min-w-0 items-center gap-2.5 rounded-md text-base font-bold tracking-tight text-foreground outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
                    >
                        <SiteLogoMark
                            logo={logo}
                            variant={isLogoOnly ? "drawer-wide" : "header-compact"}
                            onImageError={handleCustomLogoError}
                        />
                        {isLogoOnly ? (
                            <span className="sr-only">{schoolName}</span>
                        ) : (
                            <span className="min-w-0 line-clamp-3 leading-snug wrap-break-word">
                                {schoolName}
                            </span>
                        )}
                    </Link>

                    <Separator/>
                </SheetHeader>

                <nav
                    aria-label={tMenu("mobile")}
                    className="flex flex-1 flex-col overflow-y-auto px-4"
                >
                    <ul className="flex flex-col gap-1">
                        {SITE_NAV_KEYS.map((item) => {
                            const active = isSiteNavActive(item, currentPath);
                            return (
                                <li key={item.key}>
                                    <Link
                                        href={item.path}
                                        onClick={() => setMenuOpen(false)}
                                        aria-current={active ? "page" : undefined}
                                        className={cn(
                                            navigationMenuTriggerStyle(),
                                            "w-full justify-start px-2 text-sm",
                                            active && "bg-muted/70 text-foreground",
                                        )}
                                    >
                                        {tNav(item.key)}
                                    </Link>

                                </li>
                            );
                        })}
                    </ul>
                </nav>

                <SheetFooter>
                    <Separator/>
                    <div className="flex items-center gap-2">
                        <ThemeSwitcher/>
                        <LanguageSwitcher/>
                    </div>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}