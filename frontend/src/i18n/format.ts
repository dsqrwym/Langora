    /**
 * 极简模板格式化，只支持 {name} 占位符。
 *
 * 为什么字典里不能放函数：字典会作为 props 从 Server Component 传给 Client Component，
 * 而 Next 只允许传"可序列化数据"，函数会直接抛
 * "Functions cannot be passed directly to Client Components"。
 * 所以参数化文案统一写成 "用户名长度不能少于 {min} 个字符"，在渲染侧展开。
 */
export type MessageParams = Record<string, string | number>;

export const formatMessage = (template: string, params: MessageParams): string =>
    template.replace(/\{(\w+)\}/g, (placeholder, name) => {
        const value = params[name];
        return value === undefined ? placeholder : String(value);
    });