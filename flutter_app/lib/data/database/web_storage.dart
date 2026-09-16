// ignore: avoid_web_libraries_in_flutter
import 'dart:html' as html;

String? getItem(String key) {
  return html.window.localStorage[key];
}

void setItem(String key, String value) {
  html.window.localStorage[key] = value;
}

void removeItem(String key) {
  html.window.localStorage.remove(key);
}
