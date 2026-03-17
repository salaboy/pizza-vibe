# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pizza Vibe is agentic pizza store, which uses Langchain4j, Quarkus and Dapr Workflows to provide a seamless experience for customers.

## Build and Run Commands

```bash
# Build the application
go build -o pizza-vibe ./...

# Run the application
go run .

# Run tests
go test ./...

# Run a specific test
go test -run TestName ./path/to/package

# Run tests with coverage
go test -coverprofile=coverage.out ./...

# Format code
go fmt ./...

# Vet code for issues
go vet ./...

# Run docker-compose validation tests
./scripts/test-docker-compose.sh

# Build and run with docker-compose
docker-compose build
docker-compose up -d
docker-compose down
```

## Architecture

The Pizza application is composed of: 
- Five services written in Go: bikes, drinks-stock, inventory, oven, store 
- Three agents written in Java with Quarkus: cooking-agent, delivery-agent, store-mgmt-agent
- MCP Server using Quarkus: pizza-mcp 

The front-end is written in React and the back-end is written in Go.



## Best practices

General:
- Do not do more than what is asked for

Frontend:
- Everytime that you send a request to the store service validate the data types to make sure that the request is valid.
- Use the store service data types (@store/models.go) to create mock data for the jest tests.
- Always use Fetch to call other services using http.
- Do not add styles unless it is specified by the user.
- When creating content in pages, only add what is explicitly requested or ask if recommending additional content is needed.
- Never add styles unless specifically requested by the user.
- `globals.css` must only contain global element styles (headings, paragraphs, links, body, resets). Component-specific styles must go in CSS module files (e.g., `Navigation.module.css`).
- Before working on the frontend, check `front-end/figma-nodes.md` for the Figma URL tree. Ask the user if new pages/nodes have been added.

### Frontend Work Modes

The frontend has two distinct work modes. Always confirm which mode is active before making changes:

**1. Component Creation/Update Mode**
- Focus: Building and updating the component library.
- Only modify files inside `src/components/` and the Components Showcase page (`src/app/components/page.tsx`).
- Do NOT modify any application pages or layouts (e.g., `layout.tsx`, `page.tsx`, route pages).
- Every new or updated component must be added to the Components Showcase page for visual validation.

**2. Page Design Mode**
- Focus: Designing application pages and layouts using the existing component library.
- Use the created components to build pages and update layouts.
- May modify `layout.tsx`, route pages, and other application files.
- Avoid creating new components in this mode — if a component is missing, switch to Component Creation mode first.
- This mode has two sub-modes. Always confirm which sub-mode is active before making changes:

  **2a. Figma-Based Page Creation**
  - Focus: Implementing pages that have an existing design in the Figma file.
  - Before starting, fetch the page design from Figma via the MCP server (`get_design_context` on the page node).
  - Document every page node in `front-end/figma-pages-nodes.md` (similar to `figma-nodes.md` for components) for reference and to track future design updates.
  - Follow the Figma design exactly — layout, spacing, typography, and colors must match the design using tokens from `tokens.css`.
  - On subsequent visits, check `figma-pages-nodes.md` and ask the user if new page nodes have been added or if existing designs have changed.

  **2b. Vibe Coding Page Creation**
  - Focus: Building pages from scratch based on user instructions, without a Figma reference.
  - Use only the existing component library to compose the page.
  - The user describes what the page needs and the developer builds it using best judgment for layout and composition.
  - Still use design tokens from `tokens.css` for any styling — no hardcoded values.
  - No Figma lookup is required, but the Figma strict style rules still apply (all values must come from `tokens.css`).

### Component Organization

- Each component must live in its own folder: `src/components/<ComponentName>/`
  - `<ComponentName>.tsx` — the component
  - `<ComponentName>.module.css` — styles (using tokens from `tokens.css`)
  - `index.ts` — barrel export (`export { default } from './<ComponentName>'`)
- Child/sub-components must be nested inside their parent component's folder:
  - Example: `src/components/Header/HeaderNavItem/HeaderNavItem.tsx`
- Images and SVG assets go in `public/images/`.

Backend:
- Always keep update the docker-compose.yaml file with all the services of the application.
- Run `./scripts/test-docker-compose.sh` to validate docker-compose changes before committing.
- Always provide Kubernetes manifests for each service and infrastructure component.
- Always implement Dockerfile for each service


## Figma Design System Integration (STRICT MODE)

The frontend design system is managed through Figma via MCP server connection. This is a **strict** workflow - no exceptions.

### Figma Connection Details
- **File URL**: https://www.figma.com/design/Iia6bIqfQwSvXxTnfedTXj/PizzaVibe-UI-Kit
- **File Key**: `Iia6bIqfQwSvXxTnfedTXj`
- **Tokens File**: `front-end/src/app/tokens.css`

### Available Token Categories and Figma Node IDs
- Typography (node: `1:2`) - H1 (Knewave), H2, H3, Body Default, Body Small (Geist)
- Colors - Text (node: `98:3`) - default, subtle, primary, secondary, tertiary, inverted
- Colors - Background (from nodes: `98:24`, `103:130`, `103:138`) - default, primary, secondary, inverted
- Colors - Border (node: `98:15`) - default, subtle
- Border Widths (node: `98:65`) - thin, default, thick, thicker
- Spacing (nodes: `98:24` padding, `98:44` margin, `98:51` gap)
- Spacing Scale (node: `102:100`) - s, m, l, xl, xxl
- Corners (node: `127:20`) - s, m, l, xl
- Cover (node: `7:2`) - brand showcase
- **File URL**: https://www.figma.com/design/Iia6bIqfQwSvXxTnfedTXj/Project-Library
- **File Key**: `Iia6bIqfQwSvXxTnfedTXj`
- **Tokens File**: `front-end/src/app/tokens.css`

### Available Token Categories (update as Figma pages are added)
- Typography (node: `0:1`) - H1, H2, H3 headings, body text

### Rules for Style Management

**STRICT: Never hardcode style values.** All visual properties must use CSS variables from `tokens.css`:
- Colors (hex, rgb, hsl, etc.) → Must use `--color-*` variables
- Font sizes → Must use `--type-*-font-size` variables
- Line heights → Must use `--type-*-line-height` variables
- Font weights → Must use `--type-*-font-weight` variables
- Spacing (padding, margin, gap) → Must use `--space-*` variables
- Border widths → Must use `--border-width-*` variables
- Border colors → Must use `--color-border-*` variables
- Border radius → Must use `--corner-*` variables

**STRICT: Check Figma before any style work.** Before adding or modifying any styles:
1. Call `mcp__figma-remote-mcp__get_variable_defs` on the relevant Figma node
2. Verify the token exists in `tokens.css`
3. If token doesn't exist, inform the user and do NOT proceed with hardcoded values

**STRICT: Block non-compliant changes.** If a style change is requested but no corresponding Figma token exists:
1. Stop and inform the user
2. Explain which token is missing
3. Ask user to add it to Figma first, then request a sync

### Sync Workflow

**Manual sync** - When user says "sync styles from Figma" or "check Figma for updates":
1. Call `get_variable_defs` on all known Figma nodes (listed above)
2. Compare with current `tokens.css`
3. Report: new tokens, changed values, removed tokens
4. Update `tokens.css` only with user approval

**Automatic check** - Before any style-related task:
1. Read current `tokens.css`
2. Verify required tokens exist
3. If missing, trigger sync workflow

### Adding New Token Categories

When user adds new pages to Figma (colors, spacing, components, etc.):
1. User provides the Figma URL with the new page/node
2. Call `get_metadata` to understand the structure
3. Call `get_variable_defs` to extract tokens
4. Add new tokens to `tokens.css` under appropriate section
5. Update this CLAUDE.md with the new category and node ID
6. If it's a new category (e.g., colors), add corresponding global styles to `globals.css` if needed

### Component Styles

For component-specific styles:
1. Use CSS modules (`.module.css` files)
2. Component styles must still use variables from `tokens.css`
3. When implementing a Figma component, use `get_design_context` on the selected component node to get the exact styles
4. Never hardcode values even in component CSS modules

### Validation Check

To verify compliance, scan for violations:
- Hardcoded hex colors: `#[0-9a-fA-F]{3,8}`
- Hardcoded rgb/hsl: `rgb\(|hsl\(`
- Hardcoded pixels for spacing/sizing (except in tokens.css)
- CSS variables used but not defined in tokens.css
