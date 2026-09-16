import type { NextRequest } from 'next/server';
import { requestHasSession } from '@/lib/session';

export function GET(request: NextRequest) {
  return Response.json({ authenticated: requestHasSession(request) }, { headers: { 'cache-control': 'no-store' } });
}
