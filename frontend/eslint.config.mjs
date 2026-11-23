import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    ".temp",
    ".tmp/**",
    ".idea",
    "out/**",
    "build/**",
    "next-env.d.ts",
    "src/i18n/messages/*.d.json.ts",
    "node_modules",
    // 迁移期的旧工程与脚手架副本
    "vite/**",
    "temp/**",
  ]),
  {
    rules: {
      "@typescript-eslint/consistent-type-imports": ["error", {fixStyle: "inline-type-imports"}]
    }
  }
]);

export default eslintConfig;
