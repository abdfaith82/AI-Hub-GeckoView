// AI Hub Page Reader — background script.
// Relays page text from content scripts to the Android app
// via native messaging (the app registered as "com.abd.aihubgecko").
browser.runtime.onMessage.addListener(function (msg) {
  if (msg && msg.type === "pageText") {
    browser.runtime.sendNativeMessage("com.abd.aihubgecko", msg);
  }
});
