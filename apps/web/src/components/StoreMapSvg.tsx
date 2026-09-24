import { useMemo, useRef, useState } from 'react'
import { LocateFixed, Minus, Plus, RotateCcw } from 'lucide-react'
import type { PointerEvent, WheelEvent } from 'react'
import type { CalculatedRoute, MapEdge, MapGeometry, MapNode, PointOfInterest, StoreMap } from '@/types/api'

interface StoreMapSvgProps {
  map: StoreMap
  route?: CalculatedRoute | null
  onSelectElement?: (id: string) => void
  title?: string
  showNetwork?: boolean
  showInactiveElements?: boolean
  selectedElementId?: string
}

const sectorColors = ['#e1eee4', '#e8eee1', '#e1ebed', '#eceade', '#deece4', '#e3eee7', '#e8ebdf']

export function StoreMapSvg({ map, route, onSelectElement, title = 'Mapa da loja', showNetwork = false, showInactiveElements = false, selectedElementId = '' }: StoreMapSvgProps) {
  const [zoom, setZoom] = useState(1)
  const [pan, setPan] = useState({ x: 0, y: 0 })
  const drag = useRef<{ x: number; y: number; startX: number; startY: number } | null>(null)
  const stopsByNode = useMemo(() => {
    const stops = new Map<string, number[]>()
    for (const stop of route?.orderedStops ?? []) {
      const orders = stops.get(stop.navigationNodeId) ?? []
      orders.push(stop.order)
      stops.set(stop.navigationNodeId, orders)
    }
    return stops
  }, [route])
  const routePoints = useMemo(() => route?.path.map(({ x, y }) => `${x},${y}`).join(' ') ?? '', [route])
  const visibleSectors = map.sectors.filter((sector) => showInactiveElements || sector.active !== false)
  const visibleAisles = map.aisles.filter((aisle) => showInactiveElements || aisle.active !== false)
  const visibleShelfBlocks = map.shelfBlocks.filter((block) => showInactiveElements || block.active !== false)
  const visiblePointsOfInterest = map.pointsOfInterest.filter((poi) => showInactiveElements || poi.active !== false)
  const visibleNodes = map.nodes.filter((node) => showInactiveElements || node.active !== false)
  const visibleEdges = map.edges.filter((edge) => showInactiveElements || edge.active !== false)

  function onWheel(event: WheelEvent<SVGSVGElement>) {
    event.preventDefault()
    setZoom((current) => Math.min(3, Math.max(0.7, current * (event.deltaY < 0 ? 1.12 : 0.89))))
  }

  function onPointerDown(event: PointerEvent<SVGSVGElement>) {
    if (event.button !== 0) return
    event.currentTarget.setPointerCapture(event.pointerId)
    drag.current = { x: event.clientX, y: event.clientY, startX: pan.x, startY: pan.y }
  }

  function onPointerMove(event: PointerEvent<SVGSVGElement>) {
    if (!drag.current) return
    const bounds = event.currentTarget.getBoundingClientRect()
    setPan({
      x: drag.current.startX + (event.clientX - drag.current.x) * map.width / bounds.width,
      y: drag.current.startY + (event.clientY - drag.current.y) * map.height / bounds.height,
    })
  }

  function stopDragging(event: PointerEvent<SVGSVGElement>) {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
    drag.current = null
  }

  function focusRoute() {
    const points = route?.path ?? []
    if (!points.length) return
    const xs = points.map(({ x }) => Number(x))
    const ys = points.map(({ y }) => Number(y))
    const routeWidth = Math.max(16, Math.max(...xs) - Math.min(...xs))
    const routeHeight = Math.max(16, Math.max(...ys) - Math.min(...ys))
    const nextZoom = Math.min(3, Math.max(0.75, Math.min(map.width / (routeWidth * 1.4), map.height / (routeHeight * 1.4))))
    const routeCenterX = (Math.min(...xs) + Math.max(...xs)) / 2
    const routeCenterY = (Math.min(...ys) + Math.max(...ys)) / 2
    setZoom(nextZoom)
    setPan({ x: (1 - nextZoom) * (centerX - routeCenterX), y: (1 - nextZoom) * (centerY - routeCenterY) })
  }

  const centerX = map.width / 2
  const centerY = map.height / 2
  const transform = `translate(${centerX + pan.x} ${centerY + pan.y}) scale(${zoom}) translate(${-centerX} ${-centerY})`

  return (
    <div className="atlas-canvas-wrap">
      <svg
        className="atlas-canvas"
        viewBox={`0 0 ${map.width} ${map.height}`}
        role="img"
        aria-labelledby="store-map-title store-map-description"
        onWheel={onWheel}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={stopDragging}
        onPointerCancel={stopDragging}
      >
        <title id="store-map-title">{title}: {map.name}</title>
        <desc id="store-map-description">
          Planta vetorial da loja. Todo o piso livre, inclusive as áreas abertas e os setores, é transitável. As faixas tracejadas identificam os corredores; as prateleiras são obstáculos. Use os controles de zoom ou arraste para explorar. A rota exibida vem da API.
        </desc>
        <rect width={map.width} height={map.height} fill="#e9e8dd" />
        <g transform={transform}>
          <rect x="2" y="2" width={map.width - 4} height={map.height - 4} rx="3" fill="#edf2e9" stroke="#526b78" strokeWidth="1.2" />
          {visibleSectors.map((sector, index) => (
            <g key={sector.id} transform={geometryTransform(sector)}>
              <rect
                x={sector.x} y={sector.y} width={sector.width} height={sector.height} rx="2"
                fill={sectorColors[index % sectorColors.length]} stroke="#c4d0c8" strokeWidth="0.45"
                className={`${sector.id === selectedElementId ? 'map-element-selected ' : ''}${sector.active === false ? 'map-element-inactive' : ''}`}
                onClick={() => onSelectElement?.(sector.id)}
              />
            </g>
          ))}
          {visibleAisles.map((aisle) => {
            const centerX = aisle.x + aisle.width / 2
            const centerY = aisle.y + aisle.height / 2
            const isVertical = aisle.height > aisle.width
            return (
              <g key={aisle.id} transform={geometryTransform(aisle)} onClick={() => onSelectElement?.(aisle.id)}>
                <rect className={`${aisle.id === selectedElementId ? 'map-element-selected ' : ''}${aisle.active === false ? 'map-element-inactive' : ''}`} x={aisle.x} y={aisle.y} width={aisle.width} height={aisle.height} rx="0.8" fill="#dcece2" stroke="#8eae9c" strokeWidth="0.48" />
                {isVertical
                  ? <line x1={centerX} y1={aisle.y + 1} x2={centerX} y2={aisle.y + aisle.height - 1} stroke="#97b3a4" strokeWidth="0.32" strokeDasharray="1.3 1" />
                  : <line x1={aisle.x + 0.7} y1={centerY} x2={aisle.x + aisle.width - 0.7} y2={centerY} stroke="#97b3a4" strokeWidth="0.32" strokeDasharray="1.3 1" />}
                <text
                  x={centerX}
                  y={centerY}
                  transform={isVertical ? `rotate(-90 ${centerX} ${centerY})` : undefined}
                  className="map-aisle-label"
                  textAnchor="middle"
                  dominantBaseline="middle"
                  pointerEvents="none"
                >{aisle.code}</text>
              </g>
            )
          })}
          {visibleShelfBlocks.map((block) => {
            const isVertical = block.height > block.width
            const sectionCount = Math.min(8, Math.max(2, Math.round(Math.max(block.width, block.height) / 5)))
            const centerX = block.x + block.width / 2
            const centerY = block.y + block.height / 2
            return (
              <g key={block.id} transform={geometryTransform(block)} onClick={() => onSelectElement?.(block.id)}>
                <rect x={block.x + 0.7} y={block.y + 0.8} width={block.width} height={block.height} rx="0.5" fill="#334d56" opacity="0.18" />
                <rect
                  className={`${block.id === selectedElementId ? 'map-element-selected ' : ''}${block.active === false ? 'map-element-inactive' : ''}`}
                  x={block.x} y={block.y} width={block.width} height={block.height} rx="0.5"
                  fill="#647c83" stroke="#405b67" strokeWidth="0.35"
                />
                <line x1={block.x + 0.5} y1={block.y + 0.8} x2={block.x + block.width - 0.5} y2={block.y + 0.8} stroke="#dce4dd" strokeWidth="0.35" />
                {Array.from({ length: sectionCount - 1 }, (_, index) => {
                  const sectionPosition = (index + 1) / sectionCount
                  return isVertical
                    ? <line key={index} x1={block.x + 0.6} y1={block.y + block.height * sectionPosition} x2={block.x + block.width - 0.6} y2={block.y + block.height * sectionPosition} stroke="#bdcbc5" strokeWidth="0.28" opacity="0.9" />
                    : <line key={index} x1={block.x + block.width * sectionPosition} y1={block.y + 0.6} x2={block.x + block.width * sectionPosition} y2={block.y + block.height - 0.6} stroke="#bdcbc5" strokeWidth="0.28" opacity="0.9" />
                })}
                <text
                  x={centerX}
                  y={centerY}
                  transform={isVertical ? `rotate(-90 ${centerX} ${centerY})` : undefined}
                  className="map-shelf-label"
                  textAnchor="middle"
                  dominantBaseline="middle"
                  pointerEvents="none"
                >{block.code}</text>
                <title>{block.code}{block.name ? ` · ${block.name}` : ''}</title>
              </g>
            )
          })}
          {visibleSectors.map((sector) => (
            <text
              key={`${sector.id}-label`}
              x={sector.x + 2}
              y={sector.y + 5}
              className="map-sector-label"
              pointerEvents="none"
            >{sector.name}</text>
          ))}
          {showNetwork && visibleEdges.map((edge) => {
            const start = map.nodes.find((node) => node.id === edge.fromNodeId)
            const end = map.nodes.find((node) => node.id === edge.toNodeId)
            if (!start || !end) return null
            return <NetworkEdge key={edge.id} edge={edge} start={start} end={end} onSelect={onSelectElement} selected={edge.id === selectedElementId} />
          })}
          {route && routePoints && <polyline points={routePoints} className="map-route-line" />}
          {visiblePointsOfInterest.map((poi) => <PoiMarker key={poi.id} poi={poi} onSelect={onSelectElement} selected={poi.id === selectedElementId} />)}
          {showNetwork && visibleNodes.map((node) => <NodeMarker key={node.id} node={node} onSelect={onSelectElement} selected={node.id === selectedElementId} />)}
          {visibleNodes.filter((node) => stopsByNode.has(node.id)).map((node) => (
            <StopMarker key={node.id} node={node} orders={stopsByNode.get(node.id) ?? []} />
          ))}
        </g>
      </svg>
      <div className="map-controls" aria-label="Controles do mapa">
        {route && <button type="button" aria-label="Enquadrar percurso" onClick={focusRoute}><LocateFixed /></button>}
        <button type="button" aria-label="Aumentar zoom" onClick={() => setZoom((value) => Math.min(3, value + 0.2))}><Plus /></button>
        <button type="button" aria-label="Diminuir zoom" onClick={() => setZoom((value) => Math.max(0.7, value - 0.2))}><Minus /></button>
        <button type="button" aria-label="Reenquadrar mapa" onClick={() => { setZoom(1); setPan({ x: 0, y: 0 }) }}><RotateCcw /></button>
      </div>
      <p className="map-zoom-hint">Use a roda do mouse ou arraste para explorar</p>
      <div className="map-scale" aria-hidden="true"><span>0</span><i /><span>{Math.round(10 * map.scaleMetersPerUnit)} m</span></div>
    </div>
  )
}

function geometryTransform(geometry: MapGeometry): string {
  const cx = geometry.x + geometry.width / 2
  const cy = geometry.y + geometry.height / 2
  return `rotate(${geometry.rotation} ${cx} ${cy})`
}

function NetworkEdge({ edge, start, end, onSelect, selected }: { edge: MapEdge; start: MapNode; end: MapNode; onSelect?: (id: string) => void; selected: boolean }) {
  return <g onClick={() => onSelect?.(edge.id)} className={`map-network-edge-hit ${selected ? 'map-element-selected' : ''}`}><line x1={start.x} y1={start.y} x2={end.x} y2={end.y} className="map-network-edge" /><line x1={start.x} y1={start.y} x2={end.x} y2={end.y} className="map-network-edge-target" /></g>
}

function PoiMarker({ poi, onSelect, selected }: { poi: PointOfInterest; onSelect?: (id: string) => void; selected: boolean }) {
  const symbol = poi.type === 'ENTRANCE' ? 'E' : poi.type === 'EXIT' ? 'S' : poi.type === 'CHECKOUT' ? 'C' : poi.type === 'CART' ? 'A' : poi.type === 'RESTROOM' ? 'B' : 'i'
  return (
    <g transform={`translate(${poi.x} ${poi.y})`} className={`map-poi ${selected ? 'map-element-selected' : ''}`} aria-label={poi.name} onClick={() => onSelect?.(poi.id)}>
      <circle r="2.2" />
      <text textAnchor="middle" dominantBaseline="central">{symbol}</text>
    </g>
  )
}

function NodeMarker({ node, onSelect, selected }: { node: MapNode; onSelect?: (id: string) => void; selected: boolean }) {
  return <g transform={`translate(${node.x} ${node.y})`} className={`map-admin-node ${selected ? 'map-element-selected' : ''}`} onClick={() => onSelect?.(node.id)} aria-label={node.label ?? node.type}><circle r="1.1" /><title>{node.type}{node.label ? ` · ${node.label}` : ''}</title></g>
}

function StopMarker({ node, orders }: { node: MapNode; orders: number[] }) {
  const label = orders.join('·')
  const description = orders.length > 1
    ? `Paradas ${orders.join(' e ')} no mesmo local`
    : `Parada ${label}`
  return (
    <g transform={`translate(${node.x} ${node.y})`} className="map-stop-marker" aria-label={description}>
      <circle r={orders.length > 1 ? 3 : 2.3} />
      {label && <text textAnchor="middle" dominantBaseline="central" style={{ fontSize: orders.length > 1 ? 1.65 : 2.3 }}>{label}</text>}
      <title>{description}</title>
    </g>
  )
}
