import { KmpWebPlugin } from 'capacitor-kotlin-multi-platform';

import type {
  DeviceInfo,
  KmpConsumerPlugin,
  MotionAvailability,
  MotionPermission,
  MotionSession,
  MotionSummary,
  StartMotionOptions,
} from './definitions';

/**
 * Web implementation: the same shared Kotlin `ConsumerModule`, compiled to JavaScript
 * (`src/generated/kmp`, produced by `npm run kmp:build`). `KmpWebPlugin` forwards calls and events.
 */
export class KmpConsumerWeb extends KmpWebPlugin implements KmpConsumerPlugin {
  constructor() {
    super(() => import('./generated/kmp/kmp-consumer-shared.mjs').then((m) => m.createKmpConsumerBridge()));
  }

  getDeviceInfo(): Promise<DeviceInfo> {
    return this.invoke('getDeviceInfo');
  }

  isMotionAvailable(): Promise<MotionAvailability> {
    return this.invoke('isMotionAvailable');
  }

  requestMotionPermission(): Promise<MotionPermission> {
    return this.invoke('requestMotionPermission');
  }

  startMotionUpdates(options: StartMotionOptions = {}): Promise<MotionSession> {
    return this.invoke('startMotionUpdates', options);
  }

  stopMotionUpdates(): Promise<MotionSummary> {
    return this.invoke('stopMotionUpdates');
  }
}
