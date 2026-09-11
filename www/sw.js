/* Service worker mínimo: deja la app disponible sin conexión.
   Solo se usa cuando Kind corre como PWA en el navegador; dentro del
   APK los archivos ya vienen empaquetados en el propio instalador. */
var CACHE = "kind-v1";
var CORE = [
  "index.html", "styles.css", "app.js", "manifest.webmanifest",
  "content/quotes.json",
  "assets/fonts/Nunito.ttf", "assets/fonts/Nunito-Italic.ttf",
  "assets/img/kind-logo.png", "assets/img/icon-192.png"
];

self.addEventListener("install", function (e) {
  e.waitUntil(
    caches.open(CACHE)
      .then(function (c) { return c.addAll(CORE); })
      .then(function () { return self.skipWaiting(); })
  );
});

self.addEventListener("activate", function (e) {
  e.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.filter(function (k) { return k !== CACHE; })
                             .map(function (k) { return caches.delete(k); }));
    }).then(function () { return self.clients.claim(); })
  );
});

self.addEventListener("fetch", function (e) {
  if (e.request.method !== "GET") return;
  e.respondWith(
    caches.match(e.request).then(function (hit) {
      return hit || fetch(e.request).then(function (res) {
        var copy = res.clone();
        caches.open(CACHE).then(function (c) { c.put(e.request, copy); });
        return res;
      });
    })
  );
});
