import { NextRequest, NextResponse } from 'next/server';
import { requestHasSameOrigin } from '@/lib/origin';
import { backendUrl } from '@/lib/backend';
import { createSessionToken, SESSION_COOKIE, sessionConfigured, sessionCookieOptions, roleFor, type AccountView } from '@/lib/session';
export async function POST(request: NextRequest) {
  if (!requestHasSameOrigin(request)) return Response.json({ message: 'Origen no permitido' }, { status: 403 });
  if (!sessionConfigured()) return Response.json({ message: 'Falta SESSION_SECRET en el servidor Next' }, { status: 503 });
  if (!request.headers.get('content-type')?.startsWith('application/json')) return Response.json({ message: 'Se requiere JSON' }, { status: 415 });
  const raw = await request.text(); if (raw.length > 2048) return Response.json({ message: 'Solicitud demasiado grande' }, { status: 413 });
  try {
    const upstream = await fetch(backendUrl('auth/login'), { method: 'POST', headers: { 'content-type': 'application/json' }, body: raw, cache: 'no-store', signal: AbortSignal.timeout(process.env.NODE_ENV === 'production' ? 90_000 : 10_000) });
    if (!upstream.ok) return new Response(await upstream.text(), { status: upstream.status, headers: { 'content-type': 'application/json', 'cache-control': 'no-store' } });
    const data = await upstream.json() as { token: string; user: AccountView }; const role = roleFor(data.user.role);
    const response = NextResponse.json({ authenticated: true, role, username: data.user.username, permissions: data.user.permissions }, { headers: { 'cache-control': 'no-store' } });
    response.cookies.set(SESSION_COOKIE, createSessionToken(data.user.username, role, data.token, data.user.permissions), sessionCookieOptions); return response;
  } catch { return Response.json({ message: 'No se pudo conectar con el backend' }, { status: 502 }); }
}
