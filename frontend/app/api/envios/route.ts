import type { NextRequest } from 'next/server';
import { forwardPrivate } from '@/lib/backend';

export function GET(request: NextRequest) {
  return forwardPrivate(request, `envios${request.nextUrl.search}`);
}

export function POST(request: NextRequest) {
  return forwardPrivate(request, 'envios');
}
