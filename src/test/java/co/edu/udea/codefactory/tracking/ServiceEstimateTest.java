package co.edu.udea.codefactory.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ServiceEstimateTest {
    private final ServiceEstimate estimate = new ServiceEstimate();
    private final LocalDate today = LocalDate.of(2026, 9, 15);

    @Test void provisionalCatalog() {
        assertEquals(new BigDecimal("10000.00"), estimate.calculate(ServiceType.ESTANDAR, null, today).costCop());
        assertEquals(today.plusDays(5), estimate.calculate(ServiceType.ESTANDAR, null, today).deliveryDate());
        assertEquals(new BigDecimal("25000.00"), estimate.calculate(ServiceType.EXPRES, null, today).costCop());
        assertEquals(today.plusDays(2), estimate.calculate(ServiceType.EXPRES, null, today).deliveryDate());
        assertEquals(today.plusDays(3), estimate.calculate(ServiceType.PROGRAMADO, today.plusDays(3), today).deliveryDate());
    }

    @Test void deliveryDateOnlyForProgrammedService() {
        assertThrows(BusinessException.class, () -> estimate.calculate(ServiceType.PROGRAMADO, null, today));
        assertThrows(BusinessException.class, () -> estimate.calculate(ServiceType.PROGRAMADO, today, today));
        assertThrows(BusinessException.class, () -> estimate.calculate(ServiceType.EXPRES, today.plusDays(2), today));
    }
}
