import { NextResponse } from 'next/server';

export async function GET() {
  try {
    const inventoryServiceUrl = process.env.INVENTORY_SERVICE_URL || 'http://localhost:8084';
    const response = await fetch(`${inventoryServiceUrl}/inventory`);

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch inventory' },
        { status: response.status }
      );
    }

    const data = await response.json();

    // Convert map to array format expected by frontend
    const inventoryArray = Object.entries(data).map(([item, quantity]) => ({
      item,
      quantity,
    }));

    return NextResponse.json(inventoryArray);
  } catch (error) {
    console.error('Error fetching inventory:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
