import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { CalculatedRoute, StoreMap } from '@/types/api'
import { StoreMapSvg } from './StoreMapSvg'

const map: StoreMap = {
  id: 'map', storeId: 'store', version: 4, name: 'Denso', width: 240, height: 160,
  scaleMetersPerUnit: 0.25, sectors: [], aisles: [], pointsOfInterest: [], nodes: [], edges: [],
  shelfBlocks: [{ id: 'g', code: 'G01', name: 'Gôndola · Arroz', sectorId: null, aisleId: null,
    x: 22, y: 32, width: 4, height: 24, rotation: 0 }],
}
const route: CalculatedRoute = {
  storeId: 'store', mapId: 'map', orderedStops: [], distanceMeters: 10,
  path: [{ nodeId: 'a', x: 180, y: 100 }, { nodeId: 'b', x: 210, y: 100 }, { nodeId: 'c', x: 210, y: 120 }],
}

describe('dense StoreMapSvg', () => {
  it('preserves every bend from the backend path', () => {
    const { container } = render(<StoreMapSvg map={map} route={route} />)
    expect(container.querySelector('polyline')).toHaveAttribute('points', '180,100 210,100 210,120')
  })

  it('centers a route away from the map center when framing it', () => {
    const { container } = render(<StoreMapSvg map={map} route={route} />)
    fireEvent.click(screen.getByRole('button', { name: 'Enquadrar percurso' }))
    const transform = container.querySelector('svg > g')!.getAttribute('transform')!
    const [tx, ty, zoom] = transform.match(/-?\d+(?:\.\d+)?/g)!.map(Number)
    expect(tx + zoom * (195 - 120)).toBeCloseTo(120)
    expect(ty + zoom * (110 - 80)).toBeCloseTo(80)
  })

  it('reveals fixture codes on zoom while preserving their full tooltip names', () => {
    render(<StoreMapSvg map={map} />)
    expect(screen.queryByText('G01', { selector: 'text' })).not.toBeInTheDocument()
    expect(screen.getByText('G01 · Gôndola · Arroz', { selector: 'title' })).toBeInTheDocument()
    for (let i = 0; i < 4; i++) fireEvent.click(screen.getByRole('button', { name: 'Aumentar zoom' }))
    expect(screen.getByText('G01', { selector: 'text' })).toBeInTheDocument()
  })
})
