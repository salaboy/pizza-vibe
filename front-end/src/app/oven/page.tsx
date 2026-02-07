'use client';

import { useState, useEffect } from 'react';

interface Oven {
  id: string;
  status: string;
  user?: string;
  updatedAt: string;
}

export default function OvenPage() {
  const [ovens, setOvens] = useState<Oven[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchOvens = async () => {
    try {
      const response = await fetch('/api/oven');
      if (!response.ok) {
        throw new Error('Failed to fetch ovens');
      }
      const data = await response.json();
      setOvens(data);
      setLoading(false);
    } catch (err) {
      setError('Error loading ovens');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOvens();
    const interval = setInterval(fetchOvens, 2000);
    return () => clearInterval(interval);
  }, []);

  const handleReserve = async (ovenId: string) => {
    try {
      const response = await fetch(`/api/oven/${ovenId}?user=user`, {
        method: 'POST',
      });
      if (response.ok) {
        fetchOvens();
      }
    } catch (err) {
      setError('Error reserving oven');
    }
  };

  const handleRelease = async (ovenId: string) => {
    try {
      const response = await fetch(`/api/oven/${ovenId}`, {
        method: 'DELETE',
      });
      if (response.ok) {
        fetchOvens();
      }
    } catch (err) {
      setError('Error releasing oven');
    }
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div>Error: {error}</div>;
  }

  return (
    <div>
      <h1>Oven Management</h1>
      <table>
        <thead>
          <tr>
            <th>Oven ID</th>
            <th>Status</th>
            <th>User</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {ovens.map((oven) => (
            <tr key={oven.id}>
              <td>{oven.id}</td>
              <td>{oven.status}</td>
              <td>{oven.user || '-'}</td>
              <td>
                {oven.status === 'AVAILABLE' ? (
                  <button onClick={() => handleReserve(oven.id)}>Reserve</button>
                ) : (
                  <button onClick={() => handleRelease(oven.id)}>Release</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
