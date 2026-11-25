"use client";

import {EllipsisVertical, Menu, School} from "lucide-react";
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
import {Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle, SheetTrigger} from "@/components/ui/sheet";
import {useState} from "react";
import {Button} from "@/components/ui/button";
import {Separator} from "@/components/ui/separator";

type NavKey = "home" | "courses" | "news" | "about";

const NAV_ITEMS: ReadonlyArray<{ key: NavKey; path: string }> = [
    {key: "home", path: "/"},
    {key: "courses", path: "/"},
    {key: "news", path: "/"},
    {key: "about", path: "/"},
];

function SchoolLogo({schoolName, className}: { schoolName: string; className?: string }) {
    return <Link
        href={NAV_ITEMS[0].path}
        className={cn("font-bold text-lg flex items-center gap-2", className)}
    >
        <School className="size-6 shrink-0" aria-hidden="true"/>
        <span className="truncate">{schoolName}</span>
    </Link>;
}

export default function SiteHeader({schoolName}: { schoolName: string }) {
    const tNav = useTranslations("site.nav");
    const tMenu = useTranslations("site.menu");
    // next-intl 的 usePathname() 已经自动剥离了 /{lang} 前缀
    const currentPath = usePathname() || "/";
    const [menuOpen, setMenuOpen] = useState(false);

    return (
        <header
            className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur-sm supports-backdrop-filter:bg-background/80">
            <div
                className="mx-auto flex h-16 w-full items-center justify-between md:justify-around max-md:px-6 max-sm:px-4">
                {/* Left logo and school name */}
                <SchoolLogo schoolName={schoolName}/>

                {/* Navigation Desktop*/}
                <NavigationMenu className="max-md:hidden" aria-label={tMenu("main")}>
                    <NavigationMenuList>
                        {NAV_ITEMS.map((item) => (
                            <NavigationMenuItem key={item.key}>
                                <NavigationMenuLink
                                    render={<Link href={item.path}/>}
                                    active={currentPath === item.path}
                                    aria-current={currentPath === item.path ? "page" : undefined}
                                    className={cn(navigationMenuTriggerStyle(), "text-sm")}
                                >
                                    {tNav(item.key)}
                                </NavigationMenuLink>
                            </NavigationMenuItem>
                        ))}
                    </NavigationMenuList>
                </NavigationMenu>

                {/* 操作区 */}
                <div className="flex items-center lg:justify-end">
                    <ThemeSwitcher/>
                    <LanguageSwitcher/>
                    {/* Mobile Navigation Trigger */}

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
                                    menuOpen ? "rotate-90 scale-0 opacity-0" : "rotate-0 scale-100 opacity-100",
                                )}
                            />
                            <EllipsisVertical
                                aria-hidden="true"
                                className={cn(
                                    "absolute inset-0 m-auto transition-all duration-300",
                                    menuOpen ? "rotate-0 scale-100 opacity-100" : "-rotate-90 scale-0 opacity-0",
                                )}
                            />
                        </SheetTrigger>

                        <SheetContent
                            side="left"
                            // 内置关闭按钮 icon-sm → icon-lg：size-6/图标12px 改成 size-8/图标16px
                            className="w-fit! **:data-[slot=sheet-close]:size-8 [&_[data-slot=sheet-close]_svg]:size-4!"
                            showCloseButton={false}
                        >
                            <SheetHeader>
                                {/* 标题仅作为 dialog 的可访问名称，视觉上仍展示 logo */}
                                <SheetTitle className="sr-only">{tMenu("mobile")}</SheetTitle>
                                <SchoolLogo schoolName={schoolName}/>
                                <Separator/>
                            </SheetHeader>

                            {/* px-4(16) + Link px-2(8) = 24，与 SheetHeader 和 SheetFooter 的 p-6 对齐 */}
                            <nav
                                aria-label={tMenu("mobile")}
                                className="flex flex-1 flex-col overflow-y-auto px-4"
                            >
                                <ul className="flex flex-col gap-1">
                                    {NAV_ITEMS.map((item, index) => (
                                        <li key={item.key}>
                                            <Link
                                                href={item.path}
                                                onClick={() => setMenuOpen(false)}
                                                aria-current={currentPath === item.path ? "page" : undefined}
                                                className={cn(navigationMenuTriggerStyle(), "w-full justify-start px-2 text-sm")}
                                            >
                                                {tNav(item.key)}
                                            </Link>
                                            {index < NAV_ITEMS.length - 1 && <Separator/>}
                                        </li>
                                    ))}
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
                </div>
            </div>
        </header>
    );
}