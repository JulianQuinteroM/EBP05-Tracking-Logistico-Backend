package co.edu.udea.codefactory.tracking;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException() {
        super("Demasiados intentos. Intenta nuevamente en 15 minutos.");
    }
}
