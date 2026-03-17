import { NextRequest, NextResponse } from 'next/server';

interface OrderItem {
  pizzaType: string;
  quantity: number;
}

interface DrinkItem {
  drinkType: string;
  quantity: number;
}

interface OrderRequest {
  orderItems: OrderItem[];
  drinkItems?: DrinkItem[];
}

function validateOrderRequest(body: unknown): body is OrderRequest {
  if (!body || typeof body !== 'object') {
    return false;
  }

  const req = body as Record<string, unknown>;

  if (!Array.isArray(req.orderItems)) {
    return false;
  }

  for (const item of req.orderItems) {
    if (!item || typeof item !== 'object') {
      return false;
    }

    const orderItem = item as Record<string, unknown>;

    if (typeof orderItem.pizzaType !== 'string') {
      return false;
    }

    if (typeof orderItem.quantity !== 'number') {
      return false;
    }
  }

  if (req.drinkItems !== undefined) {
    if (!Array.isArray(req.drinkItems)) {
      return false;
    }

    for (const item of req.drinkItems) {
      if (!item || typeof item !== 'object') {
        return false;
      }

      const drinkItem = item as Record<string, unknown>;

      if (typeof drinkItem.drinkType !== 'string') {
        return false;
      }

      if (typeof drinkItem.quantity !== 'number') {
        return false;
      }
    }
  }

  return true;
}

export async function OPTIONS() {
  return new NextResponse(null, {
    status: 200,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
    },
  });
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();

    if (!validateOrderRequest(body)) {
      return NextResponse.json(
        { error: 'Invalid request body' },
        {
          status: 400,
          headers: {
            'Access-Control-Allow-Origin': '*',
          },
        }
      );
    }

    const storeServiceUrl = process.env.STORE_SERVICE_URL || 'http://localhost:8080';
    const response = await fetch(`${storeServiceUrl}/order`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to place order' },
        {
          status: response.status,
          headers: {
            'Access-Control-Allow-Origin': '*',
          },
        }
      );
    }

    const data = await response.json();
    return NextResponse.json(data, {
      status: 200,
      headers: {
        'Access-Control-Allow-Origin': '*',
      },
    });
  } catch (error) {
    console.error('Error placing order:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      {
        status: 500,
        headers: {
          'Access-Control-Allow-Origin': '*',
        },
      }
    );
  }
}
