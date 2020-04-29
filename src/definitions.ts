declare module "@capacitor/core" {
  interface PluginRegistry {
    LiveBroadcastPlugin: LiveBroadcastPluginPlugin;
  }
}

export interface LiveBroadcastPluginPlugin {
  callLive(options: { value: string }): Promise<{value: string}>;
}
