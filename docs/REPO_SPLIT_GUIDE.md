# REPO SPLIT GUIDE
- Frontend: `alturas-frontend/`
- Backend: `alturas-backend/`

## Copiar frontend
```bash
rsync -av --exclude node_modules --exclude dist alturas-frontend/ /ruta/repo-frontend/
```
## Copiar backend
```bash
rsync -av --exclude target --exclude uploads alturas-backend/ /ruta/repo-backend/
```
## No subir
- `node_modules/`, `target/`, `uploads/`, `.env`, credenciales reales, PDFs cargados.
