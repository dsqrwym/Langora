/** https://next-intl.dev/docs/workflows/typescript */
import type {routing} from "@/i18n/routing";
import type zh from "@/i18n/messages/zh.json";

declare module "next-intl" {
    interface AppConfig{
        Locale: (typeof routing.locales)[number];
        /** change Messages type to default locale */
        Messages: typeof zh;
    }
}
