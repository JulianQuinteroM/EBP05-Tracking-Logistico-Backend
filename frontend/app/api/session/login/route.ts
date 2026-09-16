import { createHash, timingSafeEqual } from 'node:crypto';
import { NextRequest, NextResponse } from 'next/server';
import { createSessionToken, SESSION_COOKIE, sessionConfigured, sessionCookieOptions } from '@/lib/session';

export async function POST(request: NextRequest) {
  const origin = request.headers.get('origin');
  if (origin && origin !== request.nextUrl.origin) {
    return NextResponse.json({ code: 'ORIGIN_REJECTED', message: 'Origen no permitido' }, { status: 403 });
  }
  if (!request.headers.get('content-type')?.startsWith('application/json')) {
    return NextResponse.json({ code: 'INVALID_CONTENT_TYPE', message: 'Se requiere JSON' }, { status: 415 });
  }
  const configuredPassword = process.env.OPERATOR_PASSWORD;
  if (!configuredPassword || !sessionConfigured()) {
    return NextResponse.json({ code: 'FRONT_CONFIGURATION', message: 'Faltan credenciales o secreto de sesión en el servidor Next' }, { status: 503 });
  }
  let credentials: { username?: unknown; password?: unknown };
  try {
    const raw = await request.text();
    if (raw.length > 2048) throw new Error('Solicitud demasiado grande');
    credentials = JSON.parse(raw);
  } catch {
    return NextResponse.json({ code: 'INVALID_BODY', message: 'Solicitud inválida' }, { status: 400 });
  }
  const username = process.env.OPERATOR_USERNAME || 'operador';
  const suppliedUsername = typeof credentials.username === 'string' ? credentials.username : '';
  const suppliedPassword = typeof credentials.password === 'string' ? credentials.password : '';
  const expected = createHash('sha256').update(configuredPassword).digest();
  const supplied = createHash('sha256').update(suppliedPassword).digest();
  if (suppliedUsername !== username || !timingSafeEqual(expected, supplied)) {
    return NextResponse.json({ code: 'INVALID_CREDENTIALS', message: 'Usuario o contraseña incorrectos' }, { status: 401 });
  }
  const response = NextResponse.json({ authenticated: true });
  response.headers.set('cache-control', 'no-store');
  response.cookies.set(SESSION_COOKIE, createSessionToken(), sessionCookieOptions);
  return response;
}
