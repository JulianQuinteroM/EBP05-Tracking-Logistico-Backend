package co.edu.udea.codefactory.tracking;

import java.util.List;
import java.util.Map;

public final class ShipmentStatusPolicy {
    private ShipmentStatusPolicy() {}

    public static final List<ShipmentStatus> JOURNEY = List.of(
        ShipmentStatus.PENDIENTE_RECOGIDA, ShipmentStatus.RECOGIDO,
        ShipmentStatus.EN_TRANSITO, ShipmentStatus.CENTRO_DISTRIBUCION,
        ShipmentStatus.RUTA_ENTREGA, ShipmentStatus.ENTREGADO);

    private static final Map<ShipmentStatus, List<ShipmentStatus>> NEXT = Map.of(
        ShipmentStatus.PENDIENTE_RECOGIDA, List.of(ShipmentStatus.RECOGIDO, ShipmentStatus.INCIDENCIA),
        ShipmentStatus.RECOGIDO, List.of(ShipmentStatus.EN_TRANSITO, ShipmentStatus.INCIDENCIA),
        ShipmentStatus.EN_TRANSITO, List.of(ShipmentStatus.CENTRO_DISTRIBUCION, ShipmentStatus.INCIDENCIA),
        ShipmentStatus.CENTRO_DISTRIBUCION, List.of(ShipmentStatus.RUTA_ENTREGA, ShipmentStatus.INCIDENCIA),
        ShipmentStatus.RUTA_ENTREGA, List.of(ShipmentStatus.ENTREGADO, ShipmentStatus.INCIDENCIA),
        ShipmentStatus.ENTREGADO, List.of(),
        ShipmentStatus.CANCELADO, List.of());

    public static List<ShipmentStatus> allowed(Shipment shipment) {
        if (shipment.status == ShipmentStatus.INCIDENCIA)
            return shipment.statusBeforeIncident == null ? List.of() : List.of(shipment.statusBeforeIncident);
        return NEXT.getOrDefault(shipment.status, List.of());
    }

    public static boolean canRegisterLocation(ShipmentStatus status) {
        return status != ShipmentStatus.PENDIENTE_RECOGIDA
            && status != ShipmentStatus.ENTREGADO && status != ShipmentStatus.CANCELADO;
    }
}
