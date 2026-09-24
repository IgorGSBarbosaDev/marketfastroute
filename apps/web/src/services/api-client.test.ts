import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiRequestError, apiUrl, getJson } from './api-client'

describe('api-client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('builds URLs from the configured API base', () => {
    expect(apiUrl('/actuator/health')).toBe('/api/actuator/health')
  })

  it('fetches JSON and sends the default accept header', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ status: 'UP' }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(getJson<{ status: string }>('/actuator/health')).resolves.toEqual({ status: 'UP' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/actuator/health',
      expect.objectContaining({ headers: expect.any(Headers) }),
    )
    const requestOptions = fetchMock.mock.calls[0]?.[1] as RequestInit
    expect(new Headers(requestOptions.headers).get('Accept')).toBe('application/json')
  })

  it('keeps the structured API error code and gives the user a clear message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 422,
      json: async () => ({
        code: 'MAP_NOT_PUBLISHABLE',
        message: 'Map is not ready for publication',
        details: { issues: [{ code: 'NO_ACTIVE_SECTORS' }] },
      }),
    }))

    await expect(getJson('/v1/admin/maps/demo/validation')).rejects.toMatchObject({
      name: 'ApiRequestError',
      status: 422,
      code: 'MAP_NOT_PUBLISHABLE',
      message: 'O mapa ainda tem pendências antes de poder ser publicado.',
      details: { issues: [{ code: 'NO_ACTIVE_SECTORS' }] },
    } satisfies Partial<ApiRequestError>)
  })

  it('sets JSON content type when sending a request body', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 201, json: async () => ({ id: 'demo' }) })
    vi.stubGlobal('fetch', fetchMock)

    await getJson('/v1/admin/stores', { method: 'POST', body: JSON.stringify({ name: 'Demo' }) })

    const requestOptions = fetchMock.mock.calls[0]?.[1] as RequestInit
    expect(new Headers(requestOptions.headers).get('Content-Type')).toBe('application/json')
  })

  it('explains a network failure in plain language', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(getJson('/actuator/health')).rejects.toMatchObject({
      code: 'NETWORK_ERROR',
      message: 'Não foi possível conectar à API. Verifique se o ambiente está em execução.',
    })
  })

  it('preserves an abort rejection so canceled searches stay canceled', async () => {
    const abort = { name: 'AbortError' }
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(abort))

    await expect(getJson('/v1/stores', { signal: new AbortController().signal })).rejects.toBe(abort)
  })
})
