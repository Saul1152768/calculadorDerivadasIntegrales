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
 *    2) Añade un  new Problema(...)  a la lista TODOS.
 *    La pestaña lo mostrará sola en el menú.
 * ============================================================================
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    record Problema(String nombre, String enunciado,
                    String[] etiquetas, String[] unidades, double[] iniciales,
                    Function<double[], Solucion> resolver) {
        @Override public String toString() { return nombre; }
    }

    // =========================================================================
    //  LISTA DE PROBLEMAS (los 5 ejemplos + los 14 propuestos + plantilla general)
    // =========================================================================
    static final List<Problema> TODOS = List.of(

        new Problema("Ejemplo 1 · Globo aerostático",
            "Se suelta un globo a 150 pies de un observador. Sube a 8 pies/segundo. ¿Qué tan rápido aumenta "
          + "la distancia al observador cuando el globo está a 50 pies de altura?",
            new String[]{"Distancia horizontal fija al observador", "Velocidad de subida del globo", "Altura del globo en el instante pedido"},
            new String[]{"pies", "pies/s", "pies"},
            new double[]{150, 8, 50}, BancoProblemas::globo),

        new Problema("Ejemplo 2 · Tanque cónico",
            "Entra agua a un tanque cónico a 8 pies³/min. La altura del tanque es 12 pies y el radio superior es 6 pies. "
          + "¿Qué tan rápido sube el nivel del agua cuando la profundidad es 4 pies?",
            new String[]{"Caudal de entrada de agua", "Altura del tanque", "Radio superior del tanque", "Profundidad del agua en el instante pedido"},
            new String[]{"pies³/min", "pies", "pies", "pies"},
            new double[]{8, 12, 6, 4}, BancoProblemas::tanqueConico),

        new Problema("Ejemplo 3 · Dos aviones (norte y este)",
            "Un avión A vuela al norte a 640 mph y un avión B vuela al este a 600 mph. A las 12:15 pm, B está sobre la ciudad "
          + "y A ya ha recorrido 160 millas. ¿Qué tan rápido se separan a la 1:15 pm?",
            new String[]{"Velocidad del avión que va al norte", "Velocidad del avión que va al este", "Millas que ya llevaba el avión del norte", "Horas transcurridas hasta el instante pedido"},
            new String[]{"mph", "mph", "millas", "horas"},
            new double[]{640, 600, 160, 1}, BancoProblemas::dosAviones),

        new Problema("Ejemplo 4 · Telescopio y bote",
            "Un telescopio a 250 pies sobre el agua observa un bote que se acerca a 20 pies/segundo. "
          + "¿A qué velocidad cambia el ángulo de observación cuando el bote está a 250 pies de la playa?",
            new String[]{"Altura del telescopio sobre el agua", "Velocidad con que se acerca el bote", "Distancia del bote a la playa en el instante pedido"},
            new String[]{"pies", "pies/s", "pies"},
            new double[]{250, 20, 250}, BancoProblemas::telescopio),

        new Problema("Ejemplo 5 · Sombra de un edificio",
            "Un edificio mide 120 pies. El sol se pone. ¿Qué tan rápido crece la sombra cuando el ángulo del sol es 45° (π/4)?",
            new String[]{"Altura del edificio", "Ángulo del sol sobre el horizonte", "Tiempo de una vuelta completa de la Tierra"},
            new String[]{"pies", "grados", "s"},
            new double[]{120, 45, 86400}, BancoProblemas::sombraEdificio),

        new Problema("Problema 1 · Cubo",
            "Una arista de un cubo aumenta a 3 pulgadas/segundo. ¿Con qué rapidez cambia el volumen cuando la arista mide 12 pulgadas?",
            new String[]{"Velocidad de aumento de la arista", "Arista en el instante pedido"},
            new String[]{"pulg/s", "pulg"},
            new double[]{3, 12}, BancoProblemas::cubo),

        new Problema("Problema 2 · Pompa de jabón",
            "Se sopla aire dentro de una pompa de jabón esférica a 3 pulgadas³/segundo. ¿Con qué rapidez aumenta el radio cuando mide 3 pulgadas?",
            new String[]{"Caudal de aire que entra", "Radio en el instante pedido"},
            new String[]{"pulg³/s", "pulg"},
            new double[]{3, 3}, BancoProblemas::pompa),

        new Problema("Problema 3 · Avión y observador",
            "Un avión vuela a 1 milla de altitud y a 400 mph. ¿Con qué rapidez aumenta la distancia entre el avión y el observador "
          + "45 segundos después de pasar por encima de él? (En 45 s recorre 5 millas.)",
            new String[]{"Altitud del avión", "Velocidad del avión", "Segundos desde que pasó por encima"},
            new String[]{"millas", "mph", "s"},
            new double[]{1, 400, 45}, BancoProblemas::avionObservador),

        new Problema("Problema 4 · Vaso cónico",
            "Se bebe agua de un vaso cónico a 3 cm³/segundo. La altura del vaso es 10 cm y el diámetro superior es 6 cm. "
          + "¿Con qué rapidez desciende el nivel cuando la profundidad es 5 cm?",
            new String[]{"Ritmo al que se bebe el agua", "Altura del vaso", "Diámetro superior del vaso", "Profundidad del agua en el instante pedido"},
            new String[]{"cm³/s", "cm", "cm", "cm"},
            new double[]{3, 10, 6, 5}, BancoProblemas::vasoConico),

        new Problema("Problema 5 · Dos aviones (oeste y norte)",
            "Un avión vuela al oeste a 300 mph y pasa por una ciudad a mediodía. Otro avión vuela al norte a 400 mph y pasa por la misma "
          + "ciudad una hora después. ¿Con qué rapidez cambia la distancia entre ellos a las 2:00 pm?",
            new String[]{"Velocidad del avión que va al oeste", "Velocidad del avión que va al norte", "Horas que pasa el avión del norte después del mediodía", "Horas desde el mediodía hasta el instante pedido"},
            new String[]{"mph", "mph", "horas", "horas"},
            new double[]{300, 400, 1, 2}, BancoProblemas::dosAvionesVariante),

        new Problema("Problema 6 · Bote y cuerda",
            "Una mujer en un muelle, a 10 pies sobre el agua, jala una cuerda atada a un bote a 2 pies/segundo. "
          + "¿Con qué rapidez se acerca el bote al muelle cuando faltan 25 pies de cuerda?",
            new String[]{"Altura de la mujer sobre el agua", "Velocidad con que jala la cuerda", "Longitud de cuerda que falta por recoger"},
            new String[]{"pies", "pies/s", "pies"},
            new double[]{10, 2, 25}, BancoProblemas::boteCuerda),

        new Problema("Problema 7 · Escalera que resbala",
            "Una escalera de 20 pies está apoyada en una pared y su base se aleja a 1 pie/segundo. "
          + "¿Con qué rapidez desciende la punta cuando la base está a 5 pies de la pared?",
            new String[]{"Longitud de la escalera", "Velocidad con que se aleja la base", "Distancia de la base a la pared en el instante pedido"},
            new String[]{"pies", "pies/s", "pies"},
            new double[]{20, 1, 5}, BancoProblemas::escalera),

        new Problema("Problema 8 · Derrame de petróleo",
            "Unas bacterias consumen una mancha de petróleo cilíndrica a 4 pies³/hora. El grosor es 0.001 pie, el diámetro es 500 pies y "
          + "el grosor disminuye a 0.0005 pie/hora. ¿Cuál es la razón de cambio del área de la mancha?",
            new String[]{"Volumen de petróleo consumido por hora", "Grosor de la mancha", "Diámetro de la mancha", "Disminución del grosor por hora"},
            new String[]{"pies³/h", "pies", "pies", "pies/h"},
            new double[]{4, 0.001, 500, 0.0005}, BancoProblemas::derrame),

        new Problema("Problema 9 · Cono de arena",
            "Sale arena de una banda a 16 pies³/segundo y forma un cono cuya altura es 1/4 del diámetro de la base. "
          + "¿Con qué rapidez aumenta la altura del cono cuando mide 4 pies?",
            new String[]{"Caudal de arena", "Relación altura / diámetro de la base", "Altura del cono en el instante pedido"},
            new String[]{"pies³/s", "", "pies"},
            new double[]{16, 0.25, 4}, BancoProblemas::conoArena),

        new Problema("Problema 10 · Avión y ángulo de elevación",
            "El ángulo de elevación de un avión visto desde un observador es 1/2 radián y aumenta a 1/10 radián/segundo. "
          + "¿Cuál es la velocidad del avión? (El enunciado no trae la altitud: se supone 1 milla; puedes cambiarla.)",
            new String[]{"Altitud del avión (dato supuesto)", "Ángulo de elevación en el instante pedido", "Velocidad con que aumenta el ángulo"},
            new String[]{"millas", "rad", "rad/s"},
            new double[]{1, 0.5, 0.1}, BancoProblemas::avionAngulo),

        new Problema("Problema 11a · Sombra de Chris (crecimiento de la sombra)",
            "Chris mide 6 pies y camina alejándose de un poste de 30 pies a 2 pies/segundo. "
          + "(a) ¿Con qué rapidez aumenta la longitud de su sombra cuando está a 24 pies del poste?",
            new String[]{"Estatura de Chris", "Altura del poste", "Velocidad de Chris", "Distancia de Chris al poste en el instante pedido"},
            new String[]{"pies", "pies", "pies/s", "pies"},
            new double[]{6, 30, 2, 24}, BancoProblemas::chrisSombra),

        new Problema("Problema 11b · Sombra de Chris (extremo de la sombra)",
            "Chris mide 6 pies y camina alejándose de un poste de 30 pies a 2 pies/segundo. "
          + "(b) ¿Con qué velocidad se mueve el extremo de su sombra?",
            new String[]{"Estatura de Chris", "Altura del poste", "Velocidad de Chris", "Distancia de Chris al poste (solo para la gráfica)"},
            new String[]{"pies", "pies", "pies/s", "pies"},
            new double[]{6, 30, 2, 24}, BancoProblemas::chrisExtremo),

        new Problema("Problema 11c · Sombra de Chris (ángulo de sus ojos)",
            "Chris mide 6 pies y camina alejándose de un poste de 30 pies a 2 pies/segundo. "
          + "(c) ¿Con qué velocidad angular cambia la dirección de su mirada hacia el extremo de su sombra cuando la sombra mide 6 pies? "
          + "(Se mide el ángulo de la mirada respecto a la horizontal, con los ojos a la altura de su estatura.)",
            new String[]{"Estatura de Chris (altura de sus ojos)", "Altura del poste", "Velocidad de Chris", "Longitud de la sombra en el instante pedido"},
            new String[]{"pies", "pies", "pies/s", "pies"},
            new double[]{6, 30, 2, 6}, BancoProblemas::chrisAngulo),

        new Problema("Problema 12 · Triángulo isósceles",
            "Un triángulo isósceles tiene dos lados iguales de 100 cm. El ángulo del vértice aumenta a 1/10 radián/minuto. "
          + "¿Con qué rapidez aumenta el área cuando ese ángulo es π/6 (30°)?",
            new String[]{"Longitud de los lados iguales", "Velocidad con que aumenta el ángulo del vértice", "Ángulo del vértice en el instante pedido"},
            new String[]{"cm", "rad/min", "grados"},
            new double[]{100, 0.1, 30}, BancoProblemas::isosceles),

        new Problema("Problema 13 · Paso a desnivel",
            "Un auto viaja a 66 pies/s sobre un paso a desnivel y un tren viaja a 88 pies/s por debajo; sus trayectorias son perpendiculares "
          + "y están separadas por 100 pies de altura. ¿Con qué rapidez se separan 10 segundos después de cruzarse?",
            new String[]{"Velocidad del auto", "Velocidad del tren", "Separación vertical entre ambos", "Segundos desde que se cruzaron"},
            new String[]{"pies/s", "pies/s", "pies", "s"},
            new double[]{66, 88, 100, 10}, BancoProblemas::pasoDesnivel),

        new Problema("Problema 14 · Tanque en cono truncado",
            "Entra agua a 2000 cm³/min a un tanque en forma de cono truncado de 80 cm de altura, con radio de 20 cm en el fondo y 40 cm arriba. "
          + "¿Con qué rapidez sube el nivel cuando la profundidad del agua es 30 cm?",
            new String[]{"Caudal de entrada de agua", "Altura del tanque", "Radio en el fondo", "Radio en la parte superior", "Profundidad del agua en el instante pedido"},
            new String[]{"cm³/min", "cm", "cm", "cm", "cm"},
            new double[]{2000, 80, 20, 40, 30}, BancoProblemas::conoTruncado),

        new Problema("Plantilla general · Distancia entre dos cuerpos en movimiento",
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
}
