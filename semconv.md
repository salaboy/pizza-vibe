# Pizza Vibe — Semantic Convention Plan (OTel Weaver)

## Overview

Use [OpenTelemetry Weaver](https://github.com/open-telemetry/weaver) to define a typed, versioned semantic convention registry for Pizza Vibe. Weaver generates plain Java constants and Go constants from YAML definitions, which are checked into the repo and consumed by all services and agents.

---

## Repository Structure

```
semconv/                          ← Weaver registry (new)
  manifest.yaml
  model/
    pizza/
      attributes.yaml
      spans.yaml
      metrics.yaml
    agent/
      attributes.yaml
      spans.yaml
      metrics.yaml
    skill/
      attributes.yaml
      spans.yaml
  templates/
    java/
      weaver.yaml
      attributes.j2
      spans.j2
      metrics.j2
    go/
      weaver.yaml
      attributes.j2
      spans.j2
      metrics.j2

agents/
  pizza-semconv/                  ← new plain Maven JAR (no Quarkus)
    pom.xml                         only dep: opentelemetry-api
    src/main/java/com/pizzavibe/semconv/
      PizzaAttributes.java          ← checked-in generated files
      PizzaAgentAttributes.java
      PizzaSkillAttributes.java
      PizzaSpans.java
      PizzaAgentSpans.java
      PizzaSkillSpans.java
      PizzaMetrics.java

internal/
  semconv/                        ← new Go package (checked-in generated)
    pizza_attributes.go
    pizza_agent_attributes.go
    pizza_skill_attributes.go
    pizza_spans.go
    pizza_metrics.go

Makefile                          ← root Makefile with `make semconv` target
```

Generated files are **checked in**. Run `make semconv` after changing YAML to regenerate. No `weaver` CLI required at Maven/Go build time.

---

## Attribute Definitions

### `pizza.*` — core domain

| Attribute | Type | Values / Notes |
|---|---|---|
| `pizza.order.id` | `string` | UUID propagated across all services |
| `pizza.order.status` | enum | `pending`, `cooking`, `cooked`, `delivering`, `delivered` |
| `pizza.type` | enum | `pepperoni`, `pineapple`, `margherita` |
| `pizza.quantity` | `int` | quantity ordered |
| `pizza.ingredient.name` | enum | `pepperoni`, `pineapple`, `pizza_dough`, `mozzarella`, `sauce` |
| `pizza.ingredient.status` | enum | `acquired`, `empty` |
| `pizza.oven.id` | `string` | e.g. `oven-1`, `oven-2` |
| `pizza.oven.status` | enum | `available`, `reserved` |
| `pizza.oven.progress` | `int` | 0–100 cooking progress % |
| `pizza.bike.id` | `string` | e.g. `bike-1`, `bike-2` |
| `pizza.bike.status` | enum | `available`, `reserved` |
| `pizza.drink.type` | enum | `beer`, `coke`, `diet_coke`, `orange_juice` |

### `pizza.agent.*` — A2A agent layer

| Attribute | Type | Values / Notes |
|---|---|---|
| `pizza.agent.name` | enum | `cooking-agent`, `delivery-agent`, `store-mgmt-agent` |
| `pizza.agent.action` | enum | `cook`, `deliver`, `process_order`, `chat` |
| `pizza.agent.invocation.id` | `string` | A2A task ID if available |

### `pizza.skill.*` — replaces existing ad-hoc `skill.*` attributes

| Attribute | Type | Notes |
|---|---|---|
| `pizza.skill.name` | enum | `bikes`, `inventory`, `ovens`, `drinks` |
| `pizza.skill.script` | `string` | script filename |
| `pizza.skill.script.path` | `string` | full path |

---

## Span Definitions

| Span name | Kind | Required attributes | Where |
|---|---|---|---|
| `pizza.order.place` | `server` | `pizza.order.id`, `pizza.type`, `pizza.quantity` | store: `POST /order` |
| `pizza.order.process` | `internal` | `pizza.order.id` | store-mgmt-agent: workflow (NEW span) |
| `pizza.agent.invoke` | `client` | `pizza.order.id`, `pizza.agent.name`, `pizza.agent.action` | store-mgmt-agent: A2A calls (NEW span) |
| `pizza.cooking` | `internal` | `pizza.order.id`, `pizza.type` | cooking-agent: handler (NEW span) |
| `pizza.ingredient.acquire` | `client` | `pizza.order.id`, `pizza.ingredient.name`, `pizza.ingredient.status` | pizza-mcp: `acquireItem` (NEW span) |
| `pizza.oven.reserve` | `client` | `pizza.order.id`, `pizza.oven.id` | pizza-mcp: `reserveOven` (NEW span) |
| `pizza.oven.cook` | `internal` | `pizza.order.id`, `pizza.oven.id`, `pizza.oven.progress` | pizza-mcp: `getOven` poll (NEW span) |
| `pizza.delivery` | `internal` | `pizza.order.id` | delivery-agent: handler (NEW span) |
| `pizza.bike.reserve` | `client` | `pizza.order.id`, `pizza.bike.id` | delivery-agent: skill (NEW span) |
| `pizza.skill.execute` | `internal` | `pizza.order.id`, `pizza.skill.name`, `pizza.skill.script` | delivery-agent: BikeTools (REPLACES existing `skill.execute`) |

---

## Metric Definitions

| Metric name | Instrument | Unit | Attributes |
|---|---|---|---|
| `pizza.orders` | `counter` | `{order}` | `pizza.type`, `pizza.order.status` |
| `pizza.order.duration` | `histogram` | `s` | `pizza.type`, `pizza.order.status` |
| `pizza.cooking.duration` | `histogram` | `s` | `pizza.type`, `pizza.oven.id` |
| `pizza.delivery.duration` | `histogram` | `s` | `pizza.bike.id` |
| `pizza.ingredient.stock` | `gauge` | `{unit}` | `pizza.ingredient.name` |
| `pizza.oven.utilization` | `gauge` | `1` | `pizza.oven.id` |
| `pizza.agent.invocations` | `counter` | `{invocation}` | `pizza.agent.name`, `pizza.agent.action` |
| `pizza.agent.duration` | `histogram` | `s` | `pizza.agent.name`, `pizza.agent.action` |

---

## Implementation Phases

### Phase 1 — Registry & Code Generation

1. Create `semconv/manifest.yaml` depending on `open-telemetry/semantic-conventions@v1.40.0`
2. Write YAML files for all three namespaces (`pizza`, `pizza.agent`, `pizza.skill`)
3. Write Jinja2 templates under `semconv/templates/java/` and `semconv/templates/go/`
4. Run `weaver registry generate` to produce Java and Go constants
5. Create `agents/pizza-semconv/pom.xml` (plain JAR, only `opentelemetry-api` dependency)
6. Place generated Java files under `agents/pizza-semconv/src/main/java/com/pizzavibe/semconv/`
7. Place generated Go files under `internal/semconv/`
8. Add root `Makefile` target `semconv` that runs weaver for both targets
9. Add `weaver registry check` to CI

### Phase 2 — Go Services (attribute enrichment only, no new spans)

All Go services already have HTTP spans via `otelhttp`. Changes are attribute additions on existing spans:

- **store**: add `pizza.order.id` to `POST /order` span; record `pizza.orders` counter on order creation; record `pizza.order.duration` histogram on DELIVERED event
- **inventory**: add `pizza.ingredient.name`, `pizza.ingredient.status` to `POST /inventory/{item}` span; expose `pizza.ingredient.stock` gauge
- **oven**: add `pizza.oven.id`, `pizza.oven.status`, `pizza.oven.progress` to oven endpoint spans; expose `pizza.oven.utilization` gauge
- **bikes**: add `pizza.order.id`, `pizza.bike.id` to reservation span; record `pizza.delivery.duration` histogram on DELIVERED event
- **drinks-stock**: add `pizza.drink.type`, `pizza.ingredient.status` to acquire span

### Phase 3 — Quarkus Agents (two parts)

**Part A — constants only (no new spans):**

The delivery-agent has exactly **one** existing manual span. Replace its string literals with generated constants:

```java
// Before
tracer.spanBuilder("skill.execute")
    .setAttribute("skill.name", "bikes")
    .setAttribute("skill.script", scriptName)
    .setAttribute("skill.script.path", scriptPath)

// After
tracer.spanBuilder(PizzaSkillSpans.PIZZA_SKILL_EXECUTE)
    .setAttribute(PizzaSkillAttributes.PIZZA_SKILL_NAME, "bikes")
    .setAttribute(PizzaSkillAttributes.PIZZA_SKILL_SCRIPT, scriptName)
    .setAttribute(PizzaSkillAttributes.PIZZA_SKILL_SCRIPT_PATH, scriptPath)
```

Add `pizza-semconv` dependency to all 4 agent `pom.xml` files.

**Part B — new spans in Quarkus (separate decision, more invasive):**

The spans marked "NEW span" in the span table above require injecting `Tracer` into workflow/handler classes and wrapping business logic. This is real instrumentation work — confirm before implementing.

### Phase 4 — Validation

- Run `weaver registry live-check` against the OTLP collector to verify emitted telemetry matches registry definitions
- Add `weaver registry diff` to CI to catch breaking attribute name changes before merge

---

## Key Design Decisions

- **`pizza.order.id` as the golden thread** — propagate as W3C Baggage so every downstream service can attach it without explicit parameter passing
- **Checked-in generated files** — no `weaver` CLI required at Maven/Go build time; regenerate manually via `make semconv` when YAML changes
- **`pizza.skill.*` replaces `skill.*`** — the one existing manual span in the agents uses non-namespaced keys; migration brings it under the typed `pizza` namespace
- **Separate `pizza.agent.*` from `gen_ai.*`** — LangChain4j already emits `gen_ai.*` for LLM calls; `pizza.agent.*` attributes go on the A2A business layer above the LLM
- **Enums over raw strings** — pizza types, ingredient names, order statuses, agent names are all finite sets; Weaver enums prevent typos and enable exhaustive dashboard queries
