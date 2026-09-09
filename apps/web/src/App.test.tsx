import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import App from './App'

describe('App', () => {
  it('renders the technical shell without product features', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: /uma base pronta/i })).toBeInTheDocument()
    expect(screen.getByText(/health check disponível/i)).toBeInTheDocument()
    expect(screen.queryByText(/selecionar loja/i)).not.toBeInTheDocument()
  })
})
