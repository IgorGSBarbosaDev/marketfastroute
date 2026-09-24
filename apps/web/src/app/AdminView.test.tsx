import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AdminView } from './AdminView'

const storeId = '00000000-0000-0000-0000-000000000101'
const mapId = '00000000-0000-0000-0000-000000000102'

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('AdminView', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('creates a catalog category through the API and keeps create fields out of the update contract', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = new URL(String(input), 'http://localhost')
      if (url.pathname === '/api/v1/admin/stores') return jsonResponse([])
      if (url.pathname === '/api/v1/admin/categories' && init?.method === 'POST') return jsonResponse({ id: 'category-1', parentId: null, name: 'Alimentos', code: 'ALIM', active: true }, 201)
      if (url.pathname === '/api/v1/admin/categories') return jsonResponse([])
      if (url.pathname === '/api/v1/admin/products') return jsonResponse([])
      throw new Error(`Unexpected request: ${url.pathname}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<AdminView activePage="admin" navigate={vi.fn()} />)
    fireEvent.change(await screen.findByLabelText('Tipo de cadastro'), { target: { value: 'category' } })
    fireEvent.change(screen.getByLabelText(/Nome da categoria/), { target: { value: 'Alimentos' } })
    fireEvent.change(screen.getByLabelText(/Código/), { target: { value: 'ALIM' } })
    fireEvent.click(screen.getByRole('button', { name: 'Criar categoria' }))

    await screen.findByText('Cadastro criado.')
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/categories', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ name: 'Alimentos', code: 'ALIM', parentId: null }),
    })))
  })

  it('shows backend validation issues and only offers activation after a publishable report', async () => {
    const draft = { id: mapId, storeId, version: 2, name: 'Rascunho v2', width: 240, height: 160, scaleMetersPerUnit: 1, status: 'DRAFT' }
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(String(input), 'http://localhost')
      if (url.pathname === '/api/v1/admin/stores') return jsonResponse([{ id: storeId, name: 'Mercado Centro', code: 'CENTRO', address: 'Rua Um', city: 'Curitiba', state: 'PR', active: true }])
      if (url.pathname === '/api/v1/admin/categories' || url.pathname === '/api/v1/admin/products') return jsonResponse([])
      if (url.pathname === `/api/v1/admin/stores/${storeId}/products` || url.pathname === `/api/v1/admin/stores/${storeId}/product-locations`) return jsonResponse([])
      if (url.pathname === `/api/v1/admin/stores/${storeId}/maps`) return jsonResponse([draft])
      if (url.pathname === `/api/v1/admin/maps/${mapId}/validation`) return jsonResponse({ mapId, publishable: false, issues: [{ code: 'ROUTE_ENTRY_NOT_FOUND', message: 'Configure uma entrada navegável.', elementType: 'MAP', elementId: mapId }] })
      if (url.pathname.includes(`/api/v1/admin/maps/${mapId}/`)) return jsonResponse([])
      throw new Error(`Unexpected request: ${url.pathname}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<AdminView activePage="admin" navigate={vi.fn()} />)
    fireEvent.click(within(screen.getByRole('navigation', { name: 'Áreas administrativas' })).getByRole('button', { name: 'Versões do mapa' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Validar versão' }))

    expect(await screen.findByText('Configure uma entrada navegável.')).toBeInTheDocument()
    expect(screen.getByText('1 pendência para corrigir')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ativar versão validada' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Abrir dados da versão' }))
    expect(await screen.findByRole('button', { name: 'Salvar alterações' })).toBeInTheDocument()
  })
})
