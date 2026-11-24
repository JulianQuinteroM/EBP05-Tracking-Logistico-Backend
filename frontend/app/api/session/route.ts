import { NextRequest, NextResponse } from 'next/server';
import { createSessionToken, requestSession, validateSession, SESSION_COOKIE, sessionCookieOptions } from '@/lib/session';
export async function GET(request: NextRequest) {
  const session = await validateSession(requestSession(request));
  const response = NextResponse.json({ authenticated: Boolean(session), role: session?.role ?? null, username: session?.username ?? null, permissions: session?.permissions ?? [] }, { headers: { 'cache-control': 'no-store' } });
  if (session) response.cookies.set(SESSION_COOKIE, createSessionToken(session.username, session.role, session.backendToken, session.permissions), sessionCookieOptions);
  else response.cookies.delete(SESSION_COOKIE);
  return response;
}
