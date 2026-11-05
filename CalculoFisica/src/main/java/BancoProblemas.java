/*
 * ============================================================================
 *  BancoProblemas.java
 *  Banco de problemas de RAZONES RELACIONADAS (derivadas respecto al tiempo).
 *  Lógica pura: NO usa JavaFX, así que se puede probar y reutilizar sola.
 * ============================================================================
 *  REGLAS DE REDACCIÓN (tomadas de ENTRENAMIENTO-SOFTWARE.md)
 *    1. Nunca variables abstractas de libro ("sea x...", "sea y...", "sea s...").
 *    2. Las ecuaciones se escriben con los NOMBRES REALES de las magnitudes
 *       ("la altura del globo", "la distancia entre los aviones") y con los
 *       VALORES NUMÉRICOS concretos del problema.
 *    3. Razonamiento paso a paso y regla de la cadena aplicada directamente
 *       sobre números y unidades.
 *
 *  CÓMO SE ARMA CADA PROBLEMA
 *    Problema  = enunciado + parámetros editables + una función resolver().
 *    Solucion  = resultado analítico (regla de la cadena) + pasos redactados
 *                + una CURVA f(t) de la magnitud derivada. La curva sirve para
 *                dibujar la tangente y para VERIFICAR el resultado de forma
 *                numérica (diferencia central) en la pestaña.
 *
 *  CÓMO AGREGAR UN PROBLEMA NUEVO
 *    1) Escribe un método  static Solucion miProblema(double[] p)  como los de abajo.
 *    2) Añade un  new Base(...)  a la lista BASE y su entrada t(...) en TODOS (sección final).
 *    La pestaña lo mostrará sola en el menú.
 * ============================================================================
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

public final class BancoProblemas {

    private BancoProblemas() { }

    /** Resultado completo de resolver un problema con unos datos concretos. */
    record Solucion(String resultado, String unidad, double valor, boolean magnitud,
                    List<String> pasos, String interpretacion,
                    DoubleUnaryOperator curva, double t0, double tMin, double tMax,
                    String unidadTiempo, String tituloGrafico, String ejeY) { }

    /** Definición de un problema: texto, datos editables y función que lo resuelve. */
    record Base(String nombre, String enunciado,
                    String[] etiquetas, String[] unidades, double[] iniciales,
                    Function<double[], Solucion> resolver) {
        @Override public String toString() { return nombre; }
    }

    // =========================================================================
    //  BASE: estado del problema EN EL INSTANTE PEDIDO (5 ejemplos + 14 propuestos + plantilla)
    //  No se muestra directo: la sección "MODELO TEMPORAL" (al final) la envuelve con t₀, t₁ y t₂.
    // =========================================================================
    static final List<Base> BASE = List.of(

        new Base("Ejemplo 1 · Globo aerostático",
            "Se suelta un globo a 150 pies de un observador. Sube a 8 pies/segundo. ¿Qué tan rápido aumenta "
          + "la distancia al observador cuando el globo está a 50 pies de altura?",
            new String[]{"Distancia horizontal fija al observador", "Velocidad de subida del globo", "Altura del globo en el instante pedido"},
            new String[]{"pies", "pies/s", "pies"},
            new double[]{150, 8, 50}, BancoProblemas::globo),

        new Base("Ejemplo 2 · Tanque cónico",
            "Entra agua a un tanque cónico a 8 pies³/min. La altura del tanque es 12 pies y el radio superior es 6 pies. "
          + "¿Qué tan rápido sube el nivel del agua cuando la profundidad es 4 pies?",
            new String[]{"Caudal de entrada de agua", "Altura del tanque", "Radio superior del tanque", "Profundidad del agua en el instante pedido"},
            new String[]{"pies³/min", "pies", "pies", "pies"},
            new double[]{8, 12, 6, 4}, BancoProblemas::tanqueConico),

        new Base("Ejemplo 3 · Dos aviones (norte y este)",
            "Un avión A vuela al norte a 640 mph y un avión B vuela al este a 600 mph. A las 12:15 pm, B está sobre la ciudad "
          + "y A ya ha recorrido 160 millas. ¿Qué tan rápido se separan a la 1:15 pm?",
            new String[]{"Velocidad del avión que va al norte", "Velocidad del avión que va al este", "Millas que ya llevaba el avión del norte", "Horas transcurridas hasta el instante pedido"},
            new String[]{"mph", "mph", "millas", "horas"},
            new double[]{640, 600, 160, 1}, BancoProblemas::dosAviones),

        new Base("Ejemplo 4 · Telescopio y bote",
            "Un telescopio a 250 pies sobre el agua observa un bote que se acerca a 20 pies/segundo. "
          + "¿A qué velocidad cambia el ángulo de observación cuando el bote está a 250 pies de la playa?",
            new String[]{"Altura del telescopio sobre el agua", "Velocidad con que se acerca el bote", "Distancia del bote a la playa en el instante pedido"},
            new String[]{"pies", "pies/s", "pies"},
            new double[]{250, 20, 250}, BancoProblemas::telescopio),

        new Base("Ejemplo 5 · Sombra de un edificio",
            "Un edificio mide 120 pies. El sol se pone. ¿Qué tan rápido crece la sombra cuando el ángulo del sol es 45° (π/4)?",
            new String[]{"Altura del edificio", "Ángulo del sol sobre el horizonte", "Tiempo de una vuelta completa de la Tierra"},
            new String[]{"pies", "grados", "s"},
            new double[]{120, 45, 86400}, BancoProblemas::sombraEdificio),

        new Base("Problema 1 · Cubo",
            "Una arista de un cubo aumenta a 3 pulgadas/segundo. ¿Con qué rapidez cambia el volumen cuando la arista mide 12 pulgadas?",
            new String[]{"Velocidad de aumento de la arista", "Arista en el instante pedido"},
            new String[]{"pulg/s", "pulg"},
            new double[]{3, 12}, BancoProblemas::cubo),

        new Base("Problema 2 · Pompa de jabón",
            "Se sopla aire dentro de una pompa de jabón esférica a 3 pulgadas³/segundo. ¿Con qué rapidez aumenta el radio cuando mide 3 pulgadas?",
            new String[]{"Caudal de aire que entra", "Radio en el instante pedido"},
            new String[]{"pulg³/s", "pulg"},
            new double[]{3, 3}, BancoProblemas::pompa),

        new Base("Problema 3 · Avión y observador",
            "Un avión vuela a 1 milla de altitud y a 400 mph. ¿Con qué rapidez aumenta la distancia entre el avión y el observador "
          + "45 segundos después de pasar por encima de él? (En 45 s recorre 5 millas.)",
            new String[]{"Altitud del avión", "Velocidad del avión", "Segundos desde que pasó por encima"},
            new String[]{"millas", "mph", "s"},
            new double[]{1, 400, 45}, BancoProblemas::avionObservador),

        new Base("Problema 4 · Vaso cónico",
            "Se bebe agua de un vaso cónico a 3 cm³/segundo. La altura del vaso es 10 cm y el diámetro superior es 6 cm. "
          + "¿Con qué rapidez desciende el nivel cuando la profundidad es 5 cm?",
            new String[]{"Ritmo al que se bebe el agua", "Altura del vaso", "Diámetro superior del vaso", "Profundidad del agua en el instante pedido"},
            new String[]{"cm³/s", "cm", "cm", "cm"},
            new double[]{3, 10, 6, 5}, BancoProblemas::vasoConico),

        new Base("Problema 5 · Dos aviones (oeste y norte)",
            "Un avión vuela al oeste a 300 mph y pasa por una ciudad a mediodía. Otro avión vuela al norte a 400 mph y pasa por la misma "
          + "ciudad una hora después. ¿Con qué rapidez cambia la distancia entre ellos a las 2:00 pm?",
            new String[]{"Velocidad del avión que va al oeste", "Velocidad del avión que va al norte", "Horas que pasa el avión del norte después del mediodía", "Horas desde el mediodía hasta el instante pedido"},
            new String[]{"mph", "mph", "horas", "horas"},
            new double[]{300, 400, 1, 2}, BancoProblemas::dosAvionesVariante),

        new Base("Problema 6 · Bote y cuerda",
            "Una mujer en un muelle, a 10 pies sobre el agua, jala una cuerda atada a un bote a 2 pies/segundo. "
          + "¿Con qué rapidez se acerca el bote al muelle cuando faltan 25 pies de cuerda?",
            new String[]{"Altura de la mujer sobre el agua", "Velocidad con que jala la cuerda", "Longitud de cuerda que falta por recoger"},
            new String[]{"pies", "pies/s", "pies"},
            new double[]{10, 2, 25}, BancoProblemas::boteCuerda),

        new Base("Problema 7 · Escalera que resbala",
            "Una escalera de 20 pies está apoyada en una pared y su base se aleja a 1 pie/segundo. "
          + "¿Con qué rapidez desciende la punta cuando la base está a 5 pies de la pared?",
            new String[]{"Longitud de la escalera", "Velocidad con que se aleja la base", "Distancia de la base a la pared en el instante pedido"},
            new String[]{"pies", "pies/s", "pies"},
            new double[]{20, 1, 5}, BancoProblemas::escalera),

        new Base("Problema 8 · Derrame de petróleo",
            "Unas bacterias consumen una mancha de petróleo cilíndrica a 4 pies³/hora. El grosor es 0.001 pie, el diámetro es 500 pies y "
          + "el grosor disminuye a 0.0005 pie/hora. ¿Cuál es la razón de cambio del área de la mancha?",
            new String[]{"Volumen de petróleo consumido por hora", "Grosor de la mancha", "Diámetro de la mancha", "Disminución del grosor por hora"},
            new String[]{"pies³/h", "pies", "pies", "pies/h"},
            new double[]{4, 0.001, 500, 0.0005}, BancoProblemas::derrame),

        new Base("Problema 9 · Cono de arena",
            "Sale arena de una banda a 16 pies³/segundo y forma un cono cuya altura es 1/4 del diámetro de la base. "
          + "¿Con qué rapidez aumenta la altura del cono cuando mide 4 pies?",
            new String[]{"Caudal de arena", "Relación altura / diámetro de la base", "Altura del cono en el instante pedido"},
            new String[]{"pies³/s", "", "pies"},
            new double[]{16, 0.25, 4}, BancoProblemas::conoArena),

        new Base("Problema 10 · Avión y ángulo de elevación",
            "El ángulo de elevación de un avión visto desde un observador es 1/2 radián y aumenta a 1/10 radián/segundo. "
          + "¿Cuál es la velocidad del avión? (El enunciado no trae la altitud: se supone 1 milla; puedes cambiarla.)",
            new String[]{"Altitud del avión (dato supuesto)", "Ángulo de elevación en el instante pedido", "Velocidad con que aumenta el ángulo"},
            new String[]{"millas", "rad", "rad/s"},
            new double[]{1, 0.5, 0.1}, BancoProblemas::avionAngulo),

        new Base("Problema 11a · Sombra de Chris (crecimiento de la sombra)",
            "Chris mide 6 pies y camina alejándose de un poste de 30 pies a 2 pies/segundo. "
          + "(a) ¿Con qué rapidez aumenta la longitud de su sombra cuando está a 24 pies del poste?",
            new String[]{"Estatura de Chris", "Altura del poste", "Velocidad de Chris", "Distancia de Chris al poste en el instante pedido"},
            new String[]{"pies", "pies", "pies/s", "pies"},
            new double[]{6, 30, 2, 24}, BancoProblemas::chrisSombra),

        new Base("Problema 11b · Sombra de Chris (extremo de la sombra)",
            "Chris mide 6 pies y camina alejándose de un poste de 30 pies a 2 pies/segundo. "
          + "(b) ¿Con qué velocidad se mueve el extremo de su sombra?",
            new String[]{"Estatura de Chris", "Altura del poste", "Velocidad de Chris", "Distancia de Chris al poste (solo para la gráfica)"},
            new String[]{"pies", "pies", "pies/s", "pies"},
            new double[]{6, 30, 2, 24}, BancoProblemas::chrisExtremo),

        new Base("Problema 11c · Sombra de Chris (ángulo de sus ojos)",
            "Chris mide 6 pies y camina alejándose de un poste de 30 pies a 2 pies/segundo. "
          + "(c) ¿Con qué velocidad angular cambia la dirección de su mirada hacia el extremo de su sombra cuando la sombra mide 6 pies? "
          + "(Se mide el ángulo de la mirada respecto a la horizontal, con los ojos a la altura de su estatura.)",
            new String[]{"Estatura de Chris (altura de sus ojos)", "Altura del poste", "Velocidad de Chris", "Longitud de la sombra en el instante pedido"},
            new String[]{"pies", "pies", "pies/s", "pies"},
            new double[]{6, 30, 2, 6}, BancoProblemas::chrisAngulo),

        new Base("Problema 12 · Triángulo isósceles",
            "Un triángulo isósceles tiene dos lados iguales de 100 cm. El ángulo del vértice aumenta a 1/10 radián/minuto. "
          + "¿Con qué rapidez aumenta el área cuando ese ángulo es π/6 (30°)?",
            new String[]{"Longitud de los lados iguales", "Velocidad con que aumenta el ángulo del vértice", "Ángulo del vértice en el instante pedido"},
            new String[]{"cm", "rad/min", "grados"},
            new double[]{100, 0.1, 30}, BancoProblemas::isosceles),

        new Base("Problema 13 · Paso a desnivel",
            "Un auto viaja a 66 pies/s sobre un paso a desnivel y un tren viaja a 88 pies/s por debajo; sus trayectorias son perpendiculares "
          + "y están separadas por 100 pies de altura. ¿Con qué rapidez se separan 10 segundos después de cruzarse?",
            new String[]{"Velocidad del auto", "Velocidad del tren", "Separación vertical entre ambos", "Segundos desde que se cruzaron"},
            new String[]{"pies/s", "pies/s", "pies", "s"},
            new double[]{66, 88, 100, 10}, BancoProblemas::pasoDesnivel),

        new Base("Problema 14 · Tanque en cono truncado",
            "Entra agua a 2000 cm³/min a un tanque en forma de cono truncado de 80 cm de altura, con radio de 20 cm en el fondo y 40 cm arriba. "
          + "¿Con qué rapidez sube el nivel cuando la profundidad del agua es 30 cm?",
            new String[]{"Caudal de entrada de agua", "Altura del tanque", "Radio en el fondo", "Radio en la parte superior", "Profundidad del agua en el instante pedido"},
            new String[]{"cm³/min", "cm", "cm", "cm", "cm"},
            new double[]{2000, 80, 20, 40, 30}, BancoProblemas::conoTruncado),

        new Base("Plantilla general · Distancia entre dos cuerpos en movimiento",
            "Dos cuerpos se mueven sobre direcciones perpendiculares (y, si quieres, a distinta altura fija). Con la posición y la velocidad "
          + "de cada uno, calcula con qué rapidez cambia la distancia entre ellos. Sirve para aviones, autos, barcos, globos, etc. "
          + "(Velocidad positiva = el cuerpo se aleja del punto de cruce; negativa = se acerca.)",
            new String[]{"Posición actual del primer cuerpo", "Velocidad del primer cuerpo", "Posición actual del segundo cuerpo", "Velocidad del segundo cuerpo", "Separación fija perpendicular (0 si no hay)"},
            new String[]{"u", "u/tiempo", "u", "u/tiempo", "u"},
            new double[]{600, 600, 800, 640, 0}, BancoProblemas::generica)
    );

    // =========================================================================
    //  UTILIDADES DE FORMATO Y VALIDACIÓN
    // =========================================================================
    /** Número legible: hasta 4 decimales sin ceros sobrantes; notación científica si es muy pequeño. */
    static String g(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return String.valueOf(v);
        if (v == 0) return "0";
        double a = Math.abs(v);
        if (a < 0.001 || a >= 1e9) return String.format(Locale.US, "%.4e", v);
        return new BigDecimal(v).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** Número entre paréntesis, como en los ejemplos: (150), (−2). */
    private static String p(double v) { return "(" + g(v) + ")"; }

    private static void pos(String nombre, double v) {
        if (!(v > 0)) throw new IllegalArgumentException("«" + nombre + "» debe ser mayor que cero.");
    }

    private static void noNeg(String nombre, double v) {
        if (!(v >= 0)) throw new IllegalArgumentException("«" + nombre + "» no puede ser negativo.");
    }

    private static String cambio(String sujeto, double v, String unidad) {
        if (Math.abs(v) < 1e-12) return sujeto + " no está cambiando en ese instante.";
        return sujeto + (v > 0 ? " está aumentando" : " está disminuyendo")
             + " a razón de " + g(Math.abs(v)) + " " + unidad + ".";
    }

    // =========================================================================
    //  EJEMPLO 1 · GLOBO
    // =========================================================================
    static Solucion globo(double[] d) {
        double base = d[0], v = d[1], alt = d[2];
        pos("Distancia horizontal fija", base); pos("Velocidad de subida", v); noNeg("Altura del globo", alt);
        double dist = Math.hypot(base, alt);
        double vel = alt * v / dist;

        List<String> s = new ArrayList<>();
        s.add("La base del triángulo rectángulo es constante: " + g(base) + " pies (entre el observador y el punto donde se soltó el globo).");
        s.add("La altura del globo cambia a " + g(v) + " pies/segundo.");
        s.add("Queremos la velocidad a la que cambia la distancia al observador cuando la altura del globo es " + g(alt) + " pies.");
        s.add("Por Pitágoras: (Distancia al observador)² = " + p(base) + "² + (Altura del globo)².");
        s.add("Derivando con respecto al tiempo (regla de la cadena): 2 × (Distancia al observador) × (Velocidad de la distancia) = 2 × (Altura del globo) × (Velocidad de la altura).");
        s.add("Distancia actual: √(" + g(base) + "² + " + g(alt) + "²) = " + g(dist) + " pies.");
        s.add("Reemplazando: 2 × " + p(dist) + " × (Velocidad buscada) = 2 × " + p(alt) + " × " + p(v) + ".");
        s.add("Velocidad buscada = " + g(2 * alt * v) + " / " + g(2 * dist) + " = " + g(vel) + " pies/segundo.");

        double t0 = alt / v;
        return new Solucion("Velocidad de cambio de la distancia al observador", "pies/s", vel, false, s,
            cambio("La distancia entre el globo y el observador", vel, "pies/segundo")
          + " Eso es menos que los " + g(v) + " pies/s a los que sube el globo, porque solo una parte de su movimiento es hacia afuera del observador.",
            t -> Math.hypot(base, v * t), t0, 0, Math.max(2 * t0, 1), "s",
            "Distancia del globo al observador", "Distancia al observador [pies]");
    }

    // =========================================================================
    //  EJEMPLO 2 · TANQUE CÓNICO
    // =========================================================================
    static Solucion tanqueConico(double[] d) {
        double q = d[0], alto = d[1], radio = d[2], prof = d[3];
        pos("Caudal de entrada", q); pos("Altura del tanque", alto); pos("Radio superior", radio); pos("Profundidad", prof);
        if (prof > alto) throw new IllegalArgumentException("La profundidad no puede ser mayor que la altura del tanque.");
        double k = radio / alto;
        double c = Math.PI * k * k / 3.0;               // Volumen = c × (Profundidad)³
        double vel = q / (3 * c * prof * prof);

        List<String> s = new ArrayList<>();
        s.add("El agua entra a " + g(q) + " pies³/minuto. Queremos la velocidad de subida del nivel cuando la profundidad es " + g(prof) + " pies.");
        s.add("Por triángulos semejantes (radio superior " + g(radio) + " / altura " + g(alto) + " = " + g(k) + "): radio del agua = " + g(k) + " × (Profundidad).");
        s.add("Volumen del agua = (π/3) × (Radio del agua)² × (Profundidad) = (π/3) × (" + g(k) + " × Profundidad)² × (Profundidad) = "
            + g(c) + " × (Profundidad)³.");
        s.add("Derivando con respecto al tiempo: " + g(q) + " = 3 × " + p(c) + " × (Profundidad)² × (Velocidad de la profundidad) = "
            + g(3 * c) + " × (Profundidad)² × (Velocidad de la profundidad).");
        s.add("Sustituyendo la profundidad de " + g(prof) + " pies: " + g(q) + " = " + g(3 * c) + " × " + p(prof) + "² × (Velocidad buscada).");
        s.add("Velocidad buscada = " + g(q) + " / " + g(3 * c * prof * prof) + " = " + g(vel) + " pies/minuto.");

        return new Solucion("Velocidad de subida del nivel del agua", "pies/min", vel, false, s,
            cambio("El nivel del agua", vel, "pies/minuto"),
            t -> Math.cbrt(Math.max(q * t, 0) / c), c * prof * prof * prof / q, 0, c * alto * alto * alto / q, "min",
            "Profundidad del agua en el tanque", "Profundidad [pies]");
    }

    // =========================================================================
    //  EJEMPLO 3 · DOS AVIONES (NORTE / ESTE)
    // =========================================================================
    static Solucion dosAviones(double[] d) {
        double vN = d[0], vE = d[1], n0 = d[2], hrs = d[3];
        pos("Velocidad del avión del norte", vN); pos("Velocidad del avión del este", vE);
        noNeg("Millas iniciales del avión del norte", n0); pos("Horas transcurridas", hrs);
        double este = vE * hrs, norte = n0 + vN * hrs;
        double dist = Math.hypot(este, norte);
        double vel = (este * vE + norte * vN) / dist;

        List<String> s = new ArrayList<>();
        s.add("En " + g(hrs) + " hora(s), el avión que va al este recorre " + g(vE) + " × " + g(hrs) + " = " + g(este) + " millas.");
        s.add("El avión que va al norte recorre " + g(vN * hrs) + " millas en ese tiempo, más las " + g(n0) + " que ya llevaba = " + g(norte) + " millas en total desde la ciudad.");
        s.add("La distancia entre ellos al cuadrado es: " + p(este) + "² + " + p(norte) + "² = " + g(dist * dist) + ". Por lo tanto, están a " + g(dist) + " millas de distancia.");
        s.add("Derivando (Distancia entre los aviones)² = (Distancia este)² + (Distancia norte)²:  2 × " + p(dist) + " × (Velocidad de separación) = 2 × "
            + p(este) + " × " + p(vE) + " + 2 × " + p(norte) + " × " + p(vN) + ".");
        s.add("Velocidad de separación = (" + g(2 * este * vE) + " + " + g(2 * norte * vN) + ") / " + g(2 * dist) + " = " + g(vel) + " mph.");

        return new Solucion("Velocidad de separación entre los aviones", "mph", vel, false, s,
            cambio("La distancia entre los dos aviones", vel, "millas/hora"),
            t -> Math.hypot(vE * t, n0 + vN * t), hrs, 0, 2 * hrs, "h",
            "Distancia entre los aviones", "Distancia entre los aviones [millas]");
    }

    // =========================================================================
    //  EJEMPLO 4 · TELESCOPIO Y BOTE
    // =========================================================================
    static Solucion telescopio(double[] d) {
        double alt = d[0], v = d[1], dist = d[2];
        pos("Altura del telescopio", alt); pos("Velocidad del bote", v); pos("Distancia a la playa", dist);
        double cot = dist / alt, csc2 = 1 + cot * cot;
        double ang = Math.atan2(alt, dist);
        double vel = (v / alt) / csc2;

        List<String> s = new ArrayList<>();
        s.add("La altura es constante: " + g(alt) + " pies. La distancia del bote a la playa disminuye a " + g(v) + " pies/segundo.");
        s.add("Queremos la velocidad de cambio del ángulo de observación cuando la distancia es " + g(dist) + " pies.");
        s.add("Por trigonometría: cot(Ángulo) = (Distancia del bote) / " + g(alt) + ".");
        s.add("Derivando con respecto al tiempo: −csc²(Ángulo) × (Velocidad del ángulo) = (Velocidad de la distancia) / " + g(alt) + ".");
        s.add("Cuando la distancia es " + g(dist) + ", cot(Ángulo) = " + g(cot) + ", el ángulo es " + g(Math.toDegrees(ang)) + "° y csc²(Ángulo) = 1 + cot² = " + g(csc2)
            + ". La velocidad de la distancia es −" + g(v) + " (se acerca).");
        s.add("−" + g(csc2) + " × (Velocidad buscada) = −" + g(v) + " / " + g(alt) + ".");
        s.add("Velocidad buscada = " + g(vel) + " radianes/segundo.");

        return new Solucion("Velocidad de cambio del ángulo de observación", "rad/s", vel, false, s,
            cambio("El ángulo de observación", vel, "radianes/segundo (" + g(Math.toDegrees(Math.abs(vel))) + " °/s)"),
            t -> Math.atan2(alt, dist - v * t), 0, -0.5 * dist / v, 0.9 * dist / v, "s",
            "Ángulo de observación del bote", "Ángulo [rad]");
    }

    // =========================================================================
    //  EJEMPLO 5 · SOMBRA DE UN EDIFICIO
    // =========================================================================
    static Solucion sombraEdificio(double[] d) {
        double alto = d[0], gr = d[1], vuelta = d[2];
        pos("Altura del edificio", alto); pos("Tiempo de una vuelta", vuelta);
        if (!(gr > 0 && gr < 90)) throw new IllegalArgumentException("El ángulo del sol debe estar entre 0° y 90°.");
        double th = Math.toRadians(gr);
        double w = -2 * Math.PI / vuelta;
        double csc2 = 1.0 / (Math.sin(th) * Math.sin(th));
        double vel = -alto * csc2 * w;

        List<String> s = new ArrayList<>();
        s.add("La altura del edificio es " + g(alto) + " pies. El ángulo del sol con el horizonte disminuye a −2π / " + g(vuelta) + " = " + g(w)
            + " radianes/segundo (por la rotación de la Tierra).");
        s.add("La longitud de la sombra es " + g(alto) + " × cot(Ángulo del sol).");
        s.add("Derivando: (Velocidad de la sombra) = −" + g(alto) + " × csc²(Ángulo del sol) × (Velocidad del ángulo).");
        s.add("Con el sol a " + g(gr) + "°, csc²(Ángulo) = " + g(csc2) + ".");
        s.add("Velocidad de la sombra = −" + g(alto) + " × " + p(csc2) + " × " + p(w) + " = " + g(vel) + " pies/segundo.");

        double span = Math.min(vuelta / 24.0, 0.8 * Math.min(th, Math.PI / 2 - th) / Math.abs(w));
        return new Solucion("Velocidad de crecimiento de la sombra", "pies/s", vel, false, s,
            cambio("La sombra del edificio", vel, "pies/segundo (" + g(vel * 3600) + " pies/hora)"),
            t -> alto / Math.tan(th + w * t), 0, -span, span, "s",
            "Longitud de la sombra del edificio", "Longitud de la sombra [pies]");
    }

    // =========================================================================
    //  PROBLEMA 1 · CUBO
    // =========================================================================
    static Solucion cubo(double[] d) {
        double v = d[0], a = d[1];
        pos("Velocidad de la arista", v); pos("Arista", a);
        double vel = 3 * a * a * v;

        List<String> s = new ArrayList<>();
        s.add("La arista del cubo aumenta a " + g(v) + " pulgadas/segundo. Queremos la rapidez de cambio del volumen cuando la arista mide " + g(a) + " pulgadas.");
        s.add("Volumen del cubo = (Arista)³.");
        s.add("Derivando con respecto al tiempo (regla de la cadena): (Velocidad del volumen) = 3 × (Arista)² × (Velocidad de la arista).");
        s.add("Sustituyendo: (Velocidad del volumen) = 3 × " + p(a) + "² × " + p(v) + " = " + g(vel) + " pulgadas³/segundo.");

        return new Solucion("Rapidez de cambio del volumen del cubo", "pulg³/s", vel, false, s,
            cambio("El volumen del cubo", vel, "pulgadas³/segundo"),
            t -> Math.pow(a + v * t, 3), 0, -0.5 * a / v, a / v, "s",
            "Volumen del cubo", "Volumen [pulg³]");
    }

    // =========================================================================
    //  PROBLEMA 2 · POMPA DE JABÓN
    // =========================================================================
    static Solucion pompa(double[] d) {
        double q = d[0], r = d[1];
        pos("Caudal de aire", q); pos("Radio", r);
        double area = 4 * Math.PI * r * r;
        double vel = q / area;
        double v0 = 4.0 / 3.0 * Math.PI * r * r * r;

        List<String> s = new ArrayList<>();
        s.add("Entra aire a " + g(q) + " pulgadas³/segundo. Queremos la rapidez de aumento del radio cuando mide " + g(r) + " pulgadas.");
        s.add("Volumen de la pompa = (4/3) × π × (Radio)³.");
        s.add("Derivando con respecto al tiempo: (Velocidad del volumen) = 4 × π × (Radio)² × (Velocidad del radio).");
        s.add("Sustituyendo: " + g(q) + " = 4 × π × " + p(r) + "² × (Velocidad buscada) = " + g(area) + " × (Velocidad buscada).");
        s.add("Velocidad buscada = " + g(q) + " / " + g(area) + " = " + g(vel) + " pulgadas/segundo.");

        return new Solucion("Rapidez de aumento del radio de la pompa", "pulg/s", vel, false, s,
            cambio("El radio de la pompa", vel, "pulgadas/segundo"),
            t -> Math.cbrt(3 * Math.max(v0 + q * t, 0) / (4 * Math.PI)), 0, -0.5 * v0 / q, v0 / q, "s",
            "Radio de la pompa de jabón", "Radio [pulg]");
    }

    // =========================================================================
    //  PROBLEMA 3 · AVIÓN Y OBSERVADOR
    // =========================================================================
    static Solucion avionObservador(double[] d) {
        double alt = d[0], v = d[1], seg = d[2];
        pos("Altitud", alt); pos("Velocidad del avión", v); noNeg("Segundos", seg);
        double rec = v * seg / 3600.0;
        double dist = Math.hypot(alt, rec);
        double vel = rec * v / dist;

        List<String> s = new ArrayList<>();
        s.add("El avión vuela a una altitud constante de " + g(alt) + " milla(s) y a " + g(v) + " millas/hora. Queremos la rapidez de aumento de la distancia al observador "
            + g(seg) + " segundos después de pasar por encima.");
        s.add("En " + g(seg) + " segundos el avión recorre " + g(v) + " × " + g(seg) + " / 3600 = " + g(rec) + " millas en horizontal.");
        s.add("Por Pitágoras: (Distancia al observador)² = " + p(alt) + "² + (Recorrido horizontal)².");
        s.add("Distancia actual: √(" + g(alt) + "² + " + g(rec) + "²) = " + g(dist) + " millas.");
        s.add("Derivando: 2 × (Distancia al observador) × (Velocidad de la distancia) = 2 × (Recorrido horizontal) × (Velocidad del avión).");
        s.add("Reemplazando: 2 × " + p(dist) + " × (Velocidad buscada) = 2 × " + p(rec) + " × " + p(v) + ".");
        s.add("Velocidad buscada = " + g(2 * rec * v) + " / " + g(2 * dist) + " = " + g(vel) + " millas/hora.");

        double t0 = seg / 3600.0;
        return new Solucion("Rapidez de aumento de la distancia al observador", "mph", vel, false, s,
            cambio("La distancia entre el avión y el observador", vel, "millas/hora")
          + (vel > 0.95 * v ? " Es casi la velocidad del avión (" + g(v) + " mph) porque ya está lejos y casi se aleja en línea recta." : ""),
            t -> Math.hypot(alt, v * t), t0, 0, Math.max(2 * t0, 0.01), "h",
            "Distancia del avión al observador", "Distancia al observador [millas]");
    }

    // =========================================================================
    //  PROBLEMA 4 · VASO CÓNICO
    // =========================================================================
    static Solucion vasoConico(double[] d) {
        double q = d[0], alto = d[1], diam = d[2], prof = d[3];
        pos("Ritmo al beber", q); pos("Altura del vaso", alto); pos("Diámetro superior", diam); pos("Profundidad", prof);
        if (prof > alto) throw new IllegalArgumentException("La profundidad no puede ser mayor que la altura del vaso.");
        double radio = diam / 2.0, k = radio / alto;
        double c = Math.PI * k * k / 3.0;
        double vel = -q / (3 * c * prof * prof);
        double v0 = c * prof * prof * prof;

        List<String> s = new ArrayList<>();
        s.add("Se bebe agua a " + g(q) + " cm³/segundo, así que el volumen de agua cambia a −" + g(q) + " cm³/segundo. Queremos la rapidez de descenso del nivel cuando la profundidad es " + g(prof) + " cm.");
        s.add("El radio superior es " + g(diam) + " / 2 = " + g(radio) + " cm. Por triángulos semejantes (" + g(radio) + " / " + g(alto) + " = " + g(k) + "): radio del agua = " + g(k) + " × (Profundidad).");
        s.add("Volumen del agua = (π/3) × (Radio del agua)² × (Profundidad) = (π/3) × (" + g(k) + " × Profundidad)² × (Profundidad) = " + g(c) + " × (Profundidad)³.");
        s.add("Derivando con respecto al tiempo: −" + g(q) + " = 3 × " + p(c) + " × (Profundidad)² × (Velocidad de la profundidad) = " + g(3 * c) + " × (Profundidad)² × (Velocidad de la profundidad).");
        s.add("Sustituyendo la profundidad de " + g(prof) + " cm: −" + g(q) + " = " + g(3 * c) + " × " + p(prof) + "² × (Velocidad buscada).");
        s.add("Velocidad buscada = −" + g(q) + " / " + g(3 * c * prof * prof) + " = " + g(vel) + " cm/segundo (negativa: el nivel baja).");

        return new Solucion("Rapidez de cambio del nivel del agua", "cm/s", vel, false, s,
            cambio("El nivel del agua del vaso", vel, "cm/segundo"),
            t -> Math.cbrt(Math.max(v0 - q * t, 0) / c), 0, -(c * alto * alto * alto - v0) / q, 0.95 * v0 / q, "s",
            "Profundidad del agua en el vaso", "Profundidad [cm]");
    }

    // =========================================================================
    //  PROBLEMA 5 · DOS AVIONES (OESTE / NORTE)
    // =========================================================================
    static Solucion dosAvionesVariante(double[] d) {
        double vW = d[0], vN = d[1], retraso = d[2], hrs = d[3];
        pos("Velocidad del avión del oeste", vW); pos("Velocidad del avión del norte", vN);
        noNeg("Retraso del avión del norte", retraso);
        if (hrs < retraso) throw new IllegalArgumentException("El instante pedido debe ser posterior al paso del avión del norte por la ciudad.");
        double oeste = vW * hrs, tNorte = hrs - retraso, norte = vN * tNorte;
        double dist = Math.hypot(oeste, norte);
        if (dist == 0) throw new IllegalArgumentException("En ese instante ambos aviones están sobre la ciudad (distancia 0).");
        double vel = (oeste * vW + norte * vN) / dist;

        List<String> s = new ArrayList<>();
        s.add("Desde el mediodía hasta el instante pedido pasan " + g(hrs) + " horas: el avión que va al oeste recorre " + g(vW) + " × " + g(hrs) + " = " + g(oeste) + " millas.");
        s.add("El avión que va al norte pasó por la ciudad " + g(retraso) + " hora(s) después, así que lleva volando " + g(hrs) + " − " + g(retraso) + " = " + g(tNorte)
            + " hora(s): " + g(vN) + " × " + g(tNorte) + " = " + g(norte) + " millas.");
        s.add("Las trayectorias son perpendiculares (oeste y norte). Por Pitágoras: (Distancia entre los aviones)² = (Distancia oeste)² + (Distancia norte)².");
        s.add("Distancia actual: √(" + g(oeste) + "² + " + g(norte) + "²) = " + g(dist) + " millas.");
        s.add("Derivando: 2 × " + p(dist) + " × (Velocidad buscada) = 2 × " + p(oeste) + " × " + p(vW) + " + 2 × " + p(norte) + " × " + p(vN) + ".");
        s.add("Velocidad buscada = (" + g(2 * oeste * vW) + " + " + g(2 * norte * vN) + ") / " + g(2 * dist) + " = " + g(vel) + " mph.");

        return new Solucion("Rapidez de cambio de la distancia entre los aviones", "mph", vel, false, s,
            cambio("La distancia entre los dos aviones", vel, "millas/hora"),
            t -> Math.hypot(vW * t, vN * Math.max(0, t - retraso)), hrs, 0, hrs + Math.max(1, hrs - retraso), "h",
            "Distancia entre los aviones", "Distancia entre los aviones [millas]");
    }

    // =========================================================================
    //  PROBLEMA 6 · BOTE Y CUERDA
    // =========================================================================
    static Solucion boteCuerda(double[] d) {
        double alt = d[0], v = d[1], cuerda = d[2];
        pos("Altura sobre el agua", alt); pos("Velocidad de la cuerda", v); pos("Longitud de cuerda", cuerda);
        if (cuerda <= alt) throw new IllegalArgumentException("La cuerda debe ser más larga que la altura de la mujer sobre el agua.");
        double horiz = Math.sqrt(cuerda * cuerda - alt * alt);
        double vel = cuerda * (-v) / horiz;

        List<String> s = new ArrayList<>();
        s.add("La mujer está a " + g(alt) + " pies sobre el agua y jala la cuerda a " + g(v) + " pies/segundo. Queremos la rapidez con que se acerca el bote cuando faltan " + g(cuerda) + " pies de cuerda.");
        s.add("La altura es constante (" + g(alt) + " pies) y la longitud de la cuerda disminuye a " + g(v) + " pies/segundo (velocidad −" + g(v) + ").");
        s.add("Por Pitágoras: (Longitud de la cuerda)² = " + p(alt) + "² + (Distancia horizontal al bote)².");
        s.add("Distancia horizontal actual: √(" + g(cuerda) + "² − " + g(alt) + "²) = " + g(horiz) + " pies.");
        s.add("Derivando: 2 × (Longitud de la cuerda) × (Velocidad de la cuerda) = 2 × (Distancia horizontal) × (Velocidad del bote).");
        s.add("Reemplazando: 2 × " + p(cuerda) + " × (−" + g(v) + ") = 2 × " + p(horiz) + " × (Velocidad buscada).");
        s.add("Velocidad buscada = " + g(-2 * cuerda * v) + " / " + g(2 * horiz) + " = " + g(vel) + " pies/segundo (negativa: el bote se acerca).");

        return new Solucion("Velocidad del bote respecto al muelle", "pies/s", vel, false, s,
            "El bote se acerca al muelle a " + g(Math.abs(vel)) + " pies/segundo, más rápido que los " + g(v) + " pies/s a los que se jala la cuerda.",
            t -> Math.sqrt(Math.max((cuerda - v * t) * (cuerda - v * t) - alt * alt, 0)), 0,
            -0.4 * (cuerda - alt) / v, 0.98 * (cuerda - alt) / v, "s",
            "Distancia horizontal del bote al muelle", "Distancia horizontal [pies]");
    }

    // =========================================================================
    //  PROBLEMA 7 · ESCALERA
    // =========================================================================
    static Solucion escalera(double[] d) {
        double largo = d[0], v = d[1], base = d[2];
        pos("Longitud de la escalera", largo); pos("Velocidad de la base", v); pos("Distancia de la base", base);
        if (base >= largo) throw new IllegalArgumentException("La base debe estar más cerca de la pared que la longitud de la escalera.");
        double punta = Math.sqrt(largo * largo - base * base);
        double vel = -base * v / punta;

        List<String> s = new ArrayList<>();
        s.add("La escalera mide " + g(largo) + " pies (constante). Su base se aleja de la pared a " + g(v) + " pie(s)/segundo. Queremos la rapidez de descenso de la punta cuando la base está a " + g(base) + " pies de la pared.");
        s.add("Por Pitágoras: " + p(largo) + "² = (Distancia de la base a la pared)² + (Altura de la punta)².");
        s.add("Altura actual de la punta: √(" + g(largo) + "² − " + g(base) + "²) = " + g(punta) + " pies.");
        s.add("Derivando (" + g(largo * largo) + " es constante, su derivada es 0): 0 = 2 × (Distancia de la base) × (Velocidad de la base) + 2 × (Altura de la punta) × (Velocidad de la punta).");
        s.add("Reemplazando: 0 = 2 × " + p(base) + " × " + p(v) + " + 2 × " + p(punta) + " × (Velocidad buscada).");
        s.add("Velocidad buscada = −" + g(2 * base * v) + " / " + g(2 * punta) + " = " + g(vel) + " pies/segundo (negativa: la punta desciende).");

        return new Solucion("Velocidad de la punta de la escalera", "pies/s", vel, false, s,
            "La punta de la escalera desciende a " + g(Math.abs(vel)) + " pies/segundo. Ojo: cuanto más se aleja la base de la pared, más rápido cae la punta.",
            t -> Math.sqrt(Math.max(largo * largo - (base + v * t) * (base + v * t), 0)), 0,
            -base / v, 0.995 * (largo - base) / v, "s",
            "Altura de la punta de la escalera", "Altura de la punta [pies]");
    }

    // =========================================================================
    //  PROBLEMA 8 · DERRAME DE PETRÓLEO
    // =========================================================================
    static Solucion derrame(double[] d) {
        double consumo = d[0], grosor = d[1], diam = d[2], bajaGrosor = d[3];
        pos("Volumen consumido por hora", consumo); pos("Grosor", grosor); pos("Diámetro", diam); noNeg("Disminución del grosor", bajaGrosor);
        double radio = diam / 2.0, area = Math.PI * radio * radio;
        double vVol = -consumo, vGrosor = -bajaGrosor;
        double vel = (vVol - area * vGrosor) / grosor;

        List<String> s = new ArrayList<>();
        s.add("Las bacterias consumen el petróleo a " + g(consumo) + " pies³/hora, así que el volumen de la mancha cambia a −" + g(consumo) + " pies³/hora. El grosor es " + g(grosor)
            + " pies y disminuye a " + g(bajaGrosor) + " pies/hora. El diámetro es " + g(diam) + " pies, o sea un radio de " + g(radio) + " pies.");
        s.add("Queremos la razón de cambio del área de la mancha.");
        s.add("La mancha es un cilindro muy delgado: Volumen = (Área de la mancha) × (Grosor).");
        s.add("Área actual = π × " + p(radio) + "² = " + g(area) + " pies².");
        s.add("Derivando con la regla del producto: (Velocidad del volumen) = (Velocidad del área) × (Grosor) + (Área) × (Velocidad del grosor).");
        s.add("Reemplazando: −" + g(consumo) + " = (Velocidad buscada) × " + p(grosor) + " + " + p(area) + " × (−" + g(bajaGrosor) + ").");
        s.add("−" + g(consumo) + " = " + g(grosor) + " × (Velocidad buscada) − " + g(area * bajaGrosor) + "  ⇒  Velocidad buscada = (−" + g(consumo) + " + " + g(area * bajaGrosor)
            + ") / " + g(grosor) + " = " + g(vel) + " pies²/hora.");

        double v0 = area * grosor;
        double span = bajaGrosor > 0 ? grosor / bajaGrosor : 1;
        return new Solucion("Razón de cambio del área de la mancha", "pies²/h", vel, false, s,
            cambio("El área de la mancha de petróleo", vel, "pies²/hora")
          + (vel > 0 ? " Aunque se consume petróleo, el grosor baja tan rápido que la mancha se extiende." : ""),
            t -> (v0 + vVol * t) / (grosor + vGrosor * t), 0, -0.5 * span, 0.9 * span, "h",
            "Área de la mancha de petróleo", "Área [pies²]");
    }

    // =========================================================================
    //  PROBLEMA 9 · CONO DE ARENA
    // =========================================================================
    static Solucion conoArena(double[] d) {
        double q = d[0], k = d[1], alt = d[2];
        pos("Caudal de arena", q); pos("Relación altura/diámetro", k); pos("Altura", alt);
        double f = 1.0 / (2 * k);                         // radio de la base = f × altura
        double c = Math.PI * f * f / 3.0;                 // Volumen = c × (Altura)³
        double vel = q / (3 * c * alt * alt);
        double v0 = c * alt * alt * alt;

        List<String> s = new ArrayList<>();
        s.add("Sale arena a " + g(q) + " pies³/segundo y forma un cono. Queremos la rapidez de aumento de la altura cuando mide " + g(alt) + " pies.");
        s.add("La altura del cono es " + g(k) + " del diámetro de la base, o sea (Radio de la base) = (Altura) / (2 × " + g(k) + ") = " + g(f) + " × (Altura).");
        s.add("Volumen = (π/3) × (Radio de la base)² × (Altura) = (π/3) × (" + g(f) + " × Altura)² × (Altura) = " + g(c) + " × (Altura)³.");
        s.add("Derivando con respecto al tiempo: " + g(q) + " = 3 × " + p(c) + " × (Altura)² × (Velocidad de la altura) = " + g(3 * c) + " × (Altura)² × (Velocidad de la altura).");
        s.add("Sustituyendo la altura de " + g(alt) + " pies: " + g(q) + " = " + g(3 * c) + " × " + p(alt) + "² × (Velocidad buscada).");
        s.add("Velocidad buscada = " + g(q) + " / " + g(3 * c * alt * alt) + " = " + g(vel) + " pies/segundo.");

        return new Solucion("Rapidez de aumento de la altura del cono", "pies/s", vel, false, s,
            cambio("La altura del montón de arena", vel, "pies/segundo"),
            t -> Math.cbrt(Math.max(v0 + q * t, 0) / c), 0, -0.9 * v0 / q, v0 / q, "s",
            "Altura del cono de arena", "Altura [pies]");
    }

    // =========================================================================
    //  PROBLEMA 10 · AVIÓN Y ÁNGULO DE ELEVACIÓN
    // =========================================================================
    static Solucion avionAngulo(double[] d) {
        double alt = d[0], th = d[1], w = d[2];
        pos("Altitud", alt); pos("Velocidad del ángulo", w);
        if (!(th > 0 && th < Math.PI / 2)) throw new IllegalArgumentException("El ángulo de elevación debe estar entre 0 y π/2 radianes (≈ 1.5708).");
        double csc2 = 1.0 / (Math.sin(th) * Math.sin(th));
        double vDist = -alt * csc2 * w;                   // velocidad de la distancia horizontal (negativa: se acerca)
        double rapidez = Math.abs(vDist);

        List<String> s = new ArrayList<>();
        s.add("El avión vuela a una altitud constante de " + g(alt) + " milla(s) (dato que el enunciado no trae; es editable). El ángulo de elevación vale " + g(th)
            + " radianes y aumenta a " + g(w) + " radianes/segundo, así que el avión se acerca al observador.");
        s.add("Por trigonometría: cot(Ángulo de elevación) = (Distancia horizontal al observador) / " + g(alt) + ".");
        s.add("Derivando con respecto al tiempo: −csc²(Ángulo) × (Velocidad del ángulo) = (Velocidad de la distancia horizontal) / " + g(alt) + ".");
        s.add("csc²(" + g(th) + " rad) = 1 / sen²(" + g(th) + ") = " + g(csc2) + ".");
        s.add("Velocidad de la distancia horizontal = −" + g(csc2) + " × " + p(w) + " × " + p(alt) + " = " + g(vDist) + " millas/segundo (negativa: se acerca).");
        s.add("La velocidad del avión es la magnitud: " + g(rapidez) + " millas/segundo = " + g(rapidez * 3600) + " millas/hora.");

        return new Solucion("Velocidad del avión", "millas/s", vDist, true, s,
            "El avión se acerca al observador a " + g(rapidez) + " millas/segundo (≈ " + g(rapidez * 3600) + " millas/hora). "
          + "El signo negativo de la pendiente en la gráfica solo indica que la distancia horizontal disminuye.",
            t -> alt / Math.tan(th + w * t), 0, -0.8 * th / w, 0.8 * (Math.PI / 2 - th) / w, "s",
            "Distancia horizontal entre el avión y el observador", "Distancia horizontal [millas]");
    }

    // =========================================================================
    //  PROBLEMA 11 · SOMBRA DE CHRIS (a, b, c)
    // =========================================================================
    private static void validarChris(double estatura, double poste, double v) {
        pos("Estatura de Chris", estatura); pos("Altura del poste", poste); pos("Velocidad de Chris", v);
        if (poste <= estatura) throw new IllegalArgumentException("El poste debe ser más alto que Chris para que se forme sombra.");
    }

    /** Pasos comunes de triángulos semejantes: sombra = q × distancia al poste. */
    private static void semejanza(List<String> s, double e, double h, double v, double q) {
        s.add("Chris mide " + g(e) + " pies, el poste mide " + g(h) + " pies y Chris se aleja del poste a " + g(v) + " pies/segundo.");
        s.add("Triángulos semejantes: el de Chris (" + g(e) + " de alto y la sombra de base) y el del poste (" + g(h) + " de alto y base = sombra + distancia al poste): "
            + "(Sombra) / " + g(e) + " = (Sombra + Distancia al poste) / " + g(h) + ".");
        s.add("Despejando: " + g(h) + " × (Sombra) = " + g(e) + " × (Sombra) + " + g(e) + " × (Distancia al poste)  ⇒  " + g(h - e) + " × (Sombra) = " + g(e)
            + " × (Distancia al poste)  ⇒  (Sombra) = " + g(q) + " × (Distancia al poste).");
    }

    static Solucion chrisSombra(double[] d) {
        double e = d[0], h = d[1], v = d[2], dist = d[3];
        validarChris(e, h, v); pos("Distancia al poste", dist);
        double q = e / (h - e);
        double vel = q * v;

        List<String> s = new ArrayList<>();
        semejanza(s, e, h, v, q);
        s.add("Con Chris a " + g(dist) + " pies del poste, la sombra mide " + g(q) + " × " + g(dist) + " = " + g(q * dist) + " pies.");
        s.add("Derivando con respecto al tiempo: (Velocidad de la sombra) = " + g(q) + " × (Velocidad de Chris) = " + g(q) + " × " + g(v) + " = " + g(vel) + " pies/segundo.");
        s.add("Observa que no depende de qué tan lejos esté Chris del poste: la sombra crece siempre al mismo ritmo.");

        return new Solucion("Rapidez de aumento de la sombra", "pies/s", vel, false, s,
            cambio("La sombra de Chris", vel, "pies/segundo"),
            t -> q * (dist + v * t), 0, -0.5 * dist / v, dist / v, "s",
            "Longitud de la sombra de Chris", "Longitud de la sombra [pies]");
    }

    static Solucion chrisExtremo(double[] d) {
        double e = d[0], h = d[1], v = d[2], dist = d[3];
        validarChris(e, h, v); pos("Distancia al poste", dist);
        double q = e / (h - e);
        double vel = (1 + q) * v;

        List<String> s = new ArrayList<>();
        semejanza(s, e, h, v, q);
        s.add("El extremo de la sombra está a (Distancia al poste) + (Sombra) = (1 + " + g(q) + ") × (Distancia al poste) = " + g(1 + q) + " × (Distancia al poste) del poste.");
        s.add("Derivando con respecto al tiempo: (Velocidad del extremo) = " + g(1 + q) + " × (Velocidad de Chris) = " + g(1 + q) + " × " + g(v) + " = " + g(vel) + " pies/segundo.");

        return new Solucion("Velocidad del extremo de la sombra", "pies/s", vel, false, s,
            "El extremo de la sombra se mueve a " + g(vel) + " pies/segundo respecto al poste: más rápido que Chris (" + g(v) + " pies/s), porque la sombra además se alarga.",
            t -> (1 + q) * (dist + v * t), 0, -0.5 * dist / v, dist / v, "s",
            "Posición del extremo de la sombra respecto al poste", "Distancia al poste [pies]");
    }

    static Solucion chrisAngulo(double[] d) {
        double e = d[0], h = d[1], v = d[2], sombra = d[3];
        validarChris(e, h, v); pos("Longitud de la sombra", sombra);
        double q = e / (h - e);
        double vSombra = q * v;
        double tan = e / sombra, sec2 = 1 + tan * tan;
        double vel = (-e / (sombra * sombra) * vSombra) / sec2;

        List<String> s = new ArrayList<>();
        semejanza(s, e, h, v, q);
        s.add("Como (Sombra) = " + g(q) + " × (Distancia al poste), la sombra crece a " + g(q) + " × " + g(v) + " = " + g(vSombra) + " pies/segundo.");
        s.add("Los ojos de Chris están a " + g(e) + " pies de altura y miran hacia el extremo de la sombra, que está en el suelo a (Sombra) de sus pies. "
            + "Por trigonometría: tan(Ángulo de la mirada) = " + g(e) + " / (Sombra).");
        s.add("Derivando con respecto al tiempo: sec²(Ángulo) × (Velocidad del ángulo) = −" + g(e) + " / (Sombra)² × (Velocidad de la sombra).");
        s.add("Con la sombra de " + g(sombra) + " pies: tan(Ángulo) = " + g(tan) + ", el ángulo es " + g(Math.toDegrees(Math.atan(tan))) + "° y sec²(Ángulo) = 1 + tan² = " + g(sec2) + ".");
        s.add(g(sec2) + " × (Velocidad buscada) = −" + g(e) + " / " + p(sombra) + "² × " + p(vSombra) + " = " + g(-e / (sombra * sombra) * vSombra) + ".");
        s.add("Velocidad buscada = " + g(vel) + " radianes/segundo (negativa: la mirada se va levantando hacia la horizontal).");

        return new Solucion("Velocidad angular de la mirada de Chris", "rad/s", vel, false, s,
            "El ángulo con que Chris mira hacia el extremo de su sombra (medido desde la horizontal) disminuye a " + g(Math.abs(vel)) + " radianes/segundo (≈ "
          + g(Math.toDegrees(Math.abs(vel))) + " °/s): conforme la sombra se alarga, la mirada se acerca a la horizontal.",
            t -> Math.atan2(e, sombra + vSombra * t), 0, -0.5 * sombra / vSombra, 3 * sombra / vSombra, "s",
            "Ángulo de la mirada de Chris hacia el extremo de su sombra", "Ángulo [rad]");
    }

    // =========================================================================
    //  PROBLEMA 12 · TRIÁNGULO ISÓSCELES
    // =========================================================================
    static Solucion isosceles(double[] d) {
        double lado = d[0], w = d[1], gr = d[2];
        pos("Lados iguales", lado); pos("Velocidad del ángulo", w);
        if (!(gr > 0 && gr < 180)) throw new IllegalArgumentException("El ángulo del vértice debe estar entre 0° y 180°.");
        double th = Math.toRadians(gr);
        double k = 0.5 * lado * lado;
        double vel = k * Math.cos(th) * w;

        List<String> s = new ArrayList<>();
        s.add("Los dos lados iguales miden " + g(lado) + " cm (constantes). El ángulo del vértice aumenta a " + g(w) + " radianes/minuto. Queremos la rapidez de aumento del área cuando ese ángulo es "
            + g(gr) + "° (" + g(th) + " rad).");
        s.add("Área del triángulo = ½ × (Lado)² × sen(Ángulo del vértice) = ½ × " + p(lado) + "² × sen(Ángulo del vértice) = " + g(k) + " × sen(Ángulo del vértice).");
        s.add("Derivando con respecto al tiempo: (Velocidad del área) = " + g(k) + " × cos(Ángulo del vértice) × (Velocidad del ángulo).");
        s.add("cos(" + g(gr) + "°) = " + g(Math.cos(th)) + ".");
        s.add("(Velocidad del área) = " + g(k) + " × " + p(Math.cos(th)) + " × " + p(w) + " = " + g(vel) + " cm²/minuto.");

        double tMax = 0.5 / w, tMin = -Math.min(0.5 / w, 0.9 * th / w);
        return new Solucion("Rapidez de aumento del área del triángulo", "cm²/min", vel, false, s,
            cambio("El área del triángulo", vel, "cm²/minuto"),
            t -> k * Math.sin(th + w * t), 0, tMin, tMax, "min",
            "Área del triángulo isósceles", "Área [cm²]");
    }

    // =========================================================================
    //  PROBLEMA 13 · PASO A DESNIVEL
    // =========================================================================
    static Solucion pasoDesnivel(double[] d) {
        double vA = d[0], vT = d[1], alto = d[2], seg = d[3];
        pos("Velocidad del auto", vA); pos("Velocidad del tren", vT); pos("Separación vertical", alto); pos("Segundos", seg);
        double a = vA * seg, tr = vT * seg;
        double dist = Math.sqrt(a * a + tr * tr + alto * alto);
        double vel = (a * vA + tr * vT) / dist;

        List<String> s = new ArrayList<>();
        s.add("El auto va a " + g(vA) + " pies/s por el paso a desnivel y el tren a " + g(vT) + " pies/s por debajo. Sus trayectorias son perpendiculares y están separadas verticalmente por "
            + g(alto) + " pies. Se supone que ambos pasan por el cruce en el instante cero y se alejan de él. Queremos la rapidez de separación " + g(seg) + " segundos después.");
        s.add("En " + g(seg) + " segundos el auto recorre " + g(vA) + " × " + g(seg) + " = " + g(a) + " pies y el tren " + g(vT) + " × " + g(seg) + " = " + g(tr) + " pies.");
        s.add("Por Pitágoras en el espacio: (Distancia entre ellos)² = (Recorrido del auto)² + (Recorrido del tren)² + " + p(alto) + "²  (la separación vertical es constante).");
        s.add("Distancia actual: √(" + g(a) + "² + " + g(tr) + "² + " + g(alto) + "²) = " + g(dist) + " pies.");
        s.add("Derivando: 2 × " + p(dist) + " × (Velocidad de separación) = 2 × " + p(a) + " × " + p(vA) + " + 2 × " + p(tr) + " × " + p(vT) + " + 0  (la separación vertical no cambia).");
        s.add("Velocidad de separación = (" + g(2 * a * vA) + " + " + g(2 * tr * vT) + ") / " + g(2 * dist) + " = " + g(vel) + " pies/segundo.");

        return new Solucion("Rapidez de separación entre el auto y el tren", "pies/s", vel, false, s,
            cambio("La distancia entre el auto y el tren", vel, "pies/segundo"),
            t -> Math.sqrt((vA * t) * (vA * t) + (vT * t) * (vT * t) + alto * alto), seg, 0, 2 * seg, "s",
            "Distancia entre el auto y el tren", "Distancia [pies]");
    }

    // =========================================================================
    //  PROBLEMA 14 · CONO TRUNCADO
    // =========================================================================
    static Solucion conoTruncado(double[] d) {
        double q = d[0], alto = d[1], r1 = d[2], r2 = d[3], prof = d[4];
        pos("Caudal", q); pos("Altura del tanque", alto); pos("Radio en el fondo", r1); pos("Radio superior", r2); pos("Profundidad", prof);
        if (r1 == r2) throw new IllegalArgumentException("Los radios deben ser distintos (si fueran iguales sería un cilindro).");
        if (prof > alto) throw new IllegalArgumentException("La profundidad no puede ser mayor que la altura del tanque.");
        double m = (r2 - r1) / alto;                      // cuánto crece el radio por cada unidad de profundidad
        double rAgua = r1 + m * prof;
        double vel = q / (Math.PI * rAgua * rAgua);

        DoubleUnaryOperator volumen = x -> Math.PI / (3 * m) * (Math.pow(r1 + m * x, 3) - r1 * r1 * r1);
        double v0 = volumen.applyAsDouble(prof), vTop = volumen.applyAsDouble(alto);

        List<String> s = new ArrayList<>();
        s.add("Entra agua a " + g(q) + " cm³/minuto a un tanque en forma de cono truncado de " + g(alto) + " cm de altura, con radio de " + g(r1) + " cm en el fondo y " + g(r2)
            + " cm arriba. Queremos la rapidez con que sube el nivel cuando la profundidad del agua es " + g(prof) + " cm.");
        s.add("El radio crece de forma lineal con la altura: (Radio del agua) = " + g(r1) + " + (" + g(r2) + " − " + g(r1) + ") / " + g(alto) + " × (Profundidad) = "
            + g(r1) + " + " + g(m) + " × (Profundidad).");
        s.add("Volumen de agua (cono truncado) = (π/3) × (Profundidad) × (" + g(r1 * r1) + " + " + g(r1) + " × (Radio del agua) + (Radio del agua)²), que equivale a (π / (3 × " + g(m)
            + ")) × ((Radio del agua)³ − " + g(r1) + "³).");
        s.add("Derivando con respecto al tiempo: (Velocidad del volumen) = (π / " + g(m) + ") × (Radio del agua)² × (Velocidad del radio) y, como (Velocidad del radio) = " + g(m)
            + " × (Velocidad de la profundidad), queda (Velocidad del volumen) = π × (Radio del agua)² × (Velocidad de la profundidad).");
        s.add("Radio del agua a " + g(prof) + " cm de profundidad: " + g(r1) + " + " + g(m) + " × " + g(prof) + " = " + g(rAgua) + " cm.");
        s.add("Sustituyendo: " + g(q) + " = π × " + p(rAgua) + "² × (Velocidad buscada) = " + g(Math.PI * rAgua * rAgua) + " × (Velocidad buscada).");
        s.add("Velocidad buscada = " + g(q) + " / " + g(Math.PI * rAgua * rAgua) + " = " + g(vel) + " cm/minuto.");

        // La profundidad como función del tiempo se obtiene invirtiendo el volumen (que crece con la profundidad) por bisección.
        DoubleUnaryOperator curva = t -> {
            double objetivo = v0 + q * t;
            if (objetivo <= 0) return 0;
            if (objetivo >= vTop) return alto;
            double lo = 0, hi = alto;
            for (int i = 0; i < 80; i++) {
                double mid = 0.5 * (lo + hi);
                if (volumen.applyAsDouble(mid) < objetivo) lo = mid; else hi = mid;
            }
            return 0.5 * (lo + hi);
        };
        double tMax = Math.max((vTop - v0) / q, 0.2 * v0 / q);
        return new Solucion("Rapidez de elevación del nivel del agua", "cm/min", vel, false, s,
            cambio("El nivel del agua", vel, "cm/minuto"),
            curva, 0, -0.9 * v0 / q, tMax, "min",
            "Profundidad del agua en el tanque", "Profundidad [cm]");
    }

    // =========================================================================
    //  PLANTILLA GENERAL · DISTANCIA ENTRE DOS CUERPOS
    // =========================================================================
    static Solucion generica(double[] d) {
        double a = d[0], vA = d[1], b = d[2], vB = d[3], c = d[4];
        noNeg("Separación fija", c);
        double dist = Math.sqrt(a * a + b * b + c * c);
        if (dist == 0) throw new IllegalArgumentException("La distancia actual es cero: no se puede calcular la razón de cambio.");
        double vel = (a * vA + b * vB) / dist;

        List<String> s = new ArrayList<>();
        s.add("El primer cuerpo está a " + g(a) + " u del punto de cruce y cambia a " + g(vA) + " u/tiempo; el segundo está a " + g(b) + " u y cambia a " + g(vB)
            + " u/tiempo. La separación fija entre sus planos es " + g(c) + " u.");
        s.add("Por Pitágoras: (Distancia entre ellos)² = (Posición del primero)² + (Posición del segundo)² + " + p(c) + "².");
        s.add("Distancia actual: √(" + g(a) + "² + " + g(b) + "² + " + g(c) + "²) = " + g(dist) + " u.");
        s.add("Derivando: 2 × " + p(dist) + " × (Velocidad de separación) = 2 × " + p(a) + " × " + p(vA) + " + 2 × " + p(b) + " × " + p(vB) + " + 0  (la separación fija no cambia).");
        s.add("Velocidad de separación = (" + g(2 * a * vA) + " + " + g(2 * b * vB) + ") / " + g(2 * dist) + " = " + g(vel) + " u/tiempo.");

        double sa = Math.abs(vA) > 1e-9 ? Math.abs(a / vA) : 0, sb = Math.abs(vB) > 1e-9 ? Math.abs(b / vB) : 0;
        double span = Math.max(sa, sb);
        if (!(span > 0) || Double.isInfinite(span)) span = 1;
        return new Solucion("Velocidad de separación entre los dos cuerpos", "u/tiempo", vel, false, s,
            cambio("La distancia entre los dos cuerpos", vel, "u/tiempo"),
            t -> Math.sqrt((a + vA * t) * (a + vA * t) + (b + vB * t) * (b + vB * t) + c * c), 0, -span, span, "tiempo",
            "Distancia entre los dos cuerpos", "Distancia [u]");
    }

    // =========================================================================
    //  MODELO TEMPORAL  ·  t₀, t₁, t₂  +  FORMA FORMAL (Leibniz)  +  GEOMETRÍA
    // =========================================================================
    //  Los datos de cada problema se dan en el instante INICIAL t₀. El usuario escoge:
    //     t₀ = instante inicial        (aquí valen los datos que escribió),
    //     t₁ = instante de evaluación  (aquí se calcula la razón de cambio),
    //     t₂ = instante final          (límite de la gráfica).
    //  "aEstado" lleva los datos desde t₀ hasta t₁ con la cinemática de razones constantes
    //  (por ejemplo: altura(t₁) = altura(t₀) + velocidad × (t₁ − t₀)). Con ese estado se
    //  resuelve el problema (resolver de Base) y se construye la curva de la magnitud.

    /** Qué esquema geométrico dibuja LienzoGeometria. */
    enum TipoGeo { TRIANGULO, CONO, NINGUNA }

    /** Datos que necesita el lienzo para dibujar (triángulo rectángulo o cono / cono truncado). */
    record Geometria(TipoGeo tipo, String titulo,
                     double a, double b, double da, double db, String nomA, String nomB, String nomC,
                     double alto, double radioTope, double radioFondo, double nivel, double dNivel, boolean pila,
                     String unidad, String unidadTasa) {
        static Geometria ninguna() {
            return new Geometria(TipoGeo.NINGUNA, "", 0, 0, 0, 0, "", "", "", 0, 0, 0, 0, 0, false, "", "");
        }
        /** Triángulo rectángulo: cateto horizontal a, cateto vertical b (con sus razones) e hipotenusa nomC. */
        static Geometria triangulo(String titulo, String nomA, double a, double da, String nomB, double b, double db,
                                   String nomC, String unidad, String unidadTasa) {
            return new Geometria(TipoGeo.TRIANGULO, titulo, a, b, da, db, nomA, nomB, nomC, 0, 0, 0, 0, 0, false, unidad, unidadTasa);
        }
        /** Cono (radioFondo = 0), cono truncado (radioFondo > 0) o montón de arena (pila = true). */
        static Geometria cono(String titulo, double alto, double radioTope, double radioFondo, double nivel, double dNivel,
                              boolean pila, String unidad, String unidadTasa) {
            return new Geometria(TipoGeo.CONO, titulo, 0, 0, 0, 0, "", "", "", alto, radioTope, radioFondo, nivel, dNivel, pila, unidad, unidadTasa);
        }
    }

    /** Las 4 partes del procedimiento formal: fórmula, derivada, sustitución y símbolo de Leibniz del resultado. */
    record Formal(String formula, String leyenda, String derivada, String sustitucion, String simbolo, String nota) { }

    record Extra(Formal formal, Geometria geo) { }

    /** Problema tal como lo ve la pestaña: datos en t₀, tiempos por defecto y cómo evolucionan hasta t₁. */
    record Problema(String nombre, String enunciado,
                    String[] etiquetas, String[] unidades, double[] iniciales,
                    double[] tiempos, String unidadTiempo, double escala,
                    BiFunction<double[], Double, double[]> aEstado, Base base,
                    BiFunction<double[], Double, Extra> extra) {
        @Override public String toString() { return nombre; }
    }

    /** Resultado completo, ya sobre el reloj del usuario (t₀, t₁, t₂). */
    record Resultado(String resultado, String unidad, double valor, boolean magnitud,
                     List<String> pasos, String interpretacion,
                     DoubleUnaryOperator curva, double t0, double t1, double t2,
                     String unidadTiempo, String tituloGrafico, String ejeY, double escala,
                     Formal formal, Geometria geo, String estado) { }

    /**
     * Resuelve un problema para los datos iniciales (en t₀), evaluando la razón de cambio en t₁
     * y graficando hasta t₂.
     *  - Derivación SIMBÓLICA: la regla de la cadena la aplica cada método de Base (resultado analítico exacto).
     *  - Aproximación NUMÉRICA: la curva devuelta se deriva luego en la pestaña con diferencia central.
     */
    static Resultado resolver(Problema pr, double[] datos, double t0, double t1, double t2) {
        if (!(t2 > t0)) throw new IllegalArgumentException("t₂ debe ser mayor que t₀.");
        if (t1 < t0 || t1 > t2) throw new IllegalArgumentException("Debe cumplirse t₀ ≤ t₁ ≤ t₂.");
        double[] e = pr.aEstado().apply(datos, t1 - t0);           // estado del problema en t₁
        Solucion s = pr.base().resolver().apply(e);                // regla de la cadena en t₁
        Extra ex = pr.extra().apply(e, s.valor());                 // fórmula, derivada, sustitución y geometría
        final double tInterno = s.t0(), esc = pr.escala();
        // La curva de Base vive en su propio reloj (tInterno = instante evaluado). Se traslada al reloj del usuario.
        DoubleUnaryOperator curva = t -> s.curva().applyAsDouble(tInterno + (t - t1) * esc);

        StringBuilder st = new StringBuilder();
        String[] et = pr.base().etiquetas(), un = pr.base().unidades();
        for (int i = 0; i < e.length; i++) {
            if (i > 0) st.append(";  ");
            st.append(et[i]).append(" = ").append(g(e[i]));
            if (!un[i].isEmpty()) st.append(" ").append(un[i]);
        }
        return new Resultado(s.resultado(), s.unidad(), s.valor(), s.magnitud(), s.pasos(), s.interpretacion(),
                curva, t0, t1, t2, pr.unidadTiempo(), s.tituloGrafico(), s.ejeY(), esc, ex.formal(), ex.geo(), st.toString());
    }

    /** Formato para los campos de texto (hasta 6 decimales, sin notación científica). */
    static String campo(double v) {
        return new BigDecimal(v).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static double r6(double v) { return Math.round(v * 1e6) / 1e6; }

    // -------------------------------------------------------------------------
    //  Evolución del estado desde t₀ hasta t₁ (cinemática con razones constantes)
    // -------------------------------------------------------------------------
    private static double volumenTruncado(double r1, double m, double prof) {
        return Math.PI / (3 * m) * (Math.pow(r1 + m * prof, 3) - r1 * r1 * r1);
    }

    /** Profundidad en un cono truncado tras agregar q·dt de volumen (se invierte V(profundidad) por bisección). */
    private static double profundidadTruncado(double q, double alto, double r1, double r2, double prof0, double dt) {
        double m = (r2 - r1) / alto;
        double objetivo = volumenTruncado(r1, m, prof0) + q * dt;
        if (objetivo <= 0) return 0;
        if (objetivo >= volumenTruncado(r1, m, alto)) return alto;
        double lo = 0, hi = alto;
        for (int i = 0; i < 80; i++) {
            double mid = 0.5 * (lo + hi);
            if (volumenTruncado(r1, m, mid) < objetivo) lo = mid; else hi = mid;
        }
        return 0.5 * (lo + hi);
    }

    private static double[] evoDerrame(double[] d, double dt) {
        double grosor = d[1] - d[3] * dt;
        double vol = Math.PI * (d[2] / 2) * (d[2] / 2) * d[1] - d[0] * dt;        // volumen = área × grosor
        if (vol <= 0 || grosor <= 0) return new double[]{d[0], Double.NaN, Double.NaN, d[3]};
        return new double[]{d[0], grosor, 2 * Math.sqrt(vol / grosor / Math.PI), d[3]};
    }

    // -------------------------------------------------------------------------
    //  LISTA QUE USA LA PESTAÑA: (índice en BASE, etiquetas, unidades, datos en t₀, {t₀, t₁, t₂}, unidad de tiempo, escala, evolución, extra)
    //  Los t₁ por defecto reproducen exactamente el instante de cada enunciado.
    // -------------------------------------------------------------------------
    private static Problema t(int i, String[] et, String[] un, double[] ini, double[] tiempos, String uT, double escala,
                              BiFunction<double[], Double, double[]> evo, BiFunction<double[], Double, Extra> ex) {
        Base ba = BASE.get(i);
        return new Problema(ba.nombre(), ba.enunciado(), et, un, ini, tiempos, uT, escala, evo, ba, ex);
    }

    static final List<Problema> TODOS = List.of(
        t(0, new String[]{"Distancia horizontal fija al observador", "Velocidad de subida del globo", "Altura del globo en t₀"},
             new String[]{"pies", "pies/s", "pies"}, new double[]{150, 8, 0}, new double[]{0, 6.25, 12.5}, "s", 1,
             (d, dt) -> new double[]{d[0], d[1], d[2] + d[1] * dt}, BancoProblemas::exGlobo),

        t(1, new String[]{"Caudal de entrada de agua", "Altura del tanque", "Radio superior del tanque", "Profundidad del agua en t₀"},
             new String[]{"pies³/min", "pies", "pies", "pies"}, new double[]{8, 12, 6, 0}, new double[]{0, r6(2 * Math.PI / 3), 6}, "min", 1,
             (d, dt) -> {
                 double k = d[2] / d[1], c = Math.PI * k * k / 3;               // volumen = c × profundidad³
                 return new double[]{d[0], d[1], d[2], Math.cbrt((c * Math.pow(d[3], 3) + d[0] * dt) / c)};
             }, BancoProblemas::exTanque),

        t(2, new String[]{"Velocidad del avión que va al norte", "Velocidad del avión que va al este", "Millas que ya llevaba el del norte en t₀"},
             new String[]{"mph", "mph", "millas"}, new double[]{640, 600, 160}, new double[]{0, 1, 2}, "h", 1,
             (d, dt) -> new double[]{d[0], d[1], d[2], dt}, BancoProblemas::exDosAviones),

        t(3, new String[]{"Altura del telescopio sobre el agua", "Velocidad con que se acerca el bote", "Distancia del bote a la playa en t₀"},
             new String[]{"pies", "pies/s", "pies"}, new double[]{250, 20, 500}, new double[]{0, 12.5, 20}, "s", 1,
             (d, dt) -> new double[]{d[0], d[1], d[2] - d[1] * dt}, BancoProblemas::exTelescopio),

        t(4, new String[]{"Altura del edificio", "Ángulo del sol sobre el horizonte en t₀", "Tiempo de una vuelta completa de la Tierra"},
             new String[]{"pies", "grados", "s"}, new double[]{120, 60, 86400}, new double[]{0, 3600, 7200}, "s", 1,
             (d, dt) -> new double[]{d[0], d[1] - Math.toDegrees(2 * Math.PI / d[2] * dt), d[2]}, BancoProblemas::exSombraEdificio),

        t(5, new String[]{"Velocidad de aumento de la arista", "Arista en t₀"},
             new String[]{"pulg/s", "pulg"}, new double[]{3, 6}, new double[]{0, 2, 4}, "s", 1,
             (d, dt) -> new double[]{d[0], d[1] + d[0] * dt}, BancoProblemas::exCubo),

        t(6, new String[]{"Caudal de aire que entra", "Radio en t₀"},
             new String[]{"pulg³/s", "pulg"}, new double[]{3, 0}, new double[]{0, r6(12 * Math.PI), 60}, "s", 1,
             (d, dt) -> new double[]{d[0], Math.cbrt(Math.pow(d[1], 3) + 3 * d[0] * dt / (4 * Math.PI))}, BancoProblemas::exPompa),

        t(7, new String[]{"Altitud del avión", "Velocidad del avión"},
             new String[]{"millas", "mph"}, new double[]{1, 400}, new double[]{0, 45, 90}, "s", 1.0 / 3600.0,
             (d, dt) -> new double[]{d[0], d[1], dt}, BancoProblemas::exAvionObservador),

        t(8, new String[]{"Ritmo al que se bebe el agua", "Altura del vaso", "Diámetro superior del vaso", "Profundidad del agua en t₀"},
             new String[]{"cm³/s", "cm", "cm", "cm"}, new double[]{3, 10, 6, 10}, new double[]{0, r6(Math.PI * 0.09 / 3 * 875 / 3), 30}, "s", 1,
             (d, dt) -> {
                 double k = d[2] / 2 / d[1], c = Math.PI * k * k / 3;
                 return new double[]{d[0], d[1], d[2], Math.cbrt(Math.max(c * Math.pow(d[3], 3) - d[0] * dt, 0) / c)};
             }, BancoProblemas::exVaso),

        t(9, new String[]{"Velocidad del avión que va al oeste", "Velocidad del avión que va al norte", "Horas después de t₀ en que el del norte pasa por la ciudad"},
             new String[]{"mph", "mph", "horas"}, new double[]{300, 400, 1}, new double[]{0, 2, 4}, "h", 1,
             (d, dt) -> new double[]{d[0], d[1], d[2], dt}, BancoProblemas::exAvionesVariante),

        t(10, new String[]{"Altura de la mujer sobre el agua", "Velocidad con que jala la cuerda", "Longitud de cuerda por recoger en t₀"},
              new String[]{"pies", "pies/s", "pies"}, new double[]{10, 2, 40}, new double[]{0, 7.5, 12}, "s", 1,
              (d, dt) -> new double[]{d[0], d[1], d[2] - d[1] * dt}, BancoProblemas::exBote),

        t(11, new String[]{"Longitud de la escalera", "Velocidad con que se aleja la base", "Distancia de la base a la pared en t₀"},
              new String[]{"pies", "pies/s", "pies"}, new double[]{20, 1, 0}, new double[]{0, 5, 15}, "s", 1,
              (d, dt) -> new double[]{d[0], d[1], d[2] + d[1] * dt}, BancoProblemas::exEscalera),

        t(12, new String[]{"Volumen de petróleo consumido por hora", "Grosor de la mancha en t₀", "Diámetro de la mancha en t₀", "Disminución del grosor por hora"},
              new String[]{"pies³/h", "pies", "pies", "pies/h"}, new double[]{4, 0.001, 500, 0.0005}, new double[]{0, 0, 1.5}, "h", 1,
              BancoProblemas::evoDerrame, BancoProblemas::exDerrame),

        t(13, new String[]{"Caudal de arena", "Relación altura / diámetro de la base", "Altura del cono en t₀"},
              new String[]{"pies³/s", "", "pies"}, new double[]{16, 0.25, 0}, new double[]{0, r6(16 * Math.PI / 3), 30}, "s", 1,
              (d, dt) -> {
                  double c = Math.PI / (12 * d[1] * d[1]);                       // volumen = c × altura³
                  return new double[]{d[0], d[1], Math.cbrt((c * Math.pow(d[2], 3) + d[0] * dt) / c)};
              }, BancoProblemas::exConoArena),

        t(14, new String[]{"Altitud del avión (dato supuesto)", "Ángulo de elevación en t₀", "Velocidad con que aumenta el ángulo"},
              new String[]{"millas", "rad", "rad/s"}, new double[]{1, 0.3, 0.1}, new double[]{0, 2, 10}, "s", 1,
              (d, dt) -> new double[]{d[0], d[1] + d[2] * dt, d[2]}, BancoProblemas::exAvionAngulo),

        t(15, new String[]{"Estatura de Chris", "Altura del poste", "Velocidad de Chris", "Distancia de Chris al poste en t₀"},
              new String[]{"pies", "pies", "pies/s", "pies"}, new double[]{6, 30, 2, 0}, new double[]{0, 12, 20}, "s", 1,
              (d, dt) -> new double[]{d[0], d[1], d[2], d[3] + d[2] * dt}, BancoProblemas::exChrisSombra),

        t(16, new String[]{"Estatura de Chris", "Altura del poste", "Velocidad de Chris", "Distancia de Chris al poste en t₀"},
              new String[]{"pies", "pies", "pies/s", "pies"}, new double[]{6, 30, 2, 0}, new double[]{0, 12, 20}, "s", 1,
              (d, dt) -> new double[]{d[0], d[1], d[2], d[3] + d[2] * dt}, BancoProblemas::exChrisExtremo),

        t(17, new String[]{"Estatura de Chris (altura de sus ojos)", "Altura del poste", "Velocidad de Chris", "Longitud de la sombra en t₀"},
              new String[]{"pies", "pies", "pies/s", "pies"}, new double[]{6, 30, 2, 0}, new double[]{0, 12, 24}, "s", 1,
              (d, dt) -> new double[]{d[0], d[1], d[2], d[3] + d[0] / (d[1] - d[0]) * d[2] * dt}, BancoProblemas::exChrisAngulo),

        t(18, new String[]{"Longitud de los lados iguales", "Velocidad con que aumenta el ángulo del vértice", "Ángulo del vértice en t₀"},
              new String[]{"cm", "rad/min", "grados"}, new double[]{100, 0.1, 0}, new double[]{0, r6(Math.PI / 6 / 0.1), 10}, "min", 1,
              (d, dt) -> new double[]{d[0], d[1], d[2] + Math.toDegrees(d[1] * dt)}, BancoProblemas::exIsosceles),

        t(19, new String[]{"Velocidad del auto", "Velocidad del tren", "Separación vertical entre ambos"},
              new String[]{"pies/s", "pies/s", "pies"}, new double[]{66, 88, 100}, new double[]{0, 10, 20}, "s", 1,
              (d, dt) -> new double[]{d[0], d[1], d[2], dt}, BancoProblemas::exDesnivel),

        t(20, new String[]{"Caudal de entrada de agua", "Altura del tanque", "Radio en el fondo", "Radio en la parte superior", "Profundidad del agua en t₀"},
              new String[]{"cm³/min", "cm", "cm", "cm", "cm"}, new double[]{2000, 80, 20, 40, 0},
              new double[]{0, r6(volumenTruncado(20, 0.25, 30) / 2000), 60}, "min", 1,
              (d, dt) -> new double[]{d[0], d[1], d[2], d[3], profundidadTruncado(d[0], d[1], d[2], d[3], d[4], dt)}, BancoProblemas::exTruncado),

        t(21, new String[]{"Posición del primer cuerpo en t₀", "Velocidad del primer cuerpo", "Posición del segundo cuerpo en t₀", "Velocidad del segundo cuerpo", "Separación fija perpendicular (0 si no hay)"},
              new String[]{"u", "u/tiempo", "u", "u/tiempo", "u"}, new double[]{600, 600, 800, 640, 0}, new double[]{0, 0, 1}, "tiempo", 1,
              (d, dt) -> new double[]{d[0] + d[1] * dt, d[1], d[2] + d[3] * dt, d[3], d[4]}, BancoProblemas::exGenerica)
    );

    // =========================================================================
    //  FORMA FORMAL DEL PROCEDIMIENTO (Fórmula → Derivada → Sustitución → Leibniz)
    //  Aquí se ESCRIBE la derivación simbólica (regla de la cadena / derivación implícita
    //  respecto al tiempo). El valor numérico de la sustitución viene del estado en t₁.
    // =========================================================================

    /** Pitágoras s² = x² + y² (+ z²): fórmula, derivada implícita y sustitución para la razón de la hipotenusa. */
    private static Formal pit(String hip, String[] sim, double[] val, double[] vel, String leyenda, String nota) {
        double c2 = 0, num = 0;
        for (int i = 0; i < val.length; i++) { c2 += val[i] * val[i]; num += val[i] * vel[i]; }
        double c = Math.sqrt(c2), res = num / c;
        String d = "d" + hip + "/dt";
        StringBuilder f = new StringBuilder(hip + "² = ");
        StringBuilder de = new StringBuilder("2·" + hip + "·(" + d + ") = ");
        StringBuilder su = new StringBuilder("2·" + p(c) + "·(" + d + ") = ");
        for (int i = 0; i < sim.length; i++) {
            String sep = i == 0 ? "" : " + ";
            f.append(sep).append(sim[i]).append("²");
            de.append(sep).append("2·").append(sim[i]).append("·(d").append(sim[i]).append("/dt)");
            su.append(sep).append("2·").append(p(val[i])).append("·").append(p(vel[i]));
        }
        su.append("   ⇒   ").append(d).append(" = ").append(g(2 * num)).append(" / ").append(g(2 * c)).append(" = ").append(g(res));
        return new Formal(f.toString(), leyenda, de.toString(), su.toString(), d, nota);
    }

    private static Extra exGlobo(double[] e, double v) {
        double base = e[0], vel = e[1], alt = e[2];
        return new Extra(
            pit("s", new String[]{"x", "y"}, new double[]{base, alt}, new double[]{0, vel},
                "s = distancia al observador, x = base horizontal (constante), y = altura del globo", ""),
            Geometria.triangulo("Triángulo del globo", "Base (x)", base, 0, "Altura del globo (y)", alt, vel,
                "Distancia al observador (s)", "pies", "pies/s"));
    }

    private static Extra exTanque(double[] e, double v) {
        double q = e[0], k = e[2] / e[1], prof = e[3], c3 = Math.PI * k * k;     // c3 = 3 × (π k² / 3)
        return new Extra(new Formal(
            "V = (1/3)·π·r²·h,   con   r = " + g(k) + "·h   ⇒   V = " + g(c3 / 3) + "·h³",
            "V = volumen de agua, r = radio de la superficie del agua, h = profundidad (la relación r = " + g(k) + "·h sale de triángulos semejantes)",
            "dV/dt = " + g(c3) + "·h²·(dh/dt)",
            "dV/dt = " + g(q) + "   ⇒   " + g(q) + " = " + g(c3) + "·" + p(prof) + "²·(dh/dt)   ⇒   dh/dt = " + g(q) + " / " + g(c3 * prof * prof) + " = " + g(v),
            "dh/dt", ""),
            Geometria.cono("Tanque cónico (vértice abajo)", e[1], e[2], 0, prof, v, false, "pies", "pies/min"));
    }

    private static Extra exDosAviones(double[] e, double v) {
        double este = e[1] * e[3], norte = e[2] + e[0] * e[3];
        return new Extra(
            pit("s", new String[]{"x", "y"}, new double[]{este, norte}, new double[]{e[1], e[0]},
                "s = distancia entre los aviones, x = distancia recorrida al este, y = distancia al norte desde la ciudad", ""),
            Geometria.triangulo("Aviones: este y norte", "Distancia al este (x)", este, e[1], "Distancia al norte (y)", norte, e[0],
                "Distancia entre los aviones (s)", "millas", "mph"));
    }

    private static Extra exTelescopio(double[] e, double v) {
        double alt = e[0], vel = e[1], dist = e[2], csc2 = 1 + (dist / alt) * (dist / alt);
        return new Extra(new Formal(
            "cot θ = x / " + g(alt),
            "θ = ángulo de observación, x = distancia del bote a la playa (la altura del telescopio, " + g(alt) + ", es constante)",
            "−csc²θ·(dθ/dt) = (1/" + g(alt) + ")·(dx/dt)",
            "csc²θ = 1 + cot²θ = " + g(csc2) + ",  dx/dt = " + g(-vel) + "   ⇒   −" + g(csc2) + "·(dθ/dt) = (1/" + g(alt) + ")·" + p(-vel) + "   ⇒   dθ/dt = " + g(v),
            "dθ/dt", ""),
            Geometria.triangulo("Telescopio y bote", "Distancia a la playa (x)", dist, -vel, "Altura del telescopio", alt, 0,
                "Línea de visión", "pies", "pies/s"));
    }

    private static Extra exSombraEdificio(double[] e, double v) {
        double alto = e[0], th = Math.toRadians(e[1]), w = -2 * Math.PI / e[2], csc2 = 1 / (Math.sin(th) * Math.sin(th));
        return new Extra(new Formal(
            "s = " + g(alto) + "·cot θ",
            "s = longitud de la sombra, θ = ángulo del sol sobre el horizonte (ahora " + g(e[1]) + "°)",
            "ds/dt = −" + g(alto) + "·csc²θ·(dθ/dt)",
            "ds/dt = −" + g(alto) + "·" + p(csc2) + "·" + p(w) + " = " + g(v),
            "ds/dt", ""),
            Geometria.triangulo("Sombra del edificio", "Sombra (s)", alto / Math.tan(th), v, "Altura del edificio", alto, 0,
                "Rayo de sol", "pies", "pies/s"));
    }

    private static Extra exCubo(double[] e, double v) {
        return new Extra(new Formal("V = a³", "V = volumen del cubo, a = arista",
            "dV/dt = 3·a²·(da/dt)",
            "dV/dt = 3·" + p(e[1]) + "²·" + p(e[0]) + " = " + g(v), "dV/dt", ""), Geometria.ninguna());
    }

    private static Extra exPompa(double[] e, double v) {
        return new Extra(new Formal("V = (4/3)·π·r³", "V = volumen de la pompa, r = radio",
            "dV/dt = 4·π·r²·(dr/dt)",
            g(e[0]) + " = 4·π·" + p(e[1]) + "²·(dr/dt)   ⇒   dr/dt = " + g(e[0]) + " / " + g(4 * Math.PI * e[1] * e[1]) + " = " + g(v),
            "dr/dt", ""), Geometria.ninguna());
    }

    private static Extra exAvionObservador(double[] e, double v) {
        double rec = e[1] * e[2] / 3600.0;
        return new Extra(
            pit("s", new String[]{"x", "y"}, new double[]{rec, e[0]}, new double[]{e[1], 0},
                "s = distancia al observador, x = recorrido horizontal del avión, y = altitud (constante)", ""),
            Geometria.triangulo("Avión y observador", "Recorrido horizontal (x)", rec, e[1], "Altitud (y)", e[0], 0,
                "Distancia al observador (s)", "millas", "mph"));
    }

    private static Extra exVaso(double[] e, double v) {
        double q = e[0], k = e[2] / 2 / e[1], prof = e[3], c3 = Math.PI * k * k;
        return new Extra(new Formal(
            "V = (1/3)·π·r²·h,   con   r = " + g(k) + "·h   ⇒   V = " + g(c3 / 3) + "·h³",
            "V = volumen de agua, r = radio de la superficie, h = profundidad (r = " + g(k) + "·h por triángulos semejantes)",
            "dV/dt = " + g(c3) + "·h²·(dh/dt)",
            "dV/dt = −" + g(q) + "   ⇒   −" + g(q) + " = " + g(c3) + "·" + p(prof) + "²·(dh/dt)   ⇒   dh/dt = −" + g(q) + " / " + g(c3 * prof * prof) + " = " + g(v),
            "dh/dt", ""),
            Geometria.cono("Vaso cónico (vértice abajo)", e[1], e[2] / 2, 0, prof, v, false, "cm", "cm/s"));
    }

    private static Extra exAvionesVariante(double[] e, double v) {
        double oeste = e[0] * e[3], norte = e[1] * (e[3] - e[2]);
        return new Extra(
            pit("s", new String[]{"x", "y"}, new double[]{oeste, norte}, new double[]{e[0], e[1]},
                "s = distancia entre los aviones, x = distancia recorrida al oeste, y = distancia recorrida al norte", ""),
            Geometria.triangulo("Aviones: oeste y norte", "Distancia al oeste (x)", oeste, e[0], "Distancia al norte (y)", norte, e[1],
                "Distancia entre los aviones (s)", "millas", "mph"));
    }

    private static Extra exBote(double[] e, double v) {
        double alt = e[0], vel = e[1], cuerda = e[2], horiz = Math.sqrt(cuerda * cuerda - alt * alt);
        return new Extra(new Formal(
            "L² = x² + y²",
            "L = longitud de la cuerda, x = distancia horizontal al bote, y = altura sobre el agua (constante)",
            "2·L·(dL/dt) = 2·x·(dx/dt) + 2·y·(dy/dt)",
            "2·" + p(cuerda) + "·" + p(-vel) + " = 2·" + p(horiz) + "·(dx/dt) + 2·" + p(alt) + "·(0)   ⇒   dx/dt = " + g(-2 * cuerda * vel) + " / " + g(2 * horiz) + " = " + g(v),
            "dx/dt", ""),
            Geometria.triangulo("Bote y cuerda", "Distancia horizontal (x)", horiz, v, "Altura (y)", alt, 0, "Cuerda (L)", "pies", "pies/s"));
    }

    private static Extra exEscalera(double[] e, double v) {
        double largo = e[0], vel = e[1], base = e[2], punta = Math.sqrt(largo * largo - base * base);
        return new Extra(new Formal(
            "L² = x² + y²",
            "L = longitud de la escalera (constante), x = distancia de la base a la pared, y = altura de la punta",
            "0 = 2·x·(dx/dt) + 2·y·(dy/dt)      (dL/dt = 0 porque L es constante)",
            "0 = 2·" + p(base) + "·" + p(vel) + " + 2·" + p(punta) + "·(dy/dt)   ⇒   dy/dt = −" + g(2 * base * vel) + " / " + g(2 * punta) + " = " + g(v),
            "dy/dt", ""),
            Geometria.triangulo("Escalera que resbala", "Base (x)", base, vel, "Punta (y)", punta, v, "Escalera (L)", "pies", "pies/s"));
    }

    private static Extra exDerrame(double[] e, double v) {
        double area = Math.PI * (e[2] / 2) * (e[2] / 2);
        return new Extra(new Formal(
            "V = A·e",
            "V = volumen de la mancha, A = área de la mancha, e = grosor",
            "dV/dt = (dA/dt)·e + A·(de/dt)      (regla del producto)",
            "−" + g(e[0]) + " = (dA/dt)·" + p(e[1]) + " + " + p(area) + "·(−" + g(e[3]) + ")   ⇒   dA/dt = (−" + g(e[0]) + " + " + g(area * e[3]) + ") / " + g(e[1]) + " = " + g(v),
            "dA/dt", ""), Geometria.ninguna());
    }

    private static Extra exConoArena(double[] e, double v) {
        double q = e[0], f = 1 / (2 * e[1]), alt = e[2], c3 = Math.PI * f * f;
        return new Extra(new Formal(
            "V = (1/3)·π·r²·h,   con   r = " + g(f) + "·h   ⇒   V = " + g(c3 / 3) + "·h³",
            "V = volumen de arena, r = radio de la base, h = altura del cono (h = " + g(e[1]) + " del diámetro ⇒ r = " + g(f) + "·h)",
            "dV/dt = " + g(c3) + "·h²·(dh/dt)",
            g(q) + " = " + g(c3) + "·" + p(alt) + "²·(dh/dt)   ⇒   dh/dt = " + g(q) + " / " + g(c3 * alt * alt) + " = " + g(v),
            "dh/dt", ""),
            Geometria.cono("Cono de arena", alt, f * alt, 0, alt, v, true, "pies", "pies/s"));
    }

    private static Extra exAvionAngulo(double[] e, double v) {
        double alt = e[0], th = e[1], w = e[2], csc2 = 1 / (Math.sin(th) * Math.sin(th));
        return new Extra(new Formal(
            "cot θ = x / " + g(alt),
            "θ = ángulo de elevación, x = distancia horizontal avión-observador (la altitud, " + g(alt) + ", es constante)",
            "−csc²θ·(dθ/dt) = (1/" + g(alt) + ")·(dx/dt)",
            "−" + g(csc2) + "·" + p(w) + " = (1/" + g(alt) + ")·(dx/dt)   ⇒   dx/dt = " + g(v),
            "dx/dt", "La velocidad del avión es la rapidez |dx/dt| = " + g(Math.abs(v)) + " millas/s; el signo negativo indica que se acerca."),
            Geometria.triangulo("Avión y ángulo de elevación", "Distancia horizontal (x)", alt / Math.tan(th), v, "Altitud", alt, 0,
                "Línea de visión", "millas", "millas/s"));
    }

    /** Pasos comunes de triángulos semejantes de Chris: s / e = (s + d) / H  ⇒  s = q·d. */
    private static String semejanzaFormal(double est, double poste, double q) {
        return "s / " + g(est) + " = (s + d) / " + g(poste) + "   ⇒   s = " + g(q) + "·d";
    }

    private static Extra exChrisSombra(double[] e, double v) {
        double q = e[0] / (e[1] - e[0]), dist = e[3];
        return new Extra(new Formal(semejanzaFormal(e[0], e[1], q),
            "s = longitud de la sombra, d = distancia de Chris al poste (triángulos semejantes: Chris y poste)",
            "ds/dt = " + g(q) + "·(dd/dt)",
            "ds/dt = " + g(q) + "·" + p(e[2]) + " = " + g(v), "ds/dt", ""),
            Geometria.triangulo("Sombra de Chris", "Sombra (s)", q * dist, v, "Estatura de Chris", e[0], 0, "Rayo de luz", "pies", "pies/s"));
    }

    private static Extra exChrisExtremo(double[] e, double v) {
        double q = e[0] / (e[1] - e[0]), dist = e[3];
        return new Extra(new Formal("u = d + s = (1 + " + g(q) + ")·d      (con s = " + g(q) + "·d por triángulos semejantes)",
            "u = distancia del extremo de la sombra al poste, d = distancia de Chris al poste, s = sombra",
            "du/dt = " + g(1 + q) + "·(dd/dt)",
            "du/dt = " + g(1 + q) + "·" + p(e[2]) + " = " + g(v), "du/dt", ""),
            Geometria.triangulo("Extremo de la sombra", "Extremo de la sombra al poste (u)", (1 + q) * dist, v, "Poste", e[1], 0, "Rayo de luz", "pies", "pies/s"));
    }

    private static Extra exChrisAngulo(double[] e, double v) {
        double q = e[0] / (e[1] - e[0]), sombra = e[3], vSombra = q * e[2], sec2 = 1 + (e[0] / sombra) * (e[0] / sombra);
        return new Extra(new Formal("tan φ = " + g(e[0]) + " / s",
            "φ = ángulo de la mirada hacia el extremo de la sombra (desde la horizontal), s = longitud de la sombra, " + g(e[0]) + " = altura de los ojos",
            "sec²φ·(dφ/dt) = −(" + g(e[0]) + " / s²)·(ds/dt)",
            "sec²φ = 1 + tan²φ = " + g(sec2) + ",  ds/dt = " + g(q) + "·" + g(e[2]) + " = " + g(vSombra) + "   ⇒   " + g(sec2) + "·(dφ/dt) = −(" + g(e[0]) + " / " + g(sombra) + "²)·" + p(vSombra) + "   ⇒   dφ/dt = " + g(v),
            "dφ/dt", ""),
            Geometria.triangulo("Mirada de Chris", "Sombra (s)", sombra, vSombra, "Altura de los ojos", e[0], 0, "Línea de la mirada", "pies", "pies/s"));
    }

    private static Extra exIsosceles(double[] e, double v) {
        double th = Math.toRadians(e[2]), k = 0.5 * e[0] * e[0];
        return new Extra(new Formal("A = ½·L²·sen θ",
            "A = área del triángulo, L = lados iguales (constante), θ = ángulo del vértice",
            "dA/dt = ½·L²·cos θ·(dθ/dt)",
            "dA/dt = " + g(k) + "·cos(" + g(e[2]) + "°)·" + p(e[1]) + " = " + g(k) + "·" + p(Math.cos(th)) + "·" + p(e[1]) + " = " + g(v),
            "dA/dt", ""), Geometria.ninguna());
    }

    private static Extra exDesnivel(double[] e, double v) {
        double a = e[0] * e[3], tr = e[1] * e[3], horiz = Math.hypot(a, tr);
        return new Extra(
            pit("s", new String[]{"x", "y", "z"}, new double[]{a, tr, e[2]}, new double[]{e[0], e[1], 0},
                "s = distancia entre auto y tren, x = recorrido del auto, y = recorrido del tren, z = separación vertical (constante)", ""),
            Geometria.triangulo("Paso a desnivel", "Distancia horizontal entre ambos", horiz, (a * e[0] + tr * e[1]) / horiz,
                "Separación vertical (z)", e[2], 0, "Distancia auto-tren (s)", "pies", "pies/s"));
    }

    private static Extra exTruncado(double[] e, double v) {
        double q = e[0], m = (e[3] - e[2]) / e[1], rAgua = e[2] + m * e[4];
        return new Extra(new Formal(
            "V = (π / (3·m))·(r³ − r₁³),   con   r = r₁ + m·h,   r₁ = " + g(e[2]) + ",  m = " + g(m),
            "V = volumen de agua, r = radio de la superficie del agua, h = profundidad, r₁ = radio del fondo, m = crecimiento del radio por unidad de altura",
            "dV/dt = (π/m)·r²·(dr/dt)   y   dr/dt = m·(dh/dt)   ⇒   dV/dt = π·r²·(dh/dt)",
            g(q) + " = π·" + p(rAgua) + "²·(dh/dt)   ⇒   dh/dt = " + g(q) + " / " + g(Math.PI * rAgua * rAgua) + " = " + g(v),
            "dh/dt", ""),
            Geometria.cono("Tanque en cono truncado", e[1], e[3], e[2], e[4], v, false, "cm", "cm/min"));
    }

    private static Extra exGenerica(double[] e, double v) {
        double a = e[0], b = e[2], c = e[4], horiz = Math.hypot(a, b);
        Formal f = pit("s", new String[]{"x", "y", "z"}, new double[]{a, b, c}, new double[]{e[1], e[3], 0},
            "s = distancia entre los cuerpos, x = posición del primero, y = posición del segundo, z = separación fija", "");
        Geometria geo = c > 0
            ? Geometria.triangulo("Dos cuerpos a distinta altura", "Distancia horizontal", horiz, (a * e[1] + b * e[3]) / horiz,
                "Separación fija (z)", c, 0, "Distancia entre ellos (s)", "u", "u/tiempo")
            : Geometria.triangulo("Dos cuerpos en movimiento", "Primer cuerpo (x)", a, e[1], "Segundo cuerpo (y)", b, e[3],
                "Distancia entre ellos (s)", "u", "u/tiempo");
        return new Extra(f, geo);
    }
}
