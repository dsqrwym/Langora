import type {UseFormRegisterReturn} from "react-hook-form";

/**
 * 以 raw → next 的差异推算格式化后的光标位置。
 * 只适用于"删除/替换型"格式化（不插入新字符）。
 */
const mapCaret = (raw: string, next: string, caret: number): number => {
    if (raw === next) return caret;

    // 光标左侧被原样保留的字符数
    let prefix = 0;
    while (prefix < caret && prefix < next.length && raw[prefix] === next[prefix]) prefix += 1;
    if (prefix === caret) return caret;

    // 光标右侧未被改动的字符数，用于从末尾回推位置
    let suffix = 0;
    const maxSuffix = raw.length - caret;
    while (
        suffix < maxSuffix &&
        suffix < next.length - prefix &&
        raw[raw.length - 1 - suffix] === next[next.length - 1 - suffix]
        ) suffix += 1;

    return next.length - suffix;
};

/**
 * 给 register(...) 的返回值套一层"输入时格式化"。
 *
 * register 是 uncontrolled——DOM
 * 所以这里先改 DOM.value（用户看得见），再调 RHF 的 onChange（它读的是已改过的值）。
 * 这样不需要 Controller、不触发额外渲染。
 *
 * 仅适用于 text/search/url/tel/password 这类支持 selectionStart 的输入框。
 */
export const withFormat = <TName extends string>(
    registration: UseFormRegisterReturn<TName>,
    format: (raw: string) => string,
): UseFormRegisterReturn<TName> => ({
    ...registration,
    onChange: (event) => {
        const element = event.target as HTMLInputElement;
        const raw = element.value;
        const next = format(raw);

        if (next !== raw) {
            const caret = element.selectionStart;
            element.value = next;
            if (caret !== null) {
                const position = mapCaret(raw, next, caret);
                element.setSelectionRange(position, position);
            }
        }
        // DOM 已就绪，再让 RHF 同步内部值并触发 onChange 校验
        return registration.onChange(event);
    },
});
