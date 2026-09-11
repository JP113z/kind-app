/* ---------------------------------------------------------------
   Kind — lógica de la pantalla única.

   La app corre en dos contextos:
     · dentro del APK  -> existe window.KindNative (puente Kotlin)
     · en el navegador -> no existe; se muestra el aviso y la
                          configuración queda solo en localStorage
----------------------------------------------------------------- */
(function () {
  "use strict";

  var N = window.KindNative || null;   // puente nativo (Android)
  var quotes = [];

  /* ---------- frase del día -------------------------------------
     Misma fórmula que en Kotlin (KindQuotes.todayIndex) para que el
     widget, la notificación y la pantalla muestren siempre lo mismo. */
  function epochDayLocal(d) {
    return Math.floor(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()) / 86400000);
  }

  function quoteOfDay() {
    if (!quotes.length) return null;
    var i = ((epochDayLocal(new Date()) % quotes.length) + quotes.length) % quotes.length;
    return quotes[i];
  }

  /* ---------- helpers ------------------------------------------- */
  function $(id) { return document.getElementById(id); }
  function show(el, yes) { el.hidden = !yes; }
  function pad(n) { return (n < 10 ? "0" : "") + n; }

  function setStatus(msg, warn) {
    var el = $("notifStatus");
    el.textContent = msg;
    el.classList.toggle("status--warn", !!warn);
  }

  /* ---------- configuración ------------------------------------- */
  var LS_KEY = "kind.settings";

  function readSettings() {
    if (N) {
      try { return JSON.parse(N.getSettings()); } catch (e) { /* cae al fallback */ }
    }
    try {
      var raw = localStorage.getItem(LS_KEY);
      if (raw) return JSON.parse(raw);
    } catch (e) { /* modo privado */ }
    return { enabled: false, hour: 8, minute: 0 };
  }

  function writeSettings(s) {
    if (N) { N.saveSettings(s.enabled, s.hour, s.minute); return; }
    try { localStorage.setItem(LS_KEY, JSON.stringify(s)); } catch (e) { /* modo privado */ }
  }

  function currentFromInputs() {
    var parts = ($("notifTime").value || "08:00").split(":");
    return {
      enabled: $("notifSwitch").checked,
      hour: parseInt(parts[0], 10) || 0,
      minute: parseInt(parts[1], 10) || 0
    };
  }

  /* ---------- pintar la pantalla -------------------------------- */
  function renderToday() {
    var q = quoteOfDay();
    if (!q) return;
    $("todayImage").src = q.image;
    $("todayImage").alt = q.text;
    $("todayText").textContent = q.text;
    $("widgetPreview").src = q.image;
  }

  function refreshNotifUi() {
    var s = currentFromInputs();

    if (!N) {
      setStatus("En el navegador no se puede programar el recordatorio diario. Instalá la app de Android para que funcione.", true);
      show($("permBtn"), false);
      show($("exactBtn"), false);
      return;
    }

    var granted = N.hasNotificationPermission();
    var exact = N.canScheduleExactAlarms();

    show($("permBtn"), !granted);
    show($("exactBtn"), granted && !exact);

    if (!s.enabled) {
      setStatus("El recordatorio está apagado. Activá el interruptor para recibir la frase cada día.");
    } else if (!granted) {
      setStatus("Falta el permiso de notificaciones. Tocá «Permitir notificaciones».", true);
    } else if (!exact) {
      setStatus("Listo, pero sin permiso de alarmas exactas la notificación puede llegar con unos minutos de retraso.", true);
    } else {
      setStatus("Todo listo: cada día a las " + pad(s.hour) + ":" + pad(s.minute) + " vas a recibir la frase de Tiger.");
    }
  }

  function applySettings() {
    var s = currentFromInputs();
    if (s.enabled && N && !N.hasNotificationPermission()) {
      N.requestNotificationPermission();   // el resultado vuelve por kindOnPermissionResult
      return;
    }
    writeSettings(s);
    refreshNotifUi();
  }

  /* ---------- arranque ------------------------------------------ */
  function init() {
    var s = readSettings();
    $("notifSwitch").checked = !!s.enabled;
    $("notifTime").value = pad(s.hour) + ":" + pad(s.minute);

    $("notifSwitch").addEventListener("change", applySettings);
    $("notifTime").addEventListener("change", applySettings);

    $("permBtn").addEventListener("click", function () {
      if (N) N.requestNotificationPermission();
    });

    $("exactBtn").addEventListener("click", function () {
      if (N) N.openExactAlarmSettings();
    });

    $("testBtn").addEventListener("click", function () {
      if (!N) {
        setStatus("La notificación de prueba solo funciona dentro de la app de Android.", true);
        return;
      }
      if (!N.hasNotificationPermission()) { N.requestNotificationPermission(); return; }
      N.sendTestNotification();
      setStatus("Enviada. Revisá tu barra de notificaciones.");
    });

    show($("webNote"), !N);
    show($("pinBtn"), !!(N && N.canPinWidget()));
    $("pinBtn").addEventListener("click", function () { if (N) N.requestPinWidget(); });

    refreshNotifUi();

    // al volver del diálogo de permisos del sistema, refrescamos el estado
    document.addEventListener("visibilitychange", function () {
      if (!document.hidden) { writeSettings(currentFromInputs()); refreshNotifUi(); }
    });
  }

  // el lado nativo llama a esto cuando se resuelve el diálogo de permiso
  window.kindOnPermissionResult = function () {
    writeSettings(currentFromInputs());
    refreshNotifUi();
  };

  fetch("content/quotes.json")
    .then(function (r) { return r.json(); })
    .then(function (data) { quotes = data.quotes || []; renderToday(); })
    .catch(function () { $("todayText").textContent = "No se pudieron cargar las frases."; })
    .then(init);

  // el service worker solo tiene sentido en el navegador; dentro del APK los
  // archivos ya están empaquetados y no hace falta cachearlos otra vez
  if (!N && "serviceWorker" in navigator && location.protocol.indexOf("http") === 0) {
    navigator.serviceWorker.register("sw.js").catch(function () { /* opcional */ });
  }
})();
