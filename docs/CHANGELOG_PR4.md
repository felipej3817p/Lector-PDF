# CHANGELOG PR4

## Ajustes de estabilidad
- Se corrigió el flujo de `upload/batch-auto` para devolver respuesta estructurada con métricas de lote (`batchId`, `batchCode`, `total`, `success`, `failed`, `apt`, `notApt`, `pendingReview`, `results`).
- Se eliminó una inconsistencia de firma en `DocumentBatchService.uploadAndAnalyze` que rompía compilación.
- Se conectó `DocumentController` con la nueva respuesta de lote.

## Estado actual
- Frontend compila (`npm run build`).
- Backend: pendiente validación final en entorno con acceso a repositorio Maven central (en este entorno falla por 403 al resolver parent POM).
