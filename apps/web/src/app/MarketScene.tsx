import { useEffect, useRef, useState } from 'react'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import WebGL from 'three/addons/capabilities/WebGL.js'
import { LocateFixed, Minus, Plus, RotateCcw } from 'lucide-react'
import type { CalculatedRoute, ProductLocation, StoreMap } from '@/types/api'

interface MarketSceneProps {
  map?: StoreMap
  route?: CalculatedRoute | null
  locations?: Array<ProductLocation & { productName?: string }>
  onSwitchTo2D?: () => void
}

const paletteBySector: Record<string, { floor: string; products: string }> = {
  HORTIFRUTI: { floor: '#d7e5ce', products: '#63864f' },
  PADARIA: { floor: '#ead9bd', products: '#a96f37' },
  BEBIDAS: { floor: '#d3e2e4', products: '#3f7180' },
  LATICINIOS: { floor: '#d5e3e3', products: '#587b87' },
  MERCEARIA: { floor: '#e5ddca', products: '#7e6949' },
  LIMPEZA: { floor: '#d8e3d5', products: '#64816a' },
  CONGELADOS: { floor: '#d1e1e1', products: '#467681' },
}

const packageColors = ['#d88956', '#6f9c72', '#d5b84f', '#6992a0', '#bd6a57', '#8c7eaa']

export function MarketScene({ map, route = null, locations = [], onSwitchTo2D }: MarketSceneProps) {
  const hostRef = useRef<HTMLDivElement>(null)
  const controlsRef = useRef<OrbitControls | null>(null)
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null)
  const [sceneError, setSceneError] = useState('')

  useEffect(() => {
    if (!map) return
    if (!WebGL.isWebGL2Available()) {
      setSceneError('Este navegador não conseguiu iniciar WebGL 2.')
      return
    }

    const host = hostRef.current
    if (!host) return

    let renderer: THREE.WebGLRenderer
    try {
      renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false, powerPreference: 'low-power' })
    } catch {
      setSceneError('Este navegador não conseguiu iniciar WebGL 2.')
      return
    }

    const resources = {
      geometries: new Set<THREE.BufferGeometry>(),
      materials: new Set<THREE.Material>(),
      textures: new Set<THREE.Texture>(),
    }
    const geometryCache = new Map<string, THREE.BufferGeometry>()
    const materialCache = new Map<string, THREE.MeshStandardMaterial>()
    const scene = new THREE.Scene()
    scene.background = new THREE.Color('#e9e7dc')

    const worldScale = Math.max(map.scaleMetersPerUnit, 0.1)
    const worldWidth = map.width * worldScale
    const worldDepth = map.height * worldScale
    const maxDimension = Math.max(worldWidth, worldDepth)
    const camera = new THREE.PerspectiveCamera(42, host.clientWidth / Math.max(host.clientHeight, 1), 0.1, maxDimension * 10)
    const homeDirection = new THREE.Vector3(worldWidth * 0.72, maxDimension * 1.45, worldDepth).normalize()
    const halfFovTangent = Math.tan(THREE.MathUtils.degToRad(camera.fov / 2))
    function homePosition(aspect: number) {
      const horizontalLength = Math.hypot(homeDirection.x, homeDirection.z)
      const horizontalX = homeDirection.x / horizontalLength
      const horizontalZ = homeDirection.z / horizontalLength
      const elevation = Math.atan2(homeDirection.y, horizontalLength)
      const horizontalSpan = worldWidth * Math.abs(horizontalZ) + worldDepth * Math.abs(horizontalX)
      const verticalSpan = Math.sin(elevation) * (worldWidth * Math.abs(horizontalX) + worldDepth * Math.abs(horizontalZ)) + Math.cos(elevation) * 3.5
      const distance = Math.max(
        horizontalSpan / (2 * halfFovTangent * Math.max(aspect, 0.1)),
        verticalSpan / (2 * halfFovTangent),
      ) * 1.12
      return homeDirection.clone().multiplyScalar(distance)
    }
    let cameraHome = homePosition(camera.aspect)
    camera.position.copy(cameraHome)
    camera.lookAt(0, 0, 0)

    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5))
    renderer.setSize(host.clientWidth, host.clientHeight)
    renderer.outputColorSpace = THREE.SRGBColorSpace
    renderer.toneMapping = THREE.ACESFilmicToneMapping
    renderer.toneMappingExposure = 1.02
    renderer.shadowMap.enabled = true
    renderer.shadowMap.type = THREE.PCFShadowMap
    renderer.domElement.setAttribute('aria-label', 'Mapa tridimensional da loja ' + map.name + '. Arraste para girar e use a roda para aproximar.')
    renderer.domElement.setAttribute('role', 'img')
    renderer.domElement.setAttribute('tabindex', '0')
    host.replaceChildren(renderer.domElement)

    const world = new THREE.Group()
    scene.add(world)
    scene.add(new THREE.HemisphereLight('#fffdf4', '#87938a', 1.4))
    const keyLight = new THREE.DirectionalLight('#fff8e9', 1.65)
    keyLight.position.set(-worldWidth * 0.25, maxDimension * 1.4, worldDepth * 0.24)
    keyLight.castShadow = true
    keyLight.shadow.mapSize.set(1024, 1024)
    keyLight.shadow.camera.left = -worldWidth
    keyLight.shadow.camera.right = worldWidth
    keyLight.shadow.camera.top = worldDepth
    keyLight.shadow.camera.bottom = -worldDepth
    scene.add(keyLight)

    function geometry(key: string, create: () => THREE.BufferGeometry) {
      const existing = geometryCache.get(key)
      if (existing) return existing
      const created = create()
      geometryCache.set(key, created)
      resources.geometries.add(created)
      return created
    }

    function material(color: string, roughness = 0.86) {
      const key = color + ':' + roughness
      const existing = materialCache.get(key)
      if (existing) return existing
      const created = new THREE.MeshStandardMaterial({ color, roughness, metalness: 0.02 })
      materialCache.set(key, created)
      resources.materials.add(created)
      return created
    }

    function boxGeometry(width: number, height: number, depth: number) {
      const key = 'box:' + width + ':' + height + ':' + depth
      return geometry(key, () => new THREE.BoxGeometry(width, height, depth))
    }

    function addBox(
      width: number,
      height: number,
      depth: number,
      position: [number, number, number],
      color: string,
      parent: THREE.Object3D = world,
      rotation = 0,
      roughness = 0.86,
    ) {
      const mesh = new THREE.Mesh(boxGeometry(width, height, depth), material(color, roughness))
      mesh.position.set(position[0], position[1], position[2])
      mesh.rotation.y = -THREE.MathUtils.degToRad(rotation)
      mesh.castShadow = height > 0.04
      mesh.receiveShadow = true
      parent.add(mesh)
      return mesh
    }

    function mapX(x: number) {
      return (x - map!.width / 2) * worldScale
    }

    function mapZ(y: number) {
      return (y - map!.height / 2) * worldScale
    }

    function canvasTexture(canvas: HTMLCanvasElement, repeatX = 1, repeatY = 1) {
      const texture = new THREE.CanvasTexture(canvas)
      texture.colorSpace = THREE.SRGBColorSpace
      texture.wrapS = THREE.RepeatWrapping
      texture.wrapT = THREE.RepeatWrapping
      texture.repeat.set(repeatX, repeatY)
      resources.textures.add(texture)
      return texture
    }

    const floorCanvas = document.createElement('canvas')
    floorCanvas.width = 512
    floorCanvas.height = 512
    const floorContext = floorCanvas.getContext('2d')
    if (floorContext) {
      floorContext.fillStyle = '#e9e5d9'
      floorContext.fillRect(0, 0, 512, 512)
      floorContext.strokeStyle = '#d8d3c6'
      floorContext.lineWidth = 2
      for (let index = 0; index <= 512; index += 64) {
        floorContext.beginPath()
        floorContext.moveTo(index, 0)
        floorContext.lineTo(index, 512)
        floorContext.stroke()
        floorContext.beginPath()
        floorContext.moveTo(0, index)
        floorContext.lineTo(512, index)
        floorContext.stroke()
      }
      for (let index = 0; index < 500; index += 1) {
        const x = (index * 73 + 29) % 512
        const y = (index * 131 + 11) % 512
        floorContext.fillStyle = index % 2 ? 'rgba(105, 105, 88, .08)' : 'rgba(255, 255, 255, .2)'
        floorContext.fillRect(x, y, 2, 2)
      }
    }
    const floorTexture = canvasTexture(floorCanvas, Math.max(2, worldWidth / 2.8), Math.max(2, worldDepth / 2.8))
    const floorMaterial = new THREE.MeshStandardMaterial({ map: floorTexture, roughness: 0.94, metalness: 0 })
    resources.materials.add(floorMaterial)
    const floor = new THREE.Mesh(new THREE.PlaneGeometry(worldWidth + 4, worldDepth + 4), floorMaterial)
    resources.geometries.add(floor.geometry)
    floor.rotation.x = -Math.PI / 2
    floor.position.y = -0.17
    floor.receiveShadow = true
    world.add(floor)

    // Low perimeter bands keep the footprint readable without hiding the merchandise.
    const wallHeight = 0.72
    const wallThickness = 0.2
    function addHorizontalWall(z: number, doorCenters: number[]) {
      let wallStart = -worldWidth / 2
      doorCenters.sort((left, right) => left - right).forEach((center) => {
        const openingStart = Math.max(wallStart, center - 1.8)
        if (openingStart > wallStart) {
          const segmentWidth = openingStart - wallStart
          addBox(segmentWidth, wallHeight, wallThickness, [wallStart + segmentWidth / 2, wallHeight / 2 - 0.12, z], '#748982')
        }
        wallStart = Math.max(wallStart, center + 1.8)
      })
      if (wallStart < worldWidth / 2) {
        const segmentWidth = worldWidth / 2 - wallStart
        addBox(segmentWidth, wallHeight, wallThickness, [wallStart + segmentWidth / 2, wallHeight / 2 - 0.12, z], '#748982')
      }
    }
    const doorPoints = map.pointsOfInterest.filter((point) => point.type === 'ENTRANCE' || point.type === 'EXIT')
    addHorizontalWall(-worldDepth / 2, doorPoints.filter((point) => point.y < map.height * 0.28).map((point) => mapX(point.x)))
    addHorizontalWall(worldDepth / 2, doorPoints.filter((point) => point.y > map.height * 0.72).map((point) => mapX(point.x)))
    addBox(wallThickness, wallHeight, worldDepth, [-worldWidth / 2, wallHeight / 2 - 0.12, 0], '#748982')
    addBox(wallThickness, wallHeight, worldDepth, [worldWidth / 2, wallHeight / 2 - 0.12, 0], '#748982')

    const activeSectors = map.sectors.filter((sector) => sector.active !== false)
    const activeAisles = map.aisles.filter((aisle) => aisle.active !== false)
    const activeShelfBlocks = map.shelfBlocks.filter((block) => block.active !== false)
    const activePointsOfInterest = map.pointsOfInterest.filter((point) => point.active !== false)
    const activeNodes = map.nodes.filter((node) => node.active !== false)
    const sectorById = new Map(activeSectors.map((sector) => [sector.id, sector]))
    activeSectors.forEach((sector) => {
      const tone = paletteFor(sector.code, sector.name)
      const width = sector.width * worldScale
      const depth = sector.height * worldScale
      addBox(width, 0.025, depth, [mapX(sector.x + sector.width / 2), -0.055, mapZ(sector.y + sector.height / 2)], tone.floor, world, sector.rotation)
      addTextSign(sector.name, 'SETOR DA LOJA', tone.products, mapX(sector.x + sector.width / 2), 2.25, mapZ(sector.y + sector.height / 2), Math.min(Math.max(2.6, width * 0.7), 7.5))
    })

    activeAisles.forEach((aisle) => {
      const width = aisle.width * worldScale
      const depth = aisle.height * worldScale
      const x = mapX(aisle.x + aisle.width / 2)
      const z = mapZ(aisle.y + aisle.height / 2)
      addBox(width, 0.028, depth, [x, -0.025, z], '#d7d4c9', world, aisle.rotation)
      const sector = aisle.sectorId ? sectorById.get(aisle.sectorId) : undefined
      const subtitle = sector?.name ?? 'ÁREA DE COMPRAS'
      addTextSign(aisle.code + '  ·  ' + aisle.name, subtitle, '#365a67', x, 2.15, z, Math.min(Math.max(2.5, width * 1.25), 6.6))
    })

    const productGeometry = {
      carton: geometry('package-carton', () => new THREE.BoxGeometry(1, 1, 1)),
      bottle: geometry('package-bottle', () => new THREE.CylinderGeometry(0.36, 0.45, 1, 9)),
      can: geometry('package-can', () => new THREE.CylinderGeometry(0.48, 0.48, 1, 12)),
      fruit: geometry('package-fruit', () => new THREE.SphereGeometry(0.5, 10, 8)),
    }
    const packageMaterials = new Map<string, THREE.MeshStandardMaterial>()

    function packageMaterial(sectorId: string | null, sectorName: string, productName: string) {
      const key = (sectorId || sectorName) + ':' + productName
      const existing = packageMaterials.get(key)
      if (existing) return existing
      const tone = paletteFor(key, sectorName)
      const canvas = document.createElement('canvas')
      canvas.width = 256
      canvas.height = 256
      const context = canvas.getContext('2d')
      if (context) {
        context.fillStyle = tone.products
        context.fillRect(0, 0, 256, 256)
        context.fillStyle = '#f7f2e7'
        context.fillRect(0, 77, 256, 122)
        context.fillStyle = '#2d4c58'
        context.font = '700 23px Arial, sans-serif'
        context.fillText('MERCADO AURORA', 18, 112)
        context.fillStyle = '#61726d'
        context.font = '600 18px Arial, sans-serif'
        context.fillText((productName || sectorName).toLocaleUpperCase('pt-BR').slice(0, 22), 18, 151)
        context.font = '500 15px Arial, sans-serif'
        context.fillText(sectorName.toLocaleUpperCase('pt-BR').slice(0, 28), 18, 181)
        context.fillStyle = '#e5bd59'
        context.fillRect(18, 171, 68, 8)
        context.fillStyle = 'rgba(255,255,255,.55)'
        context.fillRect(17, 16, 222, 8)
      }
      const texture = canvasTexture(canvas)
      const created = new THREE.MeshStandardMaterial({ map: texture, roughness: 0.7, metalness: 0 })
      packageMaterials.set(key, created)
      resources.materials.add(created)
      return created
    }

    function createInstances(
      parent: THREE.Object3D,
      shape: THREE.BufferGeometry,
      displayMaterial: THREE.Material,
      transforms: Array<{ position: THREE.Vector3; scale: THREE.Vector3; color: string }>,
    ) {
      if (!transforms.length) return
      const mesh = new THREE.InstancedMesh(shape, displayMaterial, transforms.length)
      mesh.castShadow = true
      mesh.receiveShadow = true
      const dummy = new THREE.Object3D()
      transforms.forEach((entry, index) => {
        dummy.position.copy(entry.position)
        dummy.scale.copy(entry.scale)
        dummy.rotation.set(0, (index % 3) * 0.12, 0)
        dummy.updateMatrix()
        mesh.setMatrixAt(index, dummy.matrix)
        mesh.setColorAt(index, new THREE.Color(entry.color))
      })
      mesh.instanceMatrix.needsUpdate = true
      if (mesh.instanceColor) mesh.instanceColor.needsUpdate = true
      parent.add(mesh)
    }

    function addShelfBlock(block: StoreMap['shelfBlocks'][number]) {
      const width = Math.max(0.48, block.width * worldScale)
      const depth = Math.max(0.48, block.height * worldScale)
      const group = new THREE.Group()
      group.position.set(mapX(block.x + block.width / 2), 0, mapZ(block.y + block.height / 2))
      group.rotation.y = -THREE.MathUtils.degToRad(block.rotation)
      world.add(group)

      const sector = block.sectorId ? sectorById.get(block.sectorId) : undefined
      const sectorName = sector?.name ?? 'Mercearia'
      const shelfCode = block.code.toUpperCase()
      const isFresh = /horti|fruta|produce/i.test(sectorName + ' ' + shelfCode)
      const isBakery = /padaria|panific/i.test(sectorName)
      const isCold = /latic|congel|frios/i.test(sectorName)
      const steel = isCold ? '#829da2' : '#526c70'
      const shelf = isCold ? '#d5e0df' : '#b8c0b5'
      const shelfHeight = isFresh ? 1.2 : isCold ? 1.68 : 1.55
      addBox(width + 0.12, 0.07, depth + 0.14, [0, 0.04, 0], '#64756f', group)
      const postPositions = [-depth * 0.47, 0, depth * 0.47]
      postPositions.forEach((z) => {
        addBox(0.055, shelfHeight, 0.055, [-width * 0.44, shelfHeight / 2, z], steel, group, 0, 0.6)
        addBox(0.055, shelfHeight, 0.055, [width * 0.44, shelfHeight / 2, z], steel, group, 0, 0.6)
      })
      const shelfLevels = isFresh ? [0.3, 0.55, 0.8, 1.05] : isCold ? [0.3, 0.66, 1.02, 1.38] : [0.28, 0.62, 0.96, 1.3]
      shelfLevels.forEach((height) => {
        addBox(width, 0.045, depth, [0, height, 0], shelf, group, 0, 0.7)
        addBox(width, 0.035, 0.04, [0, height + 0.04, depth * 0.49], '#e7e3d7', group)
      })

      const shelfProducts = locations.filter((location) => location.shelfBlock === block.code || location.shelfBlock === block.name)
      const productName = shelfProducts.map((location) => location.productName).filter(Boolean).join(' / ')
      const displayMaterial = packageMaterial(block.sectorId, sectorName, productName)
      const countAlongShelf = Math.max(4, Math.min(18, Math.floor(depth / 0.42)))
      const cartons: Array<{ position: THREE.Vector3; scale: THREE.Vector3; color: string }> = []
      const bottles: Array<{ position: THREE.Vector3; scale: THREE.Vector3; color: string }> = []
      const cans: Array<{ position: THREE.Vector3; scale: THREE.Vector3; color: string }> = []
      const fruit: Array<{ position: THREE.Vector3; scale: THREE.Vector3; color: string }> = []
      shelfLevels.forEach((height, level) => {
        for (let index = 0; index < countAlongShelf; index += 1) {
          const z = -depth * 0.43 + (index + 0.5) * (depth * 0.86 / countAlongShelf)
          const x = ((index + level) % 3 - 1) * Math.min(width * 0.23, 0.14)
          const hue = packageColors[(index + level * 2 + block.code.length) % packageColors.length]
          if (isFresh) {
            fruit.push({ position: new THREE.Vector3(x, height + 0.14, z), scale: new THREE.Vector3(0.14, 0.16, 0.14), color: packageColors[(index + level) % packageColors.length] })
          } else if (isBakery) {
            cartons.push({ position: new THREE.Vector3(x, height + 0.12, z), scale: new THREE.Vector3(0.18, 0.21, 0.18), color: index % 2 ? '#d9a455' : '#a96b3c' })
          } else if (isCold) {
            if (index % 3 === 0) bottles.push({ position: new THREE.Vector3(x, height + 0.14, z), scale: new THREE.Vector3(0.12, 0.28, 0.12), color: hue })
            else cartons.push({ position: new THREE.Vector3(x, height + 0.12, z), scale: new THREE.Vector3(0.16, 0.22, 0.14), color: hue })
          } else if (index % 4 === 0) {
            bottles.push({ position: new THREE.Vector3(x, height + 0.16, z), scale: new THREE.Vector3(0.13, 0.31, 0.13), color: hue })
          } else if (index % 3 === 0) {
            cans.push({ position: new THREE.Vector3(x, height + 0.1, z), scale: new THREE.Vector3(0.12, 0.18, 0.12), color: hue })
          } else {
            cartons.push({ position: new THREE.Vector3(x, height + 0.12, z), scale: new THREE.Vector3(0.18, 0.22, 0.16), color: hue })
          }
        }
      })
      createInstances(group, productGeometry.carton, displayMaterial, cartons)
      createInstances(group, productGeometry.bottle, displayMaterial, bottles)
      createInstances(group, productGeometry.can, displayMaterial, cans)
      createInstances(group, productGeometry.fruit, material('#ffffff', 0.62), fruit)
    }

    activeShelfBlocks.forEach(addShelfBlock)

    activePointsOfInterest.forEach((point) => {
      const x = mapX(point.x)
      const z = mapZ(point.y)
      if (point.type === 'ENTRANCE' || point.type === 'EXIT') {
        const doorZ = point.y < map.height * 0.28
          ? -worldDepth / 2 + wallThickness / 2
          : point.y > map.height * 0.72
            ? worldDepth / 2 - wallThickness / 2
            : z
        addBox(1.7, 1.85, 0.14, [x - 0.84, 0.92, doorZ], '#31596d', world, 0, 0.45)
        addBox(1.7, 1.85, 0.14, [x + 0.84, 0.92, doorZ], '#31596d', world, 0, 0.45)
        addBox(3.38, 0.22, 0.18, [x, 1.86, doorZ], '#648b77', world, 0, 0.45)
        addTextSign(point.name, point.type === 'ENTRANCE' ? 'ENTRADA PRINCIPAL' : 'SAÍDA', '#31596d', x, 2.55, doorZ, 2.5)
      } else if (point.type === 'CHECKOUT') {
        for (let lane = 0; lane < 4; lane += 1) {
          const laneX = x - 2.55 + lane * 0.62
          addBox(0.53, 0.7, 1.18, [laneX, 0.35, z], '#416879', world, 0, 0.65)
          addBox(0.24, 0.055, 0.7, [laneX - 0.08, 0.73, z - 0.12], '#d8b66a', world, 0, 0.62)
          addBox(0.09, 0.36, 0.16, [laneX + 0.19, 0.31, z + 0.42], '#bec8b9')
        }
        addTextSign(point.name, 'CAIXAS', '#527d68', x, 2.3, z, 3.5)
      } else if (point.type === 'CART') {
        for (let index = 0; index < 3; index += 1) {
          const cartX = x + index * 0.48
          addBox(0.37, 0.06, 0.42, [cartX, 0.42, z], '#9da9a2')
          addBox(0.38, 0.27, 0.06, [cartX, 0.57, z - 0.17], '#6e8588')
          addBox(0.045, 0.3, 0.045, [cartX - 0.17, 0.55, z + 0.08], '#6e8588')
          addBox(0.045, 0.3, 0.045, [cartX + 0.17, 0.55, z + 0.08], '#6e8588')
        }
        addTextSign(point.name, 'CARRINHOS', '#527d68', x, 1.4, z, 2.7)
      } else {
        addBox(0.62, 0.12, 0.62, [x, 0.12, z], '#66897a')
        addTextSign(point.name, point.type.replaceAll('_', ' '), '#365a67', x, 1.2, z, 2.8)
      }
    })

    function addTextSign(title: string, subtitle: string, accent: string, x: number, y: number, z: number, width: number) {
      const canvas = document.createElement('canvas')
      canvas.width = 512
      canvas.height = 132
      const context = canvas.getContext('2d')
      if (!context) return
      context.fillStyle = 'rgba(248,246,237,.97)'
      context.fillRect(2, 2, 508, 128)
      context.strokeStyle = accent
      context.lineWidth = 7
      context.strokeRect(5, 5, 502, 122)
      context.fillStyle = accent
      context.fillRect(10, 10, 14, 112)
      context.fillStyle = '#284756'
      context.font = '700 35px Arial, sans-serif'
      context.fillText(title.slice(0, 25), 38, 58)
      context.fillStyle = '#63736f'
      context.font = '600 20px Arial, sans-serif'
      context.fillText(subtitle.slice(0, 33), 38, 94)
      const texture = canvasTexture(canvas)
      const signMaterial = new THREE.SpriteMaterial({ map: texture, depthTest: true, depthWrite: false, transparent: true })
      resources.materials.add(signMaterial)
      const sign = new THREE.Sprite(signMaterial)
      sign.position.set(x, y, z)
      sign.scale.set(width, width * 0.258, 1)
      world.add(sign)
    }

    if (route && route.path.length > 0) {
      const points = route.path.map((node) => new THREE.Vector3(mapX(node.x), 0.22, mapZ(node.y)))
      const darkRoute = material('#465d56', 0.58)
      const routeMaterial = new THREE.MeshStandardMaterial({ color: '#f0a625', roughness: 0.45, emissive: '#9c570a', emissiveIntensity: 0.46 })
      resources.materials.add(routeMaterial)
      points.slice(1).forEach((point, index) => {
        const previous = points[index]
        const direction = point.clone().sub(previous)
        const length = direction.length()
        if (length < 0.001) return
        const underlayGeometry = geometry('route-outline-cylinder', () => new THREE.CylinderGeometry(0.16, 0.16, 1, 10))
        const routeGeometry = geometry('route-core-cylinder', () => new THREE.CylinderGeometry(0.095, 0.095, 1, 10))
        const orientation = new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, 1, 0), direction.clone().normalize())
        const underlay = new THREE.Mesh(underlayGeometry, darkRoute)
        underlay.position.copy(previous).add(point).multiplyScalar(0.5)
        underlay.position.y = 0.2
        underlay.quaternion.copy(orientation)
        underlay.scale.y = length
        world.add(underlay)
        const path = new THREE.Mesh(routeGeometry, routeMaterial)
        path.position.copy(underlay.position)
        path.position.y = 0.29
        path.quaternion.copy(orientation)
        path.scale.y = length
        world.add(path)
      })
      points.forEach((point) => {
        const joint = new THREE.Mesh(geometry('route-joint', () => new THREE.SphereGeometry(0.17, 12, 8)), routeMaterial)
        joint.position.copy(point)
        joint.position.y = 0.29
        world.add(joint)
      })
      const first = points[0]
      const last = points[points.length - 1]
      addBox(0.56, 0.11, 0.56, [first.x, 0.09, first.z], '#5a916f', world, 0, 0.5)
      addTextSign('INÍCIO', 'ENTRADA', '#487b60', first.x, 1.75, first.z, 2.45)
      addBox(0.56, 0.11, 0.56, [last.x, 0.1, last.z], '#31596d', world, 0, 0.5)
      addTextSign('FINAL', 'CAIXAS', '#31596d', last.x, 1.75, last.z, 2.45)

      const nodeById = new Map(activeNodes.map((node) => [node.id, node]))
      const stopsByNode = new Map<string, typeof route.orderedStops>()
      route.orderedStops.forEach((stop) => {
        const stopsAtNode = stopsByNode.get(stop.navigationNodeId) ?? []
        stopsAtNode.push(stop)
        stopsByNode.set(stop.navigationNodeId, stopsAtNode)
      })
      stopsByNode.forEach((stops, nodeId) => {
        const node = nodeById.get(nodeId)
        if (!node) return
        const x = mapX(node.x)
        const z = mapZ(node.y)
        const badgeGeometry = geometry('route-stop-badge', () => new THREE.CylinderGeometry(0.29, 0.29, 0.12, 28))
        const badge = new THREE.Mesh(badgeGeometry, material('#f4c75d', 0.52))
        badge.position.set(x, 0.52, z)
        badge.castShadow = true
        world.add(badge)
        const stopNumber = stops.map((stop) => String(stop.order).padStart(2, '0')).join('·')
        const productNames = stops.map((stop) => stop.productName).join(', ')
        addTextSign(stopNumber + '  ' + productNames, 'LOCAL DO PRODUTO', '#a8731d', x + 1.28, 2.15, z, 4.2)
        stops.forEach((stop) => {
          const location = locations.find((item) => item.id === stop.productLocationId)
          const shelf = location?.shelfBlock ? activeShelfBlocks.find((block) => block.code === location.shelfBlock || block.name === location.shelfBlock) : undefined
          if (!shelf) return
          const blockWidth = shelf.width * worldScale
          const blockDepth = shelf.height * worldScale
          const highlight = addBox(blockWidth + 0.22, 0.04, blockDepth + 0.22, [
            mapX(shelf.x + shelf.width / 2), 0.13, mapZ(shelf.y + shelf.height / 2),
          ], '#e1a632', world, shelf.rotation, 0.55)
          highlight.material = new THREE.MeshStandardMaterial({ color: '#e1a632', roughness: 0.55, transparent: true, opacity: 0.64, emissive: '#6e4712', emissiveIntensity: 0.2 })
          resources.materials.add(highlight.material as THREE.Material)
        })
      })
    }

    function frameRoute() {
      if (!route?.path.length) return
      const direction = camera.position.clone().sub(controls.target).normalize()
      const forward = direction.clone().negate()
      const right = forward.clone().cross(new THREE.Vector3(0, 1, 0)).normalize()
      const viewUp = right.clone().cross(forward).normalize()
      const points = route.path.map((point) => new THREE.Vector3(mapX(point.x), 0.22, mapZ(point.y)))
      const horizontalPositions = points.map((point) => point.dot(right))
      const verticalPositions = points.map((point) => point.dot(viewUp))
      const minRight = Math.min(...horizontalPositions)
      const maxRight = Math.max(...horizontalPositions)
      const minUp = Math.min(...verticalPositions)
      const maxUp = Math.max(...verticalPositions)
      const center = right.clone().multiplyScalar((minRight + maxRight) / 2)
        .add(viewUp.clone().multiplyScalar((minUp + maxUp) / 2))
      const routeWidth = maxRight - minRight + 2.4
      const routeHeight = maxUp - minUp + 3.5
      const distance = Math.max(
        routeWidth / (2 * halfFovTangent * Math.max(camera.aspect, 0.1)),
        routeHeight / (2 * halfFovTangent),
      ) * 1.18
      cameraMoved = true
      controls.target.copy(center)
      camera.position.copy(center.clone().add(direction.multiplyScalar(distance)))
      controls.update()
    }

    const controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.075
    controls.minPolarAngle = 0.12
    controls.maxPolarAngle = Math.PI / 2.02
    controls.minDistance = maxDimension * 0.25
    controls.maxDistance = maxDimension * 8
    controls.target.set(0, 0, 0)
    controls.update()
    let cameraMoved = false
    const markCameraMoved = () => { cameraMoved = true }
    controls.addEventListener('start', markCameraMoved)
    controlsRef.current = controls
    cameraRef.current = camera

    const render = () => {
      controls.update()
      renderer.render(scene, camera)
    }
    renderer.setAnimationLoop(render)

    const resize = () => {
      if (!host.clientWidth || !host.clientHeight) return
      camera.aspect = host.clientWidth / host.clientHeight
      camera.updateProjectionMatrix()
      if (!cameraMoved) {
        cameraHome = homePosition(camera.aspect)
        camera.position.copy(cameraHome)
        controls.update()
      }
      renderer.setSize(host.clientWidth, host.clientHeight)
      render()
    }
    const observer = new ResizeObserver(resize)
    observer.observe(host)

    const focus = () => frameRoute()
    host.addEventListener('focus-route', focus)
    const resetCamera = () => {
      controls.target.set(0, 0, 0)
      cameraHome = homePosition(camera.aspect)
      camera.position.copy(cameraHome)
      cameraMoved = false
      controls.update()
    }
    host.addEventListener('reset-map', resetCamera)
    const onContextLost = (event: Event) => {
      event.preventDefault()
      setSceneError('A sessão WebGL foi interrompida. Alterne para a planta 2D para continuar.')
    }
    renderer.domElement.addEventListener('webglcontextlost', onContextLost)
    render()

    return () => {
      host.removeEventListener('focus-route', focus)
      host.removeEventListener('reset-map', resetCamera)
      controls.removeEventListener('start', markCameraMoved)
      renderer.domElement.removeEventListener('webglcontextlost', onContextLost)
      observer.disconnect()
      controls.dispose()
      controlsRef.current = null
      cameraRef.current = null
      renderer.setAnimationLoop(null)
      scene.traverse((object) => {
        const mesh = object as THREE.Mesh
        if (mesh.isMesh && mesh.castShadow) mesh.castShadow = false
      })
      resources.geometries.forEach((entry) => entry.dispose())
      resources.materials.forEach((entry) => entry.dispose())
      resources.textures.forEach((entry) => entry.dispose())
      renderer.dispose()
      if (host.contains(renderer.domElement)) host.removeChild(renderer.domElement)
    }
  }, [map, route, locations])

  function zoomBy(factor: number) {
    const camera = cameraRef.current
    const controls = controlsRef.current
    if (!camera || !controls) return
    camera.position.copy(controls.target.clone().add(camera.position.clone().sub(controls.target).multiplyScalar(factor)))
    controls.update()
  }

  if (!map) {
    return <div className="three-map-empty"><strong>O mapa 3D está pronto para a loja ativa.</strong><span>Selecione uma loja para carregar os setores, corredores e produtos.</span></div>
  }

  return (
    <div className="three-map-view" aria-label="Mapa tridimensional interativo da loja">
      <div className="three-map-canvas" ref={hostRef} />
      {sceneError && <div className="three-map-error" role="alert"><span>{sceneError}</span>{onSwitchTo2D && <button type="button" onClick={onSwitchTo2D}>Abrir planta 2D</button>}</div>}
      {!sceneError && <div className="three-map-tools" aria-label="Controles do mapa 3D">
        {route && <button type="button" aria-label="Enquadrar percurso" title="Enquadrar percurso" onClick={() => hostRef.current?.dispatchEvent(new CustomEvent('focus-route'))}><LocateFixed /></button>}
        <button type="button" aria-label="Aproximar" title="Aproximar" onClick={() => zoomBy(0.82)}><Plus /></button>
        <button type="button" aria-label="Afastar" title="Afastar" onClick={() => zoomBy(1.22)}><Minus /></button>
        <button type="button" aria-label="Reenquadrar mapa" title="Reenquadrar mapa" onClick={() => hostRef.current?.dispatchEvent(new CustomEvent('reset-map'))}><RotateCcw /></button>
      </div>}
      {!sceneError && <p className="three-map-hint">Arraste para girar · roda para aproximar · botão direito para mover</p>}
      {!sceneError && route && <span className="three-map-route-chip">{route.orderedStops.length} {route.orderedStops.length === 1 ? 'parada' : 'paradas'} · rota da loja</span>}
    </div>
  )
}

function paletteFor(code: string, name: string) {
  const key = (code + ' ' + name).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase()
  const match = Object.entries(paletteBySector).find(([sector]) => key.includes(sector))
  return match?.[1] ?? { floor: '#e5e5dc', products: '#71898a' }
}
