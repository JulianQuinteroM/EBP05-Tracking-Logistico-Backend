import { NextRequest, NextResponse } from 'next/server';
import { createSessionToken, requestSession, SESSION_COOKIE, sessionCookieOptions } from '@/lib/session';

export function GET(request: NextRequest) {
  const session = requestSession(request);
  const response = NextResponse.json({ authenticated: Boolean(session), role: session?.role ?? null,
    username: session?.username ?? null }, { headers: { 'cache-control': 'no-store' } });
  if (session) response.cookies.set(SESSION_COOKIE, createSessionToken(session.username, session.role), sessionCookieOptions);
  return response;
}
