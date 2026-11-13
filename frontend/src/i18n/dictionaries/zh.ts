const zh = {
    common: {
        loading: "加载中",
        errors: {
            network: "网络连接失败，请检查网络后重试",
            timeout: "响应超时，请稍后重试",
            rateLimited: "尝试次数过多，请稍后再试",
            accessDenied: "没有访问权限",
            sessionExpired: "登录已过期，请重新登录",
            accountDisabled: "账号已停用，请联系管理员",

            server: "服务器暂时不可用，请稍后重试",
            requestFailed: "操作未完成，请重试",

            invalidResponse: "服务返回了无法处理的数据，请联系开发者",
            validationFailed: "请求参数有误，请联系开发者",
            unexpected: "出了点问题，请稍后重试",

            errorCode: "错误代码：{code}",
        },
    },
    auth: {
        login: "登录",
        title: "登录你的账户",
        subtitle: "输入邮箱或用户名继续",
        identifierLabel: "邮箱或用户名",
        identifierPlaceholder: "请输入邮箱或用户名",
        passwordLabel: "密码",
        passwordPlaceholder: "请输入密码",
        forgotPassword: "忘记密码？",
        submitting: "登录中…",
        captchaRequired: "请完成人机验证",
        captchaFailed: "验证未通过，请重新完成验证",
        captchaUnavailable: "验证服务暂时不可用，请稍后重试",
        /** 表单字段错误：字典是纯数据，参数化文案用 {name} 占位符 */
        errors: {
            invalidUsernameCredentials: "用户名或密码不正确",
            invalidEmailCredentials: "邮箱或密码不正确",
            identifierRequired: "请输入邮箱或用户名",
            invalidEmail: "请输入有效的邮箱地址",
            usernameMinLength: "用户名至少 {min} 个字符",
            usernameInvalidCharacters: "用户名只能包含字母、数字、点、下划线或连字符",
            passwordRequired: "请输入有效的密码",
            passwordTooLong: "密码过长（最多 {max} 字节）",
        },
    },
};
export default zh;

/** 类型基准：所有语言都必须和 zh 的形状一致 */
export type Dictionary = typeof zh;