import { afterEach, describe, expect, it, vi } from 'vitest'

import { calculateStoreRoute, searchStoreProducts } from './customer-api'

describe('customer-api', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('encodes product search and keeps the API version in the configured base path', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => [] })
    vi.stubGlobal('fetch', fetchMock)

    await searchStoreProducts('store-1', 'leite integral')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/stores/store-1/products?search=leite+integral',
      expect.any(Object),
    )
  })

  it('sends product IDs to the route service without changing their order', async () => {
    const route = { storeId: 'store-1', mapId: 'map-1', orderedStops: [], path: [], distanceMeters: 10 }
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => route })
    vi.stubGlobal('fetch', fetchMock)

    await expect(calculateStoreRoute('store-1', ['product-b', 'product-a'])).resolves.toEqual(route)

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/routes', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ storeId: 'store-1', productIds: ['product-b', 'product-a'] }),
    }))
  })

  it('passes an abort signal through to product search', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => [] })
    const controller = new AbortController()
    vi.stubGlobal('fetch', fetchMock)

    await searchStoreProducts('store-1', 'leite', controller.signal)

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/stores/store-1/products?search=leite',
      expect.objectContaining({ signal: controller.signal }),
    )
  })
})
