import { ArrowLeft, ArrowUpRight, MapPinned, ShoppingBasket } from 'lucide-react'
import { MarketScene } from './MarketScene'

type Page = 'shop' | 'admin' | 'demo'

export default function DemoView({ activePage, navigate }: { activePage: Page; navigate: (page: Page) => void }) {
  return (
    <main className="app-frame demo-frame">
      <header className="topbar">
        <a className="brand-lockup" href="#shop" onClick={(event) => { event.preventDefault(); navigate('shop') }} aria-label="Market Fast Route, início">
          <span className="brand-mark"><MapPinned aria-hidden="true" /></span><span><strong>Market Fast Route</strong><small>Mercado em três dimensões</small></span>
        </a>
        <nav className="main-nav" aria-label="Navegação principal">
          <button className={activePage === 'shop' ? 'nav-link active' : 'nav-link'} onClick={() => navigate('shop')}>Compras</button>
          <button className={activePage === 'admin' ? 'nav-link active' : 'nav-link'} onClick={() => navigate('admin')}>Administração</button>
          <button className={activePage === 'demo' ? 'nav-link active' : 'nav-link'} onClick={() => navigate('demo')}>Mercado 3D</button>
        </nav>
        <button className="demo-back" onClick={() => navigate('shop')}><ArrowLeft size={16} /> Voltar às compras</button>
      </header>

      <section className="demo-intro">
        <div><h1>Um mercado inventado,<br />feito para explorar.</h1><p>Uma maquete ilustrativa com setores, corredores, curvas e pontos de referência. Gire a cena para perceber como o percurso se organiza no espaço.</p></div>
        <div className="demo-intro-actions"><button onClick={() => navigate('shop')}><ShoppingBasket size={16} /> Montar lista</button><span><ArrowUpRight size={16} /> WebGL sob demanda</span></div>
      </section>

      <MarketScene />
      <section className="demo-note"><strong>O atlas de compras continua operacional.</strong><span>Para buscar produtos e receber uma rota real da loja, use a área de Compras.</span><button onClick={() => navigate('shop')}>Abrir compras <ArrowUpRight size={15} /></button></section>
    </main>
  )
}
