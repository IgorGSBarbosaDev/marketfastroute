export interface Store {
  id: string
  name: string
  code: string
  address: string
  city: string
  state: string
  active: boolean
}

export interface Product {
  id: string
  name: string
  sku: string
  ean: string | null
  brand: string | null
  category: string
}

export interface Category {
  id: string
  parentId: string | null
  name: string
  code: string
  active: boolean
}

export interface ProductAdmin {
  id: string
  categoryId: string
  sku: string
  ean: string | null
  name: string
  brand: string | null
  description: string | null
  active: boolean
}

export interface StoreProduct {
  id: string
  storeId: string
  productId: string
  active: boolean
}

export type MapStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
export type MapNodeType = 'PATH' | 'INTERSECTION' | 'ENTRANCE' | 'EXIT' | 'PRODUCT_ACCESS' | 'CHECKOUT'
export type PointOfInterestType = 'ENTRANCE' | 'EXIT' | 'CHECKOUT' | 'CART' | 'RESTROOM'
  | 'CUSTOMER_SERVICE' | 'PARKING' | 'ELEVATOR' | 'STAIRS' | 'OTHER'
export type ProductLocationSide = 'LEFT' | 'RIGHT' | 'CENTER'

export interface MapGeometry {
  id: string
  x: number
  y: number
  width: number
  height: number
  rotation: number
  active?: boolean
}

export interface Sector extends MapGeometry {
  mapId?: string
  name: string
  code: string
}

export interface Aisle extends MapGeometry {
  mapId?: string
  sectorId: string | null
  code: string
  name: string
}

export interface ShelfBlock extends MapGeometry {
  mapId?: string
  sectorId: string | null
  aisleId: string | null
  code: string
  name: string | null
}

export interface PointOfInterest {
  id: string
  mapId?: string
  navigationNodeId: string | null
  type: PointOfInterestType
  name: string
  x: number
  y: number
  active?: boolean
}

export interface MapNode {
  id: string
  mapId?: string
  type: MapNodeType
  x: number
  y: number
  label: string | null
  active?: boolean
}

export interface MapEdge {
  id: string
  mapId?: string
  fromNodeId: string
  toNodeId: string
  distanceMeters: number
  bidirectional: boolean
  active?: boolean
}

export interface StoreMap {
  id: string
  storeId: string
  version: number
  name: string
  width: number
  height: number
  scaleMetersPerUnit: number
  status?: MapStatus
  sectors: Sector[]
  aisles: Aisle[]
  shelfBlocks: ShelfBlock[]
  pointsOfInterest: PointOfInterest[]
  nodes: MapNode[]
  edges: MapEdge[]
}

export interface ProductLocation {
  id: string
  productId: string
  storeId: string
  mapId: string
  sector: string | null
  aisle: string | null
  shelfBlock: string | null
  side: ProductLocationSide | null
  module: string | null
  shelfLevel: number | null
  x: number | null
  y: number | null
  navigationNodeId: string
  primaryLocation: boolean
}

export interface ProductLocationAdmin extends Omit<ProductLocation, 'productId' | 'sector' | 'aisle' | 'shelfBlock'> {
  storeProductId: string
  sectorId: string | null
  aisleId: string | null
  shelfBlockId: string | null
  active: boolean
}

export interface RouteStop {
  productId: string
  productName: string
  productLocationId: string
  navigationNodeId: string
  order: number
}

export interface RoutePathNode {
  nodeId: string
  x: number
  y: number
}

export interface CalculatedRoute {
  storeId: string
  mapId: string
  orderedStops: RouteStop[]
  path: RoutePathNode[]
  distanceMeters: number
}

export interface MapPublicationIssue {
  code: string
  message: string
  elementType: string
  elementId: string | null
}

export interface MapPublicationValidation {
  mapId: string
  publishable: boolean
  issues: MapPublicationIssue[]
}

interface RectangleRequest {
  x: number
  y: number
  width: number
  height: number
  rotation: number
  active?: boolean
}

export interface SectorRequest extends RectangleRequest {
  name: string
  code: string
}

export interface SectorUpdateRequest extends SectorRequest {
  active: boolean
}

export interface AisleRequest extends RectangleRequest {
  sectorId: string | null
  code: string
  name: string
}

export interface AisleUpdateRequest extends AisleRequest {
  active: boolean
}

export interface ShelfBlockRequest extends RectangleRequest {
  sectorId: string | null
  aisleId: string | null
  code: string
  name: string | null
}

export interface ShelfBlockUpdateRequest extends ShelfBlockRequest {
  active: boolean
}

export interface PointOfInterestRequest {
  navigationNodeId: string | null
  type: PointOfInterestType
  name: string
  x: number
  y: number
  active?: boolean
}

export interface PointOfInterestUpdateRequest extends PointOfInterestRequest {
  active: boolean
}

export interface MapNodeRequest {
  type: MapNodeType
  x: number
  y: number
  label: string | null
  active?: boolean
}

export interface MapNodeUpdateRequest extends MapNodeRequest {
  active: boolean
}

export interface MapEdgeRequest {
  fromNodeId: string
  toNodeId: string
  distanceMeters: number
  bidirectional: boolean
  active?: boolean
}

export interface MapEdgeUpdateRequest extends MapEdgeRequest {
  active: boolean
}

export interface ProductLocationRequest {
  storeProductId: string
  mapId: string
  sectorId: string | null
  aisleId: string | null
  shelfBlockId: string | null
  side: ProductLocationSide | null
  module: string | null
  shelfLevel: number | null
  x: number | null
  y: number | null
  navigationNodeId: string
  primaryLocation: boolean
  active?: boolean
}

export interface ProductLocationUpdateRequest extends ProductLocationRequest {
  active: boolean
}

export interface StoreRequest {
  name: string
  code: string
  address: string
  city: string
  state: string
}

export interface StoreUpdateRequest extends StoreRequest {
  active: boolean
}

export interface CategoryRequest {
  name: string
  code: string
  parentId: string | null
}

export interface CategoryUpdateRequest extends CategoryRequest {
  active: boolean
}

export interface ProductRequest {
  categoryId: string
  sku: string
  ean: string | null
  name: string
  brand: string | null
  description: string | null
}

export interface ProductUpdateRequest extends ProductRequest {
  active: boolean
}

export interface StoreMapRequest {
  version: number
  name: string
  width: number
  height: number
  scaleMetersPerUnit: number
  status: MapStatus
}

export interface StoreProductRequest {
  productId: string
  active?: boolean
}

export interface StoreProductUpdateRequest {
  active: boolean
}

export interface StoreMapAdmin extends Omit<StoreMap, 'sectors' | 'aisles' | 'shelfBlocks' | 'pointsOfInterest' | 'nodes' | 'edges'> {
  status: MapStatus
}

export interface ApiErrorResponse {
  code: string
  message: string
  details: Record<string, unknown>
}
