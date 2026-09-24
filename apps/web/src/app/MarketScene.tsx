import { useEffect, useRef, useState } from 'react'
import * as THREE from 'three'
import WebGL from 'three/addons/capabilities/WebGL.js'
import { Pause, Play, RotateCcw } from 'lucide-react'

const sectorNames = ['Hortifruti', 'Laticínios', 'Padaria', 'Mercearia', 'Higiene', 'Congelados']
const zoneColors = ['#d5e6d9', '#d7e4e8', '#eee1c6', '#e7dfcf', '#e1e7d8', '#d9e5e9']
const webglFallback = 'Este navegador não conseguiu iniciar WebGL. A planta esquemática abaixo mostra os setores, corredores, entrada e caixas da demonstração.'
const motionDurationMs = 8_000

export function MarketScene() {
  const hostRef = useRef<HTMLDivElement>(null)
  const animationControlRef = useRef<((enabled: boolean) => void) | null>(null)
  const [playing, setPlaying] = useState(() => !window.matchMedia('(prefers-reduced-motion: reduce)').matches)
  const playingRef = useRef(playing)
  const [fallback, setFallback] = useState(() => WebGL.isWebGL2Available() ? '' : webglFallback)

  useEffect(() => {
    playingRef.current = playing
    animationControlRef.current?.(playing)
  }, [playing])

  useEffect(() => {
    if (!playing || fallback) return
    const timeoutId = window.setTimeout(() => setPlaying(false), motionDurationMs)
    return () => window.clearTimeout(timeoutId)
  }, [playing, fallback])

  useEffect(() => {
    const host = hostRef.current
    if (!host || fallback) return
    let renderer: THREE.WebGLRenderer
    try {
      renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false, powerPreference: 'low-power' })
    } catch {
      queueMicrotask(() => setFallback(webglFallback))
      return
    }

    const scene = new THREE.Scene()
    scene.background = new THREE.Color('#eeeade')
    const camera = new THREE.OrthographicCamera(-10, 10, 7.2, -7.2, 0.1, 100)
    camera.position.set(15, 17, 19)
    camera.lookAt(0, 0, 0)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5))
    renderer.setSize(host.clientWidth, host.clientHeight)
    renderer.outputColorSpace = THREE.SRGBColorSpace
    renderer.toneMapping = THREE.ACESFilmicToneMapping
    renderer.toneMappingExposure = 1.15
    renderer.domElement.setAttribute('aria-label', 'Modelo tridimensional esquemático do supermercado fictício Mercado Aurora')
    renderer.domElement.setAttribute('role', 'img')
    renderer.domElement.setAttribute('tabindex', '0')
    host.replaceChildren(renderer.domElement)

    const world = new THREE.Group()
    scene.add(world)
    scene.add(new THREE.HemisphereLight('#fffdf3', '#82918c', 2.3))
    const keyLight = new THREE.DirectionalLight('#fff8e8', 2.4)
    keyLight.position.set(-8, 15, 11)
    scene.add(keyLight)

    const materials = new Map<string, THREE.MeshStandardMaterial>()
    const geometries = new Map<string, THREE.BoxGeometry>()
    const material = (color: string, roughness = 0.9) => {
      const key = `${color}:${roughness}`
      const cached = materials.get(key)
      if (cached) return cached
      const result = new THREE.MeshStandardMaterial({ color, roughness, metalness: 0 })
      materials.set(key, result)
      return result
    }
    const box = (size: [number, number, number], color: string, position: [number, number, number], target = world, roughness?: number) => {
      const key = size.join(':')
      let geometry = geometries.get(key)
      if (!geometry) {
        geometry = new THREE.BoxGeometry(...size)
        geometries.set(key, geometry)
      }
      const mesh = new THREE.Mesh(geometry, material(color, roughness))
      mesh.position.set(...position)
      target.add(mesh)
      return mesh
    }

    box([17, 0.35, 12], '#f8f6ee', [0, -0.27, 0])
    box([17.1, 0.16, 0.18], '#73898a', [0, -0.13, -6])
    box([17.1, 0.16, 0.18], '#73898a', [0, -0.13, 6])
    box([0.18, 0.16, 12], '#73898a', [-8.5, -0.13, 0])
    box([0.18, 0.16, 12], '#73898a', [8.5, -0.13, 0])

    const zonePositions: Array<[number, number]> = [[-5.5, -3.7], [0, -3.7], [5.5, -3.7], [-5.5, 3.7], [0, 3.7], [5.5, 3.7]]
    zonePositions.forEach(([x, z], index) => {
      box([4.9, 0.08, 3.6], zoneColors[index], [x, -0.06, z])
      box([2.3, 0.06, 0.06], '#a6b2a7', [x, 0.02, z - 1.33])
      box([2.3, 0.06, 0.06], '#a6b2a7', [x, 0.02, z + 1.33])
    })

    const shelfRows = [-3.15, -1.85, -0.55, 0.75, 2.05, 3.35]
    shelfRows.forEach((z, row) => {
      const centers = [-5.1, -1.7, 1.7, 5.1]
      centers.forEach((x, part) => {
        if ((row === 1 && part === 1) || (row === 4 && part === 2)) return
        const sectionLength = (row === 0 || row === 5) ? 2.2 : 2.85
        box([sectionLength, 1.15, 0.56], (row + part) % 2 ? '#607b83' : '#748a8b', [x, 0.63, z])
      })
    })
    // Cross aisles create turns and a continuous path through the six sectors.
    ;[-6.2, -0.05, 6.15].forEach((z) => box([16.1, 0.035, 0.48], '#ebe7d9', [0, 0.015, z]))

    // A decorative route belongs only to this fictional scene; it is independent of the API map.
    const illustrativePath = new THREE.CatmullRomCurve3([
      new THREE.Vector3(-7.4, 0.14, 5.55), new THREE.Vector3(-7.1, 0.14, 4.7), new THREE.Vector3(-5.7, 0.14, 4.7),
      new THREE.Vector3(-5.7, 0.14, 2.8), new THREE.Vector3(-2.2, 0.14, 2.8), new THREE.Vector3(-2.2, 0.14, -4.8),
      new THREE.Vector3(3.8, 0.14, -4.8), new THREE.Vector3(3.8, 0.14, 4.8), new THREE.Vector3(7.2, 0.14, 4.8),
    ])
    const pathGeometry = new THREE.TubeGeometry(illustrativePath, 72, 0.075, 7, false)
    const standaloneGeometries = new Set<THREE.BufferGeometry>([pathGeometry])
    const pathMaterial = new THREE.MeshStandardMaterial({ color: '#a8731d', roughness: 0.65, emissive: '#4b2e05', emissiveIntensity: 0.16 })
    world.add(new THREE.Mesh(pathGeometry, pathMaterial))

    // Entrance and cart corral.
    box([2.2, 0.6, 0.26], '#355b73', [-6.8, 0.24, 5.42])
    for (let index = 0; index < 3; index += 1) {
      const cart = new THREE.Group()
      cart.position.set(-5.8 + index * 0.48, 0.05, 5.55)
      world.add(cart)
      box([0.42, 0.04, 0.48], '#9da9a2', [0, 0.38, 0], cart)
      box([0.42, 0.3, 0.05], '#718890', [0, 0.54, -0.2], cart)
      box([0.05, 0.33, 0.06], '#718890', [-0.19, 0.54, 0.05], cart)
      box([0.05, 0.33, 0.06], '#718890', [0.19, 0.54, 0.05], cart)
      const wheelGeometry = new THREE.CylinderGeometry(0.07, 0.07, 0.045, 10)
      standaloneGeometries.add(wheelGeometry)
      for (const wheelX of [-0.15, 0.15]) {
        const wheel = new THREE.Mesh(wheelGeometry, material('#40545b'))
        wheel.rotation.z = Math.PI / 2
        wheel.position.set(wheelX, 0.16, 0.2)
        cart.add(wheel)
      }
    }

    // Checkout counters and lane dividers at the far right.
    for (let index = 0; index < 4; index += 1) {
      const z = -2.4 + index * 1.38
      box([1.55, 0.72, 0.6], '#416b7a', [7.1, 0.36, z])
      box([0.42, 0.06, 0.46], '#d4b66b', [6.65, 0.77, z])
      box([0.07, 0.38, 0.07], '#acb4a4', [7.8, 0.19, z + 0.48])
    }
    box([2.8, 0.12, 0.15], '#355b73', [7.05, 0.05, -3.35])

    let disposed = false
    let pointerStart: { x: number; rotation: number } | null = null
    let manualRotation = -0.08
    let previousTime = 0
    const renderScene = () => {
      camera.lookAt(0, 0, 0)
      renderer.render(scene, camera)
    }
    const draw = (time: number) => {
      if (disposed || !playingRef.current) return
      if (previousTime) camera.position.x = Math.sin(time * 0.00008) * 1.4 + 15
      world.rotation.y += (manualRotation - world.rotation.y) * 0.018
      renderScene()
      previousTime = time
    }
    const setMotion = (enabled: boolean) => {
      if (disposed) return
      renderer.setAnimationLoop(enabled ? draw : null)
      if (!enabled) {
        previousTime = 0
        world.rotation.y = manualRotation
        renderScene()
      }
    }
    animationControlRef.current = setMotion
    setMotion(playingRef.current)

    const resize = () => {
      if (!host.clientWidth || !host.clientHeight) return
      const aspect = host.clientWidth / host.clientHeight
      const viewHeight = 14.4
      camera.left = -viewHeight * aspect / 2
      camera.right = viewHeight * aspect / 2
      camera.top = viewHeight / 2
      camera.bottom = -viewHeight / 2
      camera.updateProjectionMatrix()
      renderer.setSize(host.clientWidth, host.clientHeight)
      if (!playingRef.current) renderScene()
    }
    const observer = new ResizeObserver(resize)
    observer.observe(host)
    const startRotate = (event: PointerEvent) => { pointerStart = { x: event.clientX, rotation: manualRotation }; renderer.domElement.setPointerCapture(event.pointerId) }
    const rotate = (event: PointerEvent) => {
      if (!pointerStart) return
      manualRotation = pointerStart.rotation + (event.clientX - pointerStart.x) * 0.006
      if (!playingRef.current) {
        world.rotation.y = manualRotation
        renderScene()
      }
    }
    const stopRotate = () => { pointerStart = null }
    const keyRotate = (event: KeyboardEvent) => {
      if (event.key === 'ArrowLeft') { manualRotation -= 0.12; event.preventDefault() }
      else if (event.key === 'ArrowRight') { manualRotation += 0.12; event.preventDefault() }
      else return
      if (!playingRef.current) {
        world.rotation.y = manualRotation
        renderScene()
      }
    }
    const resetScene = () => {
      manualRotation = -0.08
      camera.position.set(15, 17, 19)
      if (!playingRef.current) {
        world.rotation.y = manualRotation
        renderScene()
      }
    }
    renderer.domElement.addEventListener('pointerdown', startRotate)
    renderer.domElement.addEventListener('pointermove', rotate)
    renderer.domElement.addEventListener('pointerup', stopRotate)
    renderer.domElement.addEventListener('pointercancel', stopRotate)
    renderer.domElement.addEventListener('keydown', keyRotate)
    host.addEventListener('scene-reset', resetScene)
    const onContextLost = (event: Event) => { event.preventDefault(); setFallback('A sessão WebGL foi interrompida. Use a planta esquemática abaixo para continuar a exploração.') }
    renderer.domElement.addEventListener('webglcontextlost', onContextLost)

    return () => {
      disposed = true
      animationControlRef.current = null
      renderer.setAnimationLoop(null)
      observer.disconnect()
      renderer.domElement.removeEventListener('pointerdown', startRotate)
      renderer.domElement.removeEventListener('pointermove', rotate)
      renderer.domElement.removeEventListener('pointerup', stopRotate)
      renderer.domElement.removeEventListener('pointercancel', stopRotate)
      renderer.domElement.removeEventListener('keydown', keyRotate)
      host.removeEventListener('scene-reset', resetScene)
      renderer.domElement.removeEventListener('webglcontextlost', onContextLost)
      geometries.forEach((geometry) => geometry.dispose())
      standaloneGeometries.forEach((geometry) => geometry.dispose())
      materials.forEach((entry) => entry.dispose())
      pathMaterial.dispose()
      renderer.dispose()
      if (host.contains(renderer.domElement)) host.removeChild(renderer.domElement)
    }
  }, [fallback])

  useEffect(() => {
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)')
    const onChange = (event: MediaQueryListEvent) => { if (event.matches) setPlaying(false) }
    preference.addEventListener('change', onChange)
    return () => preference.removeEventListener('change', onChange)
  }, [])

  return (
    <section className="scene-card" aria-label="Demonstração 3D do Mercado Aurora">
      <div className="scene-disclaimer"><span className="fiction-dot" /><strong>Mercado fictício — demonstração visual</strong><span>Este caminho é decorativo e não corresponde à rota de compras.</span></div>
      {fallback ? <StaticMarketFallback message={fallback} /> : <div className="scene-canvas" ref={hostRef} />}
      {!fallback && <div className="scene-controls">
        <button type="button" onClick={() => setPlaying((value) => !value)}>{playing ? <Pause /> : <Play />}{playing ? 'Pausar movimento' : 'Reproduzir movimento'}</button>
        <span>Arraste a cena ou use as setas ← → para girar</span>
        <button type="button" className="scene-reset" onClick={() => { if (hostRef.current) hostRef.current.dispatchEvent(new CustomEvent('scene-reset')) }} aria-label="Reiniciar posição da cena"><RotateCcw /></button>
      </div>}
      <div className="scene-sectors" aria-label="Setores representados na cena">{sectorNames.map((name, index) => <span key={name}><i style={{ backgroundColor: zoneColors[index] }} />{name}</span>)}</div>
    </section>
  )
}

function StaticMarketFallback({ message }: { message: string }) {
  return <div className="static-market-fallback"><p>{message}</p><div className="fallback-market-map">
    <div className="fallback-entrance">Entrada <i /></div>
    {sectorNames.map((name, index) => <div key={name} className={`fallback-sector sector-${index + 1}`}><strong>{name}</strong><div className="fallback-aisles"><i /><i /><i /></div></div>)}
    <div className="fallback-checkouts">Caixas <i /><i /><i /></div>
    <svg viewBox="0 0 100 58" aria-hidden="true"><path d="M9 51V40H22V29H38V12H59V24H76V45H92" /></svg>
  </div></div>
}
