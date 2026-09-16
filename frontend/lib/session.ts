import { createHmac, randomBytes, timingSafeEqual } from 'node:crypto';
import { cookies } from 'next/headers';
import type { NextRequest } from 'next/server';

export const SESSION_COOKIE = 'tracking_operador';
const SESSION_SECONDS = 8 * 60 * 60;

function signingKey() {
  const value = process.env.SESSION_SECRET;
  return value && value.length >= 32 ? value : null;
}

export function sessionConfigured() {
  return signingKey() !== null;
}

export function createSessionToken() {
  const secret = signingKey();
  if (!secret) throw new Error('SESSION_SECRET no configurado');
  const expires = Math.floor(Date.now() / 1000) + SESSION_SECONDS;
  const payload = `${expires}.${randomBytes(16).toString('base64url')}`;
  const signature = createHmac('sha256', secret).update(payload).digest('base64url');
  return `${payload}.${signature}`;
}

export function validSession(token?: string) {
  const secret = signingKey();
  if (!secret || !token) return false;
  const parts = token.split('.');
  if (parts.length !== 3 || !/^\d+$/.test(parts[0])) return false;
  const expires = Number(parts[0]);
  if (!Number.isSafeInteger(expires) || expires <= Date.now() / 1000) return false;
  const expected = createHmac('sha256', secret).update(`${parts[0]}.${parts[1]}`).digest();
  const supplied = Buffer.from(parts[2], 'base64url');
  return supplied.length === expected.length && timingSafeEqual(supplied, expected);
}

export function requestHasSession(request: NextRequest) {
  return validSession(request.cookies.get(SESSION_COOKIE)?.value);
}

export async function pageHasSession() {
  return validSession((await cookies()).get(SESSION_COOKIE)?.value);
}

export const sessionCookieOptions = {
  httpOnly: true,
  secure: process.env.NODE_ENV === 'production',
  sameSite: 'strict' as const,
  path: '/',
  maxAge: SESSION_SECONDS,
};
