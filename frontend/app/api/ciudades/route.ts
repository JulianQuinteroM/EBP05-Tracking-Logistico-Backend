import type { NextRequest } from 'next/server';
import { forwardPrivate } from '@/lib/backend';

export async function GET(request: NextRequest) {
  const q = request.nextUrl.searchParams.get('q') ?? '';
  return forwardPrivate(request, `ciudades?q=${encodeURIComponent(q)}`);
}
