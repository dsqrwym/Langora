/**
 * media_assets + media_asset_translations
 */
export type SiteMediaImage = {
    url: string;
    alt: string | null;
    width: number | null;
    height: number | null;
}

export type SiteContactInfo = {
    email: string;
    phone: string;
    addressLine1: string;
    addressLine2: string | null;
    city: string;
    stateProvince: string | null;
    postalCode: string | null;
    countryCode: string;
    timezone: string;
}

export type SiteShellData = {
    schoolName: string;
    showHeaderSchoolName: boolean;
    logo: SiteMediaImage | null;
    tagLine: string | null;
    footerText: string | null;
    contact: SiteContactInfo;
}