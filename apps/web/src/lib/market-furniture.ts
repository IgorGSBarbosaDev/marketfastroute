import * as THREE from 'three'
import type { FixtureKind } from './map-fixtures'

export interface FurnitureBuilder {
  kind: FixtureKind
  name: string
  width: number
  depth: number
  group: THREE.Group
  box: (w: number, h: number, d: number, x: number, y: number, z: number, color: string) => THREE.Mesh
  material: (color: string, roughness?: number) => THREE.MeshStandardMaterial
  geometry: (key: string, create: () => THREE.BufferGeometry) => THREE.BufferGeometry
  label: (title: string, subtitle: string, y: number, width: number) => void
  panel: (title: string, subtitle: string, position: [number, number, number], width: number, height: number) => void
}

/** Architectural furniture uses the same footprint as its 2D block. No live queue or checkout logic. */
export function buildMarketFurniture({ kind, name, width: w, depth: d, group, box, material, geometry, label, panel }: FurnitureBuilder) {
  const steel = '#768b8d'
  const dark = '#304d56'
  const cream = '#e9e7da'
  const glass = () => {
    const result = material('#b8dae0', 0.18)
    result.transparent = true
    result.opacity = 0.24
    result.depthWrite = false
    return result
  }
  const sphere = (x: number, y: number, z: number, sx: number, sy: number, sz: number, color: string) => {
    const mesh = new THREE.Mesh(geometry('furniture-rounded', () => new THREE.SphereGeometry(0.5, 12, 8)), material(color, 0.65))
    mesh.position.set(x, y, z)
    mesh.scale.set(sx, sy, sz)
    mesh.castShadow = true
    group.add(mesh)
  }

  if (kind === 'butcher') {
    box(w, 0.66, d, 0, 0.33, 0, '#865c50')
    box(w * 0.94, 0.08, d * 0.98, 0, 0.72, 0, steel)
    box(w * 0.9, 0.04, d * 0.96, 0, 0.81, 0, '#e5e3d6')
    const front = box(0.025, 0.53, d * 0.96, w * 0.48, 1.08, 0, '#b8dae0')
    front.material = glass()
    front.castShadow = false
    const canopy = box(w * 0.94, 0.025, d * 0.98, 0, 1.35, 0, '#b8dae0')
    canopy.material = glass()
    canopy.castShadow = false
    const trays = Math.max(2, Math.floor(d / 0.55))
    for (let i = 0; i < trays; i++) {
      const z = -d * 0.45 + (i + 0.5) * d * 0.9 / trays
      box(w * 0.65, 0.04, d * 0.8 / trays, 0.03, 0.86, z, '#516770')
      for (let cut = 0; cut < 3; cut++) {
        const fish = /tilápia|salmão|camarão/i.test(name)
        sphere((cut - 1) * w * 0.18, 0.94, z, w * 0.16, 0.11, 0.28, fish ? '#cc9980' : i % 2 ? '#ae6960' : '#c98579')
      }
      box(0.025, 0.1, 0.2, w * 0.47, 0.78, z, '#eee5c5')
    }
    for (const z of [-d * 0.48, d * 0.48]) {
      box(0.035, 0.6, 0.035, w * 0.46, 1.05, z, steel)
    }
    return true
  }
  if (kind === 'service-wall') {
    box(w, 2.55, d, 0, 1.275, 0, '#d9ddd1')
    box(w, 0.28, d, 0, 2.45, 0, '#835f50')
    for (let y = 0.4; y < 2.3; y += 0.4) box(0.012, 0.008, d * 0.99, w * 0.495, y, 0, '#bcc7bf')
    for (let z = -d / 2 + 0.4; z < d / 2; z += 0.6) box(0.012, 2.1, 0.008, w * 0.495, 1.1, z, '#bcc7bf')
    if (/Açougue/i.test(name)) {
      label('AÇOUGUE E PESCADOS', '', 3.0, Math.min(d * 0.6, 7))
      panel('SENHA 042', 'GUICHÊ 02 · DEMONSTRAÇÃO', [w / 2 + 0.01, 1.85, 0], 3.2, 0.85)
    }
    return true
  }
  if (kind === 'worktop') {
    box(w * 0.95, 0.86, d * 0.98, 0, 0.43, 0, steel)
    box(w, 0.08, d, 0, 0.9, 0, '#dce1d9')
    const stations = Math.max(1, Math.floor(d / 1.4))
    for (let i = 0; i < stations; i++) {
      const z = -d / 2 + (i + 0.5) * d / stations
      box(w * 0.65, 0.03, Math.min(0.65, d / stations * 0.6), 0, 0.965, z, '#eadfc4')
      box(0.22, 0.08, 0.3, -w * 0.24, 1.02, z + 0.18, dark)
      box(0.08, 0.14, 0.19, -w * 0.24, 1.1, z + 0.18, '#435d64')
    }
    return true
  }
  if (kind === 'seating') {
    const seats = Math.max(1, Math.floor(d / 0.85))
    for (let i = 0; i < seats; i++) {
      const z = -d / 2 + (i + 0.5) * d / seats
      box(w * 0.75, 0.09, 0.54, 0, 0.46, z, '#729186')
      box(0.07, 0.42, 0.54, -w * 0.34, 0.72, z, '#729186')
      for (const side of [-1, 1]) {
        box(0.035, 0.43, 0.035, w * 0.26, 0.22, z + side * 0.2, steel)
        box(0.035, 0.43, 0.035, -w * 0.26, 0.22, z + side * 0.2, steel)
      }
    }
    return true
  }
  if (kind === 'ticket') {
    box(w * 0.8, 0.09, d * 0.8, 0, 0.045, 0, dark)
    box(w * 0.42, 1.15, d * 0.4, 0, 0.62, 0, '#638779')
    box(w * 0.65, 0.38, d * 0.5, 0, 1.32, 0, dark)
    box(0.03, 0.17, d * 0.36, w * 0.33, 1.37, 0, '#cee1d8')
    box(0.08, 0.015, d * 0.18, w * 0.33, 1.18, 0, '#f6eed8')
    label('RETIRE SUA SENHA', 'ESPERA DO AÇOUGUE', 1.95, 2.5)
    return true
  }
  if (kind === 'checkout') {
    box(w * 0.94, 0.82, d * 0.94, 0, 0.41, 0, '#587a70')
    box(w, 0.065, d, 0, 0.85, 0, steel)
    box(w * 0.78, 0.035, d * 0.48, 0, 0.9, -d * 0.2, '#344649')
    for (let z = -d * 0.42; z < d * 0.02; z += 0.18) box(w * 0.76, 0.008, 0.015, 0, 0.92, z, '#657671')
    box(w * 0.65, 0.035, d * 0.22, 0, 0.92, d * 0.3, cream)
    box(w * 0.28, 0.03, 0.3, 0, 0.94, d * 0.06, '#b2d7d9')
    box(0.04, 0.28, 0.04, -w * 0.35, 1.06, d * 0.14, dark)
    box(0.32, 0.24, 0.055, -w * 0.3, 1.27, d * 0.14, dark)
    box(0.26, 0.17, 0.02, -w * 0.3, 1.27, d * 0.14 - 0.035, '#99beb1')
    label(name.split(' · ')[1] ?? 'CAIXA', '', 2.05, 0.9)
    return true
  }
  if (kind === 'entrance' || kind === 'exit') {
    // Local long axis spans the opening; posts stay at the sides, leaving the portal clear.
    for (const side of [-1, 1]) {
      box(w * 0.75, 2.3, 0.1, 0, 1.15, side * (d / 2 - 0.06), dark)
      const pane = box(w * 0.18, 1.95, d * 0.2, 0, 1.05, side * d * 0.38, '#b8dae0')
      pane.material = glass()
      pane.castShadow = false
    }
    box(w, 0.24, d, 0, 2.28, 0, kind === 'entrance' ? '#567d66' : '#416575')
    label(kind === 'entrance' ? 'ENTRADA' : 'SAÍDA', kind === 'entrance' ? 'BEM-VINDO' : 'ATÉ BREVE', 2.65, Math.min(d, 3))
    return true
  }
  if (kind === 'carts') {
    const count = Math.max(2, Math.floor(d / 0.5))
    for (let i = 0; i < count; i++) {
      const z = -d * 0.4 + i * d * 0.8 / count
      box(w * 0.6, 0.035, 0.4, 0, 0.38, z, steel)
      for (const side of [-1, 1]) {
        box(0.025, 0.35, 0.4, side * w * 0.3, 0.57, z, steel)
        sphere(side * w * 0.25, 0.12, z + 0.12, 0.09, 0.09, 0.09, dark)
        sphere(side * w * 0.25, 0.12, z - 0.12, 0.09, 0.09, 0.09, dark)
      }
      box(w * 0.6, 0.04, 0.04, 0, 0.8, z + 0.2, '#527965')
    }
    return true
  }
  return false
}
