import {showErrorToast, TOAST_DURATION} from "@/lib/toast-manager";
import {Dictionary, formatMessage} from "@/i18n";
import {ApiError, AppError} from "@/lib/api/api-types";
import {getCommonErrorString} from "@/lib/api/api-error-i18n";

export const reportError = (error: AppError, dictionary: Dictionary): void => {
    const title = getCommonErrorString(error, dictionary);

    console.error("[api-error]", title, error);

    const code = error instanceof ApiError ? (error.traceId ?? error.code) : error.kind;

    showErrorToast({
        title,
        description: formatMessage(dictionary.common.errors.errorCode, {code}),
        duration: TOAST_DURATION.long,
    });
};