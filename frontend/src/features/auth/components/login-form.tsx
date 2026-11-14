"use client";

import {cn} from "cn"

import {Button} from "@/components/ui/button"
import {Field, FieldError, FieldGroup, FieldLabel,} from "@/components/ui/field"
import {Input} from "@/components/ui/input"
import * as React from "react";
import {useEffect, useRef, useState} from "react";
import {Spinner} from "@/components/ui/spinner";
import {Dictionary} from "@/i18n/dictionaries/zh";
import {createLoginSchema, LoginFormValues} from "@/features/auth/auth-schemas";
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import {authRepository} from "@/features/auth/api/auth-client";
import {showErrorToast} from "@/lib/toast-manager";
import {getTurnstileToken, removeTurnstile, renderTurnstile, resetTurnstile} from "@/features/auth/captcha/turnstile";
import {loginAdmin} from "@/features/auth/session/auth-session";
import {ApiError} from "@/lib/api/api-types";
import {ErrorCode} from "@/lib/api/api-error-codes";
import {ADMIN_USERNAME_MAX_LENGTH, EMAIL_MAX_LENGTH} from "@/lib/validation/field-limits";
import {withFormat} from "@/lib/form/with-format";
import {reportError} from "@/lib/reportError";

type LoginFormProps = {
    dictionary: Dictionary
}

export function LoginForm(
    {
        className,
        dictionary,
        ...props
    }: React.ComponentProps<"form"> & LoginFormProps
) {
    const {auth} = dictionary;
    const schema = createLoginSchema(dictionary);

    const {
        watch, register, handleSubmit, setError, setFocus,
        formState: {errors, isSubmitting, isValid}
    } = useForm<LoginFormValues>({
        resolver: zodResolver(schema),
        defaultValues: {identifier: "", password: ""},
        mode: "onChange"
    });

    // 后端要求人机验证时才为 true 绝大多数用户永远不加载 Cloudflare 脚本
    const [captchaRequired, setCaptchaRequired] = useState(false);
    const captchaSlot = useRef<HTMLDivElement | null>(null);
    const captchaWidget = useRef<string | null>(null);

    const isEmail = watch("identifier").includes("@");
    // React 把 disabled 写进 DOM 之前连点两下会双触发 而 ref 读写立即生效
    const submittingRef = useRef(false);

    const identifier = withFormat(register("identifier"), (raw) => {
        const formatted = raw.trim();
        if (formatted.includes("@")) {
            return formatted.toLowerCase().slice(0, EMAIL_MAX_LENGTH);
        }

        return formatted.slice(0, ADMIN_USERNAME_MAX_LENGTH);
    });
    const identifierError = identifier.name + "-error";

    const password = register("password");
    const passwordError = password.name + "-error";

    useEffect(() => {
        if (!captchaRequired || captchaWidget.current || !captchaSlot.current) return;

        let cancelled = false;
        void (async () => {
            const config = await authRepository.captchaConfig();
            if (cancelled || !captchaSlot.current) return;

            if (!config.success) {
                console.error("[login] captcha config failed", config.error);
                showErrorToast({title: dictionary.common.errors.unexpected});
                return;
            }

            if (!config.data.siteKey || !captchaSlot.current) {
                console.error("[login] captcha config missing data or not enabled", config.data);
                showErrorToast({title: dictionary.common.errors.unexpected});
                return;
            }

            captchaWidget.current = await renderTurnstile(captchaSlot.current, {
                siteKey: config.data.siteKey,
                action: config.data.action
            })
        })();

        return () => {
            cancelled = true;
        };
    }, [captchaRequired, dictionary]);


    const onSubmit = handleSubmit(async ({identifier, password}) => {
        if (submittingRef.current) return;
        submittingRef.current = true;
        try {
            // widget 存在但没拿到 token，说明用户还没验完或 token 已过期
            const captchaToken = captchaWidget.current
                ? getTurnstileToken(captchaWidget.current) ?? undefined
                : undefined;

            const result = await loginAdmin(identifier, password, captchaToken);
            if (result.success) return;

            const {error} = result;

            if (error instanceof ApiError) {
                switch (error.code) {
                    case ErrorCode.AUTH_CAPTCHA_REQUIRED:
                        setCaptchaRequired(true);
                        // 失效时必须销毁，否则 effect 因 id 非空永远跳过重建
                        if (captchaWidget.current && !getTurnstileToken(captchaWidget.current)) {
                            removeTurnstile(captchaWidget.current);
                            captchaWidget.current = null;
                        }
                        showErrorToast({title: auth.captchaRequired});
                        return;
                    case ErrorCode.AUTH_CAPTCHA_INVALID:
                        // token 已被后端消费：重置 widget 让用户重新验证
                        if (captchaWidget.current) resetTurnstile(captchaWidget.current);
                        showErrorToast({title: auth.captchaFailed});
                        return;
                    case ErrorCode.AUTH_INVALID_CREDENTIALS:
                        const error = isEmail ? auth.errors.invalidEmailCredentials : auth.errors.invalidUsernameCredentials;
                        showErrorToast({title: error});
                        setError("password", {message: error});
                        setFocus("password", {shouldSelect: true});
                        return;

                }

                console.error("[login]", error);
                return;
            }

            reportError(error, dictionary);
        } finally {
            submittingRef.current = false;
        }

    });

    return (
        <form onSubmit={onSubmit} noValidate={true} className={cn("flex flex-col gap-6", className)} {...props}>
            <FieldGroup>
                <div className="flex flex-col items-center gap-1 text-center">
                    <h1 className="text-2xl font-bold">{auth.title}</h1>
                    <p className="text-sm text-balance text-muted-foreground">
                        {auth.subtitle}
                    </p>
                </div>
                <Field>
                    <FieldLabel htmlFor={identifier.name}>{auth.identifierLabel}</FieldLabel>
                    <Input
                        id={identifier.name}
                        aria-invalid={errors.identifier ? true : undefined}
                        aria-describedby={errors.identifier ? identifierError : undefined}
                        autoCapitalize="none"
                        autoCorrect="off"
                        type="text"
                        placeholder={auth.identifierPlaceholder}
                        autoComplete={isEmail ? "email" : "username"}
                        disabled={isSubmitting}
                        {...identifier}
                        required
                    />
                    <FieldError id={identifierError} errors={[errors.identifier]}/>
                </Field>
                <Field>
                    <div className="flex items-center">
                        <FieldLabel htmlFor={password.name}>{auth.passwordLabel}</FieldLabel>
                    </div>
                    <Input
                        id={password.name}
                        aria-invalid={errors.password ? true : undefined}
                        aria-describedby={errors.password ? passwordError : undefined}
                        type="password"
                        autoComplete="current-password"
                        placeholder={auth.passwordPlaceholder}
                        disabled={isSubmitting}
                        {...password}
                        required
                    />
                    <FieldError id={passwordError} errors={[errors.password]}/>
                </Field>
                <Field>
                    <Button type="submit" disabled={isSubmitting || !isValid}>
                        {isSubmitting ?
                            <>
                                <Spinner
                                    aria-hidden="true"
                                    data-icon="inline-start"
                                />
                                {auth.submitting}
                            </> : auth.login}
                    </Button>
                </Field>

                {captchaRequired && <div ref={captchaSlot} className="flex justify-center"/>}
            </FieldGroup>
        </form>
    )
}
