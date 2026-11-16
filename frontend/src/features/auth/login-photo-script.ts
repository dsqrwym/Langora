import type {Dictionary} from "@/i18n";
import {LOGIN_PHOTOS} from "@/features/auth/login-photos-generated";
import {toInlineScriptWithArgs} from "@/lib/inline-script";

/** with no- prefix means silent, only for screen readers  */
const SILENT_PREFIX = "no-";
export const LOGIN_PHOTO_ID = "login-photo";
export const LOGIN_QUOTE_BODY_ROLE = "quote-body";

export type LoginPhotoQuote = {
    original: string;
    translation: string;
    source: string;
};

export type LoginPhotoRender = {
    id: string;
    src: string;
    alt: string;
    /** will be null when starts with "no-" or missing dictionary key  */
    quote?: LoginPhotoQuote | null;
    /** index in the sequence of photos with quotes, -1 for none */
    quoteIndex: number;
};

export function buildLoginPhotos(dictionary: Dictionary): LoginPhotoRender[] {
    const quotes = dictionary.auth.loginQuotes as Record<string, LoginPhotoQuote>;
    let quoteIndex = 0;

    return LOGIN_PHOTOS.map((photo) => {
        const entry = quotes[photo.id] ?? null;
        const visible = photo.id.startsWith(SILENT_PREFIX) ? null : entry;

        return {
            id: photo.id,
            src: photo.src,
            alt: entry ? [entry.original, entry.translation].filter(Boolean).join("\n") : "",
            quote: visible,
            quoteIndex: visible ? quoteIndex++ : -1,
        };
    });
}

/**
 * First frame inline script. Runs while the HTML is still parsing, before React hydrates and before first paint, so there is no grey-box flash.
 */
function bootstrapLoginPhoto({id, photos, quoteBodyRole}: {
    id: string,
    photos: readonly LoginPhotoRender[],
    quoteBodyRole: string
}): void {
    try {
        const image = document.getElementById(id) as HTMLImageElement | null;
        if (!image || photos.length === 0) return;

        const photo = photos[Math.floor(Math.random() * photos.length)];
        if (!photo) return;

        image.src = photo.src;
        image.alt = photo.alt;

        const figure = image.closest("figure");
        const cards = figure?.querySelectorAll<HTMLElement>(`[data-role="${quoteBodyRole}"]`);
        cards?.forEach((card, index) => {
            card.hidden = index !== photo.quoteIndex;
        });
    } catch {
    }
}

export function buildLoginPhotoScript(photos: readonly LoginPhotoRender[]): string {
    if (photos.length === 0) return "";
    const slim = photos.map(({id, src, alt, quoteIndex}) => ({id, src, alt, quoteIndex}));

    return toInlineScriptWithArgs(bootstrapLoginPhoto, {
        id: LOGIN_PHOTO_ID,
        photos: slim,
        quoteBodyRole: LOGIN_QUOTE_BODY_ROLE,
    });
}
