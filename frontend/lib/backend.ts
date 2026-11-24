import { NextRequest, NextResponse } from 'next/server';
import { createSessionToken, pageSession, requestSession, SESSION_COOKIE, sessionCookieOptions } from './session';
import { requestHasSameOrigin } from './origin';
const TIMEOUT = process.env.NODE_ENV === 'production' ? 90_000 : 55_000;
export function backendUrl(path: string) { return (process.env.BACKEND_URL || 'http://localhost:8080').replace(/\/$/, '') + '/api/' + path; }
async function forward(request: NextRequest, path: string, isPrivate: boolean) {
  const session = isPrivate ? requestSession(request) : null;
  if (isPrivate && !session) return Response.json({ message: 'Ingresa al panel para continuar' }, { status: 401 });
  if (!requestHasSameOrigin(request)) return Response.json({ message: 'Origen no permitido' }, { status: 403 });
  const headers = new Headers({ accept: '*/*' });
  if (session) headers.set('authorization', 'Bearer ' + session.backendToken);
  let body: string | undefined;
  if (!['GET', 'HEAD'].includes(request.method)) {
    if (!request.headers.get('content-type')?.startsWith('application/json')) return Response.json({ message: 'Se requiere JSON' }, { status: 415 });
    body = await request.text();
    if (Buffer.byteLength(body, 'utf8') > 2_100_000) return Response.json({ message: 'Solicitud demasiado grande' }, { status: 413 });
    headers.set('content-type', 'application/json');
  }
  try {
    const upstream = await fetch(backendUrl(path), { method: request.method, headers, body, cache: 'no-store', signal: AbortSignal.timeout(TIMEOUT) });
    const responseHeaders = new Headers({ 'content-type': upstream.headers.get('content-type') || 'application/json', 'cache-control': 'no-store', 'x-content-type-options': 'nosniff' });
    const disposition = upstream.headers.get('content-disposition'); if (disposition) responseHeaders.set('content-disposition', disposition);
    const response = new NextResponse(upstream.status === 204 ? null : await upstream.arrayBuffer(), { status: upstream.status, headers: responseHeaders });
    if (session && upstream.ok) response.cookies.set(SESSION_COOKIE, createSessionToken(session.username, session.role, session.backendToken, session.permissions), sessionCookieOptions);
    if (upstream.status === 401 && isPrivate) response.cookies.delete(SESSION_COOKIE);
    return response;
  } catch { return Response.json({ message: 'No se pudo conectar con el backend. Comprueba que esté encendido.' }, { status: 502 }); }
}
export function forwardPrivate(request: NextRequest, path: string) { return forward(request, path, true); }
export function forwardPublic(request: NextRequest, path: string) { return forward(request, path, false); }
export function forwardCsvPrivate(request: NextRequest, path: string) { return forward(request, path, true); }
export async function serverFetchPrivate<T>(path: string): Promise<{ status: number; data: T | null }> {
  const session = await pageSession(); if (!session) return { status: 401, data: null };
  return read<T>(path, 'Bearer ' + session.backendToken);
}
async function read<T>(path: string, authorization?: string): Promise<{ status: number; data: T | null }> {
  try {
    const response = await fetch(backendUrl(path), { headers: authorization ? { authorization } : {}, cache: 'no-store', signal: AbortSignal.timeout(TIMEOUT) });
    return { status: response.status, data: response.ok ? await response.json() as T : null };
  } catch { return { status: 502, data: null }; }
}
export function serverFetchPublic<T>(path: string) { return read<T>(path); }
