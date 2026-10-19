import type { NextRequest } from 'next/server';
import { forwardPrivate } from '@/lib/backend';

export function POST(request: NextRequest) {
  return forwardPrivate(request, 'estimaciones');
}
