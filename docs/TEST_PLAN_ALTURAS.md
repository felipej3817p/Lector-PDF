# Plan de pruebas manuales Alturas (PR4)

## 1) Login y permisos
1. Iniciar sesión con usuario APROBADOR.
2. Verificar acceso a panel de revisión y detalle de documentos.
3. Iniciar sesión con usuario no aprobador y validar restricciones.

## 2) Carga individual
1. Ir a carga de documento.
2. Subir un PDF válido asociado a trabajador existente.
3. Validar creación de documento, estado de revisión `PENDING_REVIEW` y análisis disponible.

## 3) Carga masiva + lote
1. Ir a carga masiva y subir varios PDFs (mezcla válidos/erróneos).
2. Confirmar respuesta con `batchId`, `batchCode`, `total`, `success`, `failed`, `apt`, `notApt`, `pendingReview`.
3. Consultar `GET /api/document-batches` y `GET /api/document-batches/{id}`.
4. Consultar `GET /api/document-batches/{id}/documents`.

## 4) Correo resumen a aprobador
1. Configurar `app.email.approver-to` en backend.
2. Finalizar una carga masiva.
3. Verificar EmailLog `APPROVER_BATCH_NOTIFICATION` con estado `SENT` o `FAILED`.
4. Probar `POST /api/document-batches/{id}/notify-approver`.

## 5) Aprobación individual
1. Abrir un documento pendiente con resultado APTO/NO_APTO.
2. Aprobar con comentario.
3. Validar `reviewedBy`, `reviewedAt`, `reviewComment`, `notificationStatus`.

## 6) Aprobación masiva
1. Enviar `POST /api/documents/approve-bulk` con varios IDs pendientes.
2. Validar resumen `total/approved/failed/errors`.
3. Enviar `POST /api/documents/reject-bulk` para rechazo masivo.

## 7) Reenvío de correo al trabajador
1. Ejecutar `POST /api/documents/{id}/resend-email` en documento `APPROVED`.
2. Validar nuevo EmailLog y actualización de estado de notificación.
3. Probar con documento no aprobado y confirmar mensaje de error.

## 8) Historial por trabajador
1. Abrir edición de trabajador.
2. Validar sección de historial con múltiples evaluaciones y acceso a detalle.

## 9) Reportes
1. Ejecutar exportación Excel consolidado (endpoint existente).
2. Validar contenido mínimo y descarga.
3. CSV Ministerio: validar si está implementado; si no, registrar pendiente.

## 10) Importación de trabajadores
1. Validar endpoints de importación/plantilla si existen.
2. Si no existen, registrar pendiente técnico en backlog.
