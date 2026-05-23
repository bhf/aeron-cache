import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
    experimental: {
        externalDir: true,
    },
    allowedDevOrigins: ['127.0.0.1'],
    output: "standalone"
};

export default nextConfig;
