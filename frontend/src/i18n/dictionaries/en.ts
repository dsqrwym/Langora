import {Dictionary} from "@/i18n/dictionaries/zh";

const en: Dictionary = {
    common: {
        loading: "Loading",
        errors: {
            network: "Can't connect. Check your connection and try again.",
            timeout: "The server took too long to respond. Try again.",
            rateLimited: "Too many attempts. Try again in a moment.",
            accessDenied: "You don't have access to this.",
            sessionExpired: "Your session has expired. Sign in again.",
            accountDisabled: "This account is disabled. Contact your administrator.",
            server: "The service is temporarily unavailable. Try again shortly.",
            requestFailed: "That didn't go through. Try again.",
            invalidResponse: "The server returned data we couldn't read. Contact your developer.",
            validationFailed: "The request was rejected. Contact your developer.",
            unexpected: "Something went wrong. Try again.",
            errorCode: "Error code: {code}",
        },
    },
    auth: {
        login: "Sign in",
        title: "Sign in to your account",
        subtitle: "Enter your email or username to continue",
        identifierLabel: "Email or username",
        identifierPlaceholder: "Enter your email or username",
        passwordLabel: "Password",
        passwordPlaceholder: "Enter your password",
        forgotPassword: "Forgot password?",
        submitting: "Signing in…",
        captchaRequired: "Complete the verification below",
        captchaFailed: "Verification failed. Please try again.",
        captchaUnavailable: "Verification is temporarily unavailable. Try again shortly.",
        errors: {
            invalidUsernameCredentials: "Incorrect username or password",
            invalidEmailCredentials: "Incorrect email or password",
            identifierRequired: "Enter your email or username",
            invalidEmail: "Enter a valid email address",
            usernameMinLength: "Username must be at least {min} characters",
            usernameInvalidCharacters: "Username can only contain letters, numbers, dots, underscores or hyphens",
            passwordRequired: "Enter a valid password",
            passwordTooLong: "Password is too long (max {max} bytes)",
        },
    },
};

export default en;