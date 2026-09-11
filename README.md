# Kind

**Pequeños actos. Grandes cambios.**

Una frase amable por día: se ve en un **widget** de la pantalla de inicio y
llega como **notificación** a la hora que vos elijas.

La app se abre solo para configurar el recordatorio y para explicar cómo
activar el widget. Todo lo demás pasa fuera de la app.

---

## Por qué no es una PWA "pura"

La idea original era una PWA, pero las dos cosas que hacen a Kind lo que es no
se pueden hacer con una PWA en Android:

- **Widgets de pantalla de inicio**: no existen para PWAs en Android. Un widget
  obliga a un `AppWidgetProvider` nativo.
- **Notificación diaria a una hora fija**: una PWA necesitaría un servidor de
  Web Push mandando un mensaje cada mañana. No hay forma confiable de programar
  una notificación local recurrente desde el navegador.

La solución mantiene la simplicidad que buscabas: **la interfaz sigue siendo
web** (HTML + CSS + JS en `www/`, con su `manifest.webmanifest` y su service
worker, o sea que funciona como PWA en el navegador), y encima hay una **capa
nativa mínima de Android** (unos pocos archivos Kotlin) que aporta el widget,
las alarmas y las notificaciones. No hay Node, ni framework, ni build de JS.

---

## Cómo está organizado

```
kind-app/
├─ content/quotes.json      ← LA fuente de las frases (editá acá)
├─ tools/generate_cards.py  ← genera imágenes, iconos y logo
├─ www/                     ← la app web (también es la PWA)
│  ├─ index.html · styles.css · app.js
│  ├─ content/quotes.json   ← copia automática de content/quotes.json
│  ├─ img/quotes/qNN.png    ← la imagen de cada frase (la que ve el widget)
│  └─ assets/               ← fuentes, logo, iconos
├─ android/                 ← proyecto Android (Kotlin)
│  └─ app/src/main/java/cr/kind/app/
│     ├─ MainActivity.kt       WebView que muestra www/
│     ├─ WebBridge.kt          puente JS ⇄ Android
│     ├─ KindWidget.kt         el widget
│     ├─ KindAlarms.kt         alarmas diaria y de medianoche
│     ├─ KindNotifications.kt  la notificación
│     ├─ KindQuotes.kt         lee content/quotes.json
│     └─ KindPrefs.kt          hora elegida por el usuario
└─ docs/marca.md            ← paleta, tipografía, logo
```

`android/app/build.gradle.kts` apunta sus *assets* a `../www`, así que la
carpeta web **no se duplica**: el APK empaqueta exactamente esos archivos.

### La frase del día

`app.js` (JavaScript) y `KindQuotes.kt` (Kotlin) calculan el índice con la
**misma fórmula**: días transcurridos desde 1970 en hora local, módulo la
cantidad de frases. Por eso la pantalla, el widget y la notificación siempre
muestran lo mismo, sin necesidad de servidor ni de guardar estado.

Con 14 frases el ciclo se repite cada 14 días: agregá más y el ciclo se alarga
solo.

---

## Agregar o cambiar frases

1. Editá `content/quotes.json` y agregá una entrada:

   ```json
   { "id": "q15", "text": "Tu frase acá.", "image": "img/quotes/q15.png", "palette": "menta" }
   ```

   `palette` puede ser `menta`, `arena`, `verde` o `sol`.

2. (Opcional) Si querés una **foto propia** de fondo en vez del degradado,
   guardala como `tools/photos/q15.jpg`.

3. Regenerá las imágenes:

   ```bash
   python tools/generate_cards.py
   ```

   Necesita Pillow: `pip install pillow`.

Eso reescribe `www/img/quotes/*.png`, los iconos y el logo. Volvé a compilar el
APK y listo.

---

## Compilar el APK

### Requisitos

- **Android Studio** (trae el SDK de Android y un JDK compatible).
  Descarga: <https://developer.android.com/studio>

> El JDK 25 que ya está instalado en la máquina **no** sirve para compilar
> Android: usá el JDK que trae Android Studio (JetBrains Runtime 21), que es lo
> que hace por defecto.

### Opción A — Android Studio (la más fácil)

1. Abrí Android Studio → **Open** → elegí la carpeta `kind-app/android`.
2. Esperá el *Gradle sync* (la primera vez descarga dependencias).
3. Conectá el teléfono por USB con la **depuración USB** activada
   (Ajustes → Acerca del teléfono → tocar 7 veces "Número de compilación" →
   Ajustes → Opciones de desarrollador → Depuración por USB).
4. Botón **Run ▶**. La app se instala y se abre sola.

Para obtener el archivo `.apk`: menú **Build → Build Bundle(s) / APK(s) →
Build APK(s)**. Queda en:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

### Opción B — línea de comandos

Desde `kind-app/android`, con el SDK ya instalado y `JAVA_HOME` apuntando a un
JDK 17–21:

```bash
./gradlew assembleDebug
```

En Windows (PowerShell):

```bash
cd android; .\gradlew.bat assembleDebug
```

Si Gradle se queja de que no encuentra el SDK, creá `android/local.properties`
con la ruta (ajustá el usuario):

```
sdk.dir=C\:\\Users\\JP113\\AppData\\Local\\Android\\Sdk
```

### Instalarlo en el teléfono

- **Por USB**: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- **Sin cable**: pasá el `.apk` al teléfono (Drive, WhatsApp, correo), abrilo y
  aceptá "instalar apps de origen desconocido".

Es un APK de *debug*, firmado con la llave de depuración: sirve perfecto para
probarlo. Para publicarlo en Play habría que firmarlo con una llave propia.

---

## Probar que todo funciona

1. **Abrí la app** → aparece la frase de hoy.
2. Activá **Notificación diaria**, aceptá el permiso y elegí la hora.
   - Tocá **Enviar una de prueba** para ver la notificación al instante.
   - Si Android pide *alarmas exactas*, el botón te lleva directo a esa opción.
3. **Widget**: mantené presionada la pantalla de inicio → *Widgets* → **Kind** →
   arrastrá **Frase del día**. (Dentro de la app hay un botón **Agregar el
   widget** si tu launcher lo permite.)
4. El widget cambia solo a la medianoche y al tocarlo abre la app.

---

## Probar solo la parte web (sin compilar nada)

```bash
cd www
python -m http.server 8000
```

Abrí <http://localhost:8000>. Se ve la interfaz completa; el widget y el
recordatorio quedan desactivados porque dependen de Android (la propia app lo
avisa).

---

## Ideas para más adelante

- Un historial con las frases ya vistas.
- Que el usuario pueda marcar favoritas.
- Widget circular como variante del rectangular.
- Descargar frases nuevas desde internet en vez de traerlas empaquetadas.
