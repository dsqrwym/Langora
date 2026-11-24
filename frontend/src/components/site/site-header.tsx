"use client";

import {School} from "lucide-react";
import {cn} from "cn";
import {useTranslations} from "next-intl";
import {
    NavigationMenu,
    NavigationMenuItem,
    NavigationMenuLink,
    NavigationMenuList,
    navigationMenuTriggerStyle,
} from "@/components/ui/navigation-menu";
import ThemeSwitcher from "@/components/common/theme-switcher";
import LanguageSwitcher from "@/components/common/language-switcher";
import {Link, usePathname} from "@/i18n/navigation";

type NavKey = "home" | "courses" | "news" | "about";

const NAV_ITEMS: ReadonlyArray<{ key: NavKey; path: string }> = [
    {key: "home", path: "/"},
    {key: "courses", path: "/"},
    {key: "news", path: "/"},
    {key: "about", path: "/"},
];

export default function SiteHeader() {
    const schoolName = "学校名字";
    const t = useTranslations("site.nav");
    // next-intl 的 usePathname() 已经自动剥离了 /{lang} 前缀
    const currentPath = usePathname() || "/";

    return (
        <header
            className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur-sm supports-backdrop-filter:bg-background/80">
            <div
                className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between px-4 sm:px-6">
                {/* Left logo and school name */}
                <Link
                    href={NAV_ITEMS[0].path}
                    className="font-bold text-lg flex items-center gap-2"
                >
                    <School className="size-4" aria-hidden="true"/>
                    {schoolName}
                </Link>

                {/* Navigation Desktop*/}
                <NavigationMenu className="">
                    <NavigationMenuList>
                        {NAV_ITEMS.map((item) => (
                            <NavigationMenuItem key={item.key}>
                                <NavigationMenuLink
                                    render={<Link href={item.path}/>}
                                    active={currentPath === item.path}
                                    aria-current={currentPath === item.path ? "page" : undefined}
                                    className={cn(navigationMenuTriggerStyle(), "text-sm")}
                                >
                                    {t(item.key)}
                                </NavigationMenuLink>
                            </NavigationMenuItem>
                        ))}
                    </NavigationMenuList>
                </NavigationMenu>

                {/* 操作区 */}
                <div className="flex items-center lg:justify-end">
                    <ThemeSwitcher/>
                    <LanguageSwitcher/>
                </div>
            </div>
        </header>
    );
}