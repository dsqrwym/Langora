import {getTranslations} from "next-intl/server";
import ThemeSwitcher from "@/components/common/theme-switcher";
import LanguageSwitcher from "@/components/common/language-switcher";
import {HeaderBrand} from "@/components/site/header/header-brand";
import {SiteDesktopNav} from "@/components/site/header/site-desktop-nav";
import {SiteMobileNav} from "@/components/site/header/site-mobile-nav";
import type {SiteMediaImage} from "@/features/site/site-types";

type SiteHeaderProps = {
    schoolName: string;
    showHeaderSchoolName: boolean;
    logo: SiteMediaImage | null;
}

export default async function SiteHeader(
    {
        schoolName,
        showHeaderSchoolName,
        logo,
    }: SiteHeaderProps
) {
    const tMenu = await getTranslations("site.menu");

    return (
        <header
            className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur-sm supports-backdrop-filter:bg-background/80">
            <div
                className="mx-auto grid h-16 w-full max-w-352 grid-cols-[minmax(0,1fr)_auto] items-center gap-3 md:gap-20 px-4 sm:px-6 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] lg:px-8">
                <HeaderBrand
                    schoolName={schoolName}
                    showHeaderSchoolName={showHeaderSchoolName}
                    logo={logo}
                />

                <SiteDesktopNav ariaLabel={tMenu("main")}/>

                <div className="flex shrink-0 items-center justify-self-end gap-1">
                    <ThemeSwitcher/>
                    <LanguageSwitcher/>
                    <SiteMobileNav
                        schoolName={schoolName}
                        showHeaderSchoolName={showHeaderSchoolName}
                        logo={logo}
                    />
                </div>
            </div>
        </header>
    );
}