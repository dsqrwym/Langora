"use client"

import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from "@/components/ui/dropdown-menu";
import {Tooltip, TooltipContent, TooltipTrigger} from "@/components/ui/tooltip";
import {Button} from "@/components/ui/button";
import {LanguagesIcon} from "lucide-react";
import {
    formatMessage,
    LANGUAGE_LABELS,
    SUPPORTED_LANGUAGES,
    type Dictionary,
    type SupportedLanguage,
    writePreferredLanguage,
} from "@/i18n";
import {usePathname, useRouter} from "next/navigation";
import {buildLanguageSwitchPath} from "@/i18n/paths";

export default function LanguageSwitcher({lang, dictionary}: {
    lang: SupportedLanguage;
    dictionary: Dictionary;
}) {
    const router = useRouter();
    const pathname = usePathname();
    const tooltipContent = formatMessage(dictionary.common.language.switch, {language: LANGUAGE_LABELS[lang]});
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
                    SUPPORTED_LANGUAGES.filter((language) => language !== lang).map((language) => (
                        <DropdownMenuItem key={language} onClick={() => {
                            writePreferredLanguage(language);
                            router.replace(buildLanguageSwitchPath(
                                pathname,
                                language,
                                window.location.search,
                                window.location.hash
                            ), {scroll: false});
                        }}>
                            {LANGUAGE_LABELS[language]}
                        </DropdownMenuItem>
                    ))
                }
            </DropdownMenuContent>
        </DropdownMenu>
    );
}