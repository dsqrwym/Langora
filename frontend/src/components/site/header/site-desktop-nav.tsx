"use client"

import {useTranslations} from "next-intl";
import {Link, usePathname} from "@/i18n/navigation";
import {
    NavigationMenu,
    NavigationMenuItem,
    NavigationMenuLink,
    NavigationMenuList,
    navigationMenuTriggerStyle
} from "@/components/ui/navigation-menu";
import {cn} from "cn";
import {isSiteNavActive, SITE_NAV_KEYS} from "@/features/site/site-navigations";

export function SiteDesktopNav({ariaLabel}: { ariaLabel: string }) {
    const tNav = useTranslations("site.nav");
    const currentPath = usePathname() || "/";

    return (
        <NavigationMenu className="max-md:hidden shrink-0 grow-0 justify-self-center" aria-label={ariaLabel}>
            <NavigationMenuList>
                {
                    SITE_NAV_KEYS.map((item) => {
                        const active = isSiteNavActive(item, currentPath);
                        return (
                            <NavigationMenuItem key={item.key}>
                                <NavigationMenuLink
                                    render={<Link href={item.path}/>}
                                    active={active}
                                    aria-current={active ? "page" : undefined}
                                    className={cn(
                                        navigationMenuTriggerStyle(),
                                        "whitespace-nowrap text-sm"
                                    )}
                                >
                                    {tNav(item.key)}
                                </NavigationMenuLink>
                            </NavigationMenuItem>
                        )
                    })
                }
            </NavigationMenuList>
        </NavigationMenu>
    );
}
