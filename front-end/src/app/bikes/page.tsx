'use client';

import { useState, useEffect } from 'react';

interface Bike {
  id: string;
  status: string;
  user?: string;
  updatedAt: string;
}

export default function BikesPage() {
  const [bikes, setBikes] = useState<Bike[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchBikes = async () => {
    try {
      const response = await fetch('/api/bikes');
      if (!response.ok) {
        throw new Error('Failed to fetch bikes');
      }
      const data = await response.json();
      setBikes(data);
      setLoading(false);
    } catch (err) {
      setError('Error loading bikes');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBikes();
    const interval = setInterval(fetchBikes, 2000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div>Error: {error}</div>;
  }

  return (
    <div>
      <h1>Bikes</h1>
      <table>
        <thead>
          <tr>
            <th>Bike ID</th>
            <th>Status</th>
            <th>User</th>
          </tr>
        </thead>
        <tbody>
          {bikes.map((bike) => (
            <tr key={bike.id}>
              <td>{bike.id}</td>
              <td>{bike.status}</td>
              <td>{bike.user || '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
