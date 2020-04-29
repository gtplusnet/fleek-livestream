import { WebPlugin } from '@capacitor/core';
import { LiveBroadcastPluginPlugin } from './definitions';

export class LiveBroadcastPluginWeb extends WebPlugin implements LiveBroadcastPluginPlugin {
  constructor() {
    super({
      name: 'LiveBroadcastPlugin',
      platforms: ['web','android']
    });
  }

  async callLive(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  }
}

const LiveBroadcastPlugin = new LiveBroadcastPluginWeb();

export { LiveBroadcastPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(LiveBroadcastPlugin);
