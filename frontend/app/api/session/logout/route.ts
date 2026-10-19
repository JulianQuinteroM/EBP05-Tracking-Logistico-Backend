import { NextRequest, NextResponse } from 'next/server';
import { SESSION_COOKIE } from '@/lib/session';
import { requestHasSameOrigin } from '@/lib/origin';

export function POST(request: NextRequest) {
  if (!requestHasSameOrigin(request)) {
    return NextResponse.json({ code: 'ORIGIN_REJECTED', message: 'Origen no permitido' }, { status: 403 });
  }
  const response = NextResponse.json({ authenticated: false });
  response.cookies.set(SESSION_COOKIE, '', { path: '/', maxAge: 0, httpOnly: true, sameSite: 'strict' });
  response.headers.set('cache-control', 'no-store');
  return response;
}
