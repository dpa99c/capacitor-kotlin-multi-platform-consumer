import { registerPlugin } from '@capacitor/core';

import type { KmpConsumerPlugin } from './definitions';
import type { KmpConsumerWeb } from './web';

// Memoised: Capacitor may call the factory several times when the first plugin calls happen
// concurrently, and every web instance would otherwise host its own Kotlin module (losing events).
let web: Promise<KmpConsumerWeb> | undefined;

const KmpConsumer = registerPlugin<KmpConsumerPlugin>('KmpConsumer', {
  web: () => (web ??= import('./web').then((m) => new m.KmpConsumerWeb())),
});

export * from './definitions';
export { KmpConsumer };
