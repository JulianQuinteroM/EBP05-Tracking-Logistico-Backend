import { createHash, timingSafeEqual } from 'node:crypto';
import { NextRequest, NextResponse } from 'next/server';
import { requestHasSameOrigin } from '@/lib/origin';
import { createSessionToken, SESSION_COOKIE, sessionConfigured, sessionCookieOptions, type UserRole } from '@/lib/session';

export async function POST(request: NextRequest) {
  if (!requestHasSameOrigin(request)) {
    return NextResponse.json({ code: 'ORIGIN_REJECTED', message: 'Origen no permitido' }, { status: 403 });
  }
  if (!request.headers.get('content-type')?.startsWith('application/json')) {
    return NextResponse.json({ code: 'INVALID_CONTENT_TYPE', message: 'Se requiere JSON' }, { status: 415 });
  }
  if (!process.env.OPERATOR_PASSWORD || !sessionConfigured()) {
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
  const suppliedUsername = typeof credentials.username === 'string' ? credentials.username : '';
  const suppliedPassword = typeof credentials.password === 'string' ? credentials.password : '';
  const accounts: { username: string; password?: string; role: UserRole }[] = [
    { username: process.env.OPERATOR_USERNAME || 'operador', password: process.env.OPERATOR_PASSWORD, role: 'operator' },
    { username: process.env.ADMIN_USERNAME || 'administrador', password: process.env.ADMIN_PASSWORD, role: 'admin' },
    { username: process.env.VIEWER_USERNAME || 'consulta', password: process.env.VIEWER_PASSWORD, role: 'viewer' },
  ];
  const account = accounts.find(item => item.username === suppliedUsername && item.password);
  const configuredPassword = account?.password || '';
  const expected = createHash('sha256').update(configuredPassword).digest();
  const supplied = createHash('sha256').update(suppliedPassword).digest();
  if (!account || !timingSafeEqual(expected, supplied)) {
    return NextResponse.json({ code: 'INVALID_CREDENTIALS', message: 'Usuario o contraseña incorrectos' }, { status: 401 });
  }
  const response = NextResponse.json({ authenticated: true, role: account.role, username: account.username });
  response.headers.set('cache-control', 'no-store');
  response.cookies.set(SESSION_COOKIE, createSessionToken(account.username, account.role), sessionCookieOptions);
  return response;
}
