import { NextRequest, NextResponse } from 'next/server';
import { requestSession, SESSION_COOKIE } from '@/lib/session';
import { requestHasSameOrigin } from '@/lib/origin';
import { backendUrl } from '@/lib/backend';
export async function POST(request: NextRequest) {
  if (!requestHasSameOrigin(request)) return Response.json({ message: 'Origen no permitido' }, { status: 403 });
  const session = requestSession(request);
  if (session) { try { await fetch(backendUrl('auth/logout'), { method: 'POST', headers: { authorization: 'Bearer ' + session.backendToken }, signal: AbortSignal.timeout(8000) }); } catch {} }
  const response = NextResponse.json({ authenticated: false }); response.cookies.delete(SESSION_COOKIE); return response;
}
