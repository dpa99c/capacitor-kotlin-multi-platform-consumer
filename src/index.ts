import { registerPlugin } from '@capacitor/core';

import type { KmpConsumerPlugin } from './definitions';

const KmpConsumer = registerPlugin<KmpConsumerPlugin>('KmpConsumer', {
  web: () => import('./web').then((m) => new m.KmpConsumerWeb()),
});

export * from './definitions';
export { KmpConsumer };
