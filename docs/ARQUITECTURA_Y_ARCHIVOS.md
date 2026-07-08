  # Arquitectura y Diccionario de Archivos - Lector PDF Alturas

  Este documento es el diccionario definitivo del proyecto. Muestra cada carpeta, qué hace esa carpeta, y qué hace exactamente cada uno de los archivos que tiene adentro, cubriendo todo el proyecto.

  ---

  ## 📁 `alturas-frontend`
  **¿Qué hace esta carpeta?** Es todo el proyecto de la interfaz gráfica web. Todo lo que el usuario ve y toca (botones, colores, pantallas) vive aquí.

  * 📄 `package.json`: Archivo de configuración que lista todas las librerías de internet que necesita el proyecto para funcionar (como Vue, Axios, Bootstrap).
  * 📄 `package-lock.json`: Archivo técnico que bloquea las versiones exactas de las librerías del `package.json` para que el proyecto no se rompa si una librería se actualiza en el futuro.
  * 📄 `vite.config.js`: Configura "Vite", que es el motor que compila el código de Vue y levanta el servidor local (en el puerto 5173). También envía las peticiones `/api` al backend.
  * 📄 `index.html`: Es la única página web real del proyecto. Vue toma este archivo vacío y dibuja toda la aplicación por dentro de él.
  * 📄 `.gitignore`: Lista los archivos que Git no debe subir a la nube (como contraseñas o librerías pesadas).
  * 📄 `README.md`: Archivo de texto básico con instrucciones de cómo correr el frontend.
  * 📄 `frontend-run.log` y `frontend-run.err.log`: Archivos temporales donde se guardan los mensajes y errores cuando ejecutas el servidor en tu computadora.

  ### 📁 `alturas-frontend/public`
  **¿Qué hace esta carpeta?** Guarda archivos públicos que el navegador puede descargar directamente sin que Vue los modifique.
  * 📄 `.htaccess`: Regla para el servidor web Apache. Evita que la página muestre error "404" si el usuario recarga la página estando en una URL como `/reportes`.
  * 📄 `sst-alturas-icon.svg`: Es el logotipo de la aplicación que se muestra en las pestañas del navegador web.

  ### 📁 `alturas-frontend/src`
  **¿Qué hace esta carpeta?** Aquí está todo el código fuente real que programaste para el frontend.
  * 📄 `main.js`: Es el archivo principal. Vue arranca aquí, lee la configuración, carga las rutas y monta la aplicación en el `index.html`.
  * 📄 `App.vue`: Es el contenedor visual principal. Todos los demás componentes se meten dentro de este "molde".

  #### 📁 `alturas-frontend/src/api`
  **¿Qué hace esta carpeta?** Guarda los archivos que se encargan exclusivamente de conectarse a internet para pedirle datos al Backend (usando Axios).
  * 📄 `http.js`: Es el conector maestro. Le pega tu "Token" (pasaporte) a todas las peticiones para demostrar que iniciaste sesión.
  * 📄 `auth.js`: Se encarga de enviar tu usuario y contraseña al backend para iniciar sesión.
  * 📄 `document.js`: Se encarga de enviar los PDFs nuevos al backend y pedir la lista de documentos subidos.
  * 📄 `employee.js`: Se encarga de pedir la lista de trabajadores o guardar uno nuevo.
  * 📄 `employeeHistory.js`: Se encarga de pedir la línea de tiempo de un trabajador específico.
  * 📄 `reports.js`: Se encarga de pedir los datos estadísticos y descargar los excels.
  * 📄 `systemSettings.js`: Se encarga de pedir o guardar la configuración de los correos.
  * 📄 `trainingCertificates.js`: Se encarga de subir o buscar certificados de entrenamiento.
  * 📄 `accessRequest.js`: Se encarga de gestionar los permisos de acceso a las zonas.

  #### 📁 `alturas-frontend/src/assets`
  **¿Qué hace esta carpeta?** Guarda cosas visuales como imágenes o código de diseño.
  * 📄 `main.css`: Guarda todos los colores, márgenes, tipos de letra y diseños que hacen que la página se vea bonita.
  * 📄 `vue.svg`: Imagen del logo de Vue.

  #### 📁 `alturas-frontend/src/components`
  **¿Qué hace esta carpeta?** Guarda pedazos pequeños de interfaz que se pueden usar muchas veces en diferentes pantallas.
  * 📄 `AppNavbar.vue`: Es el código de la barra de navegación lateral y superior (el menú).
  * 📄 `TrainingCertificatesPanel.vue`: Es un cuadrito o panel que se usa para mostrar los certificados de entrenamiento de una persona.
  * 📄 `HelloWorld.vue`: Es un archivo genérico de prueba (se puede borrar).

  #### 📁 `alturas-frontend/src/layouts`
  **¿Qué hace esta carpeta?** Define las estructuras o "marcos" de la página.
  * 📄 `AppLayout.vue`: Define que el menú siempre va a ir a la izquierda, la barra de usuario arriba, y el contenido en el centro.

  #### 📁 `alturas-frontend/src/router`
  **¿Qué hace esta carpeta?** Define el "GPS" de la aplicación.
  * 📄 `index.js`: Le dice al navegador que si el usuario escribe `/login` muestre la pantalla de Login, y si escribe `/reportes` muestre la de reportes. Además, actúa como guardia de seguridad, impidiendo que usuarios sin sesión entren a las rutas protegidas.

  #### 📁 `alturas-frontend/src/stores`
  **¿Qué hace esta carpeta?** Guarda la memoria temporal del usuario mientras navega.
  * 📄 `auth.js`: Recuerda quién eres, cómo te llamas y qué rol tienes (Ej. Admin) para no tener que preguntarle al backend en cada clic que das.

  #### 📁 `alturas-frontend/src/utils`
  **¿Qué hace esta carpeta?** Guarda funciones matemáticas o utilidades pequeñas que ayudan a otras partes del código.
  * 📄 `areaCatalog.js`: Tiene guardada una lista en texto con todas las "zonas" o "áreas" disponibles en la empresa.
  * 📄 `documentUploadHelpers.js`: Tiene matemáticas para calcular cuánto pesan los PDFs y partirlos en grupos de 10 o 20 antes de enviarlos.
  * 📄 `themePreferences.js`: Recuerda si te gusta el modo oscuro o el modo claro en tu pantalla.

  #### 📁 `alturas-frontend/src/views`
  **¿Qué hace esta carpeta?** Aquí viven todas las "Pantallas completas" del sistema. Cada archivo es una página distinta.
  * 📄 `LoginView.vue`: La pantalla donde pones usuario y clave.
  * 📄 `HomeView.vue`: La pantalla principal de bienvenida.
  * 📄 `DocumentUploadView.vue`: La pantalla donde arrastras y sueltas los archivos PDF para cargarlos.
  * 📄 `ReviewPanelView.vue`: La pantalla donde el "Aprobador" ve los PDF pendientes y les da "Aprobar" o "Rechazar".
  * 📄 `ReportsView.vue`: La pantalla que tiene las tablas y filtros para descargar los Excel.
  * 📄 `SettingsView.vue`: La pantalla gigante de configuración donde creas usuarios y configuras los correos.
  * 📄 `EmployeeListView.vue`: La pantalla que muestra la tabla con todos los trabajadores.
  * 📄 `EmployeeFormView.vue`: La pantalla que tiene los cuadros de texto para crear o editar un trabajador manualmente.
  * 📄 `EmployeeHistoryView.vue`: La pantalla que dibuja la línea de tiempo (historial) de los conceptos de un trabajador.
  * 📄 `DocumentDetailView.vue`: La pantalla para abrir un PDF, leerlo y ver qué datos le sacó el sistema.
  * 📄 `DocumentsListView.vue`: Una tabla gigante que muestra todos los documentos que existen.
  * 📄 `UserAuditView.vue`: La pantalla donde el jefe puede ver "quién hizo qué y a qué hora" (Auditoría).
  * 📄 `UsersView.vue`: Redirige a la pantalla de configuración de usuarios.
  * 📄 `HistoricalImportIssuesView.vue`: Pantalla para resolver problemas si se suben PDFs viejos que no se pudieron leer bien.
  * 📄 `ForgotPasswordView.vue`: Pantalla para pedir recuperar la clave perdida.
  * 📄 `ResetPasswordView.vue`: Pantalla para escribir la clave nueva.

  ---

  ## 2. ⚙️ ALTURAS-BACKEND
  **¿Qué hace esta carpeta?** Es el cerebro de la operación (El Servidor Java). Se encarga de guardar los datos, leer los PDFs y enviar correos.

  * 📄 `pom.xml`: Es la lista de mercado del backend. Le dice a Maven qué librerías descargar (como Spring Boot, MongoDB y PDFBox).
  * 📄 `mvnw` y `mvnw.cmd`: Son scripts que te permiten ejecutar el backend aunque no tengas Maven instalado en tu PC.
  * 📄 `.gitignore`: Le dice a Git que ignore la carpeta `target` donde se compila el Java.

  ### 📁 `alturas-backend/src/main/resources`
  **¿Qué hace esta carpeta?** Guarda propiedades del servidor.
  * 📄 `application.properties`: Es la configuración vital del servidor. Le dice a Java en qué puerto arrancar (8081), la IP de la base de datos MongoDB, y las credenciales del servidor de correos (10.1.200.20).

  ### 📁 `alturas-backend/src/main/java/com/backend`
  **¿Qué hace esta carpeta?** Contiene todo el código Java escrito.
  * 📄 `BackendApplication.java`: Es el interruptor principal. Cuando lo ejecutas, enciende todo el servidor de Spring Boot.

  #### 📁 `alturas-backend/src/main/java/com/backend/config`
  **¿Qué hace esta carpeta?** Configuraciones iniciales del sistema.
  * 📄 `CorsConfig.java`: Le dice al servidor que confíe en el Frontend (puerto 5173) y le permita conectarse.
  * 📄 `DataSeeder.java`: Cuando el servidor prende, revisa si hay usuarios. Si no hay ninguno, este archivo crea el usuario "admin" mágicamente para que no te quedes por fuera.
  * 📄 `MongoConfig.java`: Configura detalles técnicos de cómo Java se conecta a MongoDB.

  #### 📁 `alturas-backend/src/main/java/com/backend/security`
  **¿Qué hace esta carpeta?** Los guardias de seguridad del servidor.
  * 📄 `SecurityConfig.java`: Es el policía de las URLs. Revisa las rutas y dice "a /api/documents entra el Operador, a /api/settings entra el Admin, a las demás nadie entra sin llave".
  * 📄 `JwtTokenProvider.java`: Se encarga de fabricar la "llave" (Token) cuando alguien inicia sesión correctamente.
  * 📄 `JwtTokenFilter.java`: Revisa la llave en cada petición que llega para ver si es falsa o si ya caducó.
  * 📄 `CustomUserDetailsService.java`: Busca tu usuario en la base de datos para ver si de verdad existes antes de dejarte entrar.

  #### 📁 `alturas-backend/src/main/java/com/backend/controller`
  **¿Qué hace esta carpeta?** Son las puertas del servidor. Reciben las llamadas de internet (del frontend) y las envían a los servicios.
  * 📄 `AuthController.java`: Recibe tu usuario y contraseña cuando le das a "Ingresar".
  * 📄 `DocumentController.java`: Recibe los PDFs que subes y los envía a procesar.
  * 📄 `DocumentBatchController.java`: Controla el progreso cuando subes 100 PDFs al mismo tiempo.
  * 📄 `EmployeeController.java`: Recibe peticiones de "muéstrame a los trabajadores" o "crea este trabajador".
  * 📄 `ReportController.java`: Recibe la orden de fabricar el archivo CSV y te lo devuelve para que lo descargues.
  * 📄 `UserController.java`: Recibe peticiones para crear o borrar usuarios del sistema.
  * 📄 `SystemSettingsController.java`: Recibe la configuración de los correos que cambias desde la pantalla de Ajustes.
  * 📄 `TrainingCertificateController.java`: Recibe certificados de formación.
  * 📄 `AccessRequestController.java`: Recibe solicitudes para permisos en zonas.

  #### 📁 `alturas-backend/src/main/java/com/backend/service`
  **¿Qué hace esta carpeta?** Es el **CORAZÓN MATEMÁTICO Y LÓGICO** de la app. Hacen todo el trabajo pesado.
  * 📄 `PdfTextExtractorService.java`: Usa la librería PDFBox para abrir tu PDF, extraer todo el texto que tiene adentro y buscar la Cédula y si dice "APTO".
  * 📄 `PdfFieldParserService.java`: Trabaja con el anterior. Recorta los textos feos y los limpia para guardar solo los nombres y números puros.
  * 📄 `DocumentService.java`: Toma lo que leyó el extractor de PDFs, crea el documento en base de datos como "Pendiente" y luego permite cambiarlo a "Aprobado".
  * 📄 `EmailSendService.java`: Toma el servidor de correos (EBSA) y envía el mensaje final al Aprobador.
  * 📄 `DocumentEmailTemplateService.java`: Le pone colores bonitos (HTML) al correo antes de enviarlo.
  * 📄 `AuthService.java`: Revisa si la contraseña que pusiste es correcta comparándola con la encriptada en la BD.
  * 📄 `UserService.java`: Registra usuarios nuevos y se asegura de que no se repitan los correos.
  * 📄 `AuditLogService.java` y `UserAuditLogService.java`: Anotan silenciosamente en la BD todo lo que hacen los usuarios (Ej. "Admin borró al empleado X").
  * 📄 `EmployeeService.java` y `EmployeeHistoryService.java`: Crean empleados nuevos y actualizan su historial cada vez que se aprueba un PDF.
  * 📄 `ApprovedExportService.java` y `ConsolidatedExportService.java`: Fabrican las tablas de Excel (CSV) reuniendo toda la información de la base de datos.
  * 📄 `SystemSettingsService.java`: Revisa que la configuración de correos sea correcta y la guarda.
  * 📄 `DocumentBatchService.java`, `DocumentBatchFacadeService.java`: Organizan y agilizan la subida masiva de archivos.
  * 📄 `TrainingCertificateService.java` y `UserAccessEvaluator.java`: Validan lógicas secundarias de certificados y accesos.

  #### 📁 `alturas-backend/src/main/java/com/backend/model`
  **¿Qué hace esta carpeta?** Define las tablas o "moldes" que se guardarán en MongoDB.
  * 📄 `User.java`: El molde de un Usuario (tiene nombre, clave, rol).
  * 📄 `Employee.java`: El molde de un Trabajador (tiene nombre, cédula, zona).
  * 📄 `ManagedDocument.java`: El molde de un PDF procesado (tiene ruta, estado "APROBADO" o "PENDIENTE").
  * 📄 `Role.java`: Define qué puede y qué no puede hacer cada tipo de persona.
  * 📄 `SystemSettings.java`, `UserAuditLog.java`, `EmployeeHistory.java`, etc.: Moldes para configuraciones, auditoría y el historial.

  #### 📁 `alturas-backend/src/main/java/com/backend/repository`
  **¿Qué hace esta carpeta?** Se conecta físicamente con la base de datos MongoDB para hacer `INSERT`, `SELECT`, `UPDATE` o `DELETE`.
  * 📄 `UserRepository.java`: Busca usuarios en MongoDB por su email.
  * 📄 `EmployeeRepository.java`: Busca trabajadores en MongoDB por su cédula.
  * 📄 `DocumentRepository.java`: Busca todos los documentos que estén "PENDING_REVIEW" para mostrárselos al aprobador.
  * (Y otros archivos iguales para cada modelo: `AuditLogRepository.java`, `SystemSettingsRepository.java`, etc).

  #### 📁 `alturas-backend/src/main/java/com/backend/dto`
  **¿Qué hace esta carpeta?** Archivos temporales (mensajeros) que llevan datos de una capa a otra sin exponer la base de datos real.
  * 📄 Ejemplos: `LoginRequest.java` (solo lleva correo y clave), `EmployeeResponse.java` (lleva el empleado pero sin datos sensibles).

  #### 📁 `alturas-backend/src/main/java/com/backend/exception`
  **¿Qué hace esta carpeta?** Maneja cuando el sistema se rompe o algo sale mal.
  * 📄 `GlobalExceptionHandler.java`: Atrapa los errores (Ej. "Contraseña incorrecta") y le envía al frontend un mensaje bonito en lugar de un código de error feo.

  ### 📁 `alturas-backend/src/test`
  **¿Qué hace esta carpeta?** Pruebas de calidad del código hechas por los programadores.
  * 📄 `BackendApplicationTests.java`: Prueba que el servidor al menos logre encender sin estrellarse.
  * 📄 `PdfFieldParserServiceTest.java`: Prueba que el extractor de PDFs siga sabiendo leer cédulas sin fallar.
  * 📄 `UserAuditLogServiceTest.java`: Prueba que la auditoría sí esté grabando los movimientos.

  ---

  ## 3. ¿CÓMO TRABAJA LA BASE DE DATOS MONGODB? (De Inicio a Fin)
  La base de datos (MongoDB) es el disco duro de la aplicación. Trabaja con Colecciones (que son como tablas). Así fluye todo:

  1. **El Inicio:** Todo arranca en la colección `users` y `system_settings`. El sistema revisa que el administrador exista y carga las reglas de correos.
  2. **La Subida del PDF:** El Operador sube un PDF. Inmediatamente el backend inyecta en la colección `managed_documents` un registro con el PDF en estado `PENDING_REVIEW`. Al mismo tiempo, en la colección `employees` se crea al empleado si es que no existía.
  3. **La Auditoría:** Cada paso de la subida inyecta un texto en `user_audit_logs` diciendo "Operador subió un documento".
  4. **La Aprobación:** El Aprobador da clic en "Aprobar". El backend busca ese registro en `managed_documents` y le cambia la palabra a `APPROVED`. 
  5. **El Historial:** Para que quede evidencia, se crea un registro en `employee_history` (una foto de cómo estaba el trabajador ese día). Y se anota en `email_logs` si se logró enviar el correo al jefe o no.
  6. **Los Reportes:** Al dar click en "Descargar Excel", el sistema lee todos los `managed_documents` que estén en `APPROVED`, los cruza con `employees` y fabrica tu archivo final.
