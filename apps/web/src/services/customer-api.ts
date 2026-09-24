import { getJson } from './api-client'
import type { CalculatedRoute, Product, ProductLocation, Store, StoreMap } from '@/types/api'

export function listStores(signal?: AbortSignal): Promise<Store[]> {
  return getJson('/v1/stores', signal ? { signal } : {})
}

export function searchStoreProducts(storeId: string, search: string, signal?: AbortSignal): Promise<Product[]> {
  const query = search.trim() ? `?${new URLSearchParams({ search: search.trim() })}` : ''
  return getJson(`/v1/stores/${storeId}/products${query}`, signal ? { signal } : {})
}

export function getActiveStoreMap(storeId: string, signal?: AbortSignal): Promise<StoreMap> {
  return getJson(`/v1/stores/${storeId}/map`, signal ? { signal } : {})
}

export function getProductLocations(storeId: string, productId: string, signal?: AbortSignal): Promise<ProductLocation[]> {
  return getJson(`/v1/stores/${storeId}/products/${productId}/locations`, signal ? { signal } : {})
}

export function calculateStoreRoute(storeId: string, productIds: string[], signal?: AbortSignal): Promise<CalculatedRoute> {
  return getJson('/v1/routes', {
    method: 'POST',
    body: JSON.stringify({ storeId, productIds }),
    signal,
  })
}
