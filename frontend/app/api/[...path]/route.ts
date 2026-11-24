import { NextRequest } from 'next/server';
import { forwardPrivate, forwardPublic } from '@/lib/backend';
async function handle(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const { path } = await params;
  if (path.some(part => !/^[a-zA-Z0-9._-]+$/.test(part) || part === '..' || part === '.')) return Response.json({ message: 'Ruta no válida' }, { status: 400 });
  if (!['contactos', 'reportes', 'envios', 'reclamaciones', 'admin'].includes(path[0])) return Response.json({ message: 'Ruta no encontrada' }, { status: 404 });
  const route = path.map(encodeURIComponent).join('/');
  const isPublic = request.method === 'POST' && ['reclamaciones/publicas', 'reclamaciones/consulta', 'reclamaciones/mensaje'].includes(route);
  return (isPublic ? forwardPublic : forwardPrivate)(request, `${route}${request.nextUrl.search}`);
}
export const GET = handle;
export const POST = handle;
export const PUT = handle;
