import type { NextRequest } from 'next/server';
import { forwardCsvPrivate } from '@/lib/backend';

export async function GET(request: NextRequest) {
  return forwardCsvPrivate(request, `admin/auditoria/exportacion?${request.nextUrl.searchParams}`);
}
