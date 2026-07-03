# Despliegue Apache - Alturas / Lector PDF

Configuracion esperada:

- Apache sirve el frontend compilado de Vue.
- Spring Boot corre localmente en `http://localhost:8081`.
- El frontend consume la API con rutas relativas `/api`.
- Apache reenvia `/api` al backend.
- Apache devuelve `index.html` para rutas internas de Vue como `/employees`, `/review`, `/reports` y `/settings`.

## Backend

En `alturas-backend/src/main/resources/application.properties`:

```properties
server.port=8081
app.frontend.base-url=https://sstalturas.ebsa.com.co
app.cors.allowed-origins=http://localhost:5173,http://localhost:5174,https://sstalturas.ebsa.com.co
```

Arranque:

```powershell
mvn spring-boot:run
```

## Frontend

El frontend debe usar rutas relativas. En produccion no debe tener `http://localhost:8080` ni `http://localhost:8081` como API base.

Compilar:

```powershell
cd alturas-frontend
npm run build
```

Subir el contenido de `alturas-frontend/dist` al `DocumentRoot` de Apache.

## Apache VirtualHost

Ejemplo de configuracion:

```apache
<VirtualHost *:8080>
    ServerName sstalturas.ebsa.com.co
    DocumentRoot "C:/ruta/al/frontend/dist"

    <Directory "C:/ruta/al/frontend/dist">
        Options FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    ProxyPreserveHost On
    ProxyPass        /api http://localhost:8081/api
    ProxyPassReverse /api http://localhost:8081/api

    RewriteEngine On
    RewriteCond %{REQUEST_URI} !^/api
    RewriteCond %{DOCUMENT_ROOT}%{REQUEST_FILENAME} !-f
    RewriteCond %{DOCUMENT_ROOT}%{REQUEST_FILENAME} !-d
    RewriteRule ^ /index.html [L]
</VirtualHost>
```

## Modulos Apache necesarios

Deben estar habilitados:

```apache
LoadModule rewrite_module modules/mod_rewrite.so
LoadModule proxy_module modules/mod_proxy.so
LoadModule proxy_http_module modules/mod_proxy_http.so
```

Si se usa `.htaccess`, el `Directory` debe tener:

```apache
AllowOverride All
```

## Verificaciones

1. Abrir `https://sstalturas.ebsa.com.co`.
2. Refrescar rutas internas:
   - `/employees`
   - `/review`
   - `/reports`
   - `/settings`
3. Verificar que no haya 404 de Apache.
4. En DevTools, confirmar que las peticiones salen a `/api/...`.
5. Confirmar que el backend escucha en `localhost:8081`.
