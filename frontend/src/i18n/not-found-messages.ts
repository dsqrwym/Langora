import type {SupportedLanguage} from "@/i18n/languages";

/**
 * 404 专用
 *
 * Next 文档明确 `not-found.js` 不接收任何 props
 * 所以语言只能在客户端从 pathname 推断。若同步 import 三份完整字典，
 * 会把整个应用字典塞进 404 chunk；这里只放 404 需要的 6 条，约 1KB。
 */
export type NotFoundMessages = {
    badge: string;
    title: string;
    description: string;
    reassurance: string;
    home: string;
    back: string;
    interactionHint: string;
};

export const NOT_FOUND_MESSAGES: Record<SupportedLanguage, NotFoundMessages> = {
    en: {
        badge: "404 · lost in the grid",
        title: "This page isn't here",
        description: "The address doesn't exist, has been moved, or never existed.",
        reassurance: "Don't worry — follow the light back.",
        home: "Back to home",
        back: "Go back",
        interactionHint: "Move your cursor to pull the nodes · Click empty space to send a shockwave",
    },
    es: {
        badge: "404 · perdido en la red",
        title: "Esta página no está aquí",
        description: "La dirección no existe, se ha movido o nunca existió.",
        reassurance: "No te preocupes: sigue la luz de vuelta.",
        home: "Volver al inicio",
        back: "Volver atrás",
        interactionHint: "Mueve el cursor para atraer los nodos · Haz clic en el vacío para liberar una onda",
    },
    zh: {
        badge: "404 · 迷失在光里",
        title: "页面不在这里",
        description: "你要找的地址不存在、已被移动，或从未存在过。",
        reassurance: "别担心，沿着光就能回去。",
        home: "返回首页",
        back: "返回上一页",
        interactionHint: "移动鼠标牵引节点 · 点击空白释放冲击波",
    },
};