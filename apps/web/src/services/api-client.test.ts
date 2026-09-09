import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiUrl, getJson } from './api-client'

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

  it('raises a stable error for non-success responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 503 }))

    await expect(getJson('/actuator/health')).rejects.toThrow('API request failed with status 503')
  })
})
