# CHANGELOG PR4

## Ajustes de estabilidad
- Se corrigió el flujo de `upload/batch-auto` para devolver respuesta estructurada con métricas de lote (`batchId`, `batchCode`, `total`, `success`, `failed`, `apt`, `notApt`, `pendingReview`, `results`).
- Se eliminó una inconsistencia de firma en `DocumentBatchService.uploadAndAnalyze` que rompía compilación.
- Se conectó `DocumentController` con la nueva respuesta de lote.

## Estado actual
- Frontend compila (`npm run build`).
- Backend: pendiente validación final en entorno con acceso a repositorio Maven central (en este entorno falla por 403 al resolver parent POM).

## 2026-05-07 - Configuración dinámica de correos y aprobadores
- Se agregó backend de `AppSetting` en Mongo (`app_settings`) con endpoints `/api/settings`, `/api/settings/email` y `/api/settings/zone-coordinators`.
- El flujo de notificación al trabajador dejó de ejecutarse automáticamente durante análisis; ahora solo ocurre en aprobación/reenvío.
- El envío al aprobador de cargas masivas se dispara automáticamente al finalizar la carga/lote.
- Los destinatarios del aprobador y copias del trabajador ahora se leen primero de MongoDB y usan `application.properties` como fallback.
- Se agregó `src/api/settings.js` y mejora visual del menú con acceso a Configuración (engranaje).
