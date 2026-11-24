import {showErrorToast, TOAST_DURATION} from "@/lib/toast-manager";
import {ApiError, type AppError} from "@/lib/api/api-types";
import {type CommonErrorTranslator, getCommonErrorString} from "@/lib/api/api-error-i18n";

export const reportError = (error: AppError, t: CommonErrorTranslator): void => {
    const title = getCommonErrorString(error, t);

    console.error("[api-error]", title, error);

    const code = error instanceof ApiError ? (error.traceId ?? error.code) : error.kind;

    showErrorToast({
        title,
        description: t("errorCode", {code}),
        duration: TOAST_DURATION.long,
    });
};