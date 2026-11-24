import type {NextConfig} from "next";
import {createRequire} from "node:module";
import createNextIntlPlugin from "next-intl/plugin";

const require = createRequire(__filename);
const tailwindLoader = require.resolve("@tailwindcss/turbopack");

const withNextIntl = createNextIntlPlugin({
    requestConfig: "./src/i18n/request.ts",
    experimental: {
        /** 类型基准：所有语言都必须和 zh 的形状一致 */
        createMessagesDeclaration: "./src/i18n/messages/zh.json"
    }
});

const nextConfig: NextConfig = {
    /* config options here */
    reactCompiler: true,
    turbopack: {
        root: __dirname,
        rules: {
            "*.css": {
                loaders: [tailwindLoader],
                as: "*.css",
            },
        },
    },
};

export default withNextIntl(nextConfig);
