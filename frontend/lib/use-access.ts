'use client';
import { useEffect, useState } from 'react';
export function useAccess() {
  const [permissions, setPermissions] = useState<string[]>([]);
  useEffect(() => { const controller = new AbortController();fetch('/api/session', { cache: 'no-store', signal: controller.signal }).then(r => r.json()).then(s => { if (!controller.signal.aborted) setPermissions(s.permissions ?? []); }).catch(() => {});return () => controller.abort(); }, []);
  return (permission: string) => permissions.includes(permission);
}
