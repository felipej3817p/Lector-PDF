# Arquitectura y Diccionario de Archivos - Lector PDF Alturas

Este documento es el diccionario técnico y funcional del proyecto. Describe la estructura de directorios, la arquitectura del sistema y la responsabilidad específica de cada archivo dentro del código fuente.

---

## 📁 `alturas-frontend`
**Propósito:** Contiene la aplicación cliente (Single Page Application) desarrollada con Vue 3. Se encarga de la interfaz de usuario, la gestión del estado global en el navegador y la comunicación asíncrona con el backend.

* 📄 `package.json`: Define las dependencias del proyecto (Vue, Vue Router, Pinia, Axios, Bootstrap, etc.) y los scripts de ejecución (dev, build, preview).
* 📄 `package-lock.json`: Asegura que las instalaciones futuras o en otros entornos utilicen exactamente las mismas versiones de las dependencias definidas.
* 📄 `vite.config.js`: Configura Vite, el empaquetador y servidor de desarrollo. Incluye configuraciones de proxy para redirigir llamadas de `/api` hacia el backend en desarrollo y la resolución de alias (ej. `@/` hacia `src/`).
* 📄 `index.html`: Punto de entrada del navegador. Contiene el div con id `app` donde Vue monta toda la jerarquía de componentes dinámicamente.
* 📄 `.gitignore`: Define exclusiones para el control de versiones (ignora `node_modules`, archivos `.env`, carpetas de build `dist`, etc.).
* 📄 `README.md`: Documentación base para el desarrollador frontend con comandos esenciales para instalación y despliegue.

### 📁 `alturas-frontend/public`
**Propósito:** Archivos estáticos que no requieren procesamiento por parte de Vite y se sirven directamente desde la raíz web.
* 📄 `.htaccess`: Configuración de Apache que redirige todas las peticiones a `index.html`, permitiendo que el enrutamiento de Vue Router funcione sin errores 404 al recargar la página.
* 📄 `sst-alturas-icon.svg`: Favicon e icono principal vectorizado de la aplicación.

### 📁 `alturas-frontend/src`
**Propósito:** Directorio principal del código fuente (Source) de la aplicación Vue.
* 📄 `main.js`: Archivo de arranque de Vue. Instancia la aplicación principal, inyecta Vue Router, Pinia (manejo de estado) y monta el componente raíz en el DOM.
* 📄 `App.vue`: Componente raíz principal que envuelve a todas las vistas mediante `<router-view>` y administra estructuras globales si es necesario.

#### 📁 `alturas-frontend/src/api`
**Propósito:** Capa de servicios REST del frontend. Encapsula las llamadas HTTP usando Axios, separando la lógica de comunicación de red de los componentes visuales.
* 📄 `http.js`: Instancia configurada de Axios. Intercepta las peticiones salientes para inyectar el token JWT en la cabecera `Authorization` y maneja errores globales (ej. cierre de sesión en 401).
* 📄 `auth.js`: Peticiones HTTP relacionadas a la autenticación (login, recuperación de contraseña).
* 📄 `document.js`: Operaciones CRUD sobre documentos. Maneja la subida de archivos (multipart/form-data) y la obtención de listados paginados.
* 📄 `employee.js`: Llamadas REST para obtener, buscar, crear o actualizar la información de los empleados en base de datos.
* 📄 `employeeHistory.js`: Recupera la línea de tiempo y trazabilidad de los conceptos (aptitudes médicas/trabajos) de un empleado.
* 📄 `reports.js`: Se comunica con los endpoints que procesan y devuelven métricas y descargas de reportes en formato CSV/Excel.
* 📄 `systemSettings.js`: Obtiene y actualiza configuraciones globales del sistema, como parámetros de conexión SMTP.
* 📄 `trainingCertificates.js`: Endpoints para gestión (subida y consulta) de certificados de entrenamiento y capacitación.
* 📄 `accessRequest.js`: Manejo de peticiones para permisos y autorizaciones de acceso a zonas específicas.

#### 📁 `alturas-frontend/src/assets`
**Propósito:** Recursos estáticos que sí son procesados por Vite (CSS, fuentes, imágenes locales).
* 📄 `main.css`: Hoja de estilos global. Define variables CSS, un reset básico y estilos de la aplicación que complementan a las librerías instaladas.

#### 📁 `alturas-frontend/src/components`
**Propósito:** Componentes de Vue reutilizables a lo largo de varias vistas.
* 📄 `AppNavbar.vue`: Componente estructural de la barra de navegación lateral o superior, implementa enlaces de enrutamiento y opciones de cierre de sesión.
* 📄 `TrainingCertificatesPanel.vue`: Sub-componente para visualizar y gestionar específicamente la lista de certificados dentro del perfil de un empleado.
* 📄 `HelloWorld.vue`: Archivo genérico o de demo temporal.

#### 📁 `alturas-frontend/src/layouts`
**Propósito:** Componentes contenedores que definen la estructura general de la página (layout).
* 📄 `AppLayout.vue`: Define el esqueleto de la aplicación post-login, incrustando el `AppNavbar` y un contenedor principal dinámico para el `<router-view>` hijo.

#### 📁 `alturas-frontend/src/router`
**Propósito:** Configuración del enrutamiento del lado del cliente (Vue Router).
* 📄 `index.js`: Define todas las rutas de la aplicación web y asigna los componentes Vista correspondientes. Implementa "Navigation Guards" (`beforeEach`) para proteger rutas privadas si no hay un token válido en sesión.

#### 📁 `alturas-frontend/src/stores`
**Propósito:** Manejadores de estado global de la aplicación (Pinia).
* 📄 `auth.js`: Store de autenticación. Guarda en memoria local la información del usuario logueado, su token JWT, sus roles y expone funciones globales para login/logout de forma reactiva.

#### 📁 `alturas-frontend/src/utils`
**Propósito:** Funciones y constantes auxiliares, utilerías que no están acopladas a los componentes visuales de Vue.
* 📄 `areaCatalog.js`: Diccionario estático o catálogo en memoria de las áreas y zonas físicas manejadas en el dominio del negocio.
* 📄 `documentUploadHelpers.js`: Funciones de lógica de negocio para gestionar pesos, extensiones y lógica de encolamiento al cargar múltiples documentos.
* 📄 `themePreferences.js`: Script auxiliar para la gestión (lectura y persistencia) de preferencias de interfaz de usuario como el modo claro o modo oscuro.

#### 📁 `alturas-frontend/src/views`
**Propósito:** Componentes principales que representan páginas completas, cada uno mapeado directamente a una ruta de navegación web.
* 📄 `LoginView.vue`: Interfaz de inicio de sesión de usuario y manejo de errores de autenticación con el servidor.
* 📄 `HomeView.vue`: Panel de inicio principal (Dashboard) que se presenta tras ingresar exitosamente.
* 📄 `DocumentUploadView.vue`: Interfaz interactiva para recepción de arrastrar-y-soltar (Drag&Drop), pre-visualización local y envío en masa de archivos PDF de los trabajadores.
* 📄 `ReviewPanelView.vue`: Panel del Rol Aprobador para auditar los documentos extraídos por el sistema y cambiar su estado entre "Aprobar" o "Rechazar", con visualización contextual.
* 📄 `ReportsView.vue`: Consola analítica que expone vistas tabulares y descargas de consolidado de excel a través de reportes parametrizables.
* 📄 `SettingsView.vue`: Panel para ajustar la configuración central de la app, correos, y acceder a configuraciones de sub-catálogos.
* 📄 `EmployeeListView.vue`: Pantalla con el listado tabular (DataGrid) central, ofreciendo filtros complejos sobre todos los perfiles de los trabajadores.
* 📄 `EmployeeFormView.vue`: Formulario extenso que captura o actualiza todos los datos demográficos y específicos de la ficha de un empleado.
* 📄 `EmployeeHistoryView.vue`: Línea de tiempo visual y listado que audita todo el progreso médico y documental de un empleado en particular.
* 📄 `DocumentDetailView.vue`: Vista para inspeccionar a fondo un `ManagedDocument` específico y visualizar datos de los motores de PDF.
* 📄 `DocumentsListView.vue`: Lista maestra de todos los documentos en el sistema con atajos para ver o eliminar según los roles asignados.
* 📄 `UserAuditView.vue`: Visor para que un Administrador lea todas las bitácoras o transacciones registradas del sistema operativo sobre los usuarios.
* 📄 `UsersView.vue`: Vista dedicada a gestionar (Crear, Editar, Borrar y Resetear Claves) a los operarios y aprobadores del sistema.
* 📄 `HistoricalImportIssuesView.vue`: Consola de solución de problemas para atender a los documentos que fallaron su importación original debido a formatos inválidos o errores del OCR.
* 📄 `ForgotPasswordView.vue`: Interfaz para ingresar un correo electrónico e iniciar la recuperación de contraseñas.
* 📄 `ResetPasswordView.vue`: Interfaz de formulario validado para confirmar y establecer la contraseña final desde el proceso de reseteo.

---

## 2. ⚙️ ALTURAS-BACKEND
**Propósito:** Servidor y API RESTful construido en Java con Spring Boot. Gestiona la lógica de negocio, procesamiento de PDFs (OCR), seguridad por JWT, envío de correos SMTP y persistencia en base de datos NoSQL.

* 📄 `pom.xml`: Project Object Model de Maven. Define dependencias de Spring Boot, librerías de procesamiento PDF (Apache PDFBox), conectores a MongoDB (spring-boot-starter-data-mongodb) y utilidades como Lombok y JSON Web Token.
* 📄 `mvnw` / `mvnw.cmd`: Wrapper de Maven. Permite compilar y ejecutar el proyecto sin requerir instalación manual de Apache Maven en el entorno del sistema operativo.
* 📄 `.gitignore`: Reglas para omitir compilados (`target/`) e IDE metadata de Git.

### 📁 `alturas-backend/src/main/resources`
**Propósito:** Archivos de propiedades y recursos empaquetados junto a la aplicación Java.
* 📄 `application.properties`: Configuraciones medulares del entorno, incluyendo variables de red (puerto 8081), URIs y configuraciones de pool de conexiones para MongoDB, e inicialización paramétrica de Mail.

### 📁 `alturas-backend/src/main/java/com/backend`
* 📄 `BackendApplication.java`: Clase principal con la anotación `@SpringBootApplication`. Arranca el contexto interno de Spring, inicia el Tomcat embebido y dispara el inicio del ciclo de vida de la API.

#### 📁 `alturas-backend/src/main/java/com/backend/config`
**Propósito:** Clases marcadas con `@Configuration` para inicialización de beans y configuraciones técnicas transversales.
* 📄 `CorsConfig.java`: Configura las políticas de control de acceso HTTP (CORS), habilitando específicamente métodos y encabezados para permitir las comunicaciones cruzadas desde el cliente en el puerto de Vue.
* 📄 `DataSeeder.java`: Componente `CommandLineRunner` que entra en acción solo al arrancar. Escanea si el sistema es "virgen" sin usuarios y crea un usuario Administrador (Seed) con contraseña predeterminada para evitar bloqueos del sistema.
* 📄 `MongoConfig.java`: Ajustes adicionales explícitos sobre las conexiones para Spring Data MongoDB, incluyendo conversiones o control de transacciones.

#### 📁 `alturas-backend/src/main/java/com/backend/security`
**Propósito:** Interceptores basados en Spring Security que aplican reglas estrictas de autorización de rutas y criptografía de identidades.
* 📄 `SecurityConfig.java`: Establece qué rutas de la API son públicas (ej. login) y cuáles exigen JWT u observancia de roles (`ROLE_ADMIN` / `ROLE_OPERATOR`), mientras desactiva protecciones estado-orientadas (CSRF y Session) propias de aplicaciones web monolíticas.
* 📄 `JwtService.java`: El motor criptográfico. Compone JSON Web Tokens asignándoles fecha de expiración, inyecta roles/claims y los firma. También revierte el proceso validando firmas criptográficas en las peticiones entrantes.
* 📄 `JwtAuthFilter.java`: El centinela principal por cada Request HTTP. Detecta el Header `Authorization: Bearer <token>`, extrae la identificación, usa `JwtService` para validarla y autoriza a Spring a conceder el paso hacia el Controlador.

#### 📁 `alturas-backend/src/main/java/com/backend/controller`
**Propósito:** Capa superior de exposición de la API REST. Contiene las anotaciones `@RestController`. Aquí se reciben las llamadas HTTP (GET, POST), se parsean los parámetros (DTOs, Params), se enruta a la capa lógica (`Service`) y se serializa la respuesta JSON devuelta.
* 📄 `AuthController.java`: Expone `/api/auth`. Valida y emite los JWT.
* 📄 `DocumentController.java`: Interfaz pública para gestionar, actualizar o leer los documentos PDF guardados (`ManagedDocument`). Soporta carga "Multipart".
* 📄 `DocumentBatchController.java`: Aceleradores REST optimizados para la subida asíncrona de lotes masivos de archivos desde la vista de importación masiva del cliente.
* 📄 `EmployeeController.java`: Endpoints para las búsquedas (querying) y las mutaciones (crear/actualizar) de trabajadores en la plataforma.
* 📄 `EmployeeHistoryController.java`: Devuelve cronologías de actualizaciones, útil para trazar el histórico documental de una persona.
* 📄 `ReportController.java`: Gatilla las tareas de volcado y agregación devolviendo encabezados de tipo octet-stream para que el navegador descargue ficheros Excel/CSV.
* 📄 `UserController.java`: Abstracción para dar control del CRUD de cuentas a los perfiles de administradores (solo a estos roles).
* 📄 `SystemSettingsController.java`: Permite obtener o guardar propiedades configurables de red (e.g. SMTP) a persistencia dinámica sin reiniciar Java.
* 📄 `TrainingCertificateController.java`: Acepta flujos de subida de PDF específicamente orientados a cursos y entrenamientos en alturas.
* 📄 `AccessRequestController.java`: Administra el flujo de negocio sobre "solicitudes de acceso físico", capturando cuándo, quién y a dónde se solicita el permiso.
* 📄 `AuditLogController.java`: API para paginar los registros forenses insertados por la herramienta de Auditoría y llevarlos a los data-tables.
* 📄 `EmailLogController.java`: Controlador que reporta los errores o bitácoras específicas sobre transacciones fallidas en las notificaciones del host de correo.
* 📄 `SpaForwardController.java`: Captura cualquier petición que no inicie con `/api/` en Tomcat y la responde con el `index.html` de Vue para que el SPA enrutamiento no marque errores 404.

#### 📁 `alturas-backend/src/main/java/com/backend/service`
**Propósito:** El "cerebro" o la lógica empresarial profunda. Orquesta las bases de datos (Repositories), algoritmos complejos o librerías ajenas a Spring y garantiza transaccionalidad de negocio.
* 📄 `CustomUserDetailsService.java`: Puente requerido por Spring Security para buscar y encapsular un usuario MongoDB dentro del objeto de sesión `UserDetails` del marco Spring.
* 📄 `PdfTextExtractorService.java`: Instancia motores de `Apache PDFBox` para el procesamiento binario de PDFs leídos en Stream, recorre páginas y convierte información vectorial en un enorme texto crudo (String) escaneable.
* 📄 `PdfFieldParserService.java`: Analizador algorítmico y motor de Expresiones Regulares (Regex). Identifica patrones lingüísticos para deducir qué parte del texto de un documento corresponde a Cédulas, a Nombres o a diagnósticos médicos como "APTO".
* 📄 `DocumentAnalysisService.java`: Coordina a Extractor y Parser para aplicar un veredicto o filtro de calificación sobre el documento analizado de acuerdo a reglas de integridad de negocio.
* 📄 `DocumentService.java`: El orquestador del ciclo de vida del Documento. Se encarga de guardarlo en estado "PENDING", o de cambiarlo a "APPROVED" lo que en cascada requiere llamadas a EmployeeService para actualizar al trabajador.
* 📄 `DocumentReportPdfService.java`: Constructor de nuevos PDF's programáticos. Utiliza librerías para ensamblar informes y plantillas tabulares exportables a archivo desde la data.
* 📄 `EmailSendService.java`: Emplea la librería de envío de correos (JavaMail) configurada desde la base de datos para intentar la comunicación de red SMTP y despachar las alertas a destinatarios y aprobadores.
* 📄 `DocumentEmailTemplateService.java`: Constructor de plantillas de HTML. Mezcla datos estáticos y dinámicos para formatear mensajes legibles con los logos de la institución.
* 📄 `AuthService.java`: Abstracción de reglas de autenticación, verificación de hashing (`BCrypt`) y generación de tokens que usa el controlador.
* 📄 `UserService.java`: Servicio CRUD transaccional con reglas rígidas como impedir la creación de usuarios o correos duplicados.
* 📄 `AuditLogService.java` y `UserAuditLogService.java`: Sistemas asíncronos que insertan trazas perennes en la base de datos ante cambios de estado de otras entidades.
* 📄 `EmployeeService.java` y `EmployeeHistoryService.java`: Controla el demográfico maestro y dispara las grabaciones o "Snapshots" temporales de la línea de tiempo de eventos cada vez que apruebas un proceso a un empleado.
* 📄 `ReportService.java`: Utiliza abstracciones agregadas desde MongoDB u otros servicios para consolidar arrays y datos estadísticos para los dashboards.
* 📄 `ApprovedExportService.java` y `ConsolidatedExportService.java`: Algoritmos que leen todo el corpus documental base para concatenar archivos en CSV delimitado.
* 📄 `SystemSettingsService.java`: Abstracción de control y guardado de ajustes técnicos para evitar malformación en configuraciones del sistema en ejecución.
* 📄 `DocumentBatchService.java`, `DocumentBatchFacadeService.java`: Gestión de concurrencia e hilos para importaciones múltiples controlando fallos parciales sin caer la petición web total.
* 📄 `TrainingCertificateService.java` y `UserAccessEvaluator.java`: Interpreta las constancias y algoritmos que validan requerimientos duros para aprobar que un empleado ingrese a determinada zona.
* 📄 `AccessRequestService.java` y `AccessScopeService.java`: Validación, transaccionalidad e historización de todos los permisos y ámbitos o rangos geográficos.

#### 📁 `alturas-backend/src/main/java/com/backend/model`
**Propósito:** POJOs (Plain Old Java Objects) mapeados por la especificación Spring Data `@Document`. Representan las entidades semánticas y estructuras exactas de las colecciones BSON de MongoDB.
* 📄 `User.java`: Entidad `users`. Almacena la contraseña hasheada y asignación de rol (`Role.java`).
* 📄 `Employee.java`: Entidad `employees`. Mantiene el estado consolidado y más reciente sobre la aptitud del trabajador frente al riesgo en alturas.
* 📄 `ManagedDocument.java`: Entidad `managed_documents`. Representa archivos procesados por OCR indicando ubicación física (path) y la etapa actual del flujo de aprobación.
* 📄 `Role.java`: Elemento que centraliza la seguridad tipificada.
* 📄 `SystemSettings.java`, `UserAuditLog.java`, `EmployeeHistory.java`, etc.: Equivalen al modelo físico para las otras colecciones descritas en servicios.

#### 📁 `alturas-backend/src/main/java/com/backend/repository`
**Propósito:** Capa de abstracción y de acceso a la capa física de MongoDB (Data Access Object - DAO). Extendiendo `MongoRepository`, Spring Boot autogenera las consultas a nivel infraestructura durante tiempo de compilación.
* 📄 `UserRepository.java`: Genera `db.users.find()` por atributos como `email`.
* 📄 `EmployeeRepository.java`: Consultas complejas para recuperación o validación sobre `identificacionNumber`.
* 📄 `DocumentRepository.java`: Busca lotes de documentos paginados agrupados por `status`.
* (Otras interfaces homólogas controlan el resto de los modelos mencionados).

#### 📁 `alturas-backend/src/main/java/com/backend/dto`
**Propósito:** Clases sin lógica (Data Transfer Objects) dedicadas exclusivamente a estandarizar y modelar la estructura JSON enviada (Request) y recibida (Response) desde el cliente HTTP, actuando como escudo protector sobre el modelo real de Base de Datos.
* 📄 Ejemplos: `LoginRequest.java` (Correo y Pass limpios), `EmployeeResponse.java` (Entidad saneada ocultando campos técnicos inútiles a la interfaz), o proyecciones compuestas para APIs complejas.

#### 📁 `alturas-backend/src/main/java/com/backend/exception`
**Propósito:** Centralización del manejo de códigos de errores HTTP, envoltura segura de "StackTrace" caóticos.
* 📄 `GlobalExceptionHandler.java`: Un controlador de tipo `@ControllerAdvice` que vigila todo el ciclo. Captura excepciones de negocio o java nativo e interrumpe construyendo una respuesta JSON con formato único, mensajes predecibles y códigos como 404, 401 o 500 para el cliente de Vue.

### 📁 `alturas-backend/src/test`
**Propósito:** Archivos diseñados con librerías JUnit, Mockito o SpringBootTest para emulación automatizada de las funciones lógicas, permitiendo al desarrollador evaluar y comprobar el código sin ejecutar Tomcat.
* 📄 `BackendApplicationTests.java`: Asegura que toda la inyección de dependencias cargue sin conflictos irresolubles en memoria.
* 📄 `PdfFieldParserServiceTest.java`: Pruebas intensivas automatizadas al extractor Regex contra múltiples escenarios de texto de cédulas o diagnósticos fallidos.
* 📄 `UserAuditLogServiceTest.java`: Validaciones de inserciones correctas usando simuladores.

---

## 3. ¿CÓMO TRABAJA LA BASE DE DATOS MONGODB? (De Inicio a Fin)
El flujo transaccional y el ciclo de vida de los datos a través de las colecciones BSON NoSQL es el siguiente:

1. **Bootstrap (Inicialización):** Al arrancar el servidor en frío, Spring Data evalúa `system_settings` y `users`. Si se trata de un entorno limpio o de primera ejecución, el `DataSeeder` insertará automáticamente y con encriptación la cuenta Administrador (Seed) al sistema para prevenir interrupción de servicios y habilitar configuración inicial de reglas de correo en `system_settings`.
2. **Recepción del Archivo (Operadores):** Tras login en el Frontend, el componente `DocumentUploadView` envía los Multipart en el Request HTTP. El controlador direcciona a los Servicios correspondientes quienes implementan `PDFBox` para efectuar un reconocimiento avanzado extra-texto, aislando metadatos de los empleados (identidad y aptitud).
3. **Persistencia Inicial Desacoplada:** Tras extraer la información exitosamente, la base de datos inserta en `managed_documents` un registro con el estado `PENDING_REVIEW`, ligándolo al documento. En paralelo, busca al usuario en la colección `employees`; si este es nuevo lo inserta, si ya existe solo lo asocia al documento. El estado o aptitud del empleado **no** se modifica en esta fase para garantizar seguridad documental.
4. **Auditoría Transaccional Continua:** Durante los procesos antes mencionados, por cada método ejecutado en el backend, los servicios asíncronos guardan información en la colección `user_audit_logs` que traza exactamente qué Usuario hizo cada modificación, manteniendo inmutabilidad de logs.
5. **Revisión Final y Autorización (Aprobadores):** Cuando un usuario Aprobador carga su respectivo panel del Dashboard, el backend busca en base de datos todos los documentos en `PENDING_REVIEW`. Al confirmar uno de ellos, actualiza directamente la tupla de `managed_documents` transaccionando hacia `APPROVED`.
6. **Desencadenamiento de Historial:** Al confirmarse la validez oficial de un PDF con la aprobación, el backend finalmente insertará o actualizará la entidad base del trabajador referenciado en `employees`, reflejando la aptitud de trabajo y la respectiva vigencia. Simultáneamente creará una "Snapshot" histórica insertada en `employee_history` donde quedará registrada de por vida esta evolución. Como etapa final se inician los envíos vía SMTP mediante colas y plantillas estáticas registrando una entrada de diagnóstico en `email_logs`.
7. **Motor de Exportación Tabular:** A solicitud explícita del frontend desde los dashboards para descargar matrices de información, el servidor proyecta de forma filtrada colecciones interconectadas como lo son `managed_documents` que se hallen en validación de `APPROVED` uniéndolas a entidades completas de `employees`. Ensambla toda esta información para escupir directamente al Stream del `ReportController` el documento `csv/excel` que consume el usuario sin carga para la base de datos.
