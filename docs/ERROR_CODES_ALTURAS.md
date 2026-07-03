# Catalogo de codigos de error Alturas

Las respuestas de error del backend ahora mantienen este formato:

```json
{
  "code": "ALT-DOC-002",
  "status": 409,
  "error": "Analisis documental no disponible",
  "message": "El documento debe tener analisis antes de aprobar.",
  "path": "/api/documents/123/approve",
  "timestamp": "2026-05-28T15:30:00Z"
}
```

## Codigos

| Codigo | HTTP | Area | Significado |
| --- | ---: | --- | --- |
| ALT-VAL-001 | 400 | Validacion | Datos invalidos o regla de negocio no cumplida. |
| ALT-VAL-002 | 400 | Validacion | Falta un parametro requerido. |
| ALT-FILE-001 | 413 | Archivos | La carga supera el tamano permitido. |
| ALT-PDF-001 | 400 | PDF | Archivo vacio, faltante o con formato no permitido. |
| ALT-PDF-002 | 404 | PDF | El registro existe, pero el archivo fisico no se encontro. |
| ALT-PDF-003 | 422 | PDF | No se pudo extraer texto del PDF. |
| ALT-DOC-001 | 404 | Documento | Documento no encontrado. |
| ALT-DOC-002 | 409 | Documento | El documento no tiene analisis guardado. |
| ALT-DOC-003 | 409 | Documento | Resultado pendiente o no concluyente. |
| ALT-EMP-001 | 404 | Trabajador | Trabajador o persona asociada no encontrada. |
| ALT-USR-001 | 404 | Usuario | Usuario no encontrado. |
| ALT-AUTH-001 | 401 | Seguridad | Credenciales invalidas. |
| ALT-AUTH-002 | 403 | Seguridad | Usuario sin permisos para la accion. |
| ALT-AUTH-003 | 400 | Seguridad | Token invalido o ya usado. |
| ALT-AUTH-004 | 400 | Seguridad | Token expirado. |
| ALT-MAIL-001 | 502 | Correo | Fallo al enviar correo SMTP. |
| ALT-CONF-001 | 500 | Configuracion | Configuracion interna incompleta o invalida. |
| ALT-DATA-001 | 409 | Datos | Registro duplicado o conflicto de datos. |
| ALT-SYS-001 | 500 | Sistema | Error interno no controlado. |

## Uso operativo

- Para usuarios finales se muestra `message`.
- Para soporte se reporta `code`, `path` y hora aproximada.
- Para logs del servidor se busca el mismo momento del `timestamp`.
- Si aparece `ALT-PDF-002`, revisar almacenamiento/ruta fisica de PDFs.
- Si aparece `ALT-MAIL-001`, revisar SMTP, credenciales y destinatarios.
- Si aparece `ALT-DOC-002`, el PDF se cargo pero no termino el analisis.
