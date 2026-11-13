import {Dictionary} from "@/i18n/dictionaries/zh";
import {z} from "zod";
import {
    ADMIN_USERNAME_INVALID_CHARS,
    ADMIN_USERNAME_MIN_LENGTH,
    PASSWORD_MAX_BYTES
} from "@/lib/validation/field-limits";
import {formatMessage} from "@/i18n";

const ENCODER = new TextEncoder();

export const createLoginSchema = (dictionary: Dictionary) => {
    const error = dictionary.auth.errors;

    return z.object({
        identifier: z.string().trim().nonempty(error.identifierRequired)
            .superRefine((value, ctx) => {
                if (value.includes("@")) {
                    if (!z.email().safeParse(value).success) {
                        ctx.addIssue({code: "custom", message: error.invalidEmail})
                    }
                } else {
                    if (value.length < ADMIN_USERNAME_MIN_LENGTH) {
                        ctx.addIssue({
                            code: "custom",
                            message: formatMessage(error.usernameMinLength, {min: ADMIN_USERNAME_MIN_LENGTH}),
                        })
                    }

                    if (ADMIN_USERNAME_INVALID_CHARS.test(value)) {
                        ctx.addIssue({code: "custom", message: error.usernameInvalidCharacters});
                    }
                }
            }),
        // 对齐后端 @NotBlank 非空且不能全都是空白字符
        password: z.string()
            .refine((val) => val.trim().length > 0, {
                message: error.passwordRequired,
            })
            // 对齐后端 72 字节上限（使用 UTF-8 编码计算字节数）
            .refine((val) => ENCODER.encode(val).length <= PASSWORD_MAX_BYTES, {
                message: formatMessage(error.passwordTooLong, {max: PASSWORD_MAX_BYTES}),
            })
    });
}

export type LoginFormValues = z.infer<ReturnType<typeof createLoginSchema>>;