---
name: bikes
description: Interact with the Pizza Vibe bikes service. Use when the user wants to list bikes, check a bike's status, or reserve a bike for delivery.
argument-hint: "[list | get <bikeId> | reserve <bikeId> <user>]"
---

# Bikes Service

Interact with the bikes delivery service to list, inspect, and reserve bikes.

## Available commands

Determine the action from `$ARGUMENTS`:

- **list** — List all bikes and their status
- **get `<bikeId>`** — Get the status of a specific bike
- **reserve `<bikeId>` `<user>`** — Reserve a bike for a user

If no arguments are provided, default to **list**.

## How to run

The scripts are located relative to this skill file. Set `BIKES_URL` from the environment or default to `http://localhost:8088`.

### List all bikes

```bash
bash skills/bikes/scripts/list-bikes.sh "${BIKES_URL:-http://localhost:8088}"
```

### Get a specific bike

```bash
bash skills/bikes/scripts/get-bike.sh "<bikeId>" "${BIKES_URL:-http://localhost:8088}"
```

### Reserve a bike

```bash
bash skills/bikes/scripts/reserve-bike.sh "<bikeId>" "<user>" "${BIKES_URL:-http://localhost:8088}"
```

## Response format

All scripts output JSON from the bikes service. Parse and present the results to the user in a readable format.

### Bike fields

| Field       | Description                              |
|-------------|------------------------------------------|
| `id`        | Bike identifier (e.g. `bike-1`)          |
| `status`    | `AVAILABLE` or `RESERVED`                |
| `user`      | User who reserved the bike (if reserved) |
| `updatedAt` | Timestamp of the last status change      |

## Notes

- A bike must be `AVAILABLE` to be reserved.
- Reserved bikes automatically become `AVAILABLE` after 10–20 seconds.
- While reserved, the bike emits delivery events to the store service.
