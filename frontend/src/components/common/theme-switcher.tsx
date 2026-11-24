"use client"

import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from "@/components/ui/dropdown-menu";
import {Button} from "@/components/ui/button";
import {MoonIcon, SunIcon, SunMoonIcon} from "lucide-react";
import {useThemeStore} from "@/lib/theme/theme-store";
import {Tooltip, TooltipContent, TooltipTrigger} from "@/components/ui/tooltip";
import {type ThemePreference, useThemePreference} from "@/lib/theme";
import {useTranslations} from "next-intl";

export default function ThemeSwitcher() {
    const tTheme = useTranslations("common.theme");
    const themeLabels: Record<ThemePreference, string> = {
        light: tTheme("light"),
        dark: tTheme("dark"),
        system: tTheme("system"),
    };
    const {setPreference} = useThemeStore.getState();
    const currentPreference = useThemePreference();
    const tooltipContent = tTheme("current", {theme: themeLabels[currentPreference]});
    return (
        <DropdownMenu>
            <Tooltip>
                <TooltipTrigger
                    render={
                        <DropdownMenuTrigger
                            render={
                                <Button variant="ghost" size="icon-lg" className="relative"
                                        aria-label={tooltipContent}/>
                            }>
                            <SunIcon data-theme-icon="light" aria-hidden="true"/>
                            <MoonIcon data-theme-icon="dark" aria-hidden="true"/>
                            <SunMoonIcon data-theme-icon="system" aria-hidden="true"/>
                        </DropdownMenuTrigger>
                    }
                />
                <TooltipContent>{tooltipContent}</TooltipContent>
            </Tooltip>

            <DropdownMenuContent className="min-w-0 w-auto">
                {
                    currentPreference !== "light" && <DropdownMenuItem onClick={() => setPreference("light")}>
                        <SunIcon aria-hidden="true"/>{themeLabels.light}
                    </DropdownMenuItem>
                }
                {
                    currentPreference !== "dark" && <DropdownMenuItem onClick={() => setPreference("dark")}>
                        <MoonIcon aria-hidden="true"/>{themeLabels.dark}
                    </DropdownMenuItem>
                }
                {
                    currentPreference !== "system" && <DropdownMenuItem onClick={() => setPreference("system")}>
                        <SunMoonIcon aria-hidden="true"/>{themeLabels.system}
                    </DropdownMenuItem>
                }
            </DropdownMenuContent>
        </DropdownMenu>
    );
}