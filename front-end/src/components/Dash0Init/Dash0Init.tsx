"use client";
import { useEffect } from "react";
import { init } from "@dash0/sdk-web";

export default function Dash0Init() {
  useEffect(() => {
    const config = (window as any).__DASH0_CONFIG__;
    if (!config?.endpointUrl || !config?.authToken) return;
    init({
      serviceName: config.serviceName ?? "pizza-vibe-frontend",
      endpoint: { url: config.endpointUrl, authToken: config.authToken },
      ...(config.environment && { environment: config.environment }),
    });
  }, []);
  return null;
}
