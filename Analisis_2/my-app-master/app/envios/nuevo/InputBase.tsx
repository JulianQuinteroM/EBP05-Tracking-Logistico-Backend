import { InputHTMLAttributes } from 'react';

// Esta interfaz permite que el componente acepte todas las propiedades 
// nativas de un input HTML (type, name, placeholder, onChange, etc.)
type InputBaseProps = InputHTMLAttributes<HTMLInputElement>;

export default function InputBase({ className = '', ...props }: InputBaseProps) {
    return (
        <input
            {...props}
            className={`border p-2 rounded bg-white text-gray-900 placeholder-gray-500 focus:ring-2 focus:ring-purple-600 outline-none w-full ${className}`}
        />
    );
}