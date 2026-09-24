import { getJson } from './api-client'
import type {
  Aisle,
  AisleRequest,
  Category,
  CategoryRequest,
  CategoryUpdateRequest,
  MapEdge,
  MapEdgeRequest,
  MapEdgeUpdateRequest,
  MapNode,
  MapNodeRequest,
  MapNodeUpdateRequest,
  MapPublicationValidation,
  PointOfInterest,
  PointOfInterestRequest,
  PointOfInterestUpdateRequest,
  ProductLocationUpdateRequest,
  ProductAdmin,
  ProductLocationAdmin,
  ProductLocationRequest,
  ProductRequest,
  ProductUpdateRequest,
  Sector,
  SectorRequest,
  SectorUpdateRequest,
  ShelfBlock,
  ShelfBlockRequest,
  ShelfBlockUpdateRequest,
  Store,
  StoreMapAdmin,
  StoreMapRequest,
  StoreProduct,
  StoreProductRequest,
  StoreProductUpdateRequest,
  StoreRequest,
  StoreUpdateRequest,
  AisleUpdateRequest,
} from '@/types/api'

const adminPrefix = '/v1/admin'

function postJson<T>(path: string, body: unknown): Promise<T> {
  return getJson(path, { method: 'POST', body: JSON.stringify(body) })
}

function putJson<T>(path: string, body: unknown): Promise<T> {
  return getJson(path, { method: 'PUT', body: JSON.stringify(body) })
}

function mapResourcePath(mapId: string, resource: string): string {
  return `${adminPrefix}/maps/${mapId}/${resource}`
}

export const adminApi = {
  listStores: () => getJson<Store[]>(`${adminPrefix}/stores`),
  createStore: (body: StoreRequest) => postJson<Store>(`${adminPrefix}/stores`, body),
  updateStore: (storeId: string, body: StoreUpdateRequest) =>
    putJson<Store>(`${adminPrefix}/stores/${storeId}`, body),

  listCategories: () => getJson<Category[]>(`${adminPrefix}/categories`),
  createCategory: (body: CategoryRequest) => postJson<Category>(`${adminPrefix}/categories`, body),
  updateCategory: (categoryId: string, body: CategoryUpdateRequest) =>
    putJson<Category>(`${adminPrefix}/categories/${categoryId}`, body),

  listProducts: () => getJson<ProductAdmin[]>(`${adminPrefix}/products`),
  createProduct: (body: ProductRequest) => postJson<ProductAdmin>(`${adminPrefix}/products`, body),
  updateProduct: (productId: string, body: ProductUpdateRequest) =>
    putJson<ProductAdmin>(`${adminPrefix}/products/${productId}`, body),

  listStoreProducts: (storeId: string) =>
    getJson<StoreProduct[]>(`${adminPrefix}/stores/${storeId}/products`),
  addProductToStore: (storeId: string, body: StoreProductRequest) =>
    postJson<StoreProduct>(`${adminPrefix}/stores/${storeId}/products`, body),
  updateStoreProduct: (storeId: string, storeProductId: string, body: StoreProductUpdateRequest) =>
    putJson<StoreProduct>(`${adminPrefix}/stores/${storeId}/products/${storeProductId}`, body),

  listMaps: (storeId: string) => getJson<StoreMapAdmin[]>(`${adminPrefix}/stores/${storeId}/maps`),
  createMap: (storeId: string, body: StoreMapRequest) =>
    postJson<StoreMapAdmin>(`${adminPrefix}/stores/${storeId}/maps`, body),
  updateMap: (storeId: string, mapId: string, body: StoreMapRequest) =>
    putJson<StoreMapAdmin>(`${adminPrefix}/stores/${storeId}/maps/${mapId}`, body),
  validateMap: (mapId: string) => getJson<MapPublicationValidation>(mapResourcePath(mapId, 'validation')),

  listSectors: (mapId: string) => getJson<Sector[]>(mapResourcePath(mapId, 'sectors')),
  createSector: (mapId: string, body: SectorRequest) =>
    postJson<Sector>(mapResourcePath(mapId, 'sectors'), body),
  updateSector: (mapId: string, sectorId: string, body: SectorUpdateRequest) =>
    putJson<Sector>(mapResourcePath(mapId, `sectors/${sectorId}`), body),

  listAisles: (mapId: string) => getJson<Aisle[]>(mapResourcePath(mapId, 'aisles')),
  createAisle: (mapId: string, body: AisleRequest) => postJson<Aisle>(mapResourcePath(mapId, 'aisles'), body),
  updateAisle: (mapId: string, aisleId: string, body: AisleUpdateRequest) =>
    putJson<Aisle>(mapResourcePath(mapId, `aisles/${aisleId}`), body),

  listShelfBlocks: (mapId: string) => getJson<ShelfBlock[]>(mapResourcePath(mapId, 'shelf-blocks')),
  createShelfBlock: (mapId: string, body: ShelfBlockRequest) =>
    postJson<ShelfBlock>(mapResourcePath(mapId, 'shelf-blocks'), body),
  updateShelfBlock: (mapId: string, shelfBlockId: string, body: ShelfBlockUpdateRequest) =>
    putJson<ShelfBlock>(mapResourcePath(mapId, `shelf-blocks/${shelfBlockId}`), body),

  listPointsOfInterest: (mapId: string) =>
    getJson<PointOfInterest[]>(mapResourcePath(mapId, 'points-of-interest')),
  createPointOfInterest: (mapId: string, body: PointOfInterestRequest) =>
    postJson<PointOfInterest>(mapResourcePath(mapId, 'points-of-interest'), body),
  updatePointOfInterest: (mapId: string, pointId: string, body: PointOfInterestUpdateRequest) =>
    putJson<PointOfInterest>(mapResourcePath(mapId, `points-of-interest/${pointId}`), body),

  listNodes: (mapId: string) => getJson<MapNode[]>(mapResourcePath(mapId, 'nodes')),
  createNode: (mapId: string, body: MapNodeRequest) =>
    postJson<MapNode>(mapResourcePath(mapId, 'nodes'), body),
  updateNode: (mapId: string, nodeId: string, body: MapNodeUpdateRequest) =>
    putJson<MapNode>(mapResourcePath(mapId, `nodes/${nodeId}`), body),

  listEdges: (mapId: string) => getJson<MapEdge[]>(mapResourcePath(mapId, 'edges')),
  createEdge: (mapId: string, body: MapEdgeRequest) =>
    postJson<MapEdge>(mapResourcePath(mapId, 'edges'), body),
  updateEdge: (mapId: string, edgeId: string, body: MapEdgeUpdateRequest) =>
    putJson<MapEdge>(mapResourcePath(mapId, `edges/${edgeId}`), body),

  listProductLocations: (storeId: string) =>
    getJson<ProductLocationAdmin[]>(`${adminPrefix}/stores/${storeId}/product-locations`),
  createProductLocation: (storeId: string, body: ProductLocationRequest) =>
    postJson<ProductLocationAdmin>(`${adminPrefix}/stores/${storeId}/product-locations`, body),
  updateProductLocation: (storeId: string, locationId: string, body: ProductLocationUpdateRequest) =>
    putJson<ProductLocationAdmin>(`${adminPrefix}/stores/${storeId}/product-locations/${locationId}`, body),
}
