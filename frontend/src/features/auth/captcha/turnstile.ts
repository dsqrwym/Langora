export {}

type TurnstileApi = {
    render: (container: HTMLElement, options: Record<string, unknown>) => string;
    reset: (widgetId: string) => void;
    remove: (widgetId: string) => void;
    getResponse: (widgetId: string) => string | undefined;
};

declare global {
    interface Window {
        turnstile?: TurnstileApi;
    }
}

const SCRIPT_SRC = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";

let loading: Promise<void> | null = null;

/** render=explicit：禁止脚本自动渲染，widget */
const loadTurnstile = (): Promise<void> => {
    if (window.turnstile) return Promise.resolve();

    loading ??= new Promise<void>((resolve, reject) => {
        const script = document.createElement("script");
        script.src = SCRIPT_SRC;
        script.async = true;
        script.defer = true;
        script.onload = () => resolve();
        script.onerror = () => {
            loading = null; // 失败允许重试
            reject(new Error("Turnstile script failed to load"));
        };
        document.head.appendChild(script);
    });

    return loading;
}

/** 渲染 widget，返回 widgetId */
export const renderTurnstile = async (
    container: HTMLElement,
    {siteKey, action}: { siteKey: string; action: string },
): Promise<string> => {
    await loadTurnstile();
    return window.turnstile!.render(container, {
        sitekey: siteKey,
        action,
        theme: "auto", // 文案语言由 Cloudflare 按浏览器自动选
    });
}

export const resetTurnstile = (widgetId: string): void => {
    try {
        window.turnstile?.reset(widgetId);
    } catch {
        // widget 已失效：调用方会走"重新销毁并渲染"的路径
    }
};

export const removeTurnstile = (widgetId: string): void => {
    try {
        window.turnstile?.remove(widgetId);
    } catch {
        // 已经不存在了，忽略
    }
};

export const getTurnstileToken = (widgetId: string): string | undefined => {
    try {
        return window.turnstile?.getResponse(widgetId) || undefined;
    } catch {
        return undefined;
    }
};