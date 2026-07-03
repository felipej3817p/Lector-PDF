# Manual de Usuario y Programador - Alturas / Lector PDF

Fecha: 15/06/2026

## 1. Objetivo

Alturas / Lector PDF permite administrar trabajadores, cargar conceptos medicos de aptitud para trabajo en alturas, revisar resultados, enviar notificaciones, conservar historial documental y controlar usuarios por rol, zona y vigencia.

El sistema separa dos flujos:

- **Evaluaciones**: PDFs actuales que cuentan para revision, resultado visible del trabajador y notificacion.
- **Historial**: PDFs antiguos o de respaldo, guardados para consulta documental. No afectan el resultado actual del trabajador.

## 2. Manual de Usuario

### 2.1 Roles

r

- **ADMIN**: administra usuarios, roles, zonas, vigencias, auditoria, configuracion, trabajadores y documentos.
- **OPERADOR**: carga PDFs, gestiona trabajadores y documentos segun permisos asignados.
- **APROBADOR**: revisa conceptos, ve PDFs y aprueba o rechaza evaluaciones.
- **VISUALIZADOR**: consulta datos y resultados permitidos. No ve PDFs ni informacion de revision/correo.

### 2.2 Inicio de sesion

El usuario ingresa con usuario/correo y contrasena.

El sistema bloquea el acceso si:

- el usuario esta inactivo,
- la cuenta esta fuera de vigencia,
- no tiene rol vigente,
- las credenciales son incorrectas.

### 2.3 Trabajadores

Desde **Trabajadores** se puede:

- buscar trabajadores,
- filtrar por zona, resultado, revision, notificacion y fechas,
- crear trabajador,
- editar trabajador,
- ver datos,
- consultar historial,
- ver PDF si el rol lo permite,
- eliminar trabajador.

Campos obligatorios para crear trabajador:

- tipo de documento,
- numero de documento,
- primer nombre,
- primer apellido,
- correo.

Campos opcionales:

- segundo nombre,
- segundo apellido,
- cargo,
- zona principal,
- zona/subzona,
- area de trabajo,
- empleador,
- ARL,
- genero,
- fecha de nacimiento.

La zona puede quedar vacia porque hay trabajadores sin zona fija. Lo importante para asociar PDFs es que el trabajador tenga documento registrado.

### 2.4 Eliminar trabajador

ADMIN y OPERADOR pueden eliminar trabajadores. APROBADOR y VISUALIZADOR no pueden eliminar trabajadores.

Al eliminarlo, tambien se eliminan sus datos asociados:

- PDFs,
- historial documental,
- evaluaciones,
- correos asociados,
- constancias/certificados asociados.

Debe usarse solo para registros creados por error o pruebas.

### 2.5 Carga normal de evaluaciones

Usar este flujo para PDFs actuales que deben entrar a revision.

El sistema intenta leer:

- cedula,
- trabajador asociado,
- resultado APTO / NO APTO,
- fecha de evaluacion,
- fecha de nacimiento,
- datos principales del concepto.

Resultado esperado:

- el documento queda en **Revision**,
- el aprobador puede aprobar o rechazar,
- el correo al trabajador se envia solo despues de aprobar/rechazar segun el flujo configurado.

### 2.6 Carga historica

Usar este flujo para PDFs antiguos o de respaldo.

Comportamiento:

- guarda el PDF en el historial del trabajador si encuentra la cedula,
- intenta leer APTO / NO APTO,
- no envia correos,
- no pasa el documento al panel de revision,
- no cambia el resultado actual del trabajador,
- si no encuentra trabajador o no puede leer el archivo, registra el caso en **PDF no asociados**.

En el historial del trabajador hay dos secciones:

- **Evaluaciones**: documentos actuales que afectan revision/notificacion.
- **Historial**: documentos historicos que solo se consultan como soporte.

En la seccion **Historial** no se muestran columnas de revision ni correo porque no aplican a ese flujo.

### 2.7 PDF no asociados

Esta pantalla muestra PDFs que no pudieron relacionarse con ningun trabajador.

Acciones:

- ver PDF,
- eliminar un registro,
- borrar todos,
- revisar motivo del error.

Uso recomendado:

1. Revisar el motivo.
2. Crear o corregir el trabajador si aplica.
3. Volver a cargar el PDF.
4. Borrar los registros no asociados cuando ya no se necesiten.

### 2.8 Ver PDF

El boton **Ver PDF** descarga el archivo usando la sesion autenticada.

Esto evita problemas en servidor con:

- rutas relativas,
- tokens,
- sesiones,
- bloqueo de ventanas emergentes.

VISUALIZADOR no puede abrir PDFs por politica de privacidad.

### 2.9 Revision

El APROBADOR puede:

- consultar pendientes,
- ver PDF,
- ver detalle,
- aprobar,
- rechazar,
- dejar comentario.

Los historicos no deben aparecer como pendientes de revision.

### 2.10 Correos

El sistema maneja principalmente:

- correo al aprobador: aviso de nuevos documentos por revisar,
- correo al trabajador: resultado final despues de aprobacion/rechazo.

Los correos de copia configurados reciben copia del correo individual cuando aplique.

La carga historica no envia correos.

### 2.11 Usuarios

Solo ADMIN gestiona usuarios.

Acciones:

- crear usuario,
- editar usuario,
- activar/inactivar,
- eliminar usuario,
- cambiar contrasena,
- asignar varios roles,
- configurar vigencia por rol,
- asignar zonas,
- configurar vigencia por zona,
- definir vigencia general de cuenta.

La vigencia general bloquea el ingreso completo. Las vigencias de rol/zona solo afectan ese permiso especifico.

### 2.12 Auditoria

La auditoria registra cambios de usuarios, roles y zonas.

Coleccion MongoDB:

- `user_audit_logs`

Registra:

- usuario modificado,
- quien modifico,
- accion,
- campo,
- valor anterior,
- valor nuevo,
- fecha.

Retencion configurable:

- 24 horas, para pruebas,
- 3 meses,
- 6 meses,
- 12 meses,
- 24 meses,
- 60 meses.

En produccion se recomienda minimo 12 meses o lo que defina la politica interna.

## 3. Manual del Programador

### 3.1 Estructura del proyecto

- `alturas-backend`: API Spring Boot.
- `alturas-frontend`: aplicacion Vue/Vite.
- `docs`: documentacion funcional y tecnica.

### 3.2 Backend

Tecnologias:

- Java,
- Spring Boot,
- Spring Security,
- JWT,
- MongoDB.

Paquetes principales:

- `controller`: endpoints REST.
- `service`: reglas de negocio.
- `model`: entidades/documentos MongoDB.
- `dto`: objetos de entrada y salida.
- `repository`: acceso a MongoDB.
- `security`: autenticacion, JWT y reglas de seguridad.
- `exception`: errores controlados y mensajes de API.
- `config`: configuracion inicial, CORS y datos base.

### 3.3 Frontend

Tecnologias:

- Vue,
- Vite,
- Pinia,
- Vue Router.

Carpetas principales:

- `views`: pantallas.
- `components`: componentes reutilizables.
- `api`: cliente HTTP y funciones de API.
- `stores`: estado global, principalmente autenticacion.
- `router`: rutas y guardas.
- `utils`: utilidades compartidas.
- `assets`: estilos globales.

### 3.4 Flujos principales de codigo

Autenticacion:

- Backend: `AuthController`, `AuthService`, `JwtService`, `JwtAuthFilter`.
- Frontend: `LoginView.vue`, `stores/auth.js`, `api/http.js`.

Trabajadores:

- Backend: `EmployeeController`, `EmployeeService`, `EmployeeRepository`.
- Frontend: `EmployeeListView.vue`, `EmployeeHistoryView.vue`, `EmployeeFormView.vue`.

Documentos:

- Backend: `DocumentController`, `DocumentService`, `DocumentAnalysisService`, `DocumentBatchService`.
- Frontend: `DocumentUploadView.vue`, `DocumentsListView.vue`, `DocumentDetailView.vue`, `ReviewPanelView.vue`.

PDF no asociados:

- Backend: `HistoricalImportIssue`, `HistoricalImportIssueRepository`, metodos historicos en `DocumentBatchService`.
- Frontend: `HistoricalImportIssuesView.vue`.

Usuarios y auditoria:

- Backend: `UserController`, `UserService`, `UserAuditLogService`, `SystemSettingsService`.
- Frontend: `SettingsView.vue`, `UserAuditView.vue`.

Reportes:

- Backend: `ReportController`, `ReportService`, `ApprovedExportService`, `ConsolidatedExportService`.
- Frontend: `ReportsView.vue`.

### 3.5 Endpoints principales

Autenticacion:

- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

Trabajadores:

- `GET /api/employees`
- `GET /api/employees/dashboard`
- `GET /api/employees/{id}`
- `POST /api/employees`
- `PUT /api/employees/{id}`
- `DELETE /api/employees/{id}`
- `GET /api/employees/{id}/history`

Documentos:

- `GET /api/documents`
- `GET /api/documents?historical=false`
- `GET /api/documents/{id}`
- `GET /api/documents/{id}/view`
- `GET /api/documents/{id}/analysis`
- `GET /api/documents/{id}/analyze`
- `POST /api/documents/upload`
- `POST /api/documents/upload/batch-auto`
- `POST /api/documents/{id}/approve`
- `POST /api/documents/{id}/reject`
- `DELETE /api/documents/{id}`

Historicos no asociados:

- `GET /api/documents/historical/issues`
- `GET /api/documents/historical/issues/{id}/view`
- `DELETE /api/documents/historical/issues/{id}`
- `DELETE /api/documents/historical/issues/all`

Usuarios:

- `GET /api/users`
- `POST /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`
- `GET /api/users/audit-logs`

Configuracion:

- `GET /api/settings`
- `PUT /api/settings`

### 3.6 Reglas importantes

- La carga normal genera documentos para revision.
- La carga historica guarda documentos de soporte y puede leer APTO/NO APTO, pero no cambia el resultado actual del trabajador.
- `historical=false` en `/api/documents` se usa para excluir historicos de pantallas operativas.
- VISUALIZADOR no puede abrir PDFs.
- APROBADOR puede abrir PDFs y revisar, pero no administra usuarios.
- ADMIN usa rol principal de informatica; no existe rol CONFIGURADOR.
- Trabajadores pueden existir sin zona.
- Para asociar PDFs se usa principalmente el documento/cedula.

### 3.7 Comentarios en codigo

No se debe llenar el codigo de comentarios obvios.

Comentarios utiles:

- reglas de negocio que no son evidentes,
- decisiones de seguridad,
- diferencias entre carga normal e historica,
- manejo de casos especiales,
- motivos de una validacion importante.

Comentarios que deben evitarse:

- explicar lo que ya dice el nombre del metodo,
- repetir linea por linea lo que hace el codigo,
- dejar comentarios desactualizados,
- escribir comentarios para ocultar codigo confuso.

Buena practica:

- primero escribir nombres claros,
- separar funciones grandes,
- documentar reglas complejas en `docs`,
- comentar solo donde ahorra confusion real.

### 3.8 Configuracion importante

Archivo:

- `alturas-backend/src/main/resources/application.properties`

Revisar antes de desplegar:

- MongoDB,
- SMTP,
- URL publica del frontend,
- tamanos multipart,
- carpeta de uploads,
- configuracion de reportes,
- retencion de auditoria.

### 3.9 Almacenamiento de archivos

Los PDFs se guardan en carpetas configuradas por backend.

En servidor se debe validar:

- permisos de escritura,
- permisos de lectura,
- backups,
- espacio en disco,
- ruta persistente fuera de carpetas temporales.

### 3.10 Pruebas recomendadas

Backend:

```bash
mvn.cmd test
```

Frontend:

```bash
npm.cmd run build
```

Pruebas funcionales:

1. Login con ADMIN, OPERADOR, APROBADOR y VISUALIZADOR.
2. Crear trabajador sin zona.
3. Cargar evaluacion normal y revisar resultado.
4. Aprobar/rechazar documento.
5. Ver PDF desde Trabajadores, Documentos, Revision, Historial y Detalle.
6. Cargar historicos y confirmar que no aparecen como resultado actual.
7. Revisar Historial separado de Evaluaciones.
8. Consultar PDF no asociados.
9. Eliminar trabajador de prueba con documentos asociados.
10. Consultar auditoria y probar filtros.

### 3.11 Despliegue

Antes de subir al servidor:

1. Ejecutar pruebas backend.
2. Compilar frontend.
3. Confirmar variables de `application.properties`.
4. Confirmar que MongoDB este disponible.
5. Confirmar carpeta `uploads`.
6. Confirmar SMTP.
7. Probar una carga normal pequena.
8. Probar una carga historica pequena.
9. Probar Ver PDF.
10. Probar roles y permisos.

## 4. Mantenimiento

Recomendaciones:

- Mantener backups de MongoDB y de `uploads`.
- No borrar auditoria sin politica definida.
- Usar retencion de auditoria segun politica interna.
- Revisar logs cuando aparezca codigo `ALT-SYS-001`.
- No usar carga historica para documentos que deban ir a revision.
- No usar carga normal para archivos antiguos si no se quiere notificacion.
