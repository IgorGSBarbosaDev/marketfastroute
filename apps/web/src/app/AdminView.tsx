import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle, ArrowRight, Check, CircleCheck, ClipboardCheck, Layers3, LoaderCircle, MapPinned, Plus, Save, Store as StoreIcon, TriangleAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { StoreMapSvg } from '@/components/StoreMapSvg'
import { adminApi } from '@/services/admin-api'
import { ApiRequestError } from '@/services/api-client'
import type {
  Aisle, Category, MapEdge, MapNode, MapPublicationIssue, MapPublicationValidation, PointOfInterest, ProductAdmin,
  ProductLocationAdmin, Sector, ShelfBlock, Store, StoreMapAdmin, StoreMap, StoreProduct,
} from '@/types/api'

type AdminPage = 'shop' | 'admin' | 'demo'
type Resource = 'store' | 'category' | 'product' | 'availability' | 'map' | 'sector' | 'aisle' | 'shelfBlock' | 'poi' | 'node' | 'edge' | 'location'
type AdminTab = 'catalog' | 'availability' | 'maps' | 'editor' | 'locations'
type FieldKind = 'text' | 'number' | 'boolean' | 'select' | 'textarea'

interface FieldSpec { key: string; label: string; kind?: FieldKind; required?: boolean; options?: string[]; source?: 'categories' | 'products' | 'sectors' | 'aisles' | 'shelves' | 'nodes' | 'maps' | 'storeProducts' }
type Draft = Record<string, string | number | boolean>

interface AdminViewProps { activePage: AdminPage; navigate: (page: AdminPage) => void }

const tabLabels: Record<AdminTab, string> = {
  catalog: 'Cadastros', availability: 'Disponibilidade', maps: 'Versões do mapa', editor: 'Editor espacial', locations: 'Localizações',
}
const resourceLabels: Record<Resource, string> = {
  store: 'Lojas', category: 'Categorias', product: 'Produtos', availability: 'Disponibilidade', map: 'Versões do mapa',
  sector: 'Setores', aisle: 'Corredores', shelfBlock: 'Blocos de prateleira', poi: 'Pontos de interesse', node: 'Nós de navegação', edge: 'Conexões do grafo', location: 'Localizações de produtos',
}

const fieldsByResource: Partial<Record<Resource, FieldSpec[]>> = {
  store: [
    { key: 'name', label: 'Nome da loja', required: true }, { key: 'code', label: 'Código', required: true },
    { key: 'address', label: 'Endereço', required: true }, { key: 'city', label: 'Cidade', required: true },
    { key: 'state', label: 'Estado', required: true }, { key: 'active', label: 'Loja ativa', kind: 'boolean' },
  ],
  category: [
    { key: 'name', label: 'Nome da categoria', required: true }, { key: 'code', label: 'Código', required: true },
    { key: 'parentId', label: 'Categoria superior', kind: 'select', source: 'categories' }, { key: 'active', label: 'Categoria ativa', kind: 'boolean' },
  ],
  product: [
    { key: 'name', label: 'Nome do produto', required: true }, { key: 'categoryId', label: 'Categoria', kind: 'select', required: true, source: 'categories' },
    { key: 'sku', label: 'SKU', required: true }, { key: 'ean', label: 'EAN' }, { key: 'brand', label: 'Marca' },
    { key: 'description', label: 'Descrição', kind: 'textarea' }, { key: 'active', label: 'Produto ativo', kind: 'boolean' },
  ],
  availability: [
    { key: 'productId', label: 'Produto', kind: 'select', required: true, source: 'products' }, { key: 'active', label: 'Disponível nesta loja', kind: 'boolean' },
  ],
  map: [
    { key: 'version', label: 'Versão', kind: 'number', required: true }, { key: 'name', label: 'Nome da planta', required: true },
    { key: 'width', label: 'Largura em unidades', kind: 'number', required: true }, { key: 'height', label: 'Altura em unidades', kind: 'number', required: true },
    { key: 'scaleMetersPerUnit', label: 'Metros por unidade', kind: 'number', required: true },
  ],
  sector: [
    { key: 'name', label: 'Nome do setor', required: true }, { key: 'code', label: 'Código', required: true },
    { key: 'x', label: 'X', kind: 'number', required: true }, { key: 'y', label: 'Y', kind: 'number', required: true },
    { key: 'width', label: 'Largura', kind: 'number', required: true }, { key: 'height', label: 'Altura', kind: 'number', required: true },
    { key: 'rotation', label: 'Rotação em graus', kind: 'number', required: true }, { key: 'active', label: 'Ativo', kind: 'boolean' },
  ],
  aisle: [
    { key: 'sectorId', label: 'Setor', kind: 'select', source: 'sectors' }, { key: 'code', label: 'Código', required: true }, { key: 'name', label: 'Nome', required: true },
    { key: 'x', label: 'X', kind: 'number', required: true }, { key: 'y', label: 'Y', kind: 'number', required: true },
    { key: 'width', label: 'Largura', kind: 'number', required: true }, { key: 'height', label: 'Altura', kind: 'number', required: true },
    { key: 'rotation', label: 'Rotação em graus', kind: 'number', required: true }, { key: 'active', label: 'Ativo', kind: 'boolean' },
  ],
  shelfBlock: [
    { key: 'sectorId', label: 'Setor', kind: 'select', source: 'sectors' }, { key: 'aisleId', label: 'Corredor', kind: 'select', source: 'aisles' },
    { key: 'code', label: 'Código', required: true }, { key: 'name', label: 'Nome' }, { key: 'x', label: 'X', kind: 'number', required: true },
    { key: 'y', label: 'Y', kind: 'number', required: true }, { key: 'width', label: 'Largura', kind: 'number', required: true },
    { key: 'height', label: 'Altura', kind: 'number', required: true }, { key: 'rotation', label: 'Rotação em graus', kind: 'number', required: true },
    { key: 'active', label: 'Ativo', kind: 'boolean' },
  ],
  poi: [
    { key: 'type', label: 'Tipo', kind: 'select', required: true, options: ['ENTRANCE', 'EXIT', 'CHECKOUT', 'CART', 'RESTROOM', 'CUSTOMER_SERVICE', 'PARKING', 'ELEVATOR', 'STAIRS', 'OTHER'] },
    { key: 'name', label: 'Nome', required: true }, { key: 'navigationNodeId', label: 'Nó de navegação', kind: 'select', source: 'nodes' },
    { key: 'x', label: 'X', kind: 'number', required: true }, { key: 'y', label: 'Y', kind: 'number', required: true }, { key: 'active', label: 'Ativo', kind: 'boolean' },
  ],
  node: [
    { key: 'type', label: 'Tipo do nó', kind: 'select', required: true, options: ['PATH', 'INTERSECTION', 'ENTRANCE', 'EXIT', 'PRODUCT_ACCESS', 'CHECKOUT'] },
    { key: 'label', label: 'Rótulo' }, { key: 'x', label: 'X', kind: 'number', required: true }, { key: 'y', label: 'Y', kind: 'number', required: true }, { key: 'active', label: 'Ativo', kind: 'boolean' },
  ],
  edge: [
    { key: 'fromNodeId', label: 'Nó de origem', kind: 'select', required: true, source: 'nodes' }, { key: 'toNodeId', label: 'Nó de destino', kind: 'select', required: true, source: 'nodes' },
    { key: 'distanceMeters', label: 'Distância em metros', kind: 'number', required: true }, { key: 'bidirectional', label: 'Mão dupla', kind: 'boolean' }, { key: 'active', label: 'Ativa', kind: 'boolean' },
  ],
  location: [
    { key: 'storeProductId', label: 'Produto disponível na loja', kind: 'select', source: 'storeProducts', required: true },
    { key: 'sectorId', label: 'Setor', kind: 'select', source: 'sectors' }, { key: 'aisleId', label: 'Corredor', kind: 'select', source: 'aisles' },
    { key: 'shelfBlockId', label: 'Bloco de prateleira', kind: 'select', source: 'shelves' },
    { key: 'side', label: 'Lado', kind: 'select', options: ['LEFT', 'RIGHT', 'CENTER'] }, { key: 'module', label: 'Módulo' },
    { key: 'shelfLevel', label: 'Nível da prateleira', kind: 'number' }, { key: 'x', label: 'X opcional', kind: 'number' }, { key: 'y', label: 'Y opcional', kind: 'number' },
    { key: 'navigationNodeId', label: 'Nó para navegação', kind: 'select', source: 'nodes', required: true },
    { key: 'primaryLocation', label: 'Localização principal', kind: 'boolean' }, { key: 'active', label: 'Ativa', kind: 'boolean' },
  ],
}

const defaults: Partial<Record<Resource, Draft>> = {
  store: { name: '', code: '', address: '', city: '', state: '', active: true },
  category: { name: '', code: '', parentId: '', active: true },
  product: { name: '', categoryId: '', sku: '', ean: '', brand: '', description: '', active: true },
  availability: { productId: '', active: true },
  map: { version: 1, name: '', width: 240, height: 160, scaleMetersPerUnit: 1 },
  sector: { name: '', code: '', x: 0, y: 0, width: 40, height: 30, rotation: 0, active: true },
  aisle: { sectorId: '', code: '', name: '', x: 0, y: 0, width: 4, height: 40, rotation: 0, active: true },
  shelfBlock: { sectorId: '', aisleId: '', code: '', name: '', x: 0, y: 0, width: 3, height: 30, rotation: 0, active: true },
  poi: { type: 'OTHER', name: '', navigationNodeId: '', x: 0, y: 0, active: true },
  node: { type: 'PATH', label: '', x: 0, y: 0, active: true },
  edge: { fromNodeId: '', toNodeId: '', distanceMeters: 1, bidirectional: true, active: true },
  location: { storeProductId: '', mapId: '', sectorId: '', aisleId: '', shelfBlockId: '', side: '', module: '', shelfLevel: '', x: '', y: '', navigationNodeId: '', primaryLocation: false, active: true },
}

const validationResources: Record<string, Resource> = {
  map: 'map', sector: 'sector', aisle: 'aisle', shelfblock: 'shelfBlock',
  pointofinterest: 'poi', poi: 'poi', node: 'node', edge: 'edge',
  productlocation: 'location', entrance: 'poi', checkout: 'poi',
}

function validationResource(elementType: string): Resource {
  return validationResources[elementType.replace(/[^a-z]/gi, '').toLowerCase()] ?? 'sector'
}

function validationActionLabel(resource: Resource): string {
  if (resource === 'map') return 'Abrir dados da versão'
  if (resource === 'location') return 'Abrir localizações'
  return 'Abrir no editor espacial'
}

export function AdminView({ activePage, navigate }: AdminViewProps) {
  const [tab, setTab] = useState<AdminTab>('catalog')
  const [resource, setResource] = useState<Resource>('store')
  const [stores, setStores] = useState<Store[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [products, setProducts] = useState<ProductAdmin[]>([])
  const [storeId, setStoreId] = useState('')
  const [storeProducts, setStoreProducts] = useState<StoreProduct[]>([])
  const [maps, setMaps] = useState<StoreMapAdmin[]>([])
  const [mapId, setMapId] = useState('')
  const [sectors, setSectors] = useState<Sector[]>([])
  const [aisles, setAisles] = useState<Aisle[]>([])
  const [shelves, setShelves] = useState<ShelfBlock[]>([])
  const [pois, setPois] = useState<PointOfInterest[]>([])
  const [nodes, setNodes] = useState<MapNode[]>([])
  const [edges, setEdges] = useState<MapEdge[]>([])
  const [locations, setLocations] = useState<ProductLocationAdmin[]>([])
  const [validation, setValidation] = useState<MapPublicationValidation | null>(null)
  const [formDraft, setFormDraft] = useState<Draft>(defaults.store ?? {})
  const [formOriginal, setFormOriginal] = useState<Draft>(defaults.store ?? {})
  const [editingId, setEditingId] = useState('')
  const [pageError, setPageError] = useState('')
  const [formMessage, setFormMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [selectedMap, setSelectedMap] = useState<StoreMap | null>(null)
  const [selectedElementId, setSelectedElementId] = useState('')

  const reloadBase = useCallback(async () => {
    setLoading(true)
    setPageError('')
    try {
      const [nextStores, nextCategories, nextProducts] = await Promise.all([adminApi.listStores(), adminApi.listCategories(), adminApi.listProducts()])
      setStores(nextStores); setCategories(nextCategories); setProducts(nextProducts)
      setStoreId((current) => nextStores.some((store) => store.id === current) ? current : nextStores[0]?.id ?? '')
    } catch (error) { setPageError(message(error)) }
    finally { setLoading(false) }
  }, [])

  const reloadStore = useCallback(async () => {
    if (!storeId) { setStoreProducts([]); setMaps([]); setLocations([]); setMapId(''); return }
    setLoading(true)
    setPageError('')
    try {
      const [nextProducts, nextMaps, nextLocations] = await Promise.all([
        adminApi.listStoreProducts(storeId), adminApi.listMaps(storeId), adminApi.listProductLocations(storeId),
      ])
      setStoreProducts(nextProducts); setMaps(nextMaps); setLocations(nextLocations)
      setMapId((current) => nextMaps.some((map) => map.id === current) ? current : nextMaps.find((map) => map.status === 'DRAFT')?.id ?? nextMaps[0]?.id ?? '')
    } catch (error) { setPageError(message(error)) }
    finally { setLoading(false) }
  }, [storeId])

  const reloadMap = useCallback(async () => {
    if (!mapId) { setSelectedMap(null); setSectors([]); setAisles([]); setShelves([]); setPois([]); setNodes([]); setEdges([]); setValidation(null); return }
    setLoading(true)
    setPageError('')
    try {
      const [mapHead, nextSectors, nextAisles, nextShelves, nextPois, nextNodes, nextEdges, report] = await Promise.all([
        maps.find((map) => map.id === mapId) ? Promise.resolve(maps.find((map) => map.id === mapId)!) : adminApi.listMaps(storeId).then((items) => items.find((map) => map.id === mapId)!),
        adminApi.listSectors(mapId), adminApi.listAisles(mapId), adminApi.listShelfBlocks(mapId), adminApi.listPointsOfInterest(mapId), adminApi.listNodes(mapId), adminApi.listEdges(mapId), adminApi.validateMap(mapId),
      ])
      setSectors(nextSectors); setAisles(nextAisles); setShelves(nextShelves); setPois(nextPois); setNodes(nextNodes); setEdges(nextEdges); setValidation(report)
      if (mapHead) setSelectedMap({ ...mapHead, sectors: nextSectors, aisles: nextAisles, shelfBlocks: nextShelves, pointsOfInterest: nextPois, nodes: nextNodes, edges: nextEdges })
    } catch (error) { setPageError(message(error)) }
    finally { setLoading(false) }
  }, [mapId, maps, storeId])

  useEffect(() => {
    // Initial API load owns the request loading and error states.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void reloadBase()
  }, [reloadBase])
  useEffect(() => {
    // Store selection synchronizes store-scoped admin collections.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void reloadStore()
  }, [reloadStore])
  useEffect(() => {
    // Map selection synchronizes the editor with the selected API version.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void reloadMap()
  }, [reloadMap])

  const currentRecords = useMemo(() => recordsFor(resource, { stores, categories, products, storeProducts, maps, sectors, aisles, shelves, pois, nodes, edges, locations }), [resource, stores, categories, products, storeProducts, maps, sectors, aisles, shelves, pois, nodes, edges, locations])

  function startNew(nextResource: Resource, shouldConfirm = true) {
    if (shouldConfirm && !confirmDiscardChanges()) return
    setResource(nextResource)
    setEditingId('')
    const nextDraft = { ...(defaults[nextResource] ?? {}) }
    setFormDraft(nextDraft)
    setFormOriginal(nextDraft)
    setFormMessage('')
  }

  function beginEdit(target: Resource, record: Record<string, unknown>, shouldConfirm = true): boolean {
    if (shouldConfirm && !confirmDiscardChanges()) return false
    setResource(target)
    setEditingId(typeof record.id === 'string' ? record.id : '')
    const nextDraft = toDraft(target, record)
    setFormDraft(nextDraft)
    setFormOriginal(nextDraft)
    setFormMessage('Editando cadastro selecionado.')
    return true
  }

  function confirmDiscardChanges(): boolean {
    return JSON.stringify(formDraft) === JSON.stringify(formOriginal)
      || window.confirm('Há alterações não salvas. Descartar e continuar?')
  }

  function goToPage(nextPage: AdminPage) {
    if (confirmDiscardChanges()) navigate(nextPage)
  }

  function selectEditorMap(nextMapId: string) {
    if (!confirmDiscardChanges()) return
    setMapId(nextMapId)
    setValidation(null)
    setSelectedElementId('')
    const editorResources: Resource[] = ['sector', 'aisle', 'shelfBlock', 'poi', 'node', 'edge']
    startNew(editorResources.includes(resource) ? resource : 'sector', false)
  }

  function selectLocationMap(nextMapId: string) {
    if (!confirmDiscardChanges()) return
    setMapId(nextMapId)
    setValidation(null)
    startNew('location', false)
  }

  function selectMapVersion(nextMapId: string) {
    if (!confirmDiscardChanges()) return
    setMapId(nextMapId)
    setValidation(null)
    const selected = maps.find((item) => item.id === nextMapId)
    if (selected) beginEdit('map', selected as unknown as Record<string, unknown>, false)
    else startNew('map', false)
  }

  function selectAdminTab(nextTab: AdminTab) {
    if (!confirmDiscardChanges()) return
    setTab(nextTab)
    if (nextTab === 'availability') startNew('availability', false)
    else if (nextTab === 'maps') startNew('map', false)
    else if (nextTab === 'editor' && !['sector', 'aisle', 'shelfBlock', 'poi', 'node', 'edge'].includes(resource)) startNew('sector', false)
    else if (nextTab === 'locations') startNew('location', false)
    else if (nextTab === 'catalog' && !['store', 'category', 'product'].includes(resource)) startNew('store', false)
  }

  function openValidationIssue(issue: MapPublicationIssue) {
    if (!confirmDiscardChanges()) return
    const target = validationResource(issue.elementType)
    setTab(target === 'map' ? 'maps' : target === 'location' ? 'locations' : 'editor')
    setSelectedElementId(issue.elementId ?? '')
    const records = recordsFor(target, { stores, categories, products, storeProducts, maps, sectors, aisles, shelves, pois, nodes, edges, locations })
    const targetId = issue.elementId ?? (target === 'map' ? mapId : '')
    const record = records.find((item) => item.id === targetId)
    if (record) {
      beginEdit(target, record, false)
      return
    }
    startNew(target, false)
    const endpointType = issue.elementType.toLowerCase() === 'entrance' ? 'ENTRANCE' : issue.elementType.toLowerCase() === 'checkout' ? 'CHECKOUT' : null
    if (target === 'poi' && endpointType) {
      const nextDraft = { ...(defaults.poi ?? {}), type: endpointType }
      setFormDraft(nextDraft)
      setFormOriginal(nextDraft)
    }
  }

  function optionsFor(source?: FieldSpec['source']): Array<{ id: string; label: string }> {
    if (!source) return []
    const display = (item: Record<string, unknown>, label: string) => ({ id: String(item.id), label })
    if (source === 'categories') return categories.filter((item) => item.active || item.id === formDraft.parentId).map((item) => display(item as unknown as Record<string, unknown>, item.name))
    if (source === 'products') return products.filter((item) => item.active).map((item) => display(item as unknown as Record<string, unknown>, `${item.name} · ${item.sku}`))
    if (source === 'sectors') return sectors.map((item) => display(item as unknown as Record<string, unknown>, item.name))
    if (source === 'aisles') return aisles.map((item) => display(item as unknown as Record<string, unknown>, `${item.code} · ${item.name}`))
    if (source === 'shelves') return shelves.map((item) => display(item as unknown as Record<string, unknown>, `${item.code}${item.name ? ` · ${item.name}` : ''}`))
    if (source === 'nodes') return nodes.map((item) => display(item as unknown as Record<string, unknown>, `${item.type} · ${item.label || item.id.slice(0, 8)}`))
    if (source === 'maps') return maps.map((item) => display(item as unknown as Record<string, unknown>, `v${item.version} · ${item.name} · ${item.status}`))
    return storeProducts.map((item) => display(item as unknown as Record<string, unknown>, `${products.find((product) => product.id === item.productId)?.name ?? item.productId} · ${item.active ? 'ativa' : 'inativa'}`))
  }

  async function saveForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true); setPageError(''); setFormMessage('')
    try {
      const body = buildPayload(resource, formDraft, Boolean(editingId), mapId)
      const id = editingId
      let savedMap: StoreMapAdmin | null = null
      if (id && body.active === false) {
        const current = currentRecords.find((record) => record.id === id)
        if (current?.active === true && !window.confirm(`Desativar este registro de ${resourceLabels[resource].toLowerCase()}? Ele continuará preservado nos dados.`)) return
      }
      switch (resource) {
        case 'store':
          if (id) await adminApi.updateStore(id, body as never)
          else await adminApi.createStore(body as never)
          break
        case 'category':
          if (id) await adminApi.updateCategory(id, body as never)
          else await adminApi.createCategory(body as never)
          break
        case 'product':
          if (id) await adminApi.updateProduct(id, body as never)
          else await adminApi.createProduct(body as never)
          break
        case 'availability':
          if (id) await adminApi.updateStoreProduct(storeId, id, body as never)
          else await adminApi.addProductToStore(storeId, body as never)
          break
        case 'map': {
          savedMap = id
            ? await adminApi.updateMap(storeId, id, { ...body, status: 'DRAFT' } as never)
            : await adminApi.createMap(storeId, { ...body, status: 'DRAFT' } as never)
          setMapId(savedMap.id)
          break
        }
        case 'sector':
          if (id) await adminApi.updateSector(mapId, id, body as never)
          else await adminApi.createSector(mapId, body as never)
          break
        case 'aisle':
          if (id) await adminApi.updateAisle(mapId, id, body as never)
          else await adminApi.createAisle(mapId, body as never)
          break
        case 'shelfBlock':
          if (id) await adminApi.updateShelfBlock(mapId, id, body as never)
          else await adminApi.createShelfBlock(mapId, body as never)
          break
        case 'poi':
          if (id) await adminApi.updatePointOfInterest(mapId, id, body as never)
          else await adminApi.createPointOfInterest(mapId, body as never)
          break
        case 'node':
          if (id) await adminApi.updateNode(mapId, id, body as never)
          else await adminApi.createNode(mapId, body as never)
          break
        case 'edge':
          if (id) await adminApi.updateEdge(mapId, id, body as never)
          else await adminApi.createEdge(mapId, body as never)
          break
        case 'location':
          if (id) await adminApi.updateProductLocation(storeId, id, body as never)
          else await adminApi.createProductLocation(storeId, body as never)
          break
      }
      setFormMessage(id ? 'Alterações salvas.' : 'Cadastro criado.')
      const nextDraft = resource === 'map' && savedMap
        ? toDraft('map', savedMap as unknown as Record<string, unknown>)
        : { ...(defaults[resource] ?? {}) }
      setEditingId(resource === 'map' ? savedMap?.id ?? '' : '')
      setFormDraft(nextDraft)
      setFormOriginal(nextDraft)
      if (resource === 'store' || resource === 'category' || resource === 'product') await reloadBase()
      if (resource === 'availability' || resource === 'map' || resource === 'location') await reloadStore()
      if (['map', 'sector', 'aisle', 'shelfBlock', 'poi', 'node', 'edge', 'location'].includes(resource)) await reloadMap()
    } catch (error) { setPageError(message(error)) }
    finally { setSaving(false) }
  }

  async function validateSelectedMap() {
    if (!mapId) return
    setLoading(true); setPageError('')
    try { setValidation(await adminApi.validateMap(mapId)) }
    catch (error) { setPageError(message(error)) }
    finally { setLoading(false) }
  }

  async function changeMapStatus(status: 'ACTIVE' | 'ARCHIVED') {
    const current = maps.find((item) => item.id === mapId)
    if (!current) return
    if (status === 'ARCHIVED' && !window.confirm(`Arquivar a versão v${current.version}? O status é terminal e a versão não poderá voltar a ser ativa.`)) return
    setLoading(true); setPageError('')
    try {
      await adminApi.updateMap(storeId, current.id, { version: current.version, name: current.name, width: Number(current.width), height: Number(current.height), scaleMetersPerUnit: Number(current.scaleMetersPerUnit), status })
      setFormMessage(status === 'ACTIVE' ? 'Versão ativada.' : 'Versão arquivada. A mudança foi explícita.')
      await reloadStore(); await reloadMap()
    } catch (error) { setPageError(message(error)) }
    finally { setLoading(false) }
  }

  const canEditMap = maps.find((item) => item.id === mapId)?.status === 'DRAFT'
  const selectedMapHead = maps.find((item) => item.id === mapId)
  const pendingMapEdits = Boolean(selectedMapHead && resource === 'map' && editingId === selectedMapHead.id && JSON.stringify(formDraft) !== JSON.stringify(formOriginal))

  return (
    <main className="app-frame admin-frame">
      <header className="topbar">
        <a className="brand-lockup" href="#shop" onClick={(event) => { event.preventDefault(); goToPage('shop') }} aria-label="Market Fast Route, início">
          <span className="brand-mark"><MapPinned aria-hidden="true" /></span><span><strong>Market Fast Route</strong><small>Administração da loja</small></span>
        </a>
        <nav className="main-nav" aria-label="Navegação principal">
          <button className={activePage === 'shop' ? 'nav-link active' : 'nav-link'} onClick={() => goToPage('shop')}>Compras</button>
          <button className={activePage === 'admin' ? 'nav-link active' : 'nav-link'} onClick={() => goToPage('admin')}>Administração</button>
          <button className={activePage === 'demo' ? 'nav-link active' : 'nav-link'} onClick={() => goToPage('demo')}>Mercado 3D</button>
        </nav>
        <div className="topbar-note"><span className="status-dot" /> Acesso privado</div>
      </header>

      <section className="admin-heading"><div><h1>Organize a loja<br />e publique com segurança.</h1><p>Cadastros, planta e validação da navegação ficam no mesmo espaço de trabalho.</p></div>
        <label className="admin-store-select"><span>Unidade em edição</span><select value={storeId} onChange={(event) => { if (!confirmDiscardChanges()) return; setStoreId(event.target.value); setMapId(''); setValidation(null) }}><option value="">Selecione uma loja</option>{stores.map((store) => <option key={store.id} value={store.id}>{store.name} · {store.code}</option>)}</select><StoreIcon size={18} /></label>
      </section>

      <nav className="admin-tabs" aria-label="Áreas administrativas">
        {(Object.keys(tabLabels) as AdminTab[]).map((item) => <button type="button" key={item} className={tab === item ? 'admin-tab active' : 'admin-tab'} onClick={() => selectAdminTab(item)}>{tabLabels[item]}</button>)}
      </nav>
      <div className="admin-content">
        {pageError && <div className="notice error-notice" role="alert"><AlertCircle size={17} /><span>{pageError}</span><button onClick={() => setPageError('')}>Dispensar</button></div>}
        {formMessage && <div className="notice success-notice" role="status"><Check size={17} /><span>{formMessage}</span><button onClick={() => setFormMessage('')}>Dispensar</button></div>}
        {loading && <div className="admin-loading" role="status"><LoaderCircle className="spin" /> Sincronizando dados da API…</div>}

        {tab === 'catalog' && <div className="admin-section-layout">
          <section className="admin-records"><div className="admin-section-title"><h2>{resourceLabels[resource]}</h2><p>Cadastros atualizados diretamente pela API administrativa.</p></div>
            <div className="resource-select-row"><label htmlFor="catalog-resource">Tipo de cadastro</label><select id="catalog-resource" value={resource} onChange={(event) => startNew(event.target.value as Resource)}><option value="store">Lojas</option><option value="category">Categorias</option><option value="product">Produtos</option></select><Button variant="outline" onClick={() => startNew(resource)}><Plus size={16} /> Novo</Button></div>
            <RecordList resource={resource} records={currentRecords} editingId={editingId} onEdit={(record) => beginEdit(resource, record)} products={products} />
          </section>
          <EntityForm resource={resource} editingId={editingId} draft={formDraft} setDraft={setFormDraft} fields={fieldsByResource[resource] ?? []} optionsFor={optionsFor} onSubmit={saveForm} onCancel={() => startNew(resource)} saving={saving} />
        </div>}

        {tab === 'availability' && <div className="admin-section-layout">
          <section className="admin-records"><div className="admin-section-title"><h2>Disponibilidade de produtos</h2><p>Ative ou desative a disponibilidade no catálogo desta loja.</p></div>
            {!storeId ? <EmptyAdminState text="Selecione uma loja para administrar seus produtos." /> : <RecordList resource="availability" records={storeProducts as unknown as Array<Record<string, unknown>>} editingId={editingId} onEdit={(record) => beginEdit('availability', record)} products={products} />}
          </section>
          <EntityForm resource="availability" editingId={editingId} draft={formDraft} setDraft={setFormDraft} fields={fieldsByResource.availability ?? []} optionsFor={optionsFor} onSubmit={saveForm} onCancel={() => startNew('availability')} saving={saving} />
        </div>}

        {tab === 'maps' && <div className="map-admin-layout">
          <section className="admin-records map-version-list"><div className="admin-section-title"><h2>Mapas da unidade</h2><p>Crie uma versão em rascunho, valide, arquive a atual de forma explícita e só então ative a nova.</p></div>
            <div className="resource-select-row"><label htmlFor="map-select">Versão selecionada</label><select id="map-select" value={mapId} onChange={(event) => selectMapVersion(event.target.value)}><option value="">Selecione uma versão</option>{maps.map((map) => <option key={map.id} value={map.id}>v{map.version} · {map.name} · {map.status}</option>)}</select><Button variant="outline" onClick={() => { if (!confirmDiscardChanges()) return; startNew('map', false); setMapId(''); setValidation(null) }}><Plus size={16} /> Nova DRAFT</Button></div>
            {maps.map((map) => <button className={`version-row ${map.id === mapId ? 'selected' : ''}`} key={map.id} onClick={() => { if (beginEdit('map', map as unknown as Record<string, unknown>)) { setMapId(map.id); setValidation(null) } }} aria-label={`Editar versão ${map.version}, ${map.name}`}><span className={`version-status ${map.status.toLowerCase()}`}>{map.status}</span><span><strong>v{map.version} · {map.name}</strong><small>{map.width} × {map.height} unidades</small></span><ArrowRight size={17} /></button>)}
            {!maps.length && !loading && <EmptyAdminState text="Esta loja ainda não tem uma versão de mapa." />}
          </section>
          <section className="map-lifecycle-panel">
            <div className="admin-section-title"><h2>{selectedMapHead ? `v${selectedMapHead.version} · ${selectedMapHead.name}` : 'Ciclo de publicação'}</h2><p>O servidor valida geometria, grafo, entrada, caixas e vínculos de produtos.</p></div>
            {selectedMapHead?.status === 'DRAFT' && editingId === selectedMapHead.id && <EntityForm compact resource="map" editingId={editingId} draft={formDraft} setDraft={setFormDraft} fields={fieldsByResource.map ?? []} optionsFor={optionsFor} onSubmit={saveForm} onCancel={() => beginEdit('map', selectedMapHead as unknown as Record<string, unknown>, false)} saving={saving} />}
            {selectedMapHead?.status === 'DRAFT' && editingId !== selectedMapHead.id && <div className="lifecycle-actions"><Button variant="outline" onClick={() => beginEdit('map', selectedMapHead as unknown as Record<string, unknown>)}>Editar metadados do rascunho</Button></div>}
            {!selectedMapHead && resource === 'map' && <EntityForm compact resource="map" editingId="" draft={formDraft} setDraft={setFormDraft} fields={fieldsByResource.map ?? []} optionsFor={optionsFor} onSubmit={saveForm} onCancel={() => startNew('map')} saving={saving} />}
            {selectedMapHead && <div className="lifecycle-actions"><Button variant="outline" onClick={() => void validateSelectedMap()} disabled={loading || pendingMapEdits}><ClipboardCheck size={16} /> Validar versão</Button>
              {selectedMapHead.status === 'ACTIVE' && <Button variant="destructive" onClick={() => void changeMapStatus('ARCHIVED')} disabled={loading || pendingMapEdits}>Arquivar esta versão</Button>}
              {selectedMapHead.status === 'DRAFT' && validation?.publishable && <Button onClick={() => void changeMapStatus('ACTIVE')} disabled={loading || pendingMapEdits}><Check size={16} /> Ativar versão validada</Button>}
            </div>}
            {pendingMapEdits && <p className="form-help">Salve as alterações desta versão antes de validar ou publicar.</p>}
            {validation && <div className={`validation-report ${validation.publishable ? 'valid' : 'invalid'}`} role="status">
              <div className="validation-report-title">{validation.publishable ? <CircleCheck /> : <TriangleAlert />}<strong>{validation.publishable ? 'Mapa pronto para ativação' : `${validation.issues.length} pendência${validation.issues.length === 1 ? '' : 's'} para corrigir`}</strong></div>
              {validation.issues.length > 0 && <ul>{validation.issues.map((issue, index) => <li key={`${issue.code}-${issue.elementId}-${index}`}><strong>{issue.elementType}{issue.elementId ? ` · ${issue.elementId.slice(0, 8)}` : ''}</strong><span>{issue.message}</span><small>{issue.code}</small><button type="button" onClick={() => openValidationIssue(issue)}>{validationActionLabel(validationResource(issue.elementType))}</button></li>)}</ul>}
              {validation.publishable && selectedMapHead && maps.some((map) => map.status === 'ACTIVE' && map.id !== selectedMapHead.id) && <p className="inline-warning"><TriangleAlert size={16} /> Arquive a versão ativa atual antes de ativar esta versão.</p>}
            </div>}
            {!selectedMapHead && resource !== 'map' && <EmptyAdminState text="Selecione ou crie uma versão para abrir o fluxo de publicação." />}
          </section>
        </div>}

        {tab === 'editor' && <div className="editor-layout">
          <section className="editor-map-panel"><div className="admin-section-title"><h2>Editor do mapa</h2><p>Selecione um elemento para revisar os seus dados. Arraste para explorar a planta.</p></div>
            <div className="editor-toolbar"><label htmlFor="editor-map-select">Versão</label><select id="editor-map-select" value={mapId} onChange={(event) => selectEditorMap(event.target.value)}><option value="">Selecione um mapa</option>{maps.map((item) => <option key={item.id} value={item.id}>v{item.version} · {item.name} · {item.status}</option>)}</select><span className={`version-status ${selectedMapHead?.status.toLowerCase() ?? ''}`}>{selectedMapHead?.status ?? '—'}</span></div>
            {selectedMap ? <StoreMapSvg map={selectedMap} title="Editor SVG do mapa" showNetwork selectedElementId={selectedElementId} onSelectElement={(id) => { setSelectedElementId(id); const item = [...sectors, ...aisles, ...shelves, ...pois, ...nodes, ...edges].find((entry) => entry.id === id); if (item) { const kind: Resource = sectors.some((entry) => entry.id === id) ? 'sector' : aisles.some((entry) => entry.id === id) ? 'aisle' : shelves.some((entry) => entry.id === id) ? 'shelfBlock' : pois.some((entry) => entry.id === id) ? 'poi' : nodes.some((entry) => entry.id === id) ? 'node' : 'edge'; beginEdit(kind, item as unknown as Record<string, unknown>) } }} /> : <EmptyAdminState text={mapId ? 'Carregando os elementos do mapa…' : 'Selecione uma versão para carregar a planta.'} />}
            <div className="editor-legend"><span><i className="legend-sector" /> Setores</span><span><i className="legend-shelf" /> Prateleiras</span><span><i className="legend-poi">i</i> POIs e nós</span></div>
          </section>
          <section className="editor-record-panel"><div className="admin-section-title"><h2>{resourceLabels[resource]}</h2><p>As alterações espaciais só podem ser gravadas enquanto a versão for DRAFT.</p></div>
            <div className="resource-select-row editor-resource-row"><label htmlFor="editor-resource">Elemento</label><select id="editor-resource" value={['sector', 'aisle', 'shelfBlock', 'poi', 'node', 'edge'].includes(resource) ? resource : 'sector'} onChange={(event) => startNew(event.target.value as Resource)}>{(['sector', 'aisle', 'shelfBlock', 'poi', 'node', 'edge'] as Resource[]).map((item) => <option key={item} value={item}>{resourceLabels[item]}</option>)}</select><Button variant="outline" disabled={!canEditMap} onClick={() => startNew(resource)}><Plus size={16} /> Novo</Button></div>
            <RecordList resource={resource} records={currentRecords} editingId={editingId} onEdit={(record) => beginEdit(resource, record)} products={products} />
            <EntityForm resource={resource} editingId={editingId} draft={formDraft} setDraft={setFormDraft} fields={fieldsByResource[resource] ?? []} optionsFor={optionsFor} onSubmit={saveForm} onCancel={() => startNew(resource)} saving={saving} disabled={!canEditMap || !mapId} />
            {selectedElementId && <p className="editor-selected">Selecionado na planta: <code>{selectedElementId.slice(0, 8)}</code></p>}
          </section>
        </div>}

        {tab === 'locations' && <div className="admin-section-layout">
          <section className="admin-records"><div className="admin-section-title"><h2>Localizações navegáveis</h2><p>Associe a disponibilidade do produto a um setor, corredor, nó e ponto opcional da planta.</p></div>
            <div className="resource-select-row"><label htmlFor="location-map">Versão de mapa</label><select id="location-map" value={mapId} onChange={(event) => selectLocationMap(event.target.value)}><option value="">Escolha uma versão</option>{maps.map((item) => <option key={item.id} value={item.id}>v{item.version} · {item.name} · {item.status}</option>)}</select></div>
            <RecordList resource="location" records={locations.filter((location) => location.mapId === mapId) as unknown as Array<Record<string, unknown>>} editingId={editingId} onEdit={(record) => beginEdit('location', record)} products={products} />
          </section>
          <EntityForm resource="location" editingId={editingId} draft={formDraft} setDraft={setFormDraft} fields={fieldsByResource.location ?? []} optionsFor={optionsFor} onSubmit={saveForm} onCancel={() => startNew('location')} saving={saving} disabled={!storeId || !mapId || !canEditMap} />
        </div>}
      </div>
      <footer className="admin-footer"><Layers3 size={15} /> Dados administrativos são enviados à API. Versões publicadas ficam imutáveis.</footer>
    </main>
  )
}

function EntityForm({ resource, editingId, draft, setDraft, fields, optionsFor, onSubmit, onCancel, saving, disabled = false, compact = false }: {
  resource: Resource; editingId: string; draft: Draft; setDraft: (draft: Draft) => void; fields: FieldSpec[];
  optionsFor: (source?: FieldSpec['source']) => Array<{ id: string; label: string }>;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void; onCancel: () => void; saving: boolean; disabled?: boolean; compact?: boolean;
}) {
  return (
    <form className={`entity-form ${compact ? 'compact-form' : ''}`} onSubmit={onSubmit}>
      <div className="form-card-heading"><h3>{editingId ? 'Revise os campos.' : 'Adicionar informação.'}</h3></div>
      <div className="entity-fields">
        {fields.map((field) => <label key={field.key} className={`entity-field ${field.kind === 'boolean' ? 'boolean-field' : ''}`}>
          {field.kind === 'boolean' ? <><input type="checkbox" checked={Boolean(draft[field.key])} onChange={(event) => setDraft({ ...draft, [field.key]: event.target.checked })} disabled={disabled} /><span>{field.label}</span></> : <>
            <span>{field.label}{field.required && <b aria-hidden="true"> *</b>}</span>
            {field.kind === 'select' ? <select required={field.required} value={String(draft[field.key] ?? '')} onChange={(event) => setDraft({ ...draft, [field.key]: event.target.value })} disabled={disabled}>
              <option value="">{field.source ? 'Selecione…' : 'Selecione…'}</option>
              {field.source ? optionsFor(field.source).map((option) => <option key={option.id} value={option.id}>{option.label}</option>) : field.options?.map((option) => <option value={option} key={option}>{enumLabel(option)}</option>)}
            </select> : field.kind === 'textarea' ? <textarea rows={3} value={String(draft[field.key] ?? '')} onChange={(event) => setDraft({ ...draft, [field.key]: event.target.value })} disabled={disabled} /> : <input type={field.kind === 'number' ? 'number' : 'text'} step={field.kind === 'number' ? 'any' : undefined} required={field.required} value={String(draft[field.key] ?? '')} onChange={(event) => setDraft({ ...draft, [field.key]: event.target.value })} disabled={disabled} />}
          </>}
        </label>)}
      </div>
      {resource === 'map' && !editingId && <p className="form-help">Novas versões sempre começam como DRAFT. O status só muda no fluxo de publicação.</p>}
      <div className="form-actions"><Button type="submit" disabled={saving || disabled || !fields.length}>{saving ? <LoaderCircle className="spin" /> : editingId ? <Save /> : <Plus />}{editingId ? 'Salvar alterações' : `Criar ${resourceLabels[resource].toLowerCase().replace(/s$/, '')}`}</Button>
        {editingId && <Button type="button" variant="ghost" onClick={onCancel}>Cancelar edição</Button>}
      </div>
      {disabled && <p className="form-help">A versão está publicada. Crie um novo rascunho para alterar dados espaciais.</p>}
    </form>
  )
}

function RecordList({ resource, records, editingId, onEdit, products }: { resource: Resource; records: Array<Record<string, unknown>>; editingId: string; onEdit: (record: Record<string, unknown>) => void; products: ProductAdmin[] }) {
  if (!records.length) return <EmptyAdminState text="Nenhum registro encontrado para este tipo." />
  return <div className="record-list" aria-label={`Lista de ${resourceLabels[resource]}`}>
    {records.map((record, index) => {
      const id = String(record.id ?? index)
      const title = String(record.name ?? (resource === 'availability' ? products.find((product) => product.id === record.productId)?.name ?? 'Produto' : record.code ?? record.type ?? record.sku ?? id.slice(0, 8)))
      const description = resource === 'store' ? `${record.code} · ${record.city}, ${record.state}`
        : resource === 'product' ? `${record.sku} · ${record.brand ?? 'Sem marca'}`
          : resource === 'availability' ? `${record.active ? 'Disponível' : 'Indisponível'} nesta unidade`
            : resource === 'map' ? `Versão ${record.version} · ${record.width} × ${record.height}`
              : resource === 'location' ? `Mapa ${String(record.mapId).slice(0, 8)} · nó ${String(record.navigationNodeId).slice(0, 8)}${record.primaryLocation ? ' · principal' : ''}`
                : ('x' in record ? `(${record.x}, ${record.y}) · ${record.active === false ? 'inativo' : 'ativo'}` : String(record.code ?? record.type ?? ''))
      const canEdit = resource !== 'availability' || typeof record.productId === 'string'
      return <button type="button" className={`record-row ${id === editingId ? 'selected' : ''}`} key={id} onClick={() => canEdit && onEdit(record)}>
        <span className="record-icon">{resource === 'map' ? <MapPinned size={16} /> : resource === 'store' ? <StoreIcon size={16} /> : <Layers3 size={16} />}</span>
        <span className="record-copy"><strong>{title}</strong><small>{description}</small></span>
        <span className={`record-state ${String(record.status ?? (record.active === false ? 'inativo' : 'ativo')).toLowerCase()}`}>{String(record.status ?? (record.active === false ? 'inativo' : 'ativo'))}</span>
      </button>
    })}
  </div>
}

function EmptyAdminState({ text }: { text: string }) {
  return <div className="admin-empty"><span><Layers3 /></span><p>{text}</p></div>
}

function recordsFor(resource: Resource, data: { stores: Store[]; categories: Category[]; products: ProductAdmin[]; storeProducts: StoreProduct[]; maps: StoreMapAdmin[]; sectors: Sector[]; aisles: Aisle[]; shelves: ShelfBlock[]; pois: PointOfInterest[]; nodes: MapNode[]; edges: MapEdge[]; locations: ProductLocationAdmin[] }): Array<Record<string, unknown>> {
  const records: Record<Resource, Array<Record<string, unknown>>> = {
    store: data.stores as unknown as Array<Record<string, unknown>>, category: data.categories as unknown as Array<Record<string, unknown>>,
    product: data.products as unknown as Array<Record<string, unknown>>, availability: data.storeProducts as unknown as Array<Record<string, unknown>>,
    map: data.maps as unknown as Array<Record<string, unknown>>, sector: data.sectors as unknown as Array<Record<string, unknown>>,
    aisle: data.aisles as unknown as Array<Record<string, unknown>>, shelfBlock: data.shelves as unknown as Array<Record<string, unknown>>,
    poi: data.pois as unknown as Array<Record<string, unknown>>, node: data.nodes as unknown as Array<Record<string, unknown>>,
    edge: data.edges as unknown as Array<Record<string, unknown>>, location: data.locations as unknown as Array<Record<string, unknown>>,
  }
  return records[resource]
}

function toDraft(resource: Resource, record: Record<string, unknown>): Draft {
  const fields = fieldsByResource[resource] ?? []
  return Object.fromEntries(fields.map(({ key, kind }) => {
    const value = record[key]
    return [key, kind === 'boolean' ? Boolean(value) : value == null ? '' : String(value)]
  }))
}

function value(draft: Draft, key: string): string { return String(draft[key] ?? '').trim() }
function nullable(draft: Draft, key: string): string | null { return value(draft, key) || null }
function numeric(draft: Draft, key: string): number { return Number(draft[key] ?? 0) }
function bool(draft: Draft, key: string): boolean { return Boolean(draft[key]) }

function buildPayload(resource: Resource, draft: Draft, includeActive = false, selectedMapId = ''): Record<string, unknown> {
  switch (resource) {
    case 'store': return { name: value(draft, 'name'), code: value(draft, 'code'), address: value(draft, 'address'), city: value(draft, 'city'), state: value(draft, 'state'), ...(includeActive ? { active: bool(draft, 'active') } : {}) }
    case 'category': return { name: value(draft, 'name'), code: value(draft, 'code'), parentId: nullable(draft, 'parentId'), ...(includeActive ? { active: bool(draft, 'active') } : {}) }
    case 'product': return { categoryId: value(draft, 'categoryId'), sku: value(draft, 'sku'), ean: nullable(draft, 'ean'), name: value(draft, 'name'), brand: nullable(draft, 'brand'), description: nullable(draft, 'description'), ...(includeActive ? { active: bool(draft, 'active') } : {}) }
    case 'availability': return draft.productId ? { productId: value(draft, 'productId'), active: bool(draft, 'active') } : { active: bool(draft, 'active') }
    case 'map': return { version: numeric(draft, 'version'), name: value(draft, 'name'), width: numeric(draft, 'width'), height: numeric(draft, 'height'), scaleMetersPerUnit: numeric(draft, 'scaleMetersPerUnit') }
    case 'sector': return { name: value(draft, 'name'), code: value(draft, 'code'), x: numeric(draft, 'x'), y: numeric(draft, 'y'), width: numeric(draft, 'width'), height: numeric(draft, 'height'), rotation: numeric(draft, 'rotation'), ...(draft.active === undefined ? {} : { active: bool(draft, 'active') }) }
    case 'aisle': return { sectorId: nullable(draft, 'sectorId'), code: value(draft, 'code'), name: value(draft, 'name'), x: numeric(draft, 'x'), y: numeric(draft, 'y'), width: numeric(draft, 'width'), height: numeric(draft, 'height'), rotation: numeric(draft, 'rotation'), ...(draft.active === undefined ? {} : { active: bool(draft, 'active') }) }
    case 'shelfBlock': return { sectorId: nullable(draft, 'sectorId'), aisleId: nullable(draft, 'aisleId'), code: value(draft, 'code'), name: nullable(draft, 'name'), x: numeric(draft, 'x'), y: numeric(draft, 'y'), width: numeric(draft, 'width'), height: numeric(draft, 'height'), rotation: numeric(draft, 'rotation'), ...(draft.active === undefined ? {} : { active: bool(draft, 'active') }) }
    case 'poi': return { type: value(draft, 'type'), name: value(draft, 'name'), navigationNodeId: nullable(draft, 'navigationNodeId'), x: numeric(draft, 'x'), y: numeric(draft, 'y'), ...(draft.active === undefined ? {} : { active: bool(draft, 'active') }) }
    case 'node': return { type: value(draft, 'type'), label: nullable(draft, 'label'), x: numeric(draft, 'x'), y: numeric(draft, 'y'), ...(draft.active === undefined ? {} : { active: bool(draft, 'active') }) }
    case 'edge': return { fromNodeId: value(draft, 'fromNodeId'), toNodeId: value(draft, 'toNodeId'), distanceMeters: numeric(draft, 'distanceMeters'), bidirectional: bool(draft, 'bidirectional'), ...(draft.active === undefined ? {} : { active: bool(draft, 'active') }) }
    case 'location': return { storeProductId: value(draft, 'storeProductId'), mapId: selectedMapId, sectorId: nullable(draft, 'sectorId'), aisleId: nullable(draft, 'aisleId'), shelfBlockId: nullable(draft, 'shelfBlockId'), side: nullable(draft, 'side'), module: nullable(draft, 'module'), shelfLevel: value(draft, 'shelfLevel') ? numeric(draft, 'shelfLevel') : null, x: value(draft, 'x') ? numeric(draft, 'x') : null, y: value(draft, 'y') ? numeric(draft, 'y') : null, navigationNodeId: value(draft, 'navigationNodeId'), primaryLocation: bool(draft, 'primaryLocation'), ...(includeActive ? { active: bool(draft, 'active') } : {}) }
  }
}

function enumLabel(value: string) {
  const labels: Record<string, string> = { ENTRANCE: 'Entrada', EXIT: 'Saída', CHECKOUT: 'Caixas', CART: 'Carrinhos', RESTROOM: 'Banheiro', CUSTOMER_SERVICE: 'Atendimento', PARKING: 'Estacionamento', ELEVATOR: 'Elevador', STAIRS: 'Escadas', OTHER: 'Outro', PATH: 'Caminho', INTERSECTION: 'Interseção', PRODUCT_ACCESS: 'Acesso a produto', LEFT: 'Esquerda', RIGHT: 'Direita', CENTER: 'Centro' }
  return labels[value] ?? value
}

function message(error: unknown) { return error instanceof ApiRequestError ? error.message : error instanceof Error ? error.message : 'Não foi possível concluir a operação. Tente novamente.' }
