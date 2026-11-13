import {type ExternalToast, toast} from "sonner";

export const TOAST_POSITIONS = [
    "top-left",
    "top-center",
    "top-right",
    "bottom-left",
    "bottom-center",
    "bottom-right",
] as const;

export type ToastPosition = (typeof TOAST_POSITIONS)[number];
export type ToastType = "success" | "error" | "info" | "warning" | "loading";

export type ShowToastOptions = {
    type?: ToastType;
    /**
     * The title of the toast or the main message.
     */
    title: string;
    /**
     * The description of the toast.
     * Whether it exists or not determines whether title serves as a title or as the main message.
     */
    description?: string;
    position?: ToastPosition;
    duration?: ToastDuration;
    id?: string;
}

export const TOAST_DURATION = {
    /** 成功类：扫一眼就够 */
    short: 2_500,
    /** 普通提示：与 sonner 默认一致 */
    default: 4_000,
    /** 需要读两行（标题 + 错误码）或需要用户行动的错误 */
    long: 8_000,
} as const;

export type ToastDuration = (typeof TOAST_DURATION)[keyof typeof TOAST_DURATION] | number;

export const showToast = (
    {
        type = "info",
        title,
        description,
        position = "top-right",
        duration = TOAST_DURATION.default,
        id,
    }: ShowToastOptions
): string | number => {
    const options: ExternalToast = {description, duration, id, position};

    switch (type) {
        case "success":
            return toast.success(title, options);
        case "error":
            return toast.error(title, options);
        case "warning":
            return toast.warning(title, options);
        case "loading":
            return toast.loading(title, options);
        case "info":
        default:
            return toast.info(title, options);
    }
};

export const dismissToast = (id?: string | number): void => {
    toast.dismiss(id);
};

export const showErrorToast = (
    {
        title,
        description,
        position = "top-right",
        duration = TOAST_DURATION.default,
        id,
    }: ShowToastOptions
): string | number => {
    return toast.error(title, {description, duration, id, position});
}