import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import security from "eslint-plugin-security";
import sonarjs from "eslint-plugin-sonarjs";
import sdl from "@microsoft/eslint-plugin-sdl";
import noUnsanitized from "eslint-plugin-no-unsanitized";
import tsParser from "@typescript-eslint/parser";
import tsPlugin from "@typescript-eslint/eslint-plugin";

const eslintConfig = defineConfig([
	...nextVitals,
	...nextTs,
	
	// Security-focused configuration
	{
		files: ["**/*.{js,jsx,ts,tsx}"],
		languageOptions: {
			parser: tsParser,
			parserOptions: {
				ecmaVersion: "latest",
				sourceType: "module",
				ecmaFeatures: {
					jsx: true,
				},
				project: "./tsconfig.json",
			},
		},
		plugins: {
			security,
			sonarjs,
			"@microsoft/sdl": sdl,
			"@typescript-eslint": tsPlugin,
			"no-unsanitized": noUnsanitized,
		},
		rules: {
			// Security rules
			"security/detect-object-injection": "error",
			"security/detect-non-literal-fs-filename": "error",
			"security/detect-unsafe-regex": "error",
			"security/detect-buffer-noassert": "error",
			"security/detect-child-process": "error",
			"security/detect-disable-mustache-escape": "error",
			"security/detect-eval-with-expression": "error",
			"security/detect-new-buffer": "error",
			"security/detect-no-csrf-before-method-override": "error",
			"security/detect-non-literal-regexp": "error",
			"security/detect-non-literal-require": "error",
			"security/detect-possible-timing-attacks": "error",
			"security/detect-pseudoRandomBytes": "error",
			
			// SonarJS rules for code quality and security
			"sonarjs/no-duplicate-string": "warn",
			"sonarjs/cognitive-complexity": ["warn", 15],
			"sonarjs/no-identical-functions": "error",
			"sonarjs/no-collapsible-if": "warn",
			"sonarjs/prefer-immediate-return": "warn",
			"sonarjs/prefer-object-literal": "warn",
			"sonarjs/prefer-single-boolean-return": "warn",
			
		// Microsoft SDL rules (Note: Some rules may not be available in current version)
		"@microsoft/sdl/no-cookies": "warn",
		"@microsoft/sdl/no-document-domain": "error",
		"@microsoft/sdl/no-document-write": "error",
		"@microsoft/sdl/no-html-method": "error",
		"@microsoft/sdl/no-inner-html": "warn",
		"@microsoft/sdl/no-insecure-url": "error",
		"@microsoft/sdl/no-msapp-exec-unsafe": "error",
		"@microsoft/sdl/no-postmessage-star-origin": "error",			// TypeScript specific security rules
			"@typescript-eslint/no-explicit-any": "warn",
			"@typescript-eslint/no-unsafe-argument": "error",
			"@typescript-eslint/no-unsafe-assignment": "error",
			"@typescript-eslint/no-unsafe-call": "error",
			"@typescript-eslint/no-unsafe-member-access": "error",
			"@typescript-eslint/no-unsafe-return": "error",
			
			// Additional DOM and XSS protection rules
			"no-unsanitized/method": "error",
			"no-unsanitized/property": "error",
			
			// Additional security-related rules
			"no-eval": "error",
			"no-implied-eval": "error",
			"no-new-func": "error",
			"no-script-url": "error",
			"no-alert": "warn",
			"no-console": "warn",
		},
	},
	
	// Override default ignores of eslint-config-next.
	globalIgnores([
		// Default ignores of eslint-config-next:
		".next/**",
		"out/**",
		"build/**",
		"next-env.d.ts",
		// Additional ignores
		"node_modules/**",
		"*.config.js",
		"*.config.ts",
		"*.config.mjs",
		"public/**",
		"coverage/**",
		"dist/**",
		".env*",
		"*.log",
		"*.tsbuildinfo",
		".tmp/**",
		"temp/**",
		"package-lock.json",
		"yarn.lock",
		"pnpm-lock.yaml",
		".vscode/**",
		".idea/**",
		"scripts/**", // Exclude our security audit script
	]),
]);

export default eslintConfig;
