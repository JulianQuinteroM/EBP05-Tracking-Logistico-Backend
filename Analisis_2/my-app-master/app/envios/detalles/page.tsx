import { redirect } from 'next/navigation';

export default async function DetailsPage({ searchParams }: { searchParams: Promise<{ tracking?: string }> }) {
  const { tracking } = await searchParams;
  if (tracking) redirect(`/envios/info?tracking=${encodeURIComponent(tracking)}`);
  return <main className="p-8 text-center text-slate-600">No se recibió un código de seguimiento.</main>;
}
