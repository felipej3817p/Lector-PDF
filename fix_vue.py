file_path = r"c:\Users\andre\Downloads\Lector-PDF-CODEX-PR4\alturas-frontend\src\views\EmployeeListView.vue"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

zone_field = """            <div class="form-field">
              <label class="label" for="zone">Zona / subzona</label>
              <input
                id="zone"
                v-model.trim="employeeForm.zone"
                type="text"
                class="form-control"
                placeholder="Ej: CENTRO / TIBANA"
                :disabled="employeeFormSaving"
              />
            </div>"""

if zone_field in content:
    content = content.replace(zone_field + '\n\n', '')
    content = content.replace(zone_field + '\n', '')
    content = content.replace(zone_field, '')
else:
    print("Warning: zone_field not found")

validate_old = """const validateEmployeeForm = () => {
  if (!employeeForm.value.documentType.trim()) {
    return 'El tipo de documento es obligatorio.'
  }

  if (!employeeForm.value.documentNumber.trim()) {
    return 'El numero de documento es obligatorio.'
  }

  if (!employeeForm.value.firstName.trim()) {
    return 'El primer nombre es obligatorio.'
  }

  if (!employeeForm.value.firstLastName.trim()) {
    return 'El primer apellido es obligatorio.'
  }

  if (!employeeForm.value.email.trim()) {
    return 'El correo es obligatorio.'
  }

  return ''
}"""

validate_new = """const validateEmployeeForm = () => {
  if (!employeeForm.value.documentType.trim()) {
    return 'El tipo de documento es obligatorio.'
  }

  if (!employeeForm.value.documentNumber.trim()) {
    return 'El numero de documento es obligatorio.'
  }

  return ''
}"""

if validate_old in content:
    content = content.replace(validate_old, validate_new)
else:
    print("Warning: validate_old not found")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Vue file updated.")
