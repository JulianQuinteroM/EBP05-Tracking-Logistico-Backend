import type { NextRequest } from 'next/server';
import { forwardPrivate } from '@/lib/backend';

export async function POST(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) return Response.json({ code: 'INVALID_ID', message: 'ID inválido' }, { status: 400 });
  return forwardPrivate(request, `envios/${id}/cancelacion`);
}
