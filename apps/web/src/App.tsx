import { useCallback, useEffect, useState } from 'react'
import { AdminView } from '@/app/AdminView'
import { CustomerView } from '@/app/CustomerView'

type Page = 'shop' | 'admin' | 'demo'

function pageFromHash(): Page {
  const value = window.location.hash.replace(/^#\/?/, '')
  return value === 'admin' || value === 'demo' ? value : 'shop'
}

function App() {
  const [page, setPage] = useState<Page>(pageFromHash)

  useEffect(() => {
    const update = () => setPage(pageFromHash())
    if (!window.location.hash) window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}#shop`)
    window.addEventListener('hashchange', update)
    return () => window.removeEventListener('hashchange', update)
  }, [])

  const navigate = useCallback((nextPage: Page) => {
    if (page === nextPage) return
    window.location.hash = nextPage
    setPage(nextPage)
  }, [page])

  if (page === 'admin') return <AdminView activePage={page} navigate={navigate} />
  return <CustomerView activePage={page} navigate={navigate} />
}

export default App
