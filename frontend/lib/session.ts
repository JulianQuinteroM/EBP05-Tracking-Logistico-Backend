import { createHmac, randomBytes, timingSafeEqual } from 'node:crypto';
import { cookies } from 'next/headers';
import type { NextRequest } from 'next/server';

export const SESSION_COOKIE = 'tracking_operador';
const SESSION_SECONDS = 30 * 60;
export type UserRole = 'operator' | 'admin' | 'viewer';
export type UserSession = { username: string; role: UserRole; expires: number };

function signingKey() {
  const value = process.env.SESSION_SECRET;
  return value && value.length >= 32 ? value : null;
}

export function sessionConfigured() {
  return signingKey() !== null;
}

export function createSessionToken(username: string, role: UserRole) {
  const secret = signingKey();
  if (!secret) throw new Error('SESSION_SECRET no configurado');
  const expires = Math.floor(Date.now() / 1000) + SESSION_SECONDS;
  const payload = Buffer.from(JSON.stringify({ username, role, expires, nonce: randomBytes(16).toString('base64url') })).toString('base64url');
  const signature = createHmac('sha256', secret).update(payload).digest('base64url');
  return `${payload}.${signature}`;
}

export function readSession(token?: string): UserSession | null {
  const secret = signingKey();
  if (!secret || !token) return null;
  const parts = token.split('.');
  if (parts.length !== 2) return null;
  const expected = createHmac('sha256', secret).update(parts[0]).digest();
  const supplied = Buffer.from(parts[1], 'base64url');
  if (supplied.length !== expected.length || !timingSafeEqual(supplied, expected)) return null;
  try {
    const payload = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
    if (typeof payload.username !== 'string' || !['operator', 'admin', 'viewer'].includes(payload.role)
      || !Number.isSafeInteger(payload.expires) || payload.expires <= Date.now() / 1000) return null;
    return { username: payload.username, role: payload.role as UserRole, expires: payload.expires };
  } catch { return null; }
}

export function validSession(token?: string) { return readSession(token) !== null; }

export function requestSession(request: NextRequest) {
  return readSession(request.cookies.get(SESSION_COOKIE)?.value);
}

export function requestHasSession(request: NextRequest) {
  return validSession(request.cookies.get(SESSION_COOKIE)?.value);
}

export async function pageHasSession() {
  return validSession((await cookies()).get(SESSION_COOKIE)?.value);
}

export async function pageSession() {
  return readSession((await cookies()).get(SESSION_COOKIE)?.value);
}

export const sessionCookieOptions = {
  httpOnly: true,
  secure: process.env.NODE_ENV === 'production',
  sameSite: 'strict' as const,
  path: '/',
  maxAge: SESSION_SECONDS,
};
