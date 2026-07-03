export const AREA_OPTIONS = [
  { value: 'CENTRO', label: 'CENTRO' },
  { value: 'NORTE', label: 'NORTE' },
  { value: 'OCCIDENTE', label: 'OCCIDENTE' },
  { value: 'ORIENTE', label: 'ORIENTE' },
  { value: 'PUERTO_BOYACA', label: 'PUERTO BOYACA' },
  { value: 'RICAURTE', label: 'RICAURTE' },
  { value: 'SUGAMUXI', label: 'SUGAMUXI' },
  { value: 'TUNDAMA', label: 'TUNDAMA' },
  { value: 'CENTRO_TIBANA', label: 'CENTRO / TIBANA' },
  { value: 'CENTRO_MIRAFLORES', label: 'CENTRO / MIRAFLORES' },
  { value: 'CENTRO_VILLA_DE_LEYVA', label: 'CENTRO / VILLA DE LEYVA' },
  { value: 'CENTRO_SAMACA', label: 'CENTRO / SAMACA' },
  { value: 'DIRECCION_MANTENIMIENTO', label: 'DIRECCION DE MANTENIMIENTO' },
  { value: 'DIRECCION_OPERACION', label: 'DIRECCION DE OPERACION' },
  { value: 'DIRECCION_PERDIDAS', label: 'DIRECCION DE PERDIDAS' }
]

export const AREA_CODES = AREA_OPTIONS.map((area) => area.value)

const PRIMARY_AREA_CODES = [
  'CENTRO',
  'NORTE',
  'OCCIDENTE',
  'ORIENTE',
  'PUERTO_BOYACA',
  'RICAURTE',
  'SUGAMUXI',
  'TUNDAMA',
  'DIRECCION_MANTENIMIENTO',
  'DIRECCION_OPERACION',
  'DIRECCION_PERDIDAS'
]

const AREA_LABEL_BY_CODE = AREA_OPTIONS.reduce((map, area) => {
  map[area.value] = area.label
  return map
}, {})

const AREA_ALIASES = {
  PUERTO: 'PUERTO_BOYACA',
  PUERTO_BOYACA: 'PUERTO_BOYACA',
  PUERTO_BOYACA_: 'PUERTO_BOYACA',
  PUERTO_BOYACA_SA: 'PUERTO_BOYACA',
  PUERTO_BOYACA_BOYACA: 'PUERTO_BOYACA',
  PUERTO_BOYACA_BOYACA_: 'PUERTO_BOYACA',
  DIRECCION_MANTENIMIENTO: 'DIRECCION_MANTENIMIENTO',
  DIRECCION_DE_MANTENIMIENTO: 'DIRECCION_MANTENIMIENTO',
  DIRECCION_OPERACION: 'DIRECCION_OPERACION',
  DIRECCION_DE_OPERACION: 'DIRECCION_OPERACION',
  DIRECCION_PERDIDAS: 'DIRECCION_PERDIDAS',
  DIRECCION_DE_PERDIDAS: 'DIRECCION_PERDIDAS',
  CENTRO_TIBANA: 'CENTRO_TIBANA',
  TIBANA: 'CENTRO_TIBANA',
  CENTRO_TIBASOSA: 'CENTRO_TIBANA',
  CENTRO_MIRAFLORES: 'CENTRO_MIRAFLORES',
  MIRAFLORES: 'CENTRO_MIRAFLORES',
  CENTRO_VILLA_DE_LEYVA: 'CENTRO_VILLA_DE_LEYVA',
  VILLA_DE_LEYVA: 'CENTRO_VILLA_DE_LEYVA',
  CENTRO_SAMACA: 'CENTRO_SAMACA',
  SAMACA: 'CENTRO_SAMACA'
}

export const normalizeAreaCode = (value) => {
  const normalized = String(value || '')
    .toUpperCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/Ñ/g, 'N')
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')

  if (!normalized) return ''

  if (AREA_LABEL_BY_CODE[normalized]) return normalized
  if (AREA_ALIASES[normalized]) return AREA_ALIASES[normalized]

  return ''
}

export const areaLabel = (value) => {
  const normalized = normalizeAreaCode(value)
  return AREA_LABEL_BY_CODE[normalized] || ''
}

export const normalizeAreaList = (values = []) => {
  const normalized = values
    .map(normalizeAreaCode)
    .filter(Boolean)

  return [...new Set(normalized)]
}

export const areaScopeSummary = (values = []) => {
  const codes = normalizeAreaList(values)
  const labels = codes.map(areaLabel).filter(Boolean)
  const hasAllAreas = AREA_CODES.every((area) => codes.includes(area))

  let summary = 'Sin áreas asignadas'

  if (hasAllAreas) {
    summary = 'Todas las zonas'
  } else if (labels.length <= 3) {
    summary = labels.join(', ') || summary
  } else {
    summary = `${labels.slice(0, 2).join(', ')} y ${labels.length - 2} más`
  }

  return {
    count: labels.length,
    detail: labels.join(', '),
    hasAllAreas,
    summary
  }
}

export const PRIMARY_AREA_OPTIONS = AREA_OPTIONS.filter((area) => PRIMARY_AREA_CODES.includes(area.value))

export const primaryAreaCode = (value) => {
  const normalized = normalizeAreaCode(value)
  return normalized.startsWith('CENTRO_') ? 'CENTRO' : normalized
}

export const SUBZONE_OPTIONS_BY_AREA = {
  CENTRO: [
    { value: 'SIN_SUBZONA', label: 'SIN SUBZONA' },
    { value: 'CENTRO_TIBANA', label: 'TIBANA' },
    { value: 'CENTRO_MIRAFLORES', label: 'MIRAFLORES' },
    { value: 'CENTRO_VILLA_DE_LEYVA', label: 'VILLA DE LEYVA' },
    { value: 'CENTRO_SAMACA', label: 'SAMACA' }
  ]
}

export const subzoneOptions = (primaryArea) => SUBZONE_OPTIONS_BY_AREA[primaryAreaCode(primaryArea)] || []

export const employeeSubzoneCode = (areaCode, zone) => {
  const normalizedArea = normalizeAreaCode(areaCode)

  if (normalizedArea.startsWith('CENTRO_')) return normalizedArea

  const normalizedZone = normalizeAreaCode(zone)

  if (normalizedZone === 'CENTRO' || normalizedZone.startsWith('CENTRO_')) {
    return normalizedZone
  }

  return 'SIN_SUBZONA'
}
