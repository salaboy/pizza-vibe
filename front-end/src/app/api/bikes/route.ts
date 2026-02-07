import { NextResponse } from 'next/server';

export async function GET() {
  try {
    const bikesServiceUrl = process.env.BIKES_SERVICE_URL || 'http://localhost:8088';
    const response = await fetch(`${bikesServiceUrl}/bikes`);

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch bikes' },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error fetching bikes:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
