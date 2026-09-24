import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { CustomerView } from './CustomerView'

const storeId = '00000000-0000-0000-0000-000000000001'
const productId = '00000000-0000-0000-0000-000000000002'
const mapId = '00000000-0000-0000-0000-000000000003'
const product = { id: productId, name: 'Banana nanica', sku: 'BAN-01', ean: '789000000001', brand: null, category: 'Hortifruti' }
const route = {
  storeId,
  mapId,
  orderedStops: [{ productId, productName: 'Banana nanica', productLocationId: 'location-1', navigationNodeId: 'node-product', order: 1 }],
  path: [{ nodeId: 'node-entry', x: 4, y: 30 }, { nodeId: 'node-product', x: 20, y: 18 }, { nodeId: 'node-checkout', x: 30, y: 29 }],
  distanceMeters: 34,
}

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

describe('CustomerView shopping flow', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('searches the selected store, builds a list, and renders the API route and ordered stops', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = new URL(String(input), 'http://localhost')
      if (url.pathname === '/api/v1/stores') return jsonResponse([{ id: storeId, name: 'Mercado Centro', code: 'CENTRO', address: 'Rua das Flores, 10', city: 'Curitiba', state: 'PR', active: true }])
      if (url.pathname.endsWith(`/stores/${storeId}/map`)) return jsonResponse({
        id: mapId, storeId, version: 3, name: 'Planta da loja', width: 40, height: 32, scaleMetersPerUnit: 1,
        sectors: [{ id: 'sector-1', name: 'Hortifruti', code: 'HORT', x: 2, y: 2, width: 12, height: 8, rotation: 0 }],
        aisles: [{ id: 'aisle-1', sectorId: 'sector-1', name: 'Corredor A', code: 'A', x: 6, y: 11, width: 3, height: 14, rotation: 0 }],
        shelfBlocks: [{ id: 'shelf-1', sectorId: 'sector-1', aisleId: 'aisle-1', code: 'P1', name: 'Prateleira 1', x: 7, y: 13, width: 1, height: 9, rotation: 0 }],
        pointsOfInterest: [
          { id: 'poi-entry', navigationNodeId: 'node-entry', type: 'ENTRANCE', name: 'Entrada principal', x: 4, y: 30 },
          { id: 'poi-checkout', navigationNodeId: 'node-checkout', type: 'CHECKOUT', name: 'Caixas', x: 30, y: 29 },
        ],
        nodes: [{ id: 'node-entry', type: 'ENTRANCE', x: 4, y: 30, label: 'Entrada' }, { id: 'node-product', type: 'PRODUCT_ACCESS', x: 20, y: 18, label: 'Acesso' }, { id: 'node-checkout', type: 'CHECKOUT', x: 30, y: 29, label: 'Caixas' }],
        edges: [],
      })
      if (url.pathname.endsWith(`/stores/${storeId}/products/${productId}/locations`)) return jsonResponse([{ id: 'location-1', productId, storeId, mapId, sector: 'Hortifruti', aisle: 'Corredor A', shelfBlock: 'P1', side: 'LEFT', module: null, shelfLevel: null, x: 20, y: 18, navigationNodeId: 'node-product', primaryLocation: true }])
      if (url.pathname === `/api/v1/stores/${storeId}/products`) return jsonResponse(url.searchParams.get('search')?.toLowerCase().includes('banana') ? [product] : [])
      if (url.pathname === '/api/v1/routes' && init?.method === 'POST') return jsonResponse(route)
      throw new Error(`Unexpected request: ${url.pathname}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<CustomerView activePage="shop" navigate={vi.fn()} />)
    const search = await screen.findByLabelText('Buscar produtos por nome, categoria, SKU ou EAN')
    fireEvent.change(search, { target: { value: 'banana' } })

    const addButton = await screen.findByRole('button', { name: 'Adicionar Banana nanica' })
    fireEvent.click(addButton)
    await screen.findByText('Hortifruti', { selector: '.line-main small' })
    fireEvent.click(screen.getByRole('button', { name: 'Aumentar Banana nanica' }))
    expect(screen.getByLabelText('Quantidade de Banana nanica')).toHaveTextContent('2')

    const calculate = await screen.findByRole('button', { name: 'Calcular meu percurso' })
    await waitFor(() => expect(calculate).toBeEnabled())
    fireEvent.click(calculate)

    expect(await screen.findByText('Percurso pronto')).toBeInTheDocument()
    expect(screen.getByText(/1 parada · 34 m/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Expandir detalhes do percurso' }))
    expect(screen.getByText('Entrada principal')).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Paradas para encontrar produtos' })).toHaveTextContent('Banana nanica')
    expect(screen.getByRole('list', { name: 'Paradas para encontrar produtos' })).toHaveTextContent('Corredor A')
    fireEvent.click(screen.getByRole('button', { name: 'Planta 2D' }))
    expect(screen.getByRole('button', { name: 'Enquadrar percurso' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Lista' }))
    expect(screen.getByRole('button', { name: 'Lista' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.queryByRole('tab')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Aumentar Banana nanica' }))
    expect(screen.getByLabelText('Quantidade de Banana nanica')).toHaveTextContent('3')
    expect(screen.getByText('Percurso pronto')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Remover Banana nanica' }))
    expect(screen.queryByText('Percurso pronto')).not.toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/routes', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ storeId, productIds: [productId] }),
    }))
  })

  it('retries a failed search with the same normalized term', async () => {
    const searchTerms: string[] = []
    let bananaAttempts = 0
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(String(input), 'http://localhost')
      if (url.pathname === '/api/v1/stores') return jsonResponse([{ id: storeId, name: 'Mercado Centro', code: 'CENTRO', address: 'Rua das Flores, 10', city: 'Curitiba', state: 'PR', active: true }])
      if (url.pathname.endsWith(`/stores/${storeId}/map`)) return jsonResponse({ id: mapId, storeId, version: 1, name: 'Planta', width: 40, height: 32, scaleMetersPerUnit: 1, sectors: [], aisles: [], shelfBlocks: [], pointsOfInterest: [], nodes: [], edges: [] })
      if (url.pathname === `/api/v1/stores/${storeId}/products`) {
        const term = url.searchParams.get('search') ?? ''
        if (term) searchTerms.push(term)
        if (term === 'banana' && bananaAttempts++ === 0) return new Response(JSON.stringify({ code: 'INTERNAL_SERVER_ERROR' }), { status: 500 })
        return jsonResponse(term === 'banana' ? [product] : [])
      }
      throw new Error(`Unexpected request: ${url.pathname}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<CustomerView activePage="shop" navigate={vi.fn()} />)
    fireEvent.change(await screen.findByLabelText('Buscar produtos por nome, categoria, SKU ou EAN'), { target: { value: '  banana  ' } })
    await screen.findByText('A API encontrou um erro. Tente novamente em instantes.')
    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByRole('button', { name: 'Adicionar Banana nanica' })).toBeInTheDocument()
    expect(searchTerms).toEqual(['banana', 'banana'])
  })

  it('recovers when the initial store list request fails', async () => {
    let storeAttempts = 0
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(String(input), 'http://localhost')
      if (url.pathname === '/api/v1/stores') {
        storeAttempts += 1
        if (storeAttempts === 1) return new Response(JSON.stringify({ code: 'INTERNAL_SERVER_ERROR' }), { status: 500 })
        return jsonResponse([{ id: storeId, name: 'Mercado Centro', code: 'CENTRO', address: 'Rua das Flores, 10', city: 'Curitiba', state: 'PR', active: true }])
      }
      if (url.pathname.endsWith(`/stores/${storeId}/map`)) return jsonResponse({ id: mapId, storeId, version: 1, name: 'Planta', width: 40, height: 32, scaleMetersPerUnit: 1, sectors: [], aisles: [], shelfBlocks: [], pointsOfInterest: [], nodes: [], edges: [] })
      if (url.pathname === `/api/v1/stores/${storeId}/products`) return jsonResponse([])
      throw new Error(`Unexpected request: ${url.pathname}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<CustomerView activePage="shop" navigate={vi.fn()} />)
    await screen.findByText('A API encontrou um erro. Tente novamente em instantes.')
    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByRole('option', { name: 'Mercado Centro · Curitiba' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('Planta')).toBeInTheDocument())
    expect(storeAttempts).toBe(2)
  })
})
