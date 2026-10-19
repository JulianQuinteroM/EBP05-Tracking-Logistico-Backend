import { InputHTMLAttributes } from 'react';

type InputBaseProps = InputHTMLAttributes<HTMLInputElement>;

export default function InputBase({ className = '', ...props }: InputBaseProps) {
    return (
        <input
            {...props}
            className={`app-field ${className}`}
        />
    );
}
