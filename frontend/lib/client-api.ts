export async function api<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const response = await fetch('/api/' + path, { method, headers: body === undefined ? {} : { 'content-type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body), cache: 'no-store' });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || (response.status === 401 ? 'Ingresa al panel para continuar.' : response.status === 403 ? 'Tu cuenta no tiene permiso para esta acción.' : 'No se pudo completar la operación.'));
  return data as T;
}
export async function download(path: string, filename: string) {
  const response = await fetch('/api/' + path, { cache: 'no-store' });
  if (!response.ok) { const error = await response.json().catch(() => ({})); throw new Error(error.message || 'No se pudo descargar el archivo.'); }
  const url = URL.createObjectURL(await response.blob());const anchor = document.createElement('a');anchor.href = url;anchor.download = filename;anchor.click();setTimeout(() => URL.revokeObjectURL(url), 1000);
}
