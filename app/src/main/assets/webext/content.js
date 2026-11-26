// AI Hub Page Reader — content script (read-only).
// Sends the visible page text to the app via the background script,
// debounced so chat pages that update live are reported a few
// seconds after they change. It never writes to the page.
(function () {
  function send() {
    try {
      var text = document.body ? (document.body.innerText || "") : "";
      if (text.trim().length < 20) return;
      browser.runtime.sendMessage({
        type: "pageText",
        url: location.href,
        title: document.title || "",
        text: text.slice(-6000) // v1.4 — آخر 6,000 حرف (أحدث إجابة) بدل أول 12,000
      });
    } catch (e) { /* ignore */ }
  }
  var timer = null;
  function schedule() {
    if (timer) clearTimeout(timer);
    timer = setTimeout(send, 2000);
  }
  document.addEventListener("DOMContentLoaded", schedule);
  window.addEventListener("load", schedule);
  if (document.body) {
    var observer = new MutationObserver(schedule);
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });
  }
  schedule();
})();
