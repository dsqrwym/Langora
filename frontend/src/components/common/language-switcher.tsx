"use client"

import {LanguagesIcon} from "lucide-react";
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from "@/components/ui/dropdown-menu";
import {Tooltip, TooltipContent, TooltipTrigger} from "@/components/ui/tooltip";
import {Button} from "@/components/ui/button";
import {usePathname, useRouter} from "@/i18n/navigation";
import {useLocale, useTranslations} from "next-intl";
import {LANGUAGE_LABELS, SUPPORTED_LANGUAGES} from "@/i18n/routing";
import {writePreferredLanguage} from "@/i18n/locale-preference";

export default function LanguageSwitcher() {
    const locale = useLocale();
    const t = useTranslations("common.language");
    const router = useRouter();
    const pathname = usePathname();
    const tooltipContent = t("switch", {language: LANGUAGE_LABELS[locale]});
    return (
        <DropdownMenu>
            <Tooltip>
                <TooltipTrigger
                    render={
                        <DropdownMenuTrigger
                            render={
                                <Button variant="ghost" size="icon-lg" aria-label={tooltipContent}/>
                            }
                        >
                            <LanguagesIcon aria-hidden="true"/>
                        </DropdownMenuTrigger>
                    }
                />
                <TooltipContent>{tooltipContent}</TooltipContent>
            </Tooltip>
            <DropdownMenuContent className="min-w-0 w-auto">
                {
                    SUPPORTED_LANGUAGES.filter((language) => language !== locale).map((language) => (
                        <DropdownMenuItem key={language} onClick={() => {
                            writePreferredLanguage(language);
                            const nextUrl = `${pathname}${window.location.search}${window.location.hash}`;
                            router.replace(nextUrl, {locale: language, scroll: false});
                        }}>
                            {LANGUAGE_LABELS[language]}
                        </DropdownMenuItem>
                    ))
                }
            </DropdownMenuContent>
        </DropdownMenu>
    );
}