import {type SessionEnvelop} from "@/lib/auth/auth-types.ts";
import {AUTH_ENVELOPE_KEY} from "@/lib/auth/auth-constant.ts";
import {canUseWindow} from "@/lib/device.ts";

const EMPTY: SessionEnvelop = {version: 0, csrfToken: null};
/**
 * 内存兜底，永远先更新它
 * memory based fallback, priority over local storage
 */
let memory: SessionEnvelop = EMPTY;

const getStorage = (): Storage | null => {
    try {
        // check if window is available
        return canUseWindow() ? window.localStorage : null;
    } catch {
        return null;
    }
};


export const readEnvelop = (): SessionEnvelop => {
    const storage = getStorage();
    // 隐私模式, 被策略禁用... if in Private mode or disabled by polity
    if (!storage) return memory;

    try {
        const raw = storage.getItem(AUTH_ENVELOPE_KEY);
        if (!raw) return EMPTY;

        const parsed = JSON.parse(raw) as Partial<SessionEnvelop> | null;
        if (!parsed || typeof parsed !== "object") return EMPTY;

        const version = Number(parsed.version);
        const csrfToken = typeof parsed.csrfToken === "string" ? parsed.csrfToken : null;

        return {
            version: Number.isFinite(version) ? version : 0,
            csrfToken
        };
    } catch {
        // illegal storage content
        return memory;
    }
}

export const writeEnvelope = (envelope: SessionEnvelop) =>{
    memory = envelope;
    try {
        getStorage()?.setItem(AUTH_ENVELOPE_KEY, JSON.stringify(envelope));
    } catch {
        // ignore: data in this storage won't be shared
    }
}
