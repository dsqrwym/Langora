
const ENCODER = new TextEncoder();
/** fatal 切在多字节字符中间直接抛错，比判断 \ufffd 可靠 */
const FATAL_DECODER = new TextDecoder("utf-8", {fatal: true});

export const utf8ByteLength = (value: string): number => ENCODER.encode(value).length;

export const isWithinUtf8Bytes = (value: string, maxBytes: number): boolean =>
    utf8ByteLength(value) <= maxBytes;

/**
 * 按 UTF-8 字节数截断，且不切碎多字节字符。
 * 做法：先按字节切，用 fatal 解码器验证；切在字符中间就回退一个字节重试（最多 3 次）。
 */
export const truncateToUtf8Bytes = (value: string, maxBytes: number): string => {
    const bytes = ENCODER.encode(value);
    if (bytes.length <= maxBytes) return value;

    for (let end = maxBytes; end > 0; end -= 1) {
        try {
            return FATAL_DECODER.decode(bytes.subarray(0, end));
        } catch {
            // 切点落在多字节字符中间，回退一个字节再看
        }
    }
    return "";
};