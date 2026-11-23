"use client"

import Link from "next/link";
import {buildLocalizedPath, type Dictionary, stripLanguagePrefix, type SupportedLanguage} from "@/i18n";
import {School} from "lucide-react";
import {
    NavigationMenu,
    NavigationMenuItem,
    NavigationMenuLink,
    NavigationMenuList,
    navigationMenuTriggerStyle
} from "@/components/ui/navigation-menu";
import {usePathname} from "next/navigation";
import {cn} from "cn";
import ThemeSwitcher from "@/components/common/theme-switcher";
import LanguageSwitcher from "@/components/common/language-switcher";

type NavKey = keyof Dictionary["site"]["nav"];

const NAV_ITEMS: ReadonlyArray<{ key: NavKey; path: string }> = [
    {key: "home", path: "/"},
    {key: "courses", path: "/"},
    {key: "news", path: "/"},
    {key: "about", path: "/"},
];

export default function SiteHeader({lang, dictionary}: { lang: SupportedLanguage; dictionary: Dictionary }) {
    const schoolName = "学校名字"
    const path = usePathname() ?? "/";
    const currentPath = stripLanguagePrefix(path);

    return (
        <header
            className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur-sm supports-backdrop-filter:bg-background/80">
            <div
                className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between px-4 sm:px-6">
                {/* Left logo and school name */}
                <Link
                    href={buildLocalizedPath(lang, NAV_ITEMS[0].path)}
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
                                    render={<Link href={buildLocalizedPath(lang, item.path)}/>}
                                    active={currentPath === item.path}
                                    aria-current={currentPath === item.path ? "page" : undefined}
                                    className={cn(navigationMenuTriggerStyle(), "text-sm")}
                                >
                                    {dictionary.site.nav[item.key]}
                                </NavigationMenuLink>
                            </NavigationMenuItem>
                        ))}
                    </NavigationMenuList>
                </NavigationMenu>

                {/* 操作区 */}
                <div className="ml-auto flex items-center gap-1 lg:ml-0 lg:justify-end">
                    <ThemeSwitcher dictionary={dictionary}/>
                    <LanguageSwitcher lang={lang} dictionary={dictionary}/>
                </div>
            </div>
        </header>
    )
}
