# Identidad visual de Kind

Resumen de lo que se tomó del manual de marca (`KIND BASE.pdf`) y dónde vive
dentro del proyecto.

## Colores

| Color   | Hex       | Dónde se usa |
|---------|-----------|--------------|
| Café    | `#7D604A` | textos secundarios, firma |
| Arena   | `#ECE1BE` | botones suaves, fondos |
| Crema   | `#F6F4EC` | fondo general de la app |
| Verde   | `#8EAB1C` | logo, títulos, botón principal |
| Menta   | `#BEE6DD` | acentos, tarjetas |
| Ámbar   | `#FFB300` | avisos |

Definidos en dos lugares (hay que cambiarlos en ambos si se ajusta la paleta):

- `www/styles.css` → variables `--brown`, `--sand`, `--cream`, `--green`, `--mint`, `--amber`
- `android/app/src/main/res/values/colors.xml`
- `tools/generate_cards.py` → constantes `BROWN`, `SAND`, `GREEN`, …

## Tipografía

El manual usa **Nunito** (títulos y logo) y **Open Sauce One** (textos), más
**Michegar** para las frases del personaje. Open Sauce y Michegar son de pago,
así que el proyecto usa **Nunito** para todo y su **itálica** para las frases:
mantiene el mismo aire redondo y amable, y se puede distribuir sin licencia
(SIL Open Font License).

Los archivos están en `www/assets/fonts/` y se empaquetan dentro del APK.

## Logo e icono

- El icono de la app es el **circulito amarillo** con degradado radial
  (`#FFD600` al centro, `#FFF582` al borde) sobre fondo crema.
- El logo es la palabra *Kind* en verde con el sol como punto de la **i**.

Ambos se generan por código en `tools/generate_cards.py` (funciones `sun()`,
`make_logo()` y `make_icons()`), así que se pueden regenerar en cualquier
tamaño sin perder calidad.

## Personaje

**Tiger** es quien firma las frases (`Att: Tiger`). Su ilustración quedó
guardada en `www/assets/img/tiger.png` por si más adelante se usa dentro de la
app o en las tarjetas.

## Tono de las frases

Cortas, en segunda persona, amables y sin exigir. Siempre firmadas por Tiger.
