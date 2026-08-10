# Plan de Trabajo y Uso del Sistema - SSTAlturas

Este documento establece el **plan paso a paso** (flujo de trabajo) que los usuarios deben seguir diariamente para gestionar correctamente los exámenes médicos en la plataforma.

---

## FASE 1: Preparación (Inicio de Jornada)

**Objetivo:** Ingresar al sistema de forma segura.

*   [ ] **Paso 1.1:** Ingresar a la URL oficial del sistema.
*   [ ] **Paso 1.2:** Introducir credenciales (Correo y Contraseña).
*   [ ] **Paso 1.3:** En caso de olvido, ejecutar el flujo de "Recuperar Contraseña".

---

## FASE 2: Registro de Personal (Si aplica)

**Objetivo:** Asegurar que los trabajadores existan en la base de datos antes de procesar sus exámenes.

*   [ ] **Paso 2.1:** Ir al módulo `Trabajadores`.
*   [ ] **Paso 2.2:** Buscar al trabajador por número de cédula para confirmar si ya existe.
*   [ ] **Paso 2.3:** Si no existe, hacer clic en `Nuevo Trabajador`.
*   [ ] **Paso 2.4:** Llenar el formulario con datos obligatorios (Cédula, Nombres, Cargo, Zona).
*   [ ] **Paso 2.5:** Guardar el registro.

---

## FASE 3: Digitalización (Cargue de Documentos)

**Objetivo:** Subir los conceptos médicos en PDF para que el sistema extraiga los datos.

### Opción A: Carga Individual (Para 1 o 2 exámenes)
*   [ ] **Paso 3.A.1:** Ir al módulo `Cargar Documentos`.
*   [ ] **Paso 3.A.2:** Buscar y seleccionar al trabajador.
*   [ ] **Paso 3.A.3:** Adjuntar el PDF del examen médico correspondiente.
*   [ ] **Paso 3.A.4:** Ejecutar la acción `Analizar y Subir`.

### Opción B: Carga Masiva (Para muchos exámenes a la vez)
*   [ ] **Paso 3.B.1:** Ir al módulo `Cargar Documentos` > `Carga por Lote`.
*   [ ] **Paso 3.B.2:** Seleccionar todos los PDFs desde la carpeta del computador.
*   [ ] **Paso 3.B.3:** Confirmar la subida masiva y esperar a que el sistema asocie cada PDF con su trabajador mediante la lectura de cédulas.

---

## FASE 4: Control de Calidad (Revisión y Aprobación)

**Objetivo:** Validar que la información leída por el sistema sea correcta y oficializar el resultado. *(Responsabilidad exclusiva del rol APROBADOR).*

*   [ ] **Paso 4.1:** Ingresar al módulo `Revisión`.
*   [ ] **Paso 4.2:** Seleccionar un documento de la lista de pendientes.
*   [ ] **Paso 4.3:** Comparar visualmente los datos extraídos (pantalla izquierda) contra el PDF original (pantalla derecha).
*   [ ] **Paso 4.4:** Tomar una decisión:
    *   **Si todo es correcto:** Clic en `Aprobar` (se notifica automáticamente por correo).
    *   **Si hay error o inconsistencia:** Clic en `Rechazar` e ingresar el motivo en la caja de comentarios.

---

## FASE 5: Cierre y Exportación (Reportes)

**Objetivo:** Generar la información necesaria para el Ministerio o reportes gerenciales.

*   [ ] **Paso 5.1:** Ir al módulo `Reportes`.
*   [ ] **Paso 5.2:** Aplicar los filtros necesarios (Fechas, Zonas, Aptos/No Aptos).
*   [ ] **Paso 5.3:** Ejecutar la exportación:
    *   Descargar el reporte general en `Excel`.
    *   Descargar el archivo `.CSV` formateado específicamente para el portal del Ministerio.

---
*Fin del Plan de Trabajo Operativo.*
