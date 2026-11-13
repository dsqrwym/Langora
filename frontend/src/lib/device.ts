const DEVICE_KEY = 'langora.device_uuid';

export const getOrCreateDeviceUuid = (): string => {
    if (!canUseWindow()) {
        throw new Error("getOrCreateDeviceUuid is browser-only");
    }
    let uuid = localStorage.getItem(DEVICE_KEY);
    if (!uuid) {
        uuid = crypto.randomUUID();
        localStorage.setItem(DEVICE_KEY, uuid);
    }
    return uuid;
}

export const canUseDom = (): boolean => {
    return canUseWindow() && typeof window.document !== "undefined"
};

export const canUseWindow = (): boolean => {
    return typeof window !== "undefined";
};