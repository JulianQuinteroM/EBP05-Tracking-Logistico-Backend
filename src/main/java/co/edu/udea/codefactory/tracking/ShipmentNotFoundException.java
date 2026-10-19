package co.edu.udea.codefactory.tracking;

public class ShipmentNotFoundException extends BusinessException {
    public ShipmentNotFoundException() { super("No se encontró el envío solicitado"); }
}
