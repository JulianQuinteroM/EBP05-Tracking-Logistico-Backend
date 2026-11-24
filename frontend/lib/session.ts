import { createHmac, timingSafeEqual } from 'node:crypto';
import { cookies } from 'next/headers';
import type { NextRequest } from 'next/server';
export const SESSION_COOKIE = 'tracking_operador';
const SESSION_SECONDS = 30 * 60;
export type UserRole = 'operator' | 'admin' | 'viewer' | 'courier';
export type UserSession = { username: string; role: UserRole; permissions: string[]; backendToken: string; expires: number };
export type AccountView = { username: string; name: string; role: string; permissions: string[]; active: boolean; version: number };
export function roleFor(role: string): UserRole { return ({ ADMIN: 'admin', OPERADOR: 'operator', VIEWER: 'viewer', REPARTIDOR: 'courier' } as Record<string, UserRole>)[role] ?? 'viewer'; }
function signingKey() { const key = process.env.SESSION_SECRET; return key && key.length >= 32 ? key : null; }
export function sessionConfigured() { return signingKey() !== null; }
export function createSessionToken(username: string, role: UserRole, backendToken: string, permissions: string[]) {
  const secret = signingKey(); if (!secret) throw new Error('SESSION_SECRET no configurado');
  const payload = Buffer.from(JSON.stringify({ username, role, backendToken, permissions, expires: Math.floor(Date.now() / 1000) + SESSION_SECONDS })).toString('base64url');
  return payload + '.' + createHmac('sha256', secret).update(payload).digest('base64url');
}
export function readSession(token?: string): UserSession | null {
  const secret = signingKey(); if (!secret || !token) return null;
  try {
    const [payload, signature, extra] = token.split('.'); if (!payload || !signature || extra) return null;
    const expected = createHmac('sha256', secret).update(payload).digest(); const supplied = Buffer.from(signature, 'base64url');
    if (supplied.length !== expected.length || !timingSafeEqual(supplied, expected)) return null;
    const value = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8')) as UserSession;
    if (typeof value.username !== 'string' || typeof value.backendToken !== 'string' || !Array.isArray(value.permissions)
      || !['operator', 'admin', 'viewer', 'courier'].includes(value.role) || !Number.isSafeInteger(value.expires) || value.expires <= Date.now() / 1000) return null;
    return value;
  } catch { return null; }
}
export function requestSession(request: NextRequest) { return readSession(request.cookies.get(SESSION_COOKIE)?.value); }
export async function validateSession(session: UserSession | null): Promise<UserSession | null> {
  if (!session) return null;
  try {
    const response = await fetch((process.env.BACKEND_URL || 'http://localhost:8080').replace(/\/$/, '') + '/api/auth/me', {
      headers: { authorization: 'Bearer ' + session.backendToken }, cache: 'no-store', signal: AbortSignal.timeout(process.env.NODE_ENV === 'production' ? 90_000 : 10_000),
    });
    if (!response.ok) return null;
    const user = await response.json() as AccountView;
    return { ...session, username: user.username, role: roleFor(user.role), permissions: user.permissions };
  } catch { return null; }
}
export async function pageSession() { return validateSession(readSession((await cookies()).get(SESSION_COOKIE)?.value)); }
export async function pageHasSession() { return (await pageSession()) !== null; }
export function requestHasSession(request: NextRequest) { return requestSession(request) !== null; }
export const sessionCookieOptions = { httpOnly: true, secure: process.env.NODE_ENV === 'production', sameSite: 'strict' as const, path: '/', maxAge: SESSION_SECONDS };
