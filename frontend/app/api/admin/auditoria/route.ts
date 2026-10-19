import type { NextRequest } from 'next/server';
import { forwardPrivate } from '@/lib/backend';

export async function GET(request: NextRequest) {
  return forwardPrivate(request, `admin/auditoria?${request.nextUrl.searchParams}`);
}
