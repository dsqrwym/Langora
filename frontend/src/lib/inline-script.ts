/**
 * 将函数转换为内联脚本，并传入参数立刻执行
 * Using Immediately Invoked Function Expression to cover a function into inline script and execute it with arguments
 * 本函数仅支持 一个参数 且参数必须是可以被 JSON 序列化后的, 传入函数不能有单行注释
 * This function only supports one parameter and the parameter must be serializable by JSON
 * the function introduced cannot have single line comments
 * @param fn
 * @param args
 * @returns {string} The inline script
 */
export const toInlineScriptWithArgs = <T>(fn: (args: T) => void, args: T): string =>{
    return `(${fn.toString()})(${JSON.stringify(args)})`;
}