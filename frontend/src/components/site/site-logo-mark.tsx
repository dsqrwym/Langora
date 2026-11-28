"use client";

import {useEffect, useRef, useState} from "react";
import {cn} from "cn";
import type {SiteMediaImage} from "@/features/site/site-types";
import {School} from "lucide-react";

export const DEFAULT_BRAND_LOGO: SiteMediaImage = {
    url: "/langora.svg",
    alt: "Langora",
    width: 628,
    height: 602,
};

export type LogoMarkVariant = "header-compact" | "header-wide" | "drawer-wide";

type SiteLogoMarkProps = {
    logo: SiteMediaImage | null;
    variant: LogoMarkVariant;
    eager?: boolean;
    className?: string;
    /** alert to parent that image failed to load */
    onImageError?: (failedUrl: string) => void;
};

const IMAGE_SIZE_BY_VARIANT: Record<LogoMarkVariant, string> = {
    "header-compact": "h-9 w-auto max-w-24",
    "header-wide": "h-10 w-auto max-w-full",
    "drawer-wide": "h-10 w-auto max-w-full",
};

export function SiteLogoMark(
    {
        logo,
        variant,
        eager = false,
        className,
        onImageError,
    }: SiteLogoMarkProps
) {
    const [failedUrls, setFailedUrls] = useState<string[]>([]);
    const imgRef = useRef<HTMLImageElement>(null);
    const customUrl = logo?.url?.trim() || null;

    const isCustomAvailable = Boolean(customUrl) && !failedUrls.includes(customUrl!);
    const isFallbackAvailable = !failedUrls.includes(DEFAULT_BRAND_LOGO.url);

    const activeLogo: SiteMediaImage | null = isCustomAvailable
        ? logo
        : isFallbackAvailable
            ? DEFAULT_BRAND_LOGO
            : null;

    const activeUrl = activeLogo?.url;
    const isUsingLangoraFallback = activeUrl === DEFAULT_BRAND_LOGO.url;

    const markFailed = (url: string) => {
        setFailedUrls((prev) => prev.includes(url) ? prev : [...prev, url]);
        if (customUrl && url === customUrl && url !== DEFAULT_BRAND_LOGO.url) {
            onImageError?.(url);
        }
    };

    useEffect(() => {
        const img = imgRef.current;
        if (!img || !activeUrl) {
            return;
        }

        let cancelled = false;
        if (img.complete && img.naturalWidth === 0) {
            queueMicrotask(() => {
                if (!cancelled) {
                    setFailedUrls((prev) => prev.includes(activeUrl) ? prev : [...prev, activeUrl]);

                    if (customUrl && activeUrl === customUrl && activeUrl !== DEFAULT_BRAND_LOGO.url) {
                        onImageError?.(activeUrl);
                    }
                }
            });
        }

        return () => {
            cancelled = true;
        };
    }, [activeUrl, customUrl, onImageError]);

    if (!activeLogo || !activeUrl) {
        return (
            <School className={cn("shrink-0 text-foreground size-6", className)} aria-hidden="true"/>
        );
    }

    return (
        // eslint-disable-next-line @next/next/no-img-element
        <img
            ref={imgRef}
            src={activeUrl}
            alt={activeLogo?.alt || ""}
            aria-hidden="true"
            width={activeLogo.width && activeLogo.width > 0 ? activeLogo.width : undefined}
            height={activeLogo.height && activeLogo.height > 0 ? activeLogo.height : undefined}
            loading={eager ? "eager" : "lazy"}
            decoding="async"
            onError={() => markFailed(activeUrl)}
            className={cn(
                "block shrink-0 rounded-sm object-contain object-left rtl:object-right",
                // langora.svg 含深蓝字体，在深色模式下加轻微浅底确保高对比度
                isUsingLangoraFallback && "dark:rounded-md dark:bg-white/90 dark:p-0.5",
                IMAGE_SIZE_BY_VARIANT[variant],
                className,
            )}
        />
    );
}