import { NextResponse } from 'next/server';

export async function GET() {
  try {
    const ovenServiceUrl = process.env.OVEN_SERVICE_URL || 'http://localhost:8085';
    const response = await fetch(`${ovenServiceUrl}/ovens/`);

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch ovens' },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error fetching ovens:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
