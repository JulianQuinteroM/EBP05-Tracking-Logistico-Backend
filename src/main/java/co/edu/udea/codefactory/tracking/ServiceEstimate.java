package co.edu.udea.codefactory.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ServiceEstimate {
    public record Estimate(BigDecimal costCop, LocalDate deliveryDate, String deliveryWindow) {}

    public Estimate calculate(ServiceType type, LocalDate scheduledDate, LocalDate today) {
        if (type == null) throw new BusinessException("Debe seleccionar un tipo de servicio");
        if (type == ServiceType.PROGRAMADO) {
            if (scheduledDate == null || !scheduledDate.isAfter(today))
                throw new BusinessException("Programado requiere una fecha de entrega futura");
            return new Estimate(new BigDecimal("15000.00"), scheduledDate, "Fecha elegida");
        }
        if (scheduledDate != null) throw new BusinessException("La fecha programada solo aplica al servicio Programado");
        return switch (type) {
            case ESTANDAR -> new Estimate(new BigDecimal("10000.00"), today.plusDays(5), "3-5 días");
            case EXPRES -> new Estimate(new BigDecimal("25000.00"), today.plusDays(2), "24-48 horas");
            case PROGRAMADO -> throw new IllegalStateException("Servicio programado ya tratado");
        };
    }
}
