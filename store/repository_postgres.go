package store

import (
	"database/sql"
	_ "embed"
	"encoding/json"
	"fmt"

	"github.com/google/uuid"
	_ "github.com/jackc/pgx/v5/stdlib"
)

//go:embed schema.sql
var schemaSQL string

// PostgresRepository implements OrderRepository using PostgreSQL.
type PostgresRepository struct {
	db *sql.DB
}

// NewPostgresRepository opens a connection to PostgreSQL, pings it, and runs schema migrations.
func NewPostgresRepository(databaseURL string) (*PostgresRepository, error) {
	db, err := sql.Open("pgx", databaseURL)
	if err != nil {
		return nil, fmt.Errorf("open db: %w", err)
	}
	if err := db.Ping(); err != nil {
		db.Close()
		return nil, fmt.Errorf("ping db: %w", err)
	}
	if _, err := db.Exec(schemaSQL); err != nil {
		db.Close()
		return nil, fmt.Errorf("run schema: %w", err)
	}
	return &PostgresRepository{db: db}, nil
}

// Close closes the database connection.
func (p *PostgresRepository) Close() error {
	return p.db.Close()
}

func (p *PostgresRepository) CreateOrder(order *Order) error {
	orderItems, err := json.Marshal(order.OrderItems)
	if err != nil {
		return fmt.Errorf("marshal order_items: %w", err)
	}
	drinkItems, err := json.Marshal(order.DrinkItems)
	if err != nil {
		return fmt.Errorf("marshal drink_items: %w", err)
	}
	_, err = p.db.Exec(
		`INSERT INTO orders (order_id, order_items, drink_items, order_data, order_status)
		 VALUES ($1, $2, $3, $4, $5)`,
		order.OrderID, orderItems, drinkItems, order.OrderData, order.OrderStatus,
	)
	if err != nil {
		return fmt.Errorf("insert order: %w", err)
	}
	return nil
}

func (p *PostgresRepository) GetOrder(orderID uuid.UUID) (*Order, bool) {
	var order Order
	var orderItemsJSON, drinkItemsJSON []byte
	err := p.db.QueryRow(
		`SELECT order_id, order_items, drink_items, order_data, order_status
		 FROM orders WHERE order_id = $1`, orderID,
	).Scan(&order.OrderID, &orderItemsJSON, &drinkItemsJSON, &order.OrderData, &order.OrderStatus)
	if err != nil {
		return nil, false
	}
	json.Unmarshal(orderItemsJSON, &order.OrderItems)
	json.Unmarshal(drinkItemsJSON, &order.DrinkItems)
	return &order, true
}

func (p *PostgresRepository) GetAllOrders() ([]*Order, error) {
	rows, err := p.db.Query(
		`SELECT order_id, order_items, drink_items, order_data, order_status FROM orders`,
	)
	if err != nil {
		return nil, fmt.Errorf("query orders: %w", err)
	}
	defer rows.Close()

	var orders []*Order
	for rows.Next() {
		var order Order
		var orderItemsJSON, drinkItemsJSON []byte
		if err := rows.Scan(&order.OrderID, &orderItemsJSON, &drinkItemsJSON, &order.OrderData, &order.OrderStatus); err != nil {
			return nil, fmt.Errorf("scan order: %w", err)
		}
		json.Unmarshal(orderItemsJSON, &order.OrderItems)
		json.Unmarshal(drinkItemsJSON, &order.DrinkItems)
		orders = append(orders, &order)
	}
	if orders == nil {
		orders = []*Order{}
	}
	return orders, rows.Err()
}

func (p *PostgresRepository) UpdateOrderStatus(orderID uuid.UUID, status string) bool {
	result, err := p.db.Exec(
		`UPDATE orders SET order_status = $1 WHERE order_id = $2`,
		status, orderID,
	)
	if err != nil {
		return false
	}
	n, _ := result.RowsAffected()
	return n > 0
}

func (p *PostgresRepository) TrackEvent(orderID uuid.UUID, event OrderEvent) error {
	_, err := p.db.Exec(
		`INSERT INTO order_events (order_id, status, source, message, tool_name, tool_input)
		 VALUES ($1, $2, $3, $4, $5, $6)`,
		orderID, event.Status, event.Source, event.Message, event.ToolName, event.ToolInput,
	)
	if err != nil {
		return fmt.Errorf("insert event: %w", err)
	}
	return nil
}

func (p *PostgresRepository) GetOrderEvents(orderID uuid.UUID) ([]OrderEvent, error) {
	rows, err := p.db.Query(
		`SELECT order_id, status, source, message, tool_name, tool_input
		 FROM order_events WHERE order_id = $1 ORDER BY id ASC`, orderID,
	)
	if err != nil {
		return nil, fmt.Errorf("query events: %w", err)
	}
	defer rows.Close()

	var events []OrderEvent
	for rows.Next() {
		var e OrderEvent
		var message, toolName, toolInput sql.NullString
		if err := rows.Scan(&e.OrderID, &e.Status, &e.Source, &message, &toolName, &toolInput); err != nil {
			return nil, fmt.Errorf("scan event: %w", err)
		}
		e.Message = message.String
		e.ToolName = toolName.String
		e.ToolInput = toolInput.String
		events = append(events, e)
	}
	return events, rows.Err()
}

func (p *PostgresRepository) TrackAgentEvent(event AgentEvent) error {
	_, err := p.db.Exec(
		`INSERT INTO agent_events (agent_id, kind, text, timestamp)
		 VALUES ($1, $2, $3, $4)`,
		event.AgentID, event.Kind, event.Text, event.Timestamp.Time,
	)
	if err != nil {
		return fmt.Errorf("insert agent event: %w", err)
	}
	return nil
}

func (p *PostgresRepository) GetAgentEventsByAgentID(agentID string) ([]AgentEvent, error) {
	rows, err := p.db.Query(
		`SELECT agent_id, kind, text, timestamp
		 FROM agent_events WHERE agent_id = $1 ORDER BY id ASC`, agentID,
	)
	if err != nil {
		return nil, fmt.Errorf("query agent events by agent: %w", err)
	}
	defer rows.Close()

	var events []AgentEvent
	for rows.Next() {
		var e AgentEvent
		if err := rows.Scan(&e.AgentID, &e.Kind, &e.Text, &e.Timestamp.Time); err != nil {
			return nil, fmt.Errorf("scan agent event: %w", err)
		}
		events = append(events, e)
	}
	return events, rows.Err()
}

func (p *PostgresRepository) GetAllAgentEvents() ([]AgentEvent, error) {
	rows, err := p.db.Query(
		`SELECT agent_id, kind, text, timestamp
		 FROM agent_events ORDER BY id ASC`,
	)
	if err != nil {
		return nil, fmt.Errorf("query all agent events: %w", err)
	}
	defer rows.Close()

	var events []AgentEvent
	for rows.Next() {
		var e AgentEvent
		if err := rows.Scan(&e.AgentID, &e.Kind, &e.Text, &e.Timestamp.Time); err != nil {
			return nil, fmt.Errorf("scan agent event: %w", err)
		}
		events = append(events, e)
	}
	return events, rows.Err()
}

func (p *PostgresRepository) DeleteAllAgentEvents() error {
	_, err := p.db.Exec(`DELETE FROM agent_events`)
	if err != nil {
		return fmt.Errorf("delete agent events: %w", err)
	}
	return nil
}
