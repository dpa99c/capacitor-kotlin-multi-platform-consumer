import { WebPlugin } from '@capacitor/core';

import type { KmpConsumerPlugin } from './definitions';

export class KmpConsumerWeb extends WebPlugin implements KmpConsumerPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
