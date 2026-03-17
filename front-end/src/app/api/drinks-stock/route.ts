import { NextResponse } from 'next/server';

export async function GET() {
  try {
    const drinksStockServiceUrl = process.env.DRINKS_STOCK_SERVICE_URL || 'http://localhost:8090';
    const response = await fetch(`${drinksStockServiceUrl}/drinks-stock`);

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch drinks stock' },
        { status: response.status }
      );
    }

    const data = await response.json();

    // Convert map to array format expected by frontend
    const stockArray = Object.entries(data).map(([item, quantity]) => ({
      item,
      quantity,
    }));

    return NextResponse.json(stockArray);
  } catch (error) {
    console.error('Error fetching drinks stock:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
