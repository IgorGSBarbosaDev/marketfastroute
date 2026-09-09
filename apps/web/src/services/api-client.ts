const defaultApiUrl = '/api'

export const apiBaseUrl = (import.meta.env.VITE_API_URL || defaultApiUrl).replace(/\/$/, '')

export function apiUrl(path: string): string {
  return `${apiBaseUrl}/${path.replace(/^\/+/, '')}`
}

export async function getJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  const response = await fetch(apiUrl(path), {
    ...init,
    headers,
  })

  if (!response.ok) {
    throw new Error(`API request failed with status ${response.status}`)
  }

  return response.json() as Promise<T>
}
