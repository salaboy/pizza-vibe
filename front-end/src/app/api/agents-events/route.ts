import { NextRequest, NextResponse } from 'next/server';

const STORE_SERVICE_URL = process.env.STORE_SERVICE_URL || 'http://localhost:8080';

export async function OPTIONS() {
  return new NextResponse(null, {
    status: 200,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
    },
  });
}

export async function GET(request: NextRequest) {
  try {
    const searchParams = request.nextUrl.searchParams;
    const agentId = searchParams.get('agentId');

    let url = `${STORE_SERVICE_URL}/agents-events`;
    if (agentId) {
      url += `?agentId=${encodeURIComponent(agentId)}`;
    }

    const response = await fetch(url, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch agent events' },
        {
          status: response.status,
          headers: { 'Access-Control-Allow-Origin': '*' },
        }
      );
    }

    const data = await response.json();
    return NextResponse.json(data, {
      status: 200,
      headers: { 'Access-Control-Allow-Origin': '*' },
    });
  } catch (error) {
    console.error('Error fetching agent events:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      {
        status: 500,
        headers: { 'Access-Control-Allow-Origin': '*' },
      }
    );
  }
}

export async function DELETE() {
  try {
    const response = await fetch(`${STORE_SERVICE_URL}/agents-events`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to delete agent events' },
        {
          status: response.status,
          headers: { 'Access-Control-Allow-Origin': '*' },
        }
      );
    }

    return new NextResponse(null, {
      status: 200,
      headers: { 'Access-Control-Allow-Origin': '*' },
    });
  } catch (error) {
    console.error('Error deleting agent events:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      {
        status: 500,
        headers: { 'Access-Control-Allow-Origin': '*' },
      }
    );
  }
}
