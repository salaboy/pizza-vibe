// Package oven provides the oven service for the Pizza Vibe application.
// It manages pizza ovens for cooking operations and provides REST endpoints for oven management.
package oven

import "time"

// Oven status constants.
const (
	StatusAvailable = "AVAILABLE"
	StatusReserved  = "RESERVED"
)

// Oven represents a pizza oven with its current state.
type Oven struct {
	ID        string    `json:"id"`
	Status    string    `json:"status"`
	User      string    `json:"user,omitempty"`
	Progress  int       `json:"progress"`
	UpdatedAt time.Time `json:"updatedAt"`
}

// DefaultOvens returns the default set of ovens.
func DefaultOvens() map[string]*Oven {
	now := time.Now()
	return map[string]*Oven{
		"oven-1": {ID: "oven-1", Status: StatusAvailable, UpdatedAt: now},
		"oven-2": {ID: "oven-2", Status: StatusAvailable, UpdatedAt: now},
		"oven-3": {ID: "oven-3", Status: StatusAvailable, UpdatedAt: now},
		"oven-4": {ID: "oven-4", Status: StatusAvailable, UpdatedAt: now},
	}
}
