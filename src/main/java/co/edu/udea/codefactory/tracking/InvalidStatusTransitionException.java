package co.edu.udea.codefactory.tracking;

public class InvalidStatusTransitionException extends ShipmentConflictException {
    public InvalidStatusTransitionException() { super("Transición de estado no permitida"); }
}
