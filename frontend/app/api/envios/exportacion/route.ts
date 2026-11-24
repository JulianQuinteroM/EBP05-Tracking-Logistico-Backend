import type { NextRequest } from 'next/server';
import { forwardCsvPrivate } from '@/lib/backend';
export function GET(request: NextRequest) { return forwardCsvPrivate(request, 'envios/exportacion' + request.nextUrl.search); }
