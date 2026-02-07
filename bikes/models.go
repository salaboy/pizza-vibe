package bikes

import "time"

const (
	StatusAvailable = "AVAILABLE"
	StatusReserved  = "RESERVED"
)

type Bike struct {
	ID        string    `json:"id"`
	Status    string    `json:"status"`
	User      string    `json:"user,omitempty"`
	UpdatedAt time.Time `json:"updatedAt"`
}

type ReserveRequest struct {
	User string `json:"user"`
}

type BikeEvent struct {
	BikeID    string    `json:"bikeId"`
	Status    string    `json:"status"`
	User      string    `json:"user"`
	Timestamp time.Time `json:"timestamp"`
}

func DefaultBikes() map[string]*Bike {
	now := time.Now()
	return map[string]*Bike{
		"bike-1": {ID: "bike-1", Status: StatusAvailable, UpdatedAt: now},
		"bike-2": {ID: "bike-2", Status: StatusAvailable, UpdatedAt: now},
		"bike-3": {ID: "bike-3", Status: StatusAvailable, UpdatedAt: now},
	}
}
