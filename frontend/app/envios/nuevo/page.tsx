'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Form from 'next/form';
import InputBase from '@/app/envios/nuevo/InputBase';
import type { Estimate, ServiceType, ShipmentDetail, ShipmentInput } from '@/lib/types';

function serviceType(value: string): ServiceType {
    return value === 'Exprés' ? 'EXPRES' : value === 'Programado' ? 'PROGRAMADO' : 'ESTANDAR';
}

export default function NuevoEnvio() {
    const router = useRouter();

    const [prioridad, setPrioridad] = useState('Estándar');
    const [formError, setFormError] = useState('');
    const [apiError, setApiError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [scheduledDate, setScheduledDate] = useState('');
    const [estimateResult, setEstimateResult] = useState<{ key: string; data: Estimate } | null>(null);
    const [estimateErrorResult, setEstimateErrorResult] = useState<{ key: string; message: string } | null>(null);
    const [sessionChecked, setSessionChecked] = useState(false);
    const estimateKey = `${serviceType(prioridad)}:${scheduledDate}`;
    const estimate = estimateResult?.key === estimateKey ? estimateResult.data : null;
    const estimateError = serviceType(prioridad) === 'PROGRAMADO' && !scheduledDate
        ? 'Elige la fecha de entrega para ver la estimación.'
        : estimateErrorResult?.key === estimateKey ? estimateErrorResult.message : '';

    useEffect(() => {
        fetch('/api/session', { cache: 'no-store' }).then(response => response.json()).then(data => {
            if (!data.authenticated) router.push('/operador/ingresar?next=/envios/nuevo');
            else setSessionChecked(true);
        }).catch(() => { setApiError('No se pudo comprobar la sesión de operador.'); setSessionChecked(true); });
    }, [router]);

    useEffect(() => {
        const selectedService = serviceType(prioridad);
        if (selectedService === 'PROGRAMADO' && !scheduledDate) {
            return;
        }
        const controller = new AbortController();
        const key = `${selectedService}:${scheduledDate}`;
        fetch('/api/estimaciones', {
            method: 'POST',
            headers: { 'content-type': 'application/json' },
            body: JSON.stringify({ serviceType: selectedService, scheduledDeliveryDate: selectedService === 'PROGRAMADO' ? scheduledDate : null }),
            signal: controller.signal,
        }).then(async response => {
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'No se pudo estimar');
            setEstimateResult({ key, data: data as Estimate });
            setEstimateErrorResult(null);
        }).catch(reason => {
            if (controller.signal.aborted) return;
            setEstimateErrorResult({ key, message: reason instanceof Error ? reason.message : 'No se pudo estimar' });
        });
        return () => controller.abort();
    }, [prioridad, scheduledDate]);

    const handleFormAction = async (formData: FormData) => {
        setFormError('');
        setApiError('');
        setIsSubmitting(true);

        const requiredFields = [
            'remitenteNombre', 'remitenteTelefono', 'remitenteCorreo',
            'destinatarioNombre', 'destinatarioTelefono', 'destinatarioCorreo',
            'direccionCalle', 'direccionNumero', 'direccionCiudad', 'peso'
        ];

        let isValid = true;
        for (const field of requiredFields) {
            if (!String(formData.get(field) ?? '').trim()) {
                isValid = false;
                break;
            }
        }

        if (prioridad === 'Programado' && !String(formData.get('fechaProgramada') ?? '').trim()) {
            isValid = false;
        }

        if (!isValid) {
            setFormError('Por favor complete todos los campos obligatorios antes de enviar.');
            setIsSubmitting(false);
            return;
        }

        try {
            const input: ShipmentInput = {
                senderName: String(formData.get('remitenteNombre')).trim(),
                senderPhone: String(formData.get('remitenteTelefono')).trim(),
                senderEmail: String(formData.get('remitenteCorreo')).trim(),
                recipientName: String(formData.get('destinatarioNombre')).trim(),
                recipientPhone: String(formData.get('destinatarioTelefono')).trim(),
                recipientEmail: String(formData.get('destinatarioCorreo')).trim(),
                destinationStreet: String(formData.get('direccionCalle')).trim(),
                destinationNumber: String(formData.get('direccionNumero')).trim(),
                destinationCity: String(formData.get('direccionCiudad')).trim(),
                weightKg: Number(formData.get('peso')),
                serviceType: serviceType(prioridad),
                scheduledDeliveryDate: prioridad === 'Programado' ? String(formData.get('fechaProgramada')) : null,
            };
            if (!Number.isFinite(input.weightKg) || input.weightKg <= 0) throw new Error('El peso debe ser mayor que cero.');
            const response = await fetch('/api/envios', {
                method: 'POST',
                headers: { 'content-type': 'application/json' },
                body: JSON.stringify(input),
            });
            const data = await response.json();
            if (response.status === 401) {
                router.push('/operador/ingresar?next=/envios/nuevo');
                throw new Error('Ingresa como operador antes de registrar un envío.');
            }
            if (!response.ok) throw new Error(data.message || 'No se pudo crear el envío.');
            router.push(`/envios/detalles?id=${(data as ShipmentDetail).id}`);
        } catch (reason) {
            setApiError(reason instanceof Error ? reason.message : 'Error al crear el envío.');
            setIsSubmitting(false);
        }
    };

    if (!sessionChecked) return <p className="mx-auto max-w-3xl rounded bg-white p-8 text-gray-700">Comprobando sesión de operador...</p>;

    return (
        <div className="max-w-3xl mx-auto bg-white p-8 rounded-xl shadow-sm border border-gray-100">
            <h2 className="text-2xl font-bold text-gray-800 mb-6 border-b pb-4">Registrar Nuevo Envío</h2>

            {formError && (
                <div className="bg-orange-50 border-l-4 border-orange-500 text-orange-700 p-4 mb-6">
                    <p>{formError}</p>
                </div>
            )}
            {apiError && <p role="alert" className="mb-6 rounded bg-red-50 p-4 text-red-700">{apiError}</p>}

            <Form action={handleFormAction} className="space-y-8">

                {/* Datos del Remitente */}
                <section>
                    <h3 className="text-lg font-semibold text-purple-900 mb-4">Datos del Remitente</h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        {/* Usamos el nuevo componente, ¡mira qué limpio queda! */}
                        <InputBase type="text" name="remitenteNombre" placeholder="Nombre completo" />
                        <InputBase type="tel" name="remitenteTelefono" placeholder="Teléfono" />
                        <InputBase type="email" name="remitenteCorreo" placeholder="Correo electrónico" />
                    </div>
                </section>

                {/* Datos del Destinatario */}
                <section>
                    <h3 className="text-lg font-semibold text-purple-900 mb-4">Datos del Destinatario</h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                        <InputBase type="text" name="destinatarioNombre" placeholder="Nombre completo" />
                        <InputBase type="tel" name="destinatarioTelefono" placeholder="Teléfono" />
                        <InputBase type="email" name="destinatarioCorreo" placeholder="Correo electrónico" />
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <InputBase type="text" name="direccionCalle" placeholder="Calle" />
                        <InputBase type="text" name="direccionNumero" placeholder="Número" />
                        <InputBase type="text" name="direccionCiudad" placeholder="Ciudad" />
                    </div>
                </section>

                {/* Detalles del Paquete */}
                <section className="bg-gray-50 p-6 rounded-lg border border-gray-200">
                    <h3 className="text-lg font-semibold text-purple-900 mb-4">Detalles del Paquete</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Peso del paquete (kg)</label>
                            <InputBase type="number" min="0.001" step="0.001" name="peso" placeholder="0.0" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Prioridad de Envío</label>
                            {/* Al select le agregamos manualmente los mismos estilos de texto/fondo */}
                            <select
                                name="prioridad"
                                value={prioridad}
                                onChange={(e) => setPrioridad(e.target.value)}
                                className="border p-2 rounded w-full bg-white text-gray-900 focus:ring-2 focus:ring-purple-600 outline-none"
                            >
                                <option value="Estándar">Estándar</option>
                                <option value="Exprés">Exprés</option>
                                <option value="Programado">Programado</option>
                            </select>
                        </div>

                        {prioridad === 'Programado' && (
                            <div className="md:col-span-2">
                                <label className="block text-sm font-medium text-gray-700 mb-1">Fecha programada de entrega</label>
                                <InputBase type="date" name="fechaProgramada" value={scheduledDate} onChange={event => setScheduledDate(event.target.value)} />
                            </div>
                        )}
                    </div>

                    <div className="mt-6 flex bg-white p-4 rounded border border-purple-100 justify-around text-center">
                        <div>
                            <span className="block text-sm text-gray-500">Tiempo Estimado</span>
                            <span className="font-bold text-purple-900 text-lg">{estimate?.deliveryWindow ?? '—'}</span>
                        </div>
                        <div className="border-l border-gray-200"></div>
                        <div>
                            <span className="block text-sm text-gray-500">Costo Informativo</span>
                            <span className="font-bold text-orange-600 text-lg">{estimate ? new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(estimate.costCop) : '—'}</span>
                        </div>
                    </div>
                    {estimate && <p className="mt-2 text-sm text-gray-600">Fecha estimada de entrega: {estimate.deliveryDate}. Valor informativo, sujeto a cambios.</p>}
                    {estimateError && <p className="mt-2 text-sm text-orange-700">{estimateError}</p>}
                </section>

                <div className="flex justify-end pt-4 border-t">
                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="bg-orange-500 hover:bg-orange-600 disabled:bg-orange-300 text-white font-bold py-3 px-8 rounded-lg transition-colors"
                    >
                        {isSubmitting ? 'Guardando...' : 'Guardar Envío'}
                    </button>
                </div>
            </Form>
        </div>
    );
}
