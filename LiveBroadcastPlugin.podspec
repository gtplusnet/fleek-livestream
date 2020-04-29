
  Pod::Spec.new do |s|
    s.name = 'LiveBroadcastPlugin'
    s.version = '0.0.1'
    s.summary = 'Broadcast live'
    s.license = 'MIT'
    s.homepage = 'https://github.com/francisdurante/live-broadcast-api.git'
    s.author = 'Francis Durante'
    s.source = { :git => 'https://github.com/francisdurante/live-broadcast-api.git', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
  end