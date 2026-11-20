require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name = 'CapacitorKotlinMultiPlatformConsumer'
  s.version = package['version']
  s.summary = package['description']
  s.license = package['license']
  s.homepage = package['repository']['url']
  s.author = package['author']
  s.source = { :git => package['repository']['url'], :tag => s.version.to_s }
  s.source_files = 'ios/Sources/**/*.{swift,h,m,c,cc,mm,cpp}'
  # Shared Kotlin code, built by `npm run kmp:build` (kmp/shared).
  s.vendored_frameworks = 'ios/Frameworks/KmpConsumerShared.xcframework'
  # Kotlin/Native targets: iosArm64 + iosSimulatorArm64 – there is no Intel (x86_64) simulator slice.
  s.pod_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'x86_64' }
  s.user_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'x86_64' }
  s.ios.deployment_target = '15.0'
  s.dependency 'Capacitor'
  s.dependency 'CapacitorKotlinMultiPlatform'
  s.swift_version = '5.9'
end
