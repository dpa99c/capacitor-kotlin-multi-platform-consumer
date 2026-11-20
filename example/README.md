# capacitor-kotlin-multi-platform-consumer – example app

Demonstrates the consumer plugin: normalised device info, and accelerometer streaming with shared
Kotlin step counting and shake detection (device or simulated source).

```bash
(cd ../../capacitor-kotlin-multi-platform && npm install && npm run build)   # base plugin
(cd .. && npm install && npm run build)                                      # consumer plugin
npm install
npm run build && npx cap sync
npx cap run android                                                          # or: npx cap run ios  |  npm start
```

The app depends on **both** plugins (`capacitor-kotlin-multi-platform` and
`capacitor-kotlin-multi-platform-consumer`) so that the Capacitor CLI links both native parts.
