import type { NextRequest } from 'next/server';
import { requestHasSession } from './session';

function backendUrl(path: string) {
  const base = process.env.BACKEND_URL || 'http://localhost:8080';
  return `${base.replace(/\/$/, '')}/api/${path}`;
}

function operatorAuthorization() {
  const username = process.env.OPERATOR_USERNAME || 'operador';
  const password = process.env.OPERATOR_PASSWORD;
  return password ? `Basic ${Buffer.from(`${username}:${password}`).toString('base64')}` : null;
}

function unavailable(message: string) {
  return Response.json({ code: 'FRONT_CONFIGURATION', message }, { status: 503 });
}

function sameOrigin(request: NextRequest) {
  const origin = request.headers.get('origin');
  return !origin || origin === new URL(request.url).origin;
}

async function forward(request: NextRequest, path: string, isPrivate: boolean) {
  if (isPrivate && !requestHasSession(request)) {
    return Response.json({ code: 'LOGIN_REQUIRED', message: 'Ingresa como operador para continuar' }, { status: 401 });
  }
  if (!sameOrigin(request)) {
    return Response.json({ code: 'ORIGIN_REJECTED', message: 'Origen no permitido' }, { status: 403 });
  }
  const auth = isPrivate ? operatorAuthorization() : null;
  if (isPrivate && !auth) return unavailable('Falta configurar OPERATOR_PASSWORD en el servidor Next');

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
      signal: AbortSignal.timeout(10_000),
    });
    if (isPrivate && upstream.status === 401) {
      return Response.json({ code: 'BACKEND_AUTH_FAILED', message: 'El front no tiene la misma contraseña de operador que el backend. Reinicia el front con la contraseña correcta.' }, { status: 503 });
    }
    const responseBody = await upstream.text();
    if (!upstream.headers.get('content-type')?.includes('application/json') && !upstream.ok) {
      return Response.json({ code: 'BACKEND_ERROR', message: 'El backend no pudo procesar la solicitud' }, { status: upstream.status });
    }
    return new Response(responseBody, {
      status: upstream.status,
      headers: {
        'content-type': 'application/json; charset=utf-8',
        'cache-control': 'no-store',
      },
    });
  } catch {
    return Response.json({ code: 'BACKEND_UNAVAILABLE', message: 'No se pudo conectar con el backend' }, { status: 502 });
  }
}

export function forwardPrivate(request: NextRequest, path: string) {
  return forward(request, path, true);
}

export function forwardPublic(request: NextRequest, path: string) {
  return forward(request, path, false);
}

export async function serverFetchPrivate<T>(path: string): Promise<{ status: number; data: T | null }> {
  const authorization = operatorAuthorization();
  if (!authorization) return { status: 503, data: null };
  try {
    const response = await fetch(backendUrl(path), {
      headers: { authorization, accept: 'application/json' },
      cache: 'no-store',
      signal: AbortSignal.timeout(10_000),
    });
    return { status: response.status, data: response.ok ? (await response.json() as T) : null };
  } catch {
    return { status: 502, data: null };
  }
}

export async function serverFetchPublic<T>(path: string): Promise<{ status: number; data: T | null }> {
  try {
    const response = await fetch(backendUrl(path), { cache: 'no-store', signal: AbortSignal.timeout(10_000) });
    return { status: response.status, data: response.ok ? (await response.json() as T) : null };
  } catch {
    return { status: 502, data: null };
  }
}
