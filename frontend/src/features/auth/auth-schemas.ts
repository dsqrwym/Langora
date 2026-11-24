import {z} from "zod";
import {
    ADMIN_USERNAME_INVALID_CHARS,
    ADMIN_USERNAME_MIN_LENGTH,
    PASSWORD_MAX_BYTES
} from "@/lib/validation/field-limits";
import type {useTranslations} from "next-intl";

const ENCODER = new TextEncoder();


export type AuthErrorTranslator = ReturnType<typeof useTranslations<"auth.errors">>;

export const createLoginSchema = (t: AuthErrorTranslator) => {
    return z.object({
        identifier: z.string().trim().nonempty(t("identifierRequired"))
            .superRefine((value, ctx) => {
                if (value.includes("@")) {
                    if (!z.email().safeParse(value).success) {
                        ctx.addIssue({code: "custom", message: t("invalidEmail")})
                    }
                } else {
                    if (value.length < ADMIN_USERNAME_MIN_LENGTH) {
                        ctx.addIssue({
                            code: "custom",
                            message: t("usernameMinLength", {min: String(ADMIN_USERNAME_MIN_LENGTH)}),
                        })
                    }

                    if (ADMIN_USERNAME_INVALID_CHARS.test(value)) {
                        ctx.addIssue({code: "custom", message: t("usernameInvalidCharacters")});
                    }
                }
            }),
        // 对齐后端 @NotBlank 非空且不能全都是空白字符
        password: z.string()
            .refine((val) => val.trim().length > 0, {
                message: t("passwordRequired"),
            })
            // 对齐后端 72 字节上限（使用 UTF-8 编码计算字节数）
            .refine((val) => ENCODER.encode(val).length <= PASSWORD_MAX_BYTES, {
                message: t("passwordTooLong", {max: String(PASSWORD_MAX_BYTES)}),
            })
    });
}

export type LoginFormValues = z.infer<ReturnType<typeof createLoginSchema>>;