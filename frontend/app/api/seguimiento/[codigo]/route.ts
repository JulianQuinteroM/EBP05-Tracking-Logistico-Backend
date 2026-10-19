import type { NextRequest } from 'next/server';
import { forwardPublic } from '@/lib/backend';

export async function GET(request: NextRequest, { params }: { params: Promise<{ codigo: string }> }) {
  const { codigo } = await params;
  if (!/^LOG-?\d{8}-?\d$/i.test(codigo)) {
    return Response.json({ code: 'INVALID_TRACKING_CODE', message: 'Formato de código inválido' }, { status: 400 });
  }
  return forwardPublic(request, `seguimiento/${encodeURIComponent(codigo)}`);
}
