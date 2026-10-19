import type { NextRequest } from 'next/server';

function firstHeaderValue(value: string | null) {
  return value?.split(',')[0]?.trim() || null;
}

export function requestHasSameOrigin(request: NextRequest) {
  const origin = request.headers.get('origin');
  if (!origin) return true;

  const host = firstHeaderValue(request.headers.get('x-forwarded-host'))
    ?? firstHeaderValue(request.headers.get('host'));
  const protocol = firstHeaderValue(request.headers.get('x-forwarded-proto'))
    ?? request.nextUrl.protocol.replace(':', '');
  if (!host || !protocol) return false;

  try {
    return new URL(origin).origin === `${protocol}://${host}`;
  } catch {
    return false;
  }
}
