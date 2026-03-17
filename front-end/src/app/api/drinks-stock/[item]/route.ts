import { NextRequest, NextResponse } from 'next/server';

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ item: string }> }
) {
  try {
    const { item } = await params;
    const drinksStockServiceUrl = process.env.DRINKS_STOCK_SERVICE_URL || 'http://localhost:8090';
    const response = await fetch(`${drinksStockServiceUrl}/drinks-stock/${item}`, {
      method: 'POST',
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to acquire item' },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error acquiring drink item:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
