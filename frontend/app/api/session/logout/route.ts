import { NextRequest, NextResponse } from 'next/server';
import { SESSION_COOKIE } from '@/lib/session';

export function POST(request: NextRequest) {
  const origin = request.headers.get('origin');
  if (origin && origin !== request.nextUrl.origin) {
    return NextResponse.json({ code: 'ORIGIN_REJECTED', message: 'Origen no permitido' }, { status: 403 });
  }
  const response = NextResponse.json({ authenticated: false });
  response.cookies.set(SESSION_COOKIE, '', { path: '/', maxAge: 0, httpOnly: true, sameSite: 'strict' });
  response.headers.set('cache-control', 'no-store');
  return response;
}
