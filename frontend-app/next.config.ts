import type { NextConfig } from "next";

const nextConfig: NextConfig = {
	/* config options here */
	turbopack: {
		root: __dirname,
	},
	experimental: {
		// Enable optimized imports for better path resolution
		optimizePackageImports: ["@/components", "@/lib", "@/services", "@/context"],
	},
};

export default nextConfig;
