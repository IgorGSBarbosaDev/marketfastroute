const defaultApiUrl = '/api'

export interface ApiErrorPayload {
  code?: string
  message?: string
  details?: Record<string, unknown>
}

export class ApiRequestError extends Error {
  readonly status: number | null
  readonly code: string
  readonly details: Record<string, unknown>

  constructor(message: string, options: {
    status?: number | null
    code?: string
    details?: Record<string, unknown>
  } = {}) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = options.status ?? null
    this.code = options.code ?? 'API_REQUEST_FAILED'
    this.details = options.details ?? {}
  }
}

export const apiBaseUrl = (import.meta.env.VITE_API_URL || defaultApiUrl).replace(/\/$/, '')

export function apiUrl(path: string): string {
  return `${apiBaseUrl}/${path.replace(/^\/+/, '')}`
}

export async function getJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  let response: Response
  try {
    response = await fetch(apiUrl(path), { ...init, headers })
  } catch (error) {
    if (isAbortError(error)) {
      throw error
    }
    throw new ApiRequestError('Não foi possível conectar à API. Verifique se o ambiente está em execução.', {
      code: 'NETWORK_ERROR',
    })
  }

  if (!response.ok) {
    const payload = await readErrorPayload(response)
    const code = payload.code ?? `HTTP_${response.status}`
    throw new ApiRequestError(messageForError(code, response.status, payload.message), {
      status: response.status,
      code,
      details: payload.details,
    })
  }

  if (response.status === 204) {
    return undefined as T
  }

  try {
    return await response.json() as T
  } catch {
    throw new ApiRequestError('A API retornou uma resposta inválida.', {
      status: response.status,
      code: 'INVALID_API_RESPONSE',
    })
  }
}

function isAbortError(error: unknown): boolean {
  return typeof error === 'object'
    && error !== null
    && 'name' in error
    && error.name === 'AbortError'
}

async function readErrorPayload(response: Response): Promise<ApiErrorPayload> {
  try {
    const payload: unknown = await response.json()
    if (typeof payload !== 'object' || payload === null) {
      return {}
    }
    return payload as ApiErrorPayload
  } catch {
    return {}
  }
}

function messageForError(code: string, status: number, serverMessage?: string): string {
  const messages: Record<string, string> = {
    ADMIN_CONFLICT: 'Este cadastro já existe ou conflita com outro registro.',
    ADMIN_RESOURCE_NOT_FOUND: 'O registro solicitado não foi encontrado.',
    ADMIN_VALIDATION_ERROR: 'Confira os dados informados e tente novamente.',
    DATA_INTEGRITY_CONFLICT: 'Esta alteração conflita com dados existentes.',
    INTERNAL_SERVER_ERROR: 'A API encontrou um erro. Tente novamente em instantes.',
    INVALID_PARAMETER: 'Um dos parâmetros informados é inválido.',
    INVALID_REQUEST: 'Confira os dados informados e tente novamente.',
    MAP_NOT_PUBLISHABLE: 'O mapa ainda tem pendências antes de poder ser publicado.',
    MAP_NOT_FOUND: 'O mapa solicitado não foi encontrado.',
    PRODUCT_LOCATION_NOT_FOUND: 'Este produto ainda não tem uma localização navegável nesta loja.',
    PRODUCT_NOT_FOUND: 'Este produto não está disponível nesta loja.',
    ROUTE_CHECKOUT_NOT_FOUND: 'O mapa não tem uma área de caixas navegável configurada.',
    ROUTE_CONFIGURATION_INVALID: 'A entrada ou a área de caixas está configurada mais de uma vez.',
    ROUTE_ENTRY_NOT_FOUND: 'O mapa não tem uma entrada navegável configurada.',
    ROUTE_PATH_NOT_FOUND: 'Não há caminho entre todos os produtos selecionados e os caixas.',
    STORE_MAP_NOT_FOUND: 'Esta loja ainda não tem um mapa ativo.',
    STORE_NOT_FOUND: 'A loja selecionada não está disponível.',
    VALIDATION_ERROR: 'Confira os campos destacados e tente novamente.',
  }
  return messages[code] ?? (status >= 500
    ? 'A API encontrou um erro. Tente novamente em instantes.'
    : serverMessage || 'Não foi possível concluir a solicitação.')
}
