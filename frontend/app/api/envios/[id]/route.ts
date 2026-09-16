import type { NextRequest } from 'next/server';
import { forwardPrivate } from '@/lib/backend';

type Context = { params: Promise<{ id: string }> };
export async function GET(request: NextRequest, context: Context) {
  const { id } = await context.params;
  if (!/^\d+$/.test(id)) return Response.json({ code: 'INVALID_ID', message: 'ID inválido' }, { status: 400 });
  return forwardPrivate(request, `envios/${id}`);
}

export async function PUT(request: NextRequest, context: Context) {
  const { id } = await context.params;
  if (!/^\d+$/.test(id)) return Response.json({ code: 'INVALID_ID', message: 'ID inválido' }, { status: 400 });
  return forwardPrivate(request, `envios/${id}`);
}
