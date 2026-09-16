// Stub implementation for non-web platforms.
// These methods will never actually be called on mobile/desktop
// because DatabaseHelper checks kIsWeb first.

String? getItem(String key) {
  throw UnsupportedError('localStorage is not available on this platform');
}

void setItem(String key, String value) {
  throw UnsupportedError('localStorage is not available on this platform');
}

void removeItem(String key) {
  throw UnsupportedError('localStorage is not available on this platform');
}
