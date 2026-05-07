<template>
  <section class="settings-shell">
    <aside class="settings-sidebar card border-0">
      <div class="card-body">
        <h1>Configuración</h1>
        <p>Administración operativa y de comunicaciones.</p>
        <button v-for="item in sections" :key="item.key" class="side-link" :class="{active:active===item.key}" @click="active=item.key">{{ item.icon }} {{ item.label }}</button>
      </div>
    </aside>

    <main class="settings-content card border-0">
      <div class="card-body">
        <div class="section-header">
          <h2>{{ current.label }}</h2>
          <p>{{ current.desc }}</p>
        </div>

        <div v-if="error" class="state-box error">{{ error }}</div>
        <div v-if="success" class="state-box info">{{ success }}</div>

        <div v-if="active==='general'" class="panel-grid">
          <div class="panel">
            <h3>Resumen</h3>
            <p>Configura correos de aprobación, copias de notificación y coordinadores por zona.</p>
          </div>
        </div>

        <form v-if="active==='emails' || active==='approvers'" class="panel-grid" @submit.prevent="saveEmails">
          <div class="panel" v-for="field in emailFieldsForSection" :key="field.key">
            <label>{{ field.label }}</label>
            <input v-model.trim="emailForm[field.key]" class="form-control" :placeholder="field.placeholder" />
          </div>
          <div class="actions"><button class="btn btn-primary" :disabled="loading">{{ loading ? 'Guardando...' : 'Guardar' }}</button><button type="button" class="secondary-btn" @click="loadAll">Cancelar</button></div>
        </form>

        <form v-if="active==='zones'" class="panel-grid" @submit.prevent="saveZones">
          <div class="panel" v-for="zone in zones" :key="zone">
            <label>{{ zone }}</label>
            <input v-model.trim="zoneForm[`zone.coordinator.${zone}`]" class="form-control" placeholder="coordinador@empresa.com" />
          </div>
          <div class="actions"><button class="btn btn-primary" :disabled="loading">{{ loading ? 'Guardando...' : 'Guardar' }}</button><button type="button" class="secondary-btn" @click="loadAll">Cancelar</button></div>
        </form>

        <div v-if="active==='users'" class="panel-grid">
          <div class="panel full">
            <div class="section-row"><h3>Usuarios y roles</h3><button class="btn btn-primary" @click="openCreate">Nuevo usuario</button></div>
            <table class="table table-sm"><thead><tr><th>Usuario</th><th>Correo</th><th>Rol</th><th>Áreas</th><th>Estado</th><th></th></tr></thead>
              <tbody><tr v-for="u in users" :key="u.id"><td>{{u.username}}</td><td>{{u.email}}</td><td>{{(u.roles||[]).join(', ')}}</td><td>{{(u.allowedAreas||[]).join(', ')||'-'}}</td><td>{{u.enabled?'Activo':'Inactivo'}}</td><td><button class="secondary-btn" @click="editUser(u)">Editar</button></td></tr></tbody></table>
          </div>
          <div class="panel full" v-if="showUserForm">
            <h3>{{ editId ? 'Editar usuario':'Crear usuario' }}</h3>
            <div class="user-grid"><input v-model.trim="userForm.username" class="form-control" placeholder="Usuario"/><input v-model.trim="userForm.email" class="form-control" placeholder="correo@empresa.com"/><input v-model="userForm.password" class="form-control" placeholder="Contraseña (solo crear)" :disabled="!!editId"/></div>
            <div class="user-grid"><select v-model="userForm.role" class="form-select"><option>OPERADOR</option><option>APROBADOR</option><option>SUPER_ADMIN</option></select><input v-model.trim="areasCsv" class="form-control" placeholder="Áreas separadas por coma"/><label><input type="checkbox" v-model="userForm.enabled"/> Activo</label></div>
            <div class="actions"><button class="btn btn-primary" @click="saveUser">Guardar</button><button class="secondary-btn" @click="resetUser">Cancelar</button></div>
          </div>
        </div>
      </div>
    </main>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api/http'
import { getEmailSettings, getZoneCoordinators, updateEmailSettings, updateZoneCoordinators } from '../api/settings'

const active = ref('general')
const loading = ref(false)
const error = ref('')
const success = ref('')
const emailForm = ref({})
const zoneForm = ref({})
const zones = ['CENTRO','NORTE','OCCIDENTE','ORIENTE','PUERTO','RICAURTE','SUGAMUXI','TUNDAMA','EDIFICIO']
const users = ref([])
const showUserForm = ref(false)
const editId = ref('')
const areasCsv = ref('')
const userForm = ref({ username:'', email:'', password:'', role:'OPERADOR', enabled:true })

const sections = [
  { key:'general', label:'General / Operativa', icon:'🧭', desc:'Parámetros de plataforma' },
  { key:'emails', label:'Correos', icon:'✉️', desc:'Correos corporativos y copias' },
  { key:'approvers', label:'Aprobadores', icon:'✅', desc:'Destinatarios de aprobación' },
  { key:'zones', label:'Coordinadores por zona', icon:'🗺️', desc:'Correo por zona operativa' },
  { key:'users', label:'Usuarios y roles', icon:'👥', desc:'Control de acceso' }
]
const current = computed(() => sections.find(s => s.key===active.value) || sections[0])
const emailFieldsForSection = computed(() => active.value === 'approvers'
  ? [{key:'approver.to',label:'Correo aprobador',placeholder:'aprobador@empresa.com'},{key:'approver.cc',label:'CC aprobador',placeholder:'cc1@empresa.com, cc2@empresa.com'}]
  : [{key:'worker.cc.hr',label:'Talento humano',placeholder:'th@empresa.com'},{key:'worker.cc.payroll',label:'Nómina',placeholder:'nomina@empresa.com'},{key:'worker.cc.default',label:'Copias por defecto',placeholder:'copia1@empresa.com'}])

const loadAll = async () => {
  error.value=''; success.value=''
  const [emails, zonesRes, usersRes] = await Promise.all([getEmailSettings(), getZoneCoordinators(), http.get('/api/users')])
  emailForm.value = { ...emails.data }
  zoneForm.value = { ...zonesRes.data }
  users.value = Array.isArray(usersRes.data) ? usersRes.data : []
}
const saveEmails = async () => { loading.value=true; error.value=''; success.value=''; try { await updateEmailSettings(emailForm.value); success.value='Correos actualizados.' } catch (e) { error.value=e?.response?.data?.message||'No se pudo guardar.' } finally { loading.value=false } }
const saveZones = async () => { loading.value=true; error.value=''; success.value=''; try { await updateZoneCoordinators(zoneForm.value); success.value='Coordinadores actualizados.' } catch (e) { error.value=e?.response?.data?.message||'No se pudo guardar.' } finally { loading.value=false } }
const openCreate = () => { showUserForm.value=true; editId.value=''; userForm.value={ username:'', email:'', password:'', role:'OPERADOR', enabled:true }; areasCsv.value='' }
const editUser = (u) => { showUserForm.value=true; editId.value=u.id; userForm.value={ username:u.username, email:u.email, password:'', role:(u.roles||['OPERADOR'])[0], enabled:!!u.enabled }; areasCsv.value=(u.allowedAreas||[]).join(', ') }
const resetUser = () => { showUserForm.value=false }
const saveUser = async () => {
  const payload = { username:userForm.value.username, email:userForm.value.email, password:userForm.value.password, roles:[userForm.value.role], allowedAreas:userForm.value.role==='OPERADOR'?areasCsv.value.split(',').map(v=>v.trim()).filter(Boolean):[], enabled:userForm.value.enabled }
  if (editId.value) await http.put(`/api/users/${editId.value}`, payload); else await http.post('/api/users', payload)
  await loadAll(); showUserForm.value=false
}

onMounted(loadAll)
</script>

<style scoped>
.settings-shell{display:grid;grid-template-columns:300px 1fr;gap:1rem;align-items:start}.settings-sidebar{position:sticky;top:84px}.side-link{display:block;width:100%;text-align:left;border:none;background:transparent;padding:.7rem;border-radius:10px}.side-link.active,.side-link:hover{background:#eef4ff}.section-header{margin-bottom:1rem}.panel-grid{display:grid;gap:1rem}.panel{background:#f8fafc;padding:1rem;border-radius:12px}.panel.full{grid-column:1/-1}.actions{display:flex;gap:.6rem}.section-row{display:flex;justify-content:space-between}.user-grid{display:grid;grid-template-columns:1fr 1fr 1fr;gap:.6rem;margin-bottom:.6rem}
</style>
