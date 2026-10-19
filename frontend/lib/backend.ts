import { NextRequest, NextResponse } from 'next/server';
import { createSessionToken, pageSession, requestSession, SESSION_COOKIE, sessionCookieOptions, type UserSession } from './session';
import { requestHasSameOrigin } from './origin';

// Render Free can take roughly a minute to wake up after being idle.
const BACKEND_TIMEOUT_MS = process.env.NODE_ENV === 'production' ? 90_000 : 10_000;

function backendUrl(path: string) {
  const base = process.env.BACKEND_URL || 'http://localhost:8080';
  return `${base.replace(/\/$/, '')}/api/${path}`;
}

function authorizationFor(session: UserSession | null) {
  if (!session) return null;
  const username = session.username;
  const password = session.role === 'admin' ? process.env.ADMIN_PASSWORD
    : session.role === 'viewer' ? process.env.VIEWER_PASSWORD : process.env.OPERATOR_PASSWORD;
  return password ? `Basic ${Buffer.from(`${username}:${password}`).toString('base64')}` : null;
}

function unavailable(message: string) {
  return Response.json({ code: 'FRONT_CONFIGURATION', message }, { status: 503 });
}

async function forward(request: NextRequest, path: string, isPrivate: boolean) {
  const session = isPrivate ? requestSession(request) : null;
  if (isPrivate && !session) {
    return Response.json({ code: 'LOGIN_REQUIRED', message: 'Ingresa como operador para continuar' }, { status: 401 });
  }
  if (!requestHasSameOrigin(request)) {
    return Response.json({ code: 'ORIGIN_REJECTED', message: 'Origen no permitido' }, { status: 403 });
  }
  const auth = isPrivate ? authorizationFor(session) : null;
  if (isPrivate && !auth) return unavailable('Falta configurar la contraseña de este usuario en el servidor Next');

  const headers = new Headers({ accept: 'application/json' });
  if (auth) headers.set('authorization', auth);
  let body: string | undefined;
  if (request.method !== 'GET') {
    if (!request.headers.get('content-type')?.startsWith('application/json')) {
      return Response.json({ code: 'INVALID_CONTENT_TYPE', message: 'Se requiere JSON' }, { status: 415 });
    }
    body = await request.text();
    if (body.length > 16_384) return Response.json({ code: 'BODY_TOO_LARGE', message: 'Solicitud demasiado grande' }, { status: 413 });
    headers.set('content-type', 'application/json');
  }

  try {
    const upstream = await fetch(backendUrl(path), {
      method: request.method,
      headers,
      body,
      cache: 'no-store',
      signal: AbortSignal.timeout(BACKEND_TIMEOUT_MS),
    });
    if (isPrivate && upstream.status === 401) {
      return Response.json({ code: 'BACKEND_AUTH_FAILED', message: 'El front no tiene las mismas credenciales que el backend. Reinícialo con las contraseñas correctas.' }, { status: 503 });
    }
    const responseBody = await upstream.text();
    if (upstream.status === 204) {
      return new Response(null, { status: 204, headers: { 'cache-control': 'no-store' } });
    }
    if (!upstream.headers.get('content-type')?.includes('application/json') && !upstream.ok) {
      return Response.json({ code: 'BACKEND_ERROR', message: 'El backend no pudo procesar la solicitud' }, { status: upstream.status });
    }
    const response = new NextResponse(responseBody, {
      status: upstream.status,
      headers: {
        'content-type': 'application/json; charset=utf-8',
        'cache-control': 'no-store',
      },
    });
    if (isPrivate && session && upstream.ok) response.cookies.set(SESSION_COOKIE,
      createSessionToken(session.username, session.role), sessionCookieOptions);
    return response;
  } catch {
    return Response.json({ code: 'BACKEND_UNAVAILABLE', message: 'No se pudo conectar con el backend' }, { status: 502 });
  }
}

export async function forwardCsvPrivate(request: NextRequest, path: string) {
  const session = requestSession(request);
  if (!session) return Response.json({ code: 'LOGIN_REQUIRED', message: 'Ingresa para continuar' }, { status: 401 });
  if (!requestHasSameOrigin(request)) return Response.json({ code: 'ORIGIN_REJECTED', message: 'Origen no permitido' }, { status: 403 });
  const authorization = authorizationFor(session);
  if (!authorization) return unavailable('Falta configurar la contraseña de este usuario en el servidor Next');
  try {
    const upstream = await fetch(backendUrl(path), { headers: { authorization, accept: 'text/csv' },
      cache: 'no-store', signal: AbortSignal.timeout(BACKEND_TIMEOUT_MS) });
    if (upstream.status === 401) return unavailable('El front no tiene las mismas credenciales que el backend. Reinícialo con las contraseñas correctas.');
    if (!upstream.ok) return Response.json({ code: 'EXPORT_FAILED', message: 'No se pudo exportar la auditoría' }, { status: upstream.status });
    const response = new NextResponse(await upstream.arrayBuffer(), { status: 200,
      headers: { 'content-type': 'text/csv; charset=utf-8', 'content-disposition': 'attachment; filename=auditoria-envios.csv', 'cache-control': 'no-store' } });
    response.cookies.set(SESSION_COOKIE, createSessionToken(session.username, session.role), sessionCookieOptions);
    return response;
  } catch { return Response.json({ code: 'BACKEND_UNAVAILABLE', message: 'No se pudo conectar con el backend' }, { status: 502 }); }
}

export function forwardPrivate(request: NextRequest, path: string) {
  return forward(request, path, true);
}

export function forwardPublic(request: NextRequest, path: string) {
  return forward(request, path, false);
}

export async function serverFetchPrivate<T>(path: string): Promise<{ status: number; data: T | null }> {
  const authorization = authorizationFor(await pageSession());
  if (!authorization) return { status: 503, data: null };
  try {
    const response = await fetch(backendUrl(path), {
      headers: { authorization, accept: 'application/json' },
      cache: 'no-store',
      signal: AbortSignal.timeout(BACKEND_TIMEOUT_MS),
    });
    return { status: response.status, data: response.ok ? (await response.json() as T) : null };
  } catch {
    return { status: 502, data: null };
  }
}

export async function serverFetchPublic<T>(path: string): Promise<{ status: number; data: T | null }> {
  try {
    const response = await fetch(backendUrl(path), { cache: 'no-store', signal: AbortSignal.timeout(BACKEND_TIMEOUT_MS) });
    return { status: response.status, data: response.ok ? (await response.json() as T) : null };
  } catch {
    return { status: 502, data: null };
  }
}
