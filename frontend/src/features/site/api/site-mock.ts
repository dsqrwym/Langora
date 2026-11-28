import type {SiteContactInfo, SiteMediaImage, SiteShellData} from "@/features/site/site-types";
import type {SupportedLanguage} from "@/i18n/routing";

type MockSiteScenario =
    | "normal"
    | "mediumName"
    | "longName"
    | "logoOnly"
    | "noLogo"
    | "brokenLogo";

const ACTIVE_MOCK_SCENARIO: MockSiteScenario = "normal";

const PLACEHOLDER_CONTACT: SiteContactInfo = {
    email: "contact@demo-school.example",
    phone: "+00 000 000 000",
    addressLine1: "Demo Street 00",
    addressLine2: "Suite 00 (Demo Building)",
    city: "Demo Location",
    stateProvince: "Demo Region",
    postalCode: "00000",
    countryCode: "XX",
    timezone: "UTC",
};

const PLACEHOLDER_LOGO: SiteMediaImage = {
    url: "/langora.svg",
    alt: null,
    width: 628,
    height: 602,
};

const PLACEHOLDER_WIDE_LOGO: SiteMediaImage = {
    url: "/langora.svg",
    alt: null,
    width: 628,
    height: 602,
};

const BASE_MOCK_BY_LOCALE: Record<SupportedLanguage, SiteShellData> = {
    zh: {
        schoolName: "示例语言学校",
        showHeaderSchoolName: true,
        logo: PLACEHOLDER_LOGO,
        tagline: "学校标语占位文案 · 用于预览页脚品牌介绍与排版间距。",
        footerText: "© 2026 示例语言学校 · 演示占位版权信息",
        contact: PLACEHOLDER_CONTACT,
    },
    en: {
        schoolName: "Demo School",
        showHeaderSchoolName: true,
        logo: PLACEHOLDER_LOGO,
        tagline: "Placeholder school tagline for layout and typography preview.",
        footerText: "© 2026 Demo School · Demo placeholder copyright",
        contact: PLACEHOLDER_CONTACT,
    },
    es: {
        schoolName: "Escuela Demo",
        showHeaderSchoolName: true,
        logo: PLACEHOLDER_LOGO,
        tagline: "Lema de demostración para previsualizar el diseño del pie de página.",
        footerText: "© 2026 Escuela Demo · Aviso de demostración",
        contact: PLACEHOLDER_CONTACT,
    },
};

const MEDIUM_NAME_BY_LOCALE: Record<SupportedLanguage, string> = {
    zh: "示例多语言示范实验学校",
    en: "Demo Heritage Language Academy",
    es: "Escuela Demo de Idiomas y Cultura",
};

const LONG_NAME_BY_LOCALE: Record<SupportedLanguage, string> = {
    zh: "示例地区国际多语言示范教育与传统文化综合实验学校（超长校名溢出与提示框测试）",
    en: "Demo International Heritage Language, Cultural Studies and Weekend Community Academy School",
    es: "Escuela Internacional de Demostración de Lengua Materna, Cultura y Educación Comunitaria",
};

export function getMockSiteShell(locale: SupportedLanguage): SiteShellData {
    const base = BASE_MOCK_BY_LOCALE[locale];
    switch (ACTIVE_MOCK_SCENARIO) {
        case "mediumName":
            return {...base, schoolName: MEDIUM_NAME_BY_LOCALE[locale]};
        case "longName":
            return {...base, schoolName: LONG_NAME_BY_LOCALE[locale]};
        case "logoOnly":
            return {
                ...base,
                showHeaderSchoolName: false,
                logo: PLACEHOLDER_WIDE_LOGO,
            };
        case "noLogo":
            return {...base, logo: null};
        case "brokenLogo":
            return {
                ...base,
                showHeaderSchoolName: false,
                logo: {
                    url: "/mock/does-not-exist.webp",
                    alt: base.schoolName,
                    width: 240,
                    height: 60,
                },
            };
        case "normal":
        default:
            return base;
    }
}