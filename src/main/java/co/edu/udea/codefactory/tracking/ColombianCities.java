package co.edu.udea.codefactory.tracking;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class ColombianCities {
    private ColombianCities() {}

    public static final List<String> CITIES = List.of(
        "Arauca", "Armenia", "Barranquilla", "Bogotá", "Bucaramanga", "Cali",
        "Cartagena", "Cúcuta", "Florencia", "Ibagué", "Inírida", "Leticia",
        "Manizales", "Medellín", "Mitú", "Mocoa", "Montería", "Neiva",
        "Pasto", "Pereira", "Popayán", "Puerto Carreño", "Quibdó", "Riohacha",
        "San Andrés", "San José del Guaviare", "Santa Marta", "Sincelejo",
        "Tunja", "Valledupar", "Villavicencio", "Yopal");

    private static String normalize(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public static String requireCity(String submitted) {
        if (submitted == null || submitted.isBlank()) throw new BusinessException("Selecciona la ciudad donde se encuentra el envío");
        return CITIES.stream().filter(city -> normalize(city).equals(normalize(submitted)))
            .findFirst().orElseThrow(() -> new BusinessException("Selecciona una ciudad de la lista"));
    }

    public static List<String> suggest(String query) {
        if (query == null || query.strip().length() < 3) return List.of();
        String normalized = normalize(query);
        return CITIES.stream().filter(city -> normalize(city).contains(normalized)).limit(10).toList();
    }
}
