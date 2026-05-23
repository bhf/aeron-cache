import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
    experimental: {
        externalDir: true,
    },
    output: "standalone"
};

export default nextConfig;
