package co.edu.udea.codefactory.tracking;

public class InvalidTrackingCodeException extends BusinessException {
    public InvalidTrackingCodeException() { super("El código ingresado no es válido"); }
}
