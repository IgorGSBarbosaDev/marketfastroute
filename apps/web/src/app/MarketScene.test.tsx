import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const sceneHarness = vi.hoisted(() => ({ available: false, setAnimationLoop: vi.fn(), render: vi.fn() }))

vi.mock('three/addons/capabilities/WebGL.js', () => ({
  default: { isWebGL2Available: () => sceneHarness.available },
}))

vi.mock('three', async (importOriginal) => {
  const actual = await importOriginal<typeof import('three')>()
  return {
    ...actual,
    WebGLRenderer: class {
      domElement = document.createElement('canvas')
      outputColorSpace = actual.SRGBColorSpace
      toneMapping = actual.ACESFilmicToneMapping
      toneMappingExposure = 1
      setPixelRatio() {}
      setSize() {}
      render() { sceneHarness.render() }
      dispose() {}
      setAnimationLoop(callback: ((time: number) => void) | null) { sceneHarness.setAnimationLoop(callback) }
    },
  }
})

import { MarketScene } from './MarketScene'

describe('MarketScene resilience and motion', () => {
  afterEach(() => vi.useRealTimers())

  beforeEach(() => {
    sceneHarness.available = false
    sceneHarness.setAnimationLoop.mockClear()
    sceneHarness.render.mockClear()
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: vi.fn().mockReturnValue({ matches: false, addEventListener: vi.fn(), removeEventListener: vi.fn() }),
    })
  })

  it('keeps the fictional market legible when WebGL2 is unavailable', () => {
    render(<MarketScene />)

    expect(screen.getByText(/não conseguiu iniciar WebGL/)).toBeInTheDocument()
    const fallbackMap = screen.getByText(/não conseguiu iniciar WebGL/).nextElementSibling
    expect(fallbackMap).toHaveTextContent('Hortifruti')
    expect(fallbackMap).toHaveTextContent('Congelados')
    expect(fallbackMap).toHaveTextContent('Entrada')
    expect(fallbackMap).toHaveTextContent('Caixas')
    expect(screen.queryByRole('button', { name: /movimento/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reiniciar posição da cena' })).not.toBeInTheDocument()
  })

  it('starts paused when reduced motion is preferred', () => {
    sceneHarness.available = true
    Object.defineProperty(window, 'ResizeObserver', {
      configurable: true,
      value: class {
        observe() {}
        disconnect() {}
      },
    })
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: vi.fn().mockReturnValue({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() }),
    })

    render(<MarketScene />)

    expect(screen.getByRole('button', { name: 'Reproduzir movimento' })).not.toHaveAttribute('aria-pressed')
  })

  it('stops the renderer loop while paused and redraws for keyboard input', () => {
    sceneHarness.available = true
    Object.defineProperty(window, 'ResizeObserver', {
      configurable: true,
      value: class {
        observe() {}
        disconnect() {}
      },
    })

    render(<MarketScene />)
    expect(sceneHarness.setAnimationLoop).toHaveBeenLastCalledWith(expect.any(Function))

    fireEvent.click(screen.getByRole('button', { name: 'Pausar movimento' }))
    expect(sceneHarness.setAnimationLoop).toHaveBeenLastCalledWith(null)
    const renderCountAtPause = sceneHarness.render.mock.calls.length

    screen.getByRole('img', { name: /Modelo tridimensional esquemático/ }).focus()
    fireEvent.keyDown(screen.getByRole('img', { name: /Modelo tridimensional esquemático/ }), { key: 'ArrowRight' })
    expect(sceneHarness.render).toHaveBeenCalledTimes(renderCountAtPause + 1)

    fireEvent.click(screen.getByRole('button', { name: 'Reproduzir movimento' }))
    expect(sceneHarness.setAnimationLoop).toHaveBeenLastCalledWith(expect.any(Function))
  })

  it('pauses the short presentation after eight seconds', () => {
    sceneHarness.available = true
    Object.defineProperty(window, 'ResizeObserver', {
      configurable: true,
      value: class {
        observe() {}
        disconnect() {}
      },
    })
    vi.useFakeTimers()

    render(<MarketScene />)
    expect(screen.getByRole('button', { name: 'Pausar movimento' })).toBeInTheDocument()

    act(() => vi.advanceTimersByTime(8_000))

    expect(screen.getByRole('button', { name: 'Reproduzir movimento' })).toBeInTheDocument()
    expect(sceneHarness.setAnimationLoop).toHaveBeenLastCalledWith(null)
  })
})
