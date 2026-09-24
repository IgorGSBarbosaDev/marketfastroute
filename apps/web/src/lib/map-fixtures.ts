import type { ShelfBlock } from '@/types/api'

export type FixtureKind = 'gondola' | 'wall' | 'produce' | 'bakery' | 'chiller' | 'freezer'
  | 'butcher' | 'service-wall' | 'worktop' | 'seating' | 'ticket' | 'checkout' | 'entrance' | 'exit' | 'carts'

// Presentation convention only: no routing or catalog rules depend on these names.
export function fixtureKind(block: Pick<ShelfBlock, 'name'>, sectorName = ''): FixtureKind {
  const name = (block.name ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
  if (name.startsWith('balcao de acougue')) return 'butcher'
  if (name.startsWith('parede de servico')) return 'service-wall'
  if (name.startsWith('bancada de preparo') || name.startsWith('balcao de apoio')) return 'worktop'
  if (name.startsWith('cadeiras')) return 'seating'
  if (name.startsWith('totem de senhas')) return 'ticket'
  if (name.startsWith('caixa paralelo')) return 'checkout'
  if (name.startsWith('portal de entrada')) return 'entrance'
  if (name.startsWith('portal de saida')) return 'exit'
  if (name.startsWith('estacao de carrinhos')) return 'carts'
  if (name.startsWith('freezer')) return 'freezer'
  if (name.startsWith('refrigerador')) return 'chiller'
  if (name.startsWith('prateleira mural')) return 'wall'
  if (name.startsWith('ilha') || /hortifruti/i.test(sectorName)) return 'produce'
  if (name.startsWith('padaria') || /padaria/i.test(sectorName)) return 'bakery'
  return 'gondola'
}

export const fixtureColors: Record<FixtureKind, { fill: string; edge: string; detail: string }> = {
  gondola: { fill: '#6e8084', edge: '#3f5962', detail: '#dce3db' },
  wall: { fill: '#89948a', edge: '#455f58', detail: '#e0e5d9' },
  produce: { fill: '#9cac78', edge: '#53683c', detail: '#e7eacb' },
  bakery: { fill: '#b6966d', edge: '#795b39', detail: '#eee1c8' },
  chiller: { fill: '#b3cfd3', edge: '#416978', detail: '#eaf6f5' },
  freezer: { fill: '#92bdcb', edge: '#355f73', detail: '#e4f3f7' },
  butcher: { fill: '#c79687', edge: '#805147', detail: '#f2e4db' },
  'service-wall': { fill: '#758888', edge: '#3c545a', detail: '#e5e7dc' },
  worktop: { fill: '#bfc9c5', edge: '#4c656a', detail: '#edf0e7' },
  seating: { fill: '#b5c5b1', edge: '#4c6555', detail: '#eef0e1' },
  ticket: { fill: '#315f70', edge: '#284957', detail: '#f2dfb5' },
  checkout: { fill: '#78988e', edge: '#365b54', detail: '#eee4c4' },
  entrance: { fill: '#b4cdb8', edge: '#456e50', detail: '#f2f4e8' },
  exit: { fill: '#b2c6ce', edge: '#345768', detail: '#eef3ef' },
  carts: { fill: '#b8c5c7', edge: '#4c6068', detail: '#f2f2e9' },
}
