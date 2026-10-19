import type { Metadata } from "next";
import "./globals.css";
import AppShell from './AppShell';

export const metadata: Metadata = {
  title: "Tracking logístico",
  description: "Registro y seguimiento de envíos",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="es" className="h-full antialiased">
      <body className="min-h-full"><AppShell>{children}</AppShell></body>
    </html>
  );
}
