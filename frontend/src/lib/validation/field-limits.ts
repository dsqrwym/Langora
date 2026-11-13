export const ADMIN_USERNAME_MIN_LENGTH = 3;
export const ADMIN_USERNAME_MAX_LENGTH = 50;
/** 只校验字符集；长度交给 min/max，错误 key 才能分开 */
export const ADMIN_USERNAME_INVALID_CHARS = /[^A-Za-z0-9._-]/;
export const EMAIL_MAX_LENGTH = 255;
/** BCrypt 的 72 字节上限（后端 StrongPasswordValidator.maxBytes） */
export const PASSWORD_MAX_BYTES = 72;