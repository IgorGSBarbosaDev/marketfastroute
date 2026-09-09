import { MapPinned, Server } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export function AppShell() {
  return (
    <main className="min-h-svh bg-background text-foreground">
      <div className="mx-auto flex min-h-svh w-full max-w-5xl flex-col px-6 py-8 sm:px-10 sm:py-12">
        <header className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary text-primary-foreground">
              <MapPinned aria-hidden="true" className="size-5" />
            </div>
            <div>
              <p className="text-sm font-semibold tracking-[0.16em] text-muted-foreground uppercase">
                Market Fast Route
              </p>
              <p className="text-sm text-muted-foreground">Indoor supermarket navigator</p>
            </div>
          </div>
          <Badge variant="secondary">Base técnica</Badge>
        </header>

        <section className="flex flex-1 items-center py-16" aria-labelledby="welcome-title">
          <div className="grid w-full gap-10 lg:grid-cols-[1.25fr_0.75fr] lg:items-center">
            <div className="space-y-6">
              <p className="text-sm font-medium tracking-[0.18em] text-muted-foreground uppercase">
                Fundação do produto
              </p>
              <div className="space-y-4">
                <h1 id="welcome-title" className="max-w-2xl text-4xl font-semibold tracking-tight sm:text-6xl">
                  Uma base pronta para orientar cada compra.
                </h1>
                <p className="max-w-xl text-lg leading-8 text-muted-foreground">
                  O ambiente inicial está configurado para receber as próximas etapas do navegador indoor,
                  mantendo frontend, API e infraestrutura separados com clareza.
                </p>
              </div>
            </div>

            <Card className="border-border/80 bg-card/80 shadow-sm">
              <CardHeader>
                <CardTitle className="text-base">Ambiente configurado</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-start gap-3">
                  <Server aria-hidden="true" className="mt-0.5 size-5 text-muted-foreground" />
                  <div className="space-y-1">
                    <p className="font-medium">API Spring Boot</p>
                    <p className="text-sm text-muted-foreground">Health check disponível em /actuator/health</p>
                  </div>
                </div>
                <div className="border-t pt-4 text-sm text-muted-foreground">
                  Lojas, produtos, mapas e rotas serão adicionados em etapas futuras.
                </div>
              </CardContent>
            </Card>
          </div>
        </section>

        <footer className="border-t pt-5 text-sm text-muted-foreground">
          React + Vite + TypeScript + shadcn/ui
        </footer>
      </div>
    </main>
  )
}
