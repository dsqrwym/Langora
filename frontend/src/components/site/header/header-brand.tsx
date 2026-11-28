"use client";

import {useCallback, useEffect, useRef, useState} from "react";
import {cn} from "cn";
import {Link} from "@/i18n/navigation";
import {Tooltip, TooltipContent, TooltipTrigger} from "@/components/ui/tooltip";
import {SiteLogoMark} from "@/components/site/site-logo-mark";
import type {SiteMediaImage} from "@/features/site/site-types";

type HeaderBrandProps = {
    schoolName: string;
    showHeaderSchoolName: boolean;
    logo: SiteMediaImage | null;
};

type NameFitState = {
    size: "lg" | "base";
    isTruncated: boolean;
};

const INITIAL_FIT: NameFitState = {size: "lg", isTruncated: false};

export function HeaderBrand(
    {
        schoolName,
        showHeaderSchoolName,
        logo,
    }: HeaderBrandProps
) {
    const [failedLogoUrl, setFailedLogoUrl] = useState<string | null>(null);
    const [fit, setFit] = useState<NameFitState>(INITIAL_FIT);

    const slotRef = useRef<HTMLDivElement>(null);
    const logoRef = useRef<HTMLSpanElement>(null);
    const probeLgRef = useRef<HTMLSpanElement>(null);
    const probeBaseRef = useRef<HTMLSpanElement>(null);

    const logoUrl = logo?.url?.trim() || null;
    const hasWorkingLogo = Boolean(logoUrl) && failedLogoUrl !== logoUrl;

    const isLogoOnly = !showHeaderSchoolName && hasWorkingLogo;
    const showVisibleText = !isLogoOnly;

    const handleCustomLogoError = useCallback((url: string) => {
        setFailedLogoUrl(url)
    }, []);

    useEffect(() => {
        if (!showVisibleText) return;
        const slotEl = slotRef.current;
        if (!slotEl) return;

        const measure = () => {
            const probeLg = probeLgRef.current;
            const probeBase = probeBaseRef.current;
            if (!probeLg || !probeBase) return;

            const slotWidth = slotEl.clientWidth;
            const logoWidth = logoRef.current?.getBoundingClientRect().width ?? 0;
            const linkEl = logoRef.current?.parentElement;
            const gap = linkEl
                ? Number.parseFloat(getComputedStyle(linkEl).columnGap) || 0
                : 0;

            const availableWidth = Math.max(
                0,
                slotWidth - logoWidth - (logoWidth > 0 ? gap : 0),
            );

            const lgWidth = probeLg.getBoundingClientRect().width;
            const baseWidth = probeBase.getBoundingClientRect().width;

            const next: NameFitState =
                lgWidth <= availableWidth + 1
                    ? {size: "lg", isTruncated: false}
                    : baseWidth <= availableWidth + 1
                        ? {size: "base", isTruncated: false}
                        : {size: "base", isTruncated: true};

            setFit((prev) =>
                prev.size === next.size && prev.isTruncated === next.isTruncated
                    ? prev
                    : next,
            );
        };

        const observer = new ResizeObserver(measure);
        observer.observe(slotEl);
        if (logoRef.current) {
            observer.observe(logoRef.current);
        }

        let cancelled = false;
        const rafId = window.requestAnimationFrame(measure);
        document.fonts?.ready.then(() => {
            if (!cancelled) measure();
        });

        return () => {
            cancelled = true;
            window.cancelAnimationFrame(rafId);
            observer.disconnect();
        };
    }, [showVisibleText, schoolName, hasWorkingLogo]);

    const tooltipEnabled = showVisibleText && fit.isTruncated;

    return (
        <div ref={slotRef} className="relative flex w-full min-w-0 items-center">
            {showVisibleText && (
                <div
                    aria-hidden="true"
                    className="pointer-events-none invisible absolute inset-x-0 top-0 h-0 overflow-hidden select-none"
                >
                    <span
                        ref={probeLgRef}
                        className="inline-block whitespace-nowrap text-lg font-bold tracking-tight"
                    >
                        {schoolName}
                    </span>
                    <span
                        ref={probeBaseRef}
                        className="inline-block whitespace-nowrap text-base font-bold tracking-tight"
                    >
                        {schoolName}
                    </span>
                </div>
            )}

            <Tooltip disabled={!tooltipEnabled}>
                <TooltipTrigger
                    render={
                        <Link
                            href="/"
                            aria-label={isLogoOnly ? schoolName : undefined}
                            className={cn(
                                "inline-flex min-w-0 items-center gap-2.5 rounded-md font-bold tracking-tight text-foreground outline-none transition-opacity hover:opacity-90 focus-visible:ring-2 focus-visible:ring-ring/50",
                                isLogoOnly
                                    ? "max-w-[min(100%,13rem)] lg:max-w-[min(100%,15rem)]"
                                    : "max-w-full",
                            )}
                        />
                    }
                >
                    <span
                        ref={logoRef}
                        className={cn(
                            "flex items-center",
                            isLogoOnly ? "min-w-0 max-w-full" : "shrink-0",
                        )}
                    >
                        <SiteLogoMark
                            logo={logo}
                            variant={isLogoOnly ? "header-wide" : "header-compact"}
                            eager
                            onImageError={handleCustomLogoError}
                        />
                    </span>

                    {showVisibleText ? (
                        <span
                            className={cn(
                                "min-w-0 truncate leading-none",
                                fit.size === "lg" ? "text-lg" : "text-base",
                            )}
                        >
                            {schoolName}
                        </span>
                    ) : (
                        <span className="sr-only">{schoolName}</span>
                    )}
                </TooltipTrigger>

                <TooltipContent
                    side="bottom"
                    align="start"
                    sideOffset={8}
                    className="max-w-sm wrap-break-word"
                >
                    {schoolName}
                </TooltipContent>
            </Tooltip>
        </div>
    );
}