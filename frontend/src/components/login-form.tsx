import {cn} from "cn"

import {Button} from "@/components/ui/button"
import {Field, FieldGroup, FieldLabel,} from "@/components/ui/field"
import {Input} from "@/components/ui/input"
import {getApiFailure} from "@/lib/api/errors.ts";
import {ErrorCode} from "@/lib/api/error-codes.ts";
import * as React from "react";
import {useState} from "react";
import {loginAdmin} from "@/lib/auth";
import {Spinner} from "@/components/ui/spinner.tsx";

const toLoginError = (error: unknown): string => {
    const failure = getApiFailure(error);

    if (failure.kind === "network") {
        return "Network error"
    }
    if (failure.kind === "unexpected") {
        return failure.message;
    }
    switch (failure.code) {
        case ErrorCode.AUTH_INVALID_CREDENTIALS:
        case ErrorCode.AUTH_ACCOUNT_DISABLED:
            return "Invalid credentials";
        case ErrorCode.AUTH_CAPTCHA_REQUIRED:
        case ErrorCode.AUTH_CAPTCHA_INVALID:
            return "Captcha required";
        case ErrorCode.RATE_LIMITED:
            return "Rate limited";
        default:
            return "Unknown error: " + failure.message;
    }
}

export function LoginForm(
    {
        className,
        ...props
    }: React.ComponentProps<"form">
) {
    const [identifier, setIdentifier] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [pending, setPending] = useState(false);

    const onSubmit = async (event: React.SubmitEvent<HTMLFormElement>) => {
        event.preventDefault(); // 阻止浏览器默认的整页提交（忘了这行 = 点登录页面闪一下重置）
        if (pending) return;

        setPending(true);
        setError(null);

        try {
            await loginAdmin(identifier, password);
            // 成功后什么都不做。
            // publishSession 让 status 变成 authenticated，<RedirectIfAuthed> 会自动跳走。
        } catch (cause) {
            setError(toLoginError(cause));
        } finally {
            setPending(false);
        }
    };

    return (
        <form onSubmit={onSubmit} className={cn("flex flex-col gap-6", className)} {...props}>
            <FieldGroup>
                <div className="flex flex-col items-center gap-1 text-center">
                    <h1 className="text-2xl font-bold">Login to your account</h1>
                    <p className="text-sm text-balance text-muted-foreground">
                        Enter your email or username below to login to your account
                    </p>
                </div>
                <Field>
                    <FieldLabel htmlFor="identifier">Email or Username</FieldLabel>
                    <Input
                        id="identifier"
                        type={identifier.includes("@") ? "email" : "text"}
                        placeholder="Introduce your email or username"
                        autoComplete={identifier.includes("@") ? "email" : "username"}
                        value={identifier}
                        onChange={(event) => setIdentifier(event.target.value)}
                        disabled={pending}
                        aria-errormessage={error ? error : undefined}
                        required/>
                </Field>
                <Field>
                    <div className="flex items-center">
                        <FieldLabel htmlFor="password">Password</FieldLabel>
                        <a
                            href="#"
                            className="ms-auto text-sm underline-offset-4 hover:underline"
                        >
                            Forgot your password?
                        </a>
                    </div>
                    <Input
                        id="password"
                        type="password"
                        autoComplete="current-password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        disabled={pending}
                        aria-errormessage={error ? error : undefined}
                        required/>
                </Field>
                <Field>
                    <Button type="submit">
                        <Spinner data-icon="inline-start"/> {pending ? "Please wait..." : "Login"}
                    </Button>
                </Field>
            </FieldGroup>
        </form>
    )
}
