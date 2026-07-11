import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(() => {
  const devPort = Number(process.env.VITE_DEV_PORT ?? 5175);
  const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://localhost:8082";

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
      },
    },
    server: {
      host: "0.0.0.0",
      port: devPort,
      strictPort: true,
      proxy: {
        "/api": apiProxyTarget,
      },
    },
  };
});
