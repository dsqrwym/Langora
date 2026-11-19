import type {NextConfig} from "next";
import {createRequire} from "node:module";

const require = createRequire(__filename);
const tailwindLoader = require.resolve("@tailwindcss/turbopack");


const nextConfig: NextConfig = {
    /* config options here */
    typedRoutes: true,
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

export default nextConfig;
