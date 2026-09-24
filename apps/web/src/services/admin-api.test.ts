import { afterEach, describe, expect, it, vi } from 'vitest'

import { adminApi } from './admin-api'

describe('admin-api', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('uses the documented publication validation endpoint', async () => {
    const validation = { mapId: 'map-1', publishable: false, issues: [] }
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => validation })
    vi.stubGlobal('fetch', fetchMock)

    await expect(adminApi.validateMap('map-1')).resolves.toEqual(validation)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/maps/map-1/validation', expect.any(Object))
  })

  it('marks a catalog record inactive with PUT and never exposes a delete operation', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ active: false }) })
    vi.stubGlobal('fetch', fetchMock)

    await adminApi.updateProduct('product-1', {
      categoryId: 'category-1',
      sku: 'SKU-1',
      ean: null,
      name: 'Produto',
      brand: null,
      description: null,
      active: false,
    })

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/products/product-1', expect.objectContaining({
      method: 'PUT',
      body: expect.stringContaining('"active":false'),
    }))
    expect('deleteProduct' in adminApi).toBe(false)
  })
})
