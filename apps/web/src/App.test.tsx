import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import App from './App'

describe('App navigation', () => {
  it('opens the customer task first and exposes the three main areas', () => {
    window.location.hash = '#shop'
    render(<App />)

    expect(screen.getByRole('heading', { name: /encontre tudo/i })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Navegação principal' })).toHaveTextContent('Compras')
    expect(screen.getByRole('button', { name: 'Administração' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Mercado 3D' })).toBeInTheDocument()
    expect(screen.queryByText(/base técnica/i)).not.toBeInTheDocument()
  })
})
