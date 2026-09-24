import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as THREE from 'three'
import type { CalculatedRoute, StoreMap } from '@/types/api'

const harness = vi.hoisted(() => ({ available: false, render: vi.fn(), dispose: vi.fn(), loop: vi.fn() }))
vi.mock('three/addons/capabilities/WebGL.js', () => ({ default: { isWebGL2Available: () => harness.available } }))
vi.mock('three', async (importOriginal) => ({
  ...await importOriginal<typeof import('three')>(),
  WebGLRenderer: class {
    domElement = document.createElement('canvas')
    shadowMap = { enabled: false, type: 0 }
    setPixelRatio() {}
    setSize() {}
    render = harness.render
    dispose = harness.dispose
    setAnimationLoop = harness.loop
  },
}))
import { MarketScene } from './MarketScene'

const map: StoreMap = {
  id: 'map', storeId: 'store', version: 4, name: 'Loja de teste', width: 80, height: 60,
  scaleMetersPerUnit: 0.25,
  sectors: [{ id: 'sector', code: 'MERCEARIA', name: 'Mercearia', x: 5, y: 5, width: 65, height: 45, rotation: 0 }],
  aisles: [], pointsOfInterest: [], nodes: [], edges: [],
  shelfBlocks: [
    { id: 'g', code: 'G01', name: 'Gôndola · Arroz', sectorId: 'sector', aisleId: null, x: 20, y: 20, width: 4, height: 24, rotation: 0 },
    { id: 'f', code: 'F01', name: 'Freezer mural · Sorvete', sectorId: 'sector', aisleId: null, x: 40, y: 5, width: 14, height: 4, rotation: 0 },
  ],
}

describe('operational MarketScene', () => {
  beforeEach(() => {
    harness.available = false
    harness.render.mockClear()
    harness.dispose.mockClear()
    harness.loop.mockClear()
    vi.stubGlobal('ResizeObserver', class { observe() {} disconnect() {} })
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({
      fillRect() {}, strokeRect() {}, fillText() {}, beginPath() {}, moveTo() {}, lineTo() {}, stroke() {},
    } as unknown as CanvasRenderingContext2D)
  })

  it('shows store selection when there is no map', () => {
    render(<MarketScene />)
    expect(screen.getByText(/Selecione uma loja/)).toBeInTheDocument()
    expect(harness.render).not.toHaveBeenCalled()
  })

  it('offers the same map in 2D when WebGL is unavailable', () => {
    const fallback = vi.fn()
    render(<MarketScene map={map} onSwitchTo2D={fallback} />)
    expect(screen.getByRole('alert')).toHaveTextContent('WebGL 2')
    fireEvent.click(screen.getByRole('button', { name: 'Abrir planta 2D' }))
    expect(fallback).toHaveBeenCalledOnce()
  })

  it('keeps narrow and horizontal fixtures inside their API footprints, with instanced merchandise', () => {
    harness.available = true
    const { unmount } = render(<MarketScene map={map} />)
    const scene = harness.render.mock.calls[0][0] as THREE.Scene
    const world = scene.children.find((item) => item instanceof THREE.Group)!
    const fixtures = world.children.filter((item) => item instanceof THREE.Group)
    expect(fixtures).toHaveLength(2)
    fixtures.forEach((group, index) => {
      const bounds = new THREE.Box3().setFromObject(group)
      const block = map.shelfBlocks[index]
      expect(bounds.min.x).toBeGreaterThanOrEqual((block.x - map.width / 2) * 0.25 - 0.025)
      expect(bounds.max.x).toBeLessThanOrEqual((block.x + block.width - map.width / 2) * 0.25 + 0.025)
      expect(bounds.min.z).toBeGreaterThanOrEqual((block.y - map.height / 2) * 0.25 - 0.025)
      expect(bounds.max.z).toBeLessThanOrEqual((block.y + block.height - map.height / 2) * 0.25 + 0.025)
      expect(group.children.some((item) => item instanceof THREE.InstancedMesh)).toBe(true)
    })
    unmount()
    expect(harness.loop).toHaveBeenLastCalledWith(null)
    expect(harness.dispose).toHaveBeenCalledOnce()
  })

  it('renders the backend path as orthogonal segments without a shortcut', () => {
    harness.available = true
    const route: CalculatedRoute = {
      storeId: map.storeId, mapId: map.id, distanceMeters: 9, orderedStops: [],
      path: [{ nodeId: 'a', x: 10, y: 10 }, { nodeId: 'b', x: 30, y: 10 }, { nodeId: 'c', x: 30, y: 26 }],
    }
    render(<MarketScene map={map} route={route} />)
    const scene = harness.render.mock.calls[0][0] as THREE.Scene
    const segments: THREE.Mesh[] = []
    scene.traverse((object) => {
      if (object instanceof THREE.Mesh && object.geometry instanceof THREE.CylinderGeometry
        && object.geometry.parameters.radiusTop === 0.095) segments.push(object)
    })
    expect(segments.map((segment) => segment.scale.y)).toEqual([5, 4])
    expect(segments.map((segment) => [segment.position.x, segment.position.z])).toEqual([[-5, -5], [-2.5, -3]])
  })

  it('builds service furniture without putting details into neighboring walkways', () => {
    harness.available = true
    const furniture = [
      ['Balcão de açougue · Patinho', 14, 5], ['Parede de serviço · Açougue', 64, 2],
      ['Bancada de preparo · Açougue', 49, 4], ['Cadeiras · Espera', 16, 3],
      ['Totem de senhas · Retire sua senha', 3, 3], ['Caixa paralelo · 01', 5, 14],
      ['Portal de entrada · Bem-vindo', 12, 2], ['Portal de saída · Até breve', 12, 2],
      ['Estação de carrinhos · Entrada', 12, 4],
    ] as const
    const furnitureMap = { ...map, width: 240, height: 160, shelfBlocks: furniture.map(([name, width, height], index) => ({
      ...map.shelfBlocks[0], id: String(index), name, width, height, x: 10, y: 10 + index * 15,
    })) }
    render(<MarketScene map={furnitureMap} />)
    const scene = harness.render.mock.calls[0][0] as THREE.Scene
    const world = scene.children.find((item) => item instanceof THREE.Group)!
    const groups = world.children.filter((item) => item instanceof THREE.Group)
    expect(groups).toHaveLength(furniture.length)
    groups.forEach((group, index) => {
      const block = furnitureMap.shelfBlocks[index]
      const bounds = new THREE.Box3().setFromObject(group)
      expect(bounds.min.x, block.name).toBeGreaterThanOrEqual((block.x - 120) * 0.25 - 0.025)
      expect(bounds.max.x, block.name).toBeLessThanOrEqual((block.x + block.width - 120) * 0.25 + 0.025)
      expect(bounds.min.z, block.name).toBeGreaterThanOrEqual((block.y - 80) * 0.25 - 0.025)
      expect(bounds.max.z, block.name).toBeLessThanOrEqual((block.y + block.height - 80) * 0.25 + 0.025)
    })
  })

  it('offers fallback if the GPU context is lost', () => {
    harness.available = true
    render(<MarketScene map={map} onSwitchTo2D={vi.fn()} />)
    fireEvent(screen.getByRole('img', { name: /Mapa tridimensional da loja/ }), new Event('webglcontextlost', { cancelable: true }))
    expect(screen.getByRole('alert')).toHaveTextContent('interrompida')
  })
})
