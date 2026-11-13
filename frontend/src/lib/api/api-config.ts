/**
* 浏览器侧必须能读到：Next 只把 NEXT_PUBLIC_ 前缀的变量内联进客户端 bundle，
*/
const baseUrl = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8088/langora";

export const API_BASE = baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`;

export const SUCCESS_CODES = new Set(["OK", "CREATED", "ACCEPTED", "NO_CONTENT"]);
export const DEFAULT_TIMEOUT_MS = 15_000;