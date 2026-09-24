import { lazy, Suspense, useEffect, useMemo, useState } from 'react'
import { AlertCircle, ArrowUp, Check, ChevronDown, CircleHelp, Compass, LoaderCircle, MapPinned, Minus, Plus, Search, ShoppingBasket, Store as StoreIcon, Trash2, X } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { StoreMapSvg } from '@/components/StoreMapSvg'
import { calculateStoreRoute, getActiveStoreMap, getProductLocations, listStores, searchStoreProducts } from '@/services/customer-api'
import type { CalculatedRoute, Product, ProductLocation, Store, StoreMap } from '@/types/api'
import { ApiRequestError } from '@/services/api-client'

const ThreeMarketScene = lazy(() => import('@/app/MarketScene').then((module) => ({ default: module.MarketScene })))

interface CustomerViewProps {
  activePage: 'shop' | 'admin' | 'demo'
  navigate: (page: 'shop' | 'admin' | 'demo') => void
}

interface ShoppingLine {
  product: Product
  quantity: number
  locationState: 'checking' | 'located' | 'missing' | 'unknown'
  locations: ProductLocation[]
}

const formatDistance = (meters: number) => `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }).format(meters)} m`
const formatStopCount = (count: number) => `${count} ${count === 1 ? 'parada' : 'paradas'}`

export function CustomerView({ activePage, navigate }: CustomerViewProps) {
  const [mapPresentation, setMapPresentation] = useState<'2d' | '3d'>(() => activePage === 'demo' ? '3d' : '2d')
  const [stores, setStores] = useState<Store[]>([])
  const [storeId, setStoreId] = useState('')
  const [map, setMap] = useState<StoreMap | null>(null)
  const [storeResponseToken, setStoreResponseToken] = useState(-1)
  const [storeError, setStoreError] = useState('')
  const [storeErrorToken, setStoreErrorToken] = useState(-1)
  const [storeRetryToken, setStoreRetryToken] = useState(0)
  const [mapResponseKey, setMapResponseKey] = useState('')
  const [mapError, setMapError] = useState('')
  const [mapErrorKey, setMapErrorKey] = useState('')
  const [mapRetryToken, setMapRetryToken] = useState(0)
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [searchRetryToken, setSearchRetryToken] = useState(0)
  const [products, setProducts] = useState<Product[]>([])
  const [productsResponseKey, setProductsResponseKey] = useState('')
  const [searchErrorKey, setSearchErrorKey] = useState('')
  const [searchError, setSearchError] = useState('')
  const [shoppingList, setShoppingList] = useState<ShoppingLine[]>([])
  const [route, setRoute] = useState<CalculatedRoute | null>(null)
  const [routeLoading, setRouteLoading] = useState(false)
  const [routeError, setRouteError] = useState('')
  const [routeSummaryOpen, setRouteSummaryOpen] = useState(false)
  const [mobilePane, setMobilePane] = useState<'list' | 'map'>('list')

  useEffect(() => {
    const controller = new AbortController()
    const requestToken = storeRetryToken
    listStores(controller.signal)
      .then((items) => {
        const active = items.filter((store) => store.active)
        setStores(active)
        setStoreId((current) => active.some((store) => store.id === current) ? current : active[0]?.id ?? '')
        if (!active.length) {
          setStoreError('Nenhuma loja ativa está cadastrada. Peça à equipe administrativa para ativar uma loja.')
          setStoreErrorToken(requestToken)
        } else {
          setStoreError('')
        }
        setStoreResponseToken(requestToken)
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setStoreError(errorMessage(error))
          setStoreErrorToken(requestToken)
          setStoreResponseToken(requestToken)
        }
      })
    return () => controller.abort()
  }, [storeRetryToken])

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedSearch(search.trim()), 280)
    return () => window.clearTimeout(timer)
  }, [search])

  useEffect(() => {
    if (!storeId) return
    const controller = new AbortController()
    const requestKey = `${storeId}:${mapRetryToken}`
    getActiveStoreMap(storeId, controller.signal)
      .then((activeMap) => {
        if (!controller.signal.aborted) {
          setMap(activeMap)
          setMapError('')
          setMapResponseKey(requestKey)
        }
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setMapError(errorMessage(error))
          setMapErrorKey(requestKey)
          setMapResponseKey(requestKey)
        }
      })
    return () => controller.abort()
  }, [storeId, mapRetryToken])

  useEffect(() => {
    if (!storeId) return
    const controller = new AbortController()
    searchStoreProducts(storeId, debouncedSearch, controller.signal)
      .then((items) => {
        if (!controller.signal.aborted) {
          setProducts(items)
          setProductsResponseKey(`${storeId}:${debouncedSearch}:${searchRetryToken}`)
          setSearchErrorKey('')
        }
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setSearchError(errorMessage(error))
          setProductsResponseKey(`${storeId}:${debouncedSearch}:${searchRetryToken}`)
          setSearchErrorKey(`${storeId}:${debouncedSearch}:${searchRetryToken}`)
        }
      })
    return () => controller.abort()
  }, [storeId, debouncedSearch, searchRetryToken])

  const selectedStore = useMemo(() => stores.find((store) => store.id === storeId), [stores, storeId])
  const storeLoading = storeResponseToken !== storeRetryToken
  const currentStoreError = storeErrorToken === storeRetryToken ? storeError : ''
  const mapRequestKey = `${storeId}:${mapRetryToken}`
  const mapLoading = Boolean(storeId && mapResponseKey !== mapRequestKey)
  const currentMapError = mapErrorKey === mapRequestKey ? mapError : ''
  const searchKey = `${storeId}:${debouncedSearch}:${searchRetryToken}`
  const productsLoading = Boolean(storeId && productsResponseKey !== searchKey)
  const currentSearchError = searchErrorKey === searchKey ? searchError : ''
  const listIds = useMemo(() => new Set(shoppingList.map(({ product }) => product.id)), [shoppingList])
  const routeUnavailable = shoppingList.some((line) => line.locationState === 'missing')
  const routeStops = useMemo(() => new Map(route?.orderedStops.map((stop) => [stop.productId, stop.order]) ?? []), [route])
  const productLocations = useMemo(() => shoppingList.flatMap(({ product, locations }) =>
    locations.map((location) => ({ ...location, productName: product.name })),
  ), [shoppingList])
  const routeLocationsById = useMemo(() => new Map(productLocations.map((location) => [location.id, location])), [productLocations])
  const entryLabel = map?.pointsOfInterest.find(({ type }) => type === 'ENTRANCE')?.name ?? map?.nodes.find(({ type }) => type === 'ENTRANCE')?.label ?? 'Entrada'
  const checkoutLabel = map?.pointsOfInterest.find(({ type }) => type === 'CHECKOUT')?.name ?? map?.nodes.find(({ type }) => type === 'CHECKOUT')?.label ?? 'Caixas'

  function chooseStore(nextStoreId: string) {
    setStoreId(nextStoreId)
    setShoppingList([])
    setRoute(null)
    setRouteError('')
    setSearch('')
    setStoreError('')
    setMapError('')
  }

  function retryStores() {
    setStoreRetryToken((token) => token + 1)
  }

  function retryMap() {
    if (!storeId) {
      retryStores()
      return
    }
    setMapRetryToken((token) => token + 1)
  }

  function retrySearch() {
    setSearchRetryToken((token) => token + 1)
  }

  async function addProduct(product: Product) {
    if (listIds.has(product.id)) return
    const line: ShoppingLine = { product, quantity: 1, locationState: 'checking', locations: [] }
    setShoppingList((current) => [...current, line])
    setRoute(null)
    try {
      const locations = await getProductLocations(storeId, product.id)
      setShoppingList((current) => current.map((item) => item.product.id === product.id
        ? { ...item, locations, locationState: locations.length ? 'located' : 'missing' }
        : item))
    } catch {
      setShoppingList((current) => current.map((item) => item.product.id === product.id
        ? { ...item, locationState: 'unknown' }
        : item))
    }
  }

  function changeQuantity(productId: string, delta: number) {
    const removesProduct = shoppingList.some((line) => line.product.id === productId && line.quantity + delta <= 0)
    setShoppingList((current) => current.flatMap((line) => {
      if (line.product.id !== productId) return [line]
      const quantity = line.quantity + delta
      return quantity > 0 ? [{ ...line, quantity }] : []
    }))
    if (removesProduct) setRoute(null)
  }

  async function calculateRoute() {
    if (!storeId || !shoppingList.length || routeUnavailable) return
    setRouteLoading(true)
    setRouteError('')
    setRoute(null)
    setRouteSummaryOpen(false)
    try {
      const result = await calculateStoreRoute(storeId, shoppingList.map(({ product }) => product.id))
      setRoute(result)
      setMapPresentation('3d')
      navigate('demo')
      setMobilePane('map')
    } catch (error) {
      setRouteError(errorMessage(error))
    } finally {
      setRouteLoading(false)
    }
  }

  return (
    <main className="app-frame">
      <header className="topbar">
        <a className="brand-lockup" href="#shop" onClick={(event) => { event.preventDefault(); navigate('shop') }} aria-label="Market Fast Route, início">
          <span className="brand-mark"><MapPinned aria-hidden="true" /></span>
          <span><strong>Market Fast Route</strong><small>Atlas interativo da loja</small></span>
        </a>
        <nav className="main-nav" aria-label="Navegação principal">
          <button className={activePage === 'shop' ? 'nav-link active' : 'nav-link'} onClick={() => { setMapPresentation('2d'); navigate('shop') }}>Compras</button>
          <button className={activePage === 'admin' ? 'nav-link active' : 'nav-link'} onClick={() => navigate('admin')}>Administração</button>
          <button className={activePage === 'demo' || mapPresentation === '3d' ? 'nav-link active' : 'nav-link'} onClick={() => { setMapPresentation('3d'); navigate('demo') }}>Mercado 3D</button>
        </nav>
        <div className="topbar-note"><span className="status-dot" /> Ambiente local</div>
      </header>

      <section className="shop-layout" aria-label="Navegação de compras">
        <div className="mobile-switch">
          <button type="button" aria-pressed={mobilePane === 'list'} onClick={() => setMobilePane('list')}><ShoppingBasket size={16} /> Lista</button>
          <button type="button" aria-pressed={mobilePane === 'map'} onClick={() => setMobilePane('map')}><Compass size={16} /> Mapa</button>
        </div>

        <aside className={`shopping-panel ${mobilePane === 'map' ? 'mobile-hidden' : ''}`} aria-label="Busca e lista de compras">
          <div className="panel-heading">
            <div><h1>Encontre tudo<br />com um bom caminho.</h1></div>
            <span className="heading-ornament" aria-hidden="true"><Compass /></span>
          </div>

          <label className="field-label" htmlFor="store-select">Loja</label>
          <div className="select-wrap">
            <StoreIcon size={17} aria-hidden="true" />
            <select id="store-select" value={storeId} onChange={(event) => chooseStore(event.target.value)} disabled={storeLoading || !stores.length}>
              {storeLoading && <option value="">Carregando lojas…</option>}
              {!storeLoading && !stores.length && <option value="">Nenhuma loja ativa</option>}
              {stores.map((store) => <option key={store.id} value={store.id}>{store.name} · {store.city}</option>)}
            </select>
            <ChevronDown size={16} aria-hidden="true" />
          </div>
          {currentStoreError && <ErrorNotice message={currentStoreError} onRetry={retryStores} />}
          {selectedStore && <p className="store-address">{selectedStore.address} · {selectedStore.city}, {selectedStore.state}</p>}

          <div className="panel-rule" />
          <div className="search-title-row"><h2>Buscar produtos</h2><span>nome, categoria, SKU ou EAN</span></div>
          <label className="search-field">
            <Search size={18} aria-hidden="true" />
            <span className="sr-only">Buscar produtos por nome, categoria, SKU ou EAN</span>
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="O que você procura?" disabled={!storeId} />
            {search && <button type="button" aria-label="Limpar busca" onClick={() => setSearch('')}><X size={16} /></button>}
            {productsLoading && <LoaderCircle size={17} className="spin" aria-label="Buscando" />}
          </label>

          <div className="search-results" aria-live="polite">
            {currentSearchError ? <ErrorNotice message={currentSearchError} onRetry={retrySearch} /> : null}
            {!currentSearchError && !productsLoading && !products.length && (
              <p className="quiet-state">{search ? 'Nenhum produto corresponde a esta busca.' : 'Pesquise para encontrar itens disponíveis nesta loja.'}</p>
            )}
            {!productsLoading && !currentSearchError && products.map((product) => (
              <article className="product-result" key={product.id}>
                <span className="product-symbol" aria-hidden="true">{product.category.slice(0, 1).toUpperCase()}</span>
                <div className="product-copy"><strong>{product.name}</strong><small>{product.category}{product.brand ? ` · ${product.brand}` : ''}</small><small className="product-code">SKU {product.sku}{product.ean ? ` · EAN ${product.ean}` : ''}</small></div>
                <Button className="add-product" size="icon" variant={listIds.has(product.id) ? 'secondary' : 'outline'} aria-label={listIds.has(product.id) ? `${product.name} já está na lista` : `Adicionar ${product.name}`} disabled={listIds.has(product.id)} onClick={() => void addProduct(product)}>
                  {listIds.has(product.id) ? <Check /> : <Plus />}
                </Button>
              </article>
            ))}
          </div>

          <div className="panel-rule list-rule" />
          <div className="list-heading"><div><h2>Minha lista</h2><span>{shoppingList.length} {shoppingList.length === 1 ? 'produto' : 'produtos'}</span></div><ShoppingBasket size={21} aria-hidden="true" /></div>
          <div className="shopping-lines" aria-live="polite">
            {!shoppingList.length && <p className="quiet-state list-empty">Adicione produtos à lista para organizar sua visita.</p>}
            {shoppingList.map(({ product, quantity, locationState }) => (
              <article className="shopping-line" key={product.id}>
                <div className="line-main"><strong>{product.name}</strong><small>{product.category}</small>
                  {locationState === 'checking' && <small className="location-label">Verificando localização…</small>}
                  {locationState === 'missing' && <small className="location-label warning">Sem localização neste mapa</small>}
                  {locationState === 'unknown' && <small className="location-label">Localização indisponível para consulta</small>}
                  {locationState === 'located' && routeStops.has(product.id) && <small className="location-label route-order">Parada {routeStops.get(product.id)} na rota</small>}
                </div>
                <div className="quantity-control" aria-label={`Quantidade de ${product.name}`}>
                  <button type="button" aria-label={`Diminuir ${product.name}`} onClick={() => changeQuantity(product.id, -1)}><Minus size={14} /></button>
                  <span>{quantity}</span>
                  <button type="button" aria-label={`Aumentar ${product.name}`} onClick={() => changeQuantity(product.id, 1)}><Plus size={14} /></button>
                </div>
                <button type="button" className="remove-line" aria-label={`Remover ${product.name}`} onClick={() => changeQuantity(product.id, -quantity)}><Trash2 size={16} /></button>
              </article>
            ))}
          </div>

          {routeError && <ErrorNotice message={routeError} onRetry={() => void calculateRoute()} />}
          {routeUnavailable && <p className="inline-warning"><AlertCircle size={16} /> Remova os itens sem localização para calcular uma rota completa.</p>}
          <div className="route-action-block">
            <Button className="route-button" size="lg" disabled={!shoppingList.length || routeUnavailable || routeLoading || shoppingList.some(({ locationState }) => locationState === 'checking')} onClick={() => void calculateRoute()}>
              {routeLoading ? <LoaderCircle className="spin" /> : <Compass />}
              {routeLoading ? 'Calculando percurso…' : route ? 'Atualizar percurso' : 'Calcular meu percurso'}
            </Button>
            <span><CircleHelp size={14} /> A ordem das paradas é calculada pela loja.</span>
          </div>
        </aside>

        <section className={`map-workspace ${mobilePane === 'list' ? 'mobile-hidden' : ''}`} aria-label="Mapa e percurso da loja">
          <div className="map-topline">
            <div><h2>{map?.name ?? 'Planta da loja'}</h2></div>
            <div className="map-presentation-switch" role="group" aria-label="Modo de visualização do mapa">
              <button type="button" aria-pressed={mapPresentation === '2d'} onClick={() => { setMapPresentation('2d'); if (activePage === 'demo') navigate('shop') }}>Planta 2D</button>
              <button type="button" aria-pressed={mapPresentation === '3d'} onClick={() => { setMapPresentation('3d'); if (activePage !== 'demo') navigate('demo') }}>Mapa 3D</button>
            </div>
            <span className="map-version">{map ? `Versão ${map.version}` : 'Mapa operacional'}</span>
          </div>
          <div className="map-stage">
            {mapLoading ? <MapLoadingState /> : map?.storeId === storeId ? (mapPresentation === '2d'
              ? <StoreMapSvg map={map} route={route} />
              : <Suspense fallback={<MapLoadingState />}><ThreeMarketScene map={map} route={route} locations={productLocations} onSwitchTo2D={() => setMapPresentation('2d')} /></Suspense>)
              : <MapEmptyState message={currentMapError || (currentStoreError ? 'Recarregue a lista de lojas pelo controle abaixo do seletor.' : 'Selecione uma loja com mapa ativo para visualizar a planta.')} onRetry={retryMap} />}
            {route && <Card className="route-summary" aria-live="polite">
              <Collapsible open={routeSummaryOpen} onOpenChange={setRouteSummaryOpen} className="route-summary-disclosure">
                <CollapsibleTrigger asChild>
                  <Button
                    type="button"
                    variant="ghost"
                    className="route-summary-trigger"
                    aria-label={`${routeSummaryOpen ? 'Recolher' : 'Expandir'} detalhes do percurso`}
                  >
                    <span className="route-icon"><Compass size={18} aria-hidden="true" /></span>
                    <span className="route-summary-copy">
                      <strong>Percurso pronto</strong>
                      <small>{formatStopCount(route.orderedStops.length)} · {formatDistance(Number(route.distanceMeters))}</small>
                    </span>
                    <ChevronDown className="route-summary-chevron" aria-hidden="true" />
                  </Button>
                </CollapsibleTrigger>
                <CollapsibleContent className="route-summary-details">
                  <CardContent className="route-summary-content">
                    <div className="route-endpoint"><span className="endpoint-dot start" /><span><small>COMECE NA</small><strong>{entryLabel}</strong></span><ArrowUp aria-hidden="true" /></div>
                    <ol className="route-stop-list" aria-label="Paradas para encontrar produtos">
                      {route.orderedStops.map((stop) => {
                        const location = routeLocationsById.get(stop.productLocationId)
                        const aisle = map?.aisles.find(({ name }) => name === location?.aisle)
                        const aisleName = aisle?.name.replace(/^Corredor(?:\s+\d+\s+de|\s+de)?\s*/i, '') ?? location?.aisle
                        const aisleLabel = aisle
                          ? `Corredor ${aisle.code} · ${aisleName}`
                          : aisleName
                            ? `Corredor · ${aisleName}`
                            : 'Corredor não identificado'
                        return (
                          <li className="route-stop" key={stop.productLocationId}>
                            <span>{stop.order}</span>
                            <div className="route-stop-copy"><strong>{stop.productName}</strong><small>{aisleLabel}</small></div>
                          </li>
                        )
                      })}
                    </ol>
                    <div className="route-endpoint"><span className="endpoint-dot finish" /><span><small>FINALIZE NOS</small><strong>{checkoutLabel}</strong></span><Check aria-hidden="true" /></div>
                  </CardContent>
                </CollapsibleContent>
              </Collapsible>
            </Card>}
          </div>
          <div className="map-legend"><span><i className="legend-floor" /> Piso livre transitável</span><span><i className="legend-aisle" /> Corredores</span><span><i className="legend-shelf" /> Prateleiras</span><span><i className="legend-route" /> Percurso calculado</span><span><i className="legend-poi">E</i> Pontos da loja</span></div>
          <div className="map-footnote"><span><MapPinned size={15} /> Planta e percurso fornecidos pela API desta loja.</span><span>{map ? `${map.width} × ${map.height} unidades` : ''}</span></div>
        </section>
      </section>
    </main>
  )
}

function ErrorNotice({ message, onRetry }: { message: string; onRetry: () => void }) {
  return <div className="notice error-notice" role="alert"><AlertCircle size={17} /><span>{message}</span><button type="button" onClick={onRetry}>Tentar de novo</button></div>
}

function MapLoadingState() {
  return <div className="map-placeholder" role="status"><LoaderCircle className="spin" /><strong>Carregando a planta…</strong><span>Buscando a versão ativa desta loja.</span></div>
}

function MapEmptyState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return <div className="map-placeholder map-error"><span className="map-placeholder-icon"><MapPinned /></span><strong>Mapa indisponível</strong><span>{message}</span><Button variant="outline" onClick={onRetry}>Recarregar mapa</Button></div>
}

function errorMessage(error: unknown): string {
  return error instanceof ApiRequestError
    ? error.message
    : error instanceof Error
      ? error.message
      : 'Não foi possível concluir a solicitação. Tente novamente.'
}
