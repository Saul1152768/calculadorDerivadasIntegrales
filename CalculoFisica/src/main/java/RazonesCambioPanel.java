import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

public class RazonesCambioPanel extends BorderPane {

    // =========================================================================
    // 1. MOTOR NUMÉRICO DE DERIVACIÓN
    // =========================================================================
    static final class MotorDerivacion {
        private MotorDerivacion() { }

        /**
         * Derivada por diferencia central (aproximación del límite cuando h → 0):
         *
         *     f'(x0) ≈ [ f(x0 + h) − f(x0 − h) ] / (2h)
         *
         * Geométricamente es la pendiente de la secante que pasa por (x0−h) y (x0+h);
         * al hacer h pequeño esa secante tiende a la recta tangente. El error de
         * truncamiento es O(h²), mucho mejor que la diferencia hacia adelante O(h).
         * Si h es demasiado pequeño (< ~1e-8) domina el error de redondeo.
         */
        static double derivadaCentral(DoubleUnaryOperator f, double x0, double h) {
            return (f.applyAsDouble(x0 + h) - f.applyAsDouble(x0 - h)) / (2.0 * h);
        }

        /** Resultado de resolver la condición f(x*) = b. */
        record Raiz(double x, int iteraciones, int pasosNewton, int pasosBiseccion,
                    double inicioIntervalo, double finIntervalo, double residuo) { }

        /**
         * Resuelve la condición B(x*) = b dentro de [xa, xb], es decir g(x) = f(x) − b = 0.
         *
         *  Fase 1 (barrido): se divide [xa, xb] en 2000 tramos y se busca el PRIMER cambio
         *          de signo de g. Eso garantiza que hay una raíz dentro del intervalo
         *          [lo, hi] encontrado (teorema del valor intermedio).
         *  Fase 2 (refinamiento): Newton-Raphson  x ← x − g(x)/g'(x)  usando la derivada
         *          central. Si el paso de Newton se sale del intervalo [lo, hi] (o g' ≈ 0),
         *          se hace un paso de bisección. Así se obtiene la velocidad de Newton
         *          (convergencia cuadrática) con la seguridad de la bisección.
         *
         * Devuelve null si no hay cambio de signo en el rango (la condición no se alcanza,
         * o la curva solo "toca" b sin cruzarlo).
         */
        static Raiz resolverCondicion(DoubleUnaryOperator f, double objetivo,
                                      double xa, double xb, double h) {
            DoubleUnaryOperator g = x -> f.applyAsDouble(x) - objetivo;

            // ---- Fase 1: barrido para aislar la raíz ----
            final int N = 2000;
            double paso = (xb - xa) / N;
            double lo = Double.NaN, hi = Double.NaN;
            double xPrev = xa, gPrev = g.applyAsDouble(xa);
            if (gPrev == 0.0) return new Raiz(xa, 0, 0, 0, xa, xa, 0.0);
            for (int i = 1; i <= N; i++) {
                double xi = xa + i * paso, gi = g.applyAsDouble(xi);
                if (gi == 0.0) return new Raiz(xi, 0, 0, 0, xPrev, xi, 0.0);
                if (Double.isFinite(gPrev) && Double.isFinite(gi) && gPrev * gi < 0) {
                    lo = xPrev;
                    hi = xi;
                    break;
                }
                xPrev = xi;
                gPrev = gi;
            }
            if (Double.isNaN(lo)) return null;

            // ---- Fase 2: Newton-Raphson protegido por bisección ----
            final double inicio = lo, fin = hi;
            double glo = g.applyAsDouble(lo);
            double x = 0.5 * (lo + hi);
            int newton = 0, biseccion = 0, it = 0;
            for (; it < 100; it++) {
                double gx = g.applyAsDouble(x);
                if (gx == 0.0) break;
                // Mantener el intervalo [lo, hi] con cambio de signo
                if (glo * gx < 0) { hi = x; } else { lo = x; glo = gx; }
                double tol = 1e-13 * Math.max(1.0, Math.abs(x));
                if (hi - lo < tol) { x = 0.5 * (lo + hi); break; }

                double d = derivadaCentral(f, x, h);
                double xn = x - gx / d;                       // paso de Newton
                if (Double.isFinite(xn) && Math.abs(xn - x) < tol) { x = xn; break; }
                if (Double.isFinite(xn) && xn > lo && xn < hi) {
                    newton++;
                } else {
                    xn = 0.5 * (lo + hi);                     // paso de bisección (respaldo)
                    biseccion++;
                }
                x = xn;
            }
            return new Raiz(x, it, newton, biseccion, inicio, fin, Math.abs(g.applyAsDouble(x)));
        }
    }

    // =========================================================================
    // 2. MODELOS FÍSICOS
    // =========================================================================
    /**
     * Un modelo asocia parámetros físicos (s₀, v₀, k...) con una función f(t) y trae su
     * derivada analítica solo para comparar contra el resultado numérico.
     */
    record Modelo(String nombre, String formula,
                  String sujeto, String unidadVar, String unidadDeriv, String nombreDeriv,
                  String[] etiquetas, String[] unidades, double[] iniciales,
                  double objetivoInicial, double tMaxInicial,
                  Function<double[], DoubleUnaryOperator> funcion,
                  Function<double[], DoubleUnaryOperator> derivadaExacta) { }

    private static final List<Modelo> MODELOS = List.of(
        new Modelo("Posición con aceleración constante", "s(t) = s₀ + v₀·t + ½·a·t²",
            "la posición", "m", "m/s", "Velocidad instantánea",
            new String[]{"Posición inicial (s₀)", "Velocidad inicial (v₀)", "Aceleración (a)"},
            new String[]{"m", "m/s", "m/s²"},
            new double[]{0, 5, 2}, 100, 20,
            p -> t -> p[0] + p[1] * t + 0.5 * p[2] * t * t,
            p -> t -> p[1] + p[2] * t),
        new Modelo("Velocidad con aceleración constante (caída libre)", "v(t) = v₀ + a·t",
            "la velocidad", "m/s", "m/s²", "Aceleración instantánea",
            new String[]{"Velocidad inicial (v₀)", "Aceleración (a)"},
            new String[]{"m/s", "m/s²"},
            new double[]{0, 9.8}, 49, 20,
            p -> t -> p[0] + p[1] * t,
            p -> t -> p[1]),
        new Modelo("Velocidad con resistencia (decaimiento exponencial)", "v(t) = v₀ · e^(−k·t)",
            "la velocidad", "m/s", "m/s²", "Aceleración instantánea",
            new String[]{"Velocidad inicial (v₀)", "Constante de resistencia (k)"},
            new String[]{"m/s", "1/s"},
            new double[]{20, 0.5}, 5, 20,
            p -> t -> p[0] * Math.exp(-p[1] * t),
            p -> t -> -p[0] * p[1] * Math.exp(-p[1] * t)),
        new Modelo("Enfriamiento de Newton (temperatura)", "T(t) = Tₐ + (T₀ − Tₐ) · e^(−k·t)",
            "la temperatura", "°C", "°C/s", "Razón de cambio de la temperatura",
            new String[]{"Temperatura inicial (T₀)", "Temperatura ambiente (Tₐ)", "Constante de enfriamiento (k)"},
            new String[]{"°C", "°C", "1/s"},
            new double[]{90, 20, 0.1}, 50, 60,
            p -> t -> p[1] + (p[0] - p[1]) * Math.exp(-p[2] * t),
            p -> t -> -p[2] * (p[0] - p[1]) * Math.exp(-p[2] * t))
    );

    enum Caso {
        INSTANTANEA("1. Tasa de cambio instantánea  (t → t₀)"),
        CONDICIONADA("2. Razón de cambio cuando se alcanza un valor  (B = b)");

        private final String texto;
        Caso(String texto) { this.texto = texto; }
        @Override public String toString() { return texto; }
    }

    // =========================================================================
    // 3. GRÁFICO: curva + recta tangente + triángulo de la pendiente
    // =========================================================================
    static class GraficoTangente extends Pane {
        private final Canvas canvas = new Canvas();
        private DoubleUnaryOperator f;
        private String titulo = "", ejeX = "", ejeY = "";
        private double xMin = 0, xMax = 1, x0, pendiente, objetivo;
        private boolean verTangente, verObjetivo;
        private double ml, mt, pw, ph, yMin, yMax;

        GraficoTangente() {
            getChildren().add(canvas);
            setMinHeight(270);
            setPrefHeight(420);
        }

        @Override
        protected void layoutChildren() {
            canvas.setWidth(getWidth());
            canvas.setHeight(getHeight());
            dibujar();
        }

        void actualizar(String titulo, String ejeX, String ejeY, DoubleUnaryOperator f,
                        double xMin, double xMax,
                        boolean verTangente, double x0, double pendiente,
                        boolean verObjetivo, double objetivo) {
            this.titulo = titulo; this.ejeX = ejeX; this.ejeY = ejeY;
            this.f = f; this.xMin = xMin; this.xMax = xMax;
            this.verTangente = verTangente; this.x0 = x0; this.pendiente = pendiente;
            this.verObjetivo = verObjetivo; this.objetivo = objetivo;
            dibujar();
        }

        void limpiar() { f = null; dibujar(); }

        private double px(double x) { return ml + (x - xMin) / (xMax - xMin) * pw; }
        private double py(double y) { return mt + (yMax - y) / (yMax - yMin) * ph; }

        private void dibujar() {
            double w = canvas.getWidth(), h = canvas.getHeight();
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.WHITE);
            g.fillRect(0, 0, w, h);
            if (f == null || w < 180 || h < 140) return;

            ml = 72; mt = 36;
            double mr = 24, mb = 50;
            pw = w - ml - mr;
            ph = h - mt - mb;

            // --- Muestreo de la curva ---
            int n = Math.max(2, (int) pw);
            double[] ys = new double[n + 1];
            yMin = Double.POSITIVE_INFINITY;
            yMax = Double.NEGATIVE_INFINITY;
            for (int i = 0; i <= n; i++) {
                ys[i] = f.applyAsDouble(xMin + (xMax - xMin) * i / n);
                if (Double.isFinite(ys[i])) {
                    yMin = Math.min(yMin, ys[i]);
                    yMax = Math.max(yMax, ys[i]);
                }
            }
            if (!Double.isFinite(yMin)) { yMin = 0; yMax = 1; }

            // --- Triángulo de la pendiente: A=(x0,y0), B=(x0+Δx,y0), C=(x0+Δx,y0+m·Δx) ---
            double y0 = 0, dx = 0;
            if (verTangente) {
                y0 = f.applyAsDouble(x0);
                dx = 0.2 * (xMax - xMin);
                if (x0 + dx > xMax) dx = -dx;           // si no cabe a la derecha, se dibuja a la izquierda
                yMin = Math.min(yMin, Math.min(y0, y0 + pendiente * dx));
                yMax = Math.max(yMax, Math.max(y0, y0 + pendiente * dx));
            }
            if (verObjetivo) {
                yMin = Math.min(yMin, objetivo);
                yMax = Math.max(yMax, objetivo);
            }
            if (yMax - yMin < 1e-12) yMax = yMin + 1;
            double pad = 0.1 * (yMax - yMin);
            yMin -= pad;
            yMax += pad;

            // --- Cuadrícula y marcas ---
            g.setFont(Font.font("System", 11));
            g.setLineWidth(1);
            double sx = pasoNice(xMax - xMin), sy = pasoNice(yMax - yMin);
            g.setTextAlign(TextAlignment.CENTER);
            long kx0 = (long) Math.ceil(xMin / sx - 1e-9), kx1 = (long) Math.floor(xMax / sx + 1e-9);
            for (long k = kx0; k <= kx1; k++) {
                double v = k * sx, x = px(v);
                g.setStroke(Color.web("#e5e7eb"));
                g.strokeLine(x, mt, x, mt + ph);
                g.setFill(Color.web("#4b5563"));
                g.fillText(fmtTick(v), x, mt + ph + 16);
            }
            g.setTextAlign(TextAlignment.RIGHT);
            long ky0 = (long) Math.ceil(yMin / sy - 1e-9), ky1 = (long) Math.floor(yMax / sy + 1e-9);
            for (long k = ky0; k <= ky1; k++) {
                double v = k * sy, y = py(v);
                g.setStroke(Color.web("#e5e7eb"));
                g.strokeLine(ml, y, ml + pw, y);
                g.setFill(Color.web("#4b5563"));
                g.fillText(fmtTick(v), ml - 6, y + 4);
            }
            g.setStroke(Color.web("#6b7280"));
            g.setLineWidth(1.5);
            if (yMin < 0 && yMax > 0) g.strokeLine(ml, py(0), ml + pw, py(0));
            if (xMin < 0 && xMax > 0) g.strokeLine(px(0), mt, px(0), mt + ph);
            g.setLineWidth(1);
            g.setStroke(Color.web("#9ca3af"));
            g.strokeRect(ml, mt, pw, ph);

            // --- Línea de la condición B = b ---
            if (verObjetivo) {
                g.setStroke(Color.web("#16a34a"));
                g.setLineWidth(1.5);
                g.setLineDashes(6, 4);
                g.strokeLine(ml, py(objetivo), ml + pw, py(objetivo));
                if (verTangente) g.strokeLine(px(x0), py(objetivo), px(x0), mt + ph);
                g.setLineDashes((double[]) null);
                g.setFill(Color.web("#16a34a"));
                g.setTextAlign(TextAlignment.LEFT);
                g.setFont(Font.font("System", FontWeight.BOLD, 12));
                g.fillText("B = b", ml + 6, py(objetivo) - 5);
            }

            // --- Curva ---
            g.setStroke(Color.rgb(37, 99, 235));
            g.setLineWidth(2.5);
            g.beginPath();
            boolean pluma = false;
            for (int i = 0; i <= n; i++) {
                if (!Double.isFinite(ys[i])) { pluma = false; continue; }
                double x = px(xMin + (xMax - xMin) * i / n), y = py(ys[i]);
                if (!pluma) { g.moveTo(x, y); pluma = true; } else { g.lineTo(x, y); }
            }
            g.stroke();

            String leyenda = "";
            if (verTangente) {
                double xB = x0 + dx, yC = y0 + pendiente * dx;

                // Triángulo de la pendiente (Δx horizontal, Δy vertical)
                g.setFill(Color.rgb(249, 115, 22, 0.18));
                g.beginPath();
                g.moveTo(px(x0), py(y0));
                g.lineTo(px(xB), py(y0));
                g.lineTo(px(xB), py(yC));
                g.closePath();
                g.fill();
                g.setStroke(Color.web("#f97316"));
                g.setLineWidth(2);
                g.strokeLine(px(x0), py(y0), px(xB), py(y0));     // Δx
                g.strokeLine(px(xB), py(y0), px(xB), py(yC));     // Δy

                // Recta tangente (prominente), recortada a la zona del gráfico
                g.save();
                g.beginPath();
                g.rect(ml, mt, pw, ph);
                g.clip();
                g.setStroke(Color.web("#dc2626"));
                g.setLineWidth(3);
                g.strokeLine(px(xMin), py(y0 + pendiente * (xMin - x0)),
                             px(xMax), py(y0 + pendiente * (xMax - x0)));
                g.restore();

                // Punto de tangencia
                g.setFill(Color.web("#dc2626"));
                g.fillOval(px(x0) - 6, py(y0) - 6, 12, 12);

                // Etiquetas Δx y Δy
                g.setFill(Color.web("#c2410c"));
                g.setFont(Font.font("System", FontWeight.BOLD, 12));
                boolean cArriba = pendiente * dx > 0;
                g.setTextAlign(TextAlignment.CENTER);
                g.fillText("Δx = " + fmtVal(dx), px(x0 + dx / 2), py(y0) + (cArriba ? 16 : -8));
                g.setTextAlign(dx > 0 ? TextAlignment.LEFT : TextAlignment.RIGHT);
                g.fillText("Δy = " + fmtVal(pendiente * dx), px(xB) + (dx > 0 ? 8 : -8), py(y0 + pendiente * dx / 2) + 4);

                leyenda = "Tangente: pendiente = Δy/Δx = " + fmtVal(pendiente);
            }

            // --- Títulos ---
            g.setFill(Color.web("#111827"));
            g.setFont(Font.font("System", FontWeight.BOLD, 13));
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(titulo, ml, 22);
            g.setFont(Font.font("System", FontWeight.BOLD, 12));
            g.setTextAlign(TextAlignment.RIGHT);
            g.setFill(Color.web("#dc2626"));
            g.fillText(leyenda, ml + pw, 22);
            g.setFont(Font.font("System", 12));
            g.setFill(Color.web("#111827"));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(ejeX, ml + pw / 2, h - 10);
            g.save();
            g.translate(16, mt + ph / 2);
            g.rotate(-90);
            g.fillText(ejeY, 0, 0);
            g.restore();
        }

        private static double pasoNice(double rango) {
            if (rango <= 0 || !Double.isFinite(rango)) return 1;
            double bruto = rango / 6.0;
            double mag = Math.pow(10, Math.floor(Math.log10(bruto)));
            double r = bruto / mag;
            double nice = r < 1.5 ? 1 : r < 3 ? 2 : r < 7 ? 5 : 10;
            return nice * mag;
        }

        private static String fmtTick(double v) {
            if (Math.abs(v) < 1e-12) return "0";
            if (Math.abs(v) >= 1e5 || Math.abs(v) < 1e-3) return String.format(Locale.US, "%.1e", v);
            return String.format(Locale.US, "%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        private static String fmtVal(double v) {
            if (v != 0 && Math.abs(v) < 0.01) return String.format(Locale.US, "%.2e", v);
            return String.format(Locale.US, "%.3f", v);
        }
    }

    // =========================================================================
    // 4. INTERFAZ Y LÓGICA DEL MÓDULO
    // =========================================================================
    private final ComboBox<Caso> cmbCaso = new ComboBox<>();
    private final ComboBox<Modelo> cmbModelo = new ComboBox<>();
    private final Label lblFormula = new Label();
    private final VBox cajaCampos = new VBox(6);
    private final List<TextField> camposParam = new ArrayList<>();
    // Caso 1: txtA = t₀ | Caso 2: txtA = valor objetivo b, txtB = t mínimo, txtC = t máximo
    private TextField txtA, txtB, txtC, txtH;
    private String hTexto = "0.00001";

    private final Label lblResultado = new Label("—");
    private final Label lblSecundario = new Label();
    private final TextFlow flujoInterp = new TextFlow();
    private final TextArea txtPasos = new TextArea();
    private final GraficoTangente grafico = new GraficoTangente();

    private boolean construyendo = false;

    public RazonesCambioPanel() {
        Label titulo = new Label("Razones de cambio e interpretación física de la derivada");
        titulo.setWrapText(true);
        titulo.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label lblTipo = etiqueta("Fenómeno físico");
        cmbCaso.getItems().addAll(Caso.values());
        cmbCaso.setMaxWidth(Double.MAX_VALUE);
        cmbModelo.getItems().addAll(MODELOS);
        cmbModelo.setMaxWidth(Double.MAX_VALUE);
        cmbModelo.setConverter(new StringConverter<Modelo>() {
            @Override public String toString(Modelo m) { return m == null ? "" : m.nombre(); }
            @Override public Modelo fromString(String s) { return null; }
        });
        cmbModelo.getSelectionModel().selectFirst();
        lblFormula.setStyle("-fx-font-family: 'Consolas','Menlo','Monospaced'; -fx-text-fill: #1d4ed8; -fx-font-size: 14px;");
        lblFormula.setWrapText(true);

        Button btn = new Button("Calcular");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        btn.setOnAction(e -> calcular());

        VBox izquierda = new VBox(10, titulo, new Separator(), etiqueta("Tipo de razón de cambio"), cmbCaso,
                lblTipo, cmbModelo, lblFormula, new Separator(), cajaCampos, btn);
        izquierda.setPadding(new Insets(16));
        izquierda.setStyle("-fx-background-color: white;");
        ScrollPane scroll = new ScrollPane(izquierda);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(370);

        // Panel de resultados + interpretación física
        lblResultado.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        lblResultado.setWrapText(true);
        lblSecundario.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151;");
        lblSecundario.setWrapText(true);
        VBox cajaInterp = new VBox(6, etiqueta("Interpretación física"), flujoInterp);
        cajaInterp.setPadding(new Insets(10));
        cajaInterp.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; "
                          + "-fx-background-radius: 6; -fx-border-radius: 6;");
        txtPasos.setEditable(false);
        txtPasos.setWrapText(true);
        txtPasos.setPrefRowCount(7);
        VBox resultados = new VBox(8, lblResultado, lblSecundario, cajaInterp, etiqueta("Procedimiento"), txtPasos);
        resultados.setPadding(new Insets(12));
        resultados.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;");

        grafico.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;");
        VBox centro = new VBox(10, grafico, resultados);
        centro.setPadding(new Insets(12));
        VBox.setVgrow(grafico, Priority.ALWAYS);

        setCenter(centro);
        setLeft(scroll);
        setStyle("-fx-background-color: #f3f4f6;");

        cmbCaso.valueProperty().addListener((o, a, b) -> { if (!construyendo && b != null) reconstruir(); });
        cmbModelo.valueProperty().addListener((o, a, b) -> { if (!construyendo && b != null) reconstruir(); });
        cmbCaso.getSelectionModel().selectFirst();   // dispara la primera construcción del formulario
    }

    // ------------------------------------------------------------------------
    //  Conectores de integración
    // ------------------------------------------------------------------------
    /** Devuelve una pestaña lista para insertar en un TabPane. */
    public static Tab comoPestana() {
        Tab tab = new Tab("Razones de cambio", new RazonesCambioPanel());
        tab.setClosable(false);
        return tab;
    }

    /** Añade el módulo como una pestaña más del TabPane principal. */
    public static void integrarEn(TabPane pestanas) {
        pestanas.getTabs().add(comoPestana());
    }

    // ------------------------------------------------------------------------
    //  Formulario dinámico
    // ------------------------------------------------------------------------
    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");
        return l;
    }

    private void reconstruir() {
        construyendo = true;
        construirCampos();
        construyendo = false;
        calcular();
    }

    private void construirCampos() {
        cajaCampos.getChildren().clear();
        camposParam.clear();
        Caso c = cmbCaso.getValue();
        Modelo m = cmbModelo.getValue();
        if (c == null || m == null) return;

        lblFormula.setText(m.formula());

        cajaCampos.getChildren().add(etiqueta("Parámetros del modelo"));
        for (int i = 0; i < m.etiquetas().length; i++) {
            camposParam.add(crearCampo(m.etiquetas()[i], m.unidades()[i], num(m.iniciales()[i])));
        }

        cajaCampos.getChildren().add(new Separator());
        if (c == Caso.INSTANTANEA) {
            cajaCampos.getChildren().add(etiqueta("Punto de evaluación"));
            txtA = crearCampo("Momento exacto (t tiende a t₀)", "s", "5");
            txtB = null;
            txtC = null;
        } else {
            cajaCampos.getChildren().add(etiqueta("Condición B = b"));
            txtA = crearCampo("Valor objetivo (b) de " + m.sujeto(), m.unidadVar(), num(m.objetivoInicial()));
            txtB = crearCampo("Rango de búsqueda: tiempo mínimo", "s", "0");
            txtC = crearCampo("Rango de búsqueda: tiempo máximo", "s", num(m.tMaxInicial()));
        }

        cajaCampos.getChildren().add(new Separator());
        cajaCampos.getChildren().add(etiqueta("Aproximación numérica"));
        txtH = crearCampo("Delta de aproximación (h)", "s", hTexto);
        txtH.textProperty().addListener((o, a, b) -> hTexto = b);
    }

    private TextField crearCampo(String etiqueta, String unidad, String valorInicial) {
        Label l = new Label(etiqueta + "  [" + unidad + "]");
        l.setStyle("-fx-text-fill: #374151;");
        l.setWrapText(true);
        TextField tf = new TextField(valorInicial);
        tf.textProperty().addListener((o, a, b) -> { if (!construyendo) calcular(); });
        cajaCampos.getChildren().addAll(l, tf);
        return tf;
    }

    private double leer(TextField tf, String nombre) {
        try {
            double v = Double.parseDouble(tf.getText().trim().replace(',', '.'));
            if (!Double.isFinite(v)) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Escribe un número válido en «" + nombre + "».");
        }
    }

    // ------------------------------------------------------------------------
    //  Cálculo
    // ------------------------------------------------------------------------
    private void calcular() {
        if (construyendo) return;
        Caso c = cmbCaso.getValue();
        Modelo m = cmbModelo.getValue();
        if (c == null || m == null || txtA == null || txtH == null) return;
        try {
            double[] p = new double[camposParam.size()];
            for (int i = 0; i < p.length; i++) p[i] = leer(camposParam.get(i), m.etiquetas()[i]);
            double h = leer(txtH, "Delta de aproximación (h)");
            if (h <= 0) throw new IllegalArgumentException("El delta de aproximación (h) debe ser positivo.");

            // MAPEO FÍSICO -> MATEMÁTICO: los parámetros (s₀, v₀, k...) se convierten en f(t).
            DoubleUnaryOperator f = m.funcion().apply(p);

            if (c == Caso.INSTANTANEA) caso1(m, p, f, h); else caso2(m, p, f, h);
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /** Caso 1: tasa de cambio instantánea cuando t → t₀. */
    private void caso1(Modelo m, double[] p, DoubleUnaryOperator f, double h) {
        double t0 = leer(txtA, "Momento exacto (t₀)");
        double fMas = f.applyAsDouble(t0 + h), fMenos = f.applyAsDouble(t0 - h);
        double d = MotorDerivacion.derivadaCentral(f, t0, h);
        double fv = f.applyAsDouble(t0);
        double exacta = m.derivadaExacta().apply(p).applyAsDouble(t0);
        verificarFinito(d, fv);

        StringBuilder sb = new StringBuilder();
        sb.append("1) Modelo: ").append(m.formula()).append("\n   con ").append(describir(m, p)).append(".\n");
        sb.append("2) Se pide la tasa de cambio de ").append(m.sujeto()).append(" cuando t tiende a t₀ = ")
          .append(f4(t0)).append(" s:\n   lím(h→0) [f(t₀+h) − f(t₀−h)] / (2h).\n");
        sb.append("3) Con h = ").append(sci(h)).append(" s:  f(t₀+h) = ").append(f6(fMas))
          .append(",  f(t₀−h) = ").append(f6(fMenos)).append("\n   ⇒ f'(t₀) ≈ ").append(f4(d)).append(" ")
          .append(m.unidadDeriv()).append(".\n");
        sb.append("4) Valor de la variable en ese instante: f(t₀) = ").append(f4(fv)).append(" ").append(m.unidadVar()).append(".\n");
        sb.append("5) Derivada analítica de referencia: ").append(f4(exacta)).append(" ").append(m.unidadDeriv())
          .append("  (error numérico ").append(sci(Math.abs(d - exacta))).append(").\n");
        sb.append("6) Recta tangente: y = ").append(f4(fv)).append(" + ").append(f4(d)).append("·(t − ").append(f4(t0))
          .append("). Cualquier triángulo sobre ella cumple Δy/Δx = f'(t₀).");
        sb.append(notaH(h, t0));

        lblResultado.setText(m.nombreDeriv() + " = " + f4(d) + " " + m.unidadDeriv());
        lblSecundario.setText("en t = " + f4(t0) + " s   |   " + cap(m.sujeto()) + " = " + f4(fv) + " " + m.unidadVar());
        txtPasos.setText(sb.toString());

        if (Math.abs(d) < 1e-9) {
            interpretar(n("En el instante "), b("t = " + f4(t0) + " s"), n(", " + m.sujeto() + " "),
                        b("no está cambiando"), n(" (tangente horizontal: equilibrio, máximo o mínimo). Vale "),
                        b(f4(fv) + " " + m.unidadVar()), n("."));
        } else {
            interpretar(n("En el instante "), b("t = " + f4(t0) + " s"), n(", " + m.sujeto() + " está "),
                        b(d > 0 ? "aumentando" : "disminuyendo"), n(" a razón de "),
                        b(f4(Math.abs(d)) + " " + m.unidadDeriv()), n(". En ese momento vale "),
                        b(f4(fv) + " " + m.unidadVar()), n(".\n\nGeométricamente, la recta tangente a la curva en ese punto tiene pendiente "),
                        b(f4(d)), n(" (Δy/Δx = " + f4(d) + ")."));
        }

        grafico.actualizar("Evolución de " + m.sujeto().substring(3) + " con el tiempo", "Tiempo t [s]",
                cap(m.sujeto().substring(3)) + " [" + m.unidadVar() + "]", f,
                Math.min(0, 1.5 * t0 - 1), Math.max(10, 2 * t0), true, t0, d, false, 0);
    }

    /** Caso 2: razón de cambio en el instante t* en que se cumple f(t*) = b. */
    private void caso2(Modelo m, double[] p, DoubleUnaryOperator f, double h) {
        double objetivo = leer(txtA, "Valor objetivo (b)");
        double tMin = leer(txtB, "Tiempo mínimo de búsqueda");
        double tMax = leer(txtC, "Tiempo máximo de búsqueda");
        if (tMax <= tMin) throw new IllegalArgumentException("El tiempo máximo de búsqueda debe ser mayor que el mínimo.");
        String tituloGrafico = "Evolución de " + m.sujeto().substring(3) + " con el tiempo";
        String ejeY = cap(m.sujeto().substring(3)) + " [" + m.unidadVar() + "]";

        MotorDerivacion.Raiz r = MotorDerivacion.resolverCondicion(f, objetivo, tMin, tMax, h);
        if (r == null) {
            double fa = f.applyAsDouble(tMin), fb = f.applyAsDouble(tMax);
            lblResultado.setText("Condición no alcanzada");
            lblSecundario.setText("");
            interpretar(n("⚠ " + cap(m.sujeto()) + " no llega a "), b(f4(objetivo) + " " + m.unidadVar()),
                    n(" entre t = " + f4(tMin) + " s y t = " + f4(tMax) + " s (en ese rango va de " + f4(fa)
                      + " a " + f4(fb) + " " + m.unidadVar() + ", o solo toca ese valor sin cruzarlo). "
                      + "Amplía el rango de búsqueda o cambia el valor objetivo."));
            txtPasos.setText("No hay cambio de signo de g(t) = f(t) − b en el rango dado, así que no se puede garantizar una solución.");
            grafico.actualizar(tituloGrafico, "Tiempo t [s]", ejeY, f, tMin, tMax, false, 0, 0, true, objetivo);
            return;
        }

        double tStar = r.x();
        double fStar = f.applyAsDouble(tStar);
        double d = MotorDerivacion.derivadaCentral(f, tStar, h);
        double exacta = m.derivadaExacta().apply(p).applyAsDouble(tStar);
        verificarFinito(tStar, d);

        StringBuilder sb = new StringBuilder();
        sb.append("1) Modelo: ").append(m.formula()).append("\n   con ").append(describir(m, p)).append(".\n");
        sb.append("2) Condición B = b:  f(t*) = ").append(f4(objetivo)).append(" ").append(m.unidadVar())
          .append(", buscando en [").append(f4(tMin)).append(", ").append(f4(tMax)).append("] s.\n");
        sb.append("3) Barrido en 2000 tramos: primer cambio de signo de g(t) = f(t) − b en [")
          .append(f6(r.inicioIntervalo())).append(", ").append(f6(r.finIntervalo())).append("].\n");
        sb.append("4) Newton-Raphson protegido por bisección:  t ← t − g(t)/g'(t)  (g' por diferencia central).\n   ")
          .append(r.iteraciones()).append(" iteraciones (").append(r.pasosNewton()).append(" de Newton, ")
          .append(r.pasosBiseccion()).append(" de bisección)  ⇒  t* = ").append(f6(tStar))
          .append(" s,  residuo |f(t*) − b| = ").append(sci(r.residuo())).append(".\n");
        sb.append("5) Derivada en t*: f'(t*) ≈ ").append(f4(d)).append(" ").append(m.unidadDeriv()).append(".\n");
        sb.append("6) Derivada analítica de referencia: ").append(f4(exacta)).append(" ").append(m.unidadDeriv())
          .append("  (error numérico ").append(sci(Math.abs(d - exacta))).append(").\n");
        sb.append("7) Tangente en (t*, b): pendiente = Δy/Δx = ").append(f4(d)).append(".");
        sb.append(notaH(h, tStar));

        lblResultado.setText("t* = " + f4(tStar) + " s");
        lblSecundario.setText(m.nombreDeriv() + " en ese instante = " + f4(d) + " " + m.unidadDeriv());
        txtPasos.setText(sb.toString());

        if (Math.abs(d) < 1e-9) {
            interpretar(n(cap(m.sujeto()) + " llega a "), b(f4(objetivo) + " " + m.unidadVar()), n(" en "),
                        b("t* = " + f4(tStar) + " s"), n(". En ese instante "), b("no está cambiando"),
                        n(" (tangente horizontal)."));
        } else {
            interpretar(n(cap(m.sujeto()) + " llega a "), b(f4(objetivo) + " " + m.unidadVar()), n(" en "),
                        b("t* = " + f4(tStar) + " s"), n(". En ese instante " + m.sujeto() + " está "),
                        b(d > 0 ? "aumentando" : "disminuyendo"), n(" a razón de "),
                        b(f4(Math.abs(d)) + " " + m.unidadDeriv()), n(".\n\nLa recta tangente en ese punto tiene pendiente "),
                        b(f4(d)), n("."));
        }

        grafico.actualizar(tituloGrafico, "Tiempo t [s]", ejeY, f, tMin, tMax, true, tStar, d, true, objetivo);
    }

    // ------------------------------------------------------------------------
    //  Utilidades
    // ------------------------------------------------------------------------
    private void mostrarError(String mensaje) {
        lblResultado.setText("—");
        lblSecundario.setText("");
        Text t = new Text("⚠ " + mensaje);
        t.setStyle("-fx-font-size: 15px; -fx-fill: #b91c1c;");
        flujoInterp.getChildren().setAll(t);
        txtPasos.setText("");
        grafico.limpiar();
    }

    private void interpretar(Text... partes) { flujoInterp.getChildren().setAll(partes); }

    private static Text n(String s) {
        Text t = new Text(s);
        t.setStyle("-fx-font-size: 15px; -fx-fill: #111827;");
        return t;
    }

    private static Text b(String s) {
        Text t = new Text(s);
        t.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-fill: #1d4ed8;");
        return t;
    }

    private static String notaH(double h, double escala) {
        if (h < 1e-8) return "\n⚠ h es muy pequeño: domina el error de redondeo y el resultado puede degradarse.";
        if (h > 0.01 * Math.max(1.0, Math.abs(escala))) return "\n⚠ h es grande: el error de truncamiento (≈ h²) puede ser apreciable.";
        return "";
    }

    private static void verificarFinito(double... valores) {
        for (double v : valores) {
            if (!Double.isFinite(v)) {
                throw new IllegalArgumentException("El resultado no es un número finito. Revisa los parámetros (¿división por cero?).");
            }
        }
    }

    private static String describir(Modelo m, double[] p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(m.etiquetas()[i]).append(" = ").append(num(p[i])).append(" ").append(m.unidades()[i]);
        }
        return sb.toString();
    }

    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String num(double v) { return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString(); }
    private static String f4(double v) { return String.format(Locale.US, "%.4f", v); }
    private static String f6(double v) { return String.format(Locale.US, "%.6f", v); }
    private static String sci(double v) { return String.format(Locale.US, "%.2e", v); }
}
