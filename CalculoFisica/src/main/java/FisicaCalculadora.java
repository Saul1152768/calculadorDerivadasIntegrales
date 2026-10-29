/*
 * ============================================================================
 *  FisicaCalculadora.java
 *  Calculadora de Física y Cálculo Aplicado (Java 17+ / JavaFX 21)
 * ============================================================================
 *  El usuario NUNCA escribe "x", "dx" ni fórmulas abstractas: trabaja con
 *  magnitudes físicas (v₀, a, t₁, t₂, F, d₁, d₂...) y la aplicación se encarga
 *  de convertirlas en funciones matemáticas y de aplicar el cálculo numérico.
 *
 *  ESTRUCTURA DEL ARCHIVO
 *    1. MotorCalculo   -> métodos numéricos (diferencias finitas, Simpson, trapecio)
 *    2. Perfil         -> "modelo físico": mapea parámetros físicos -> función f(t) o F(d)
 *    3. GraficoCanvas  -> gráfico propio (curva, área sombreada, recta tangente)
 *    4. FisicaCalculadora -> interfaz JavaFX y lógica de los 3 problemas
 *
 *  MAPEO FÍSICO -> NUMÉRICO (resumen)
 *    Parámetros ingresados (v₀, a, k, ...)  --Perfil.funcion()-->  DoubleUnaryOperator v(t)
 *    Esa función se evalúa punto a punto dentro del MotorCalculo:
 *      - Integral:  Simpson 1/3 sobre [t₁, t₂]  (o [d₁, d₂] para el trabajo)
 *      - Derivada:  diferencia central  [v(t+h) - v(t-h)] / (2h)  en t_exacto
 *    Además cada Perfil trae su derivada y su primitiva analíticas SOLO para
 *    comparar contra el resultado numérico y mostrar el error.
 *
 *  COMPILAR Y EJECUTAR: ver instrucciones en la respuesta / README.
 * ============================================================================
 */

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
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
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

public class FisicaCalculadora extends Application {

    // Subintervalos para Simpson/trapecio (Simpson exige n par).
    private static final int N_SIMPSON = 2000;
    // Para ∫|v| dt (la función tiene "picos" donde v cruza el cero) usamos más puntos.
    private static final int N_ABS = 20000;

    // =========================================================================
    // 1. MOTOR DE CÁLCULO NUMÉRICO
    // =========================================================================
    static final class MotorCalculo {
        private MotorCalculo() { }

        /** Derivada por diferencia central: f'(x0) ≈ [f(x0+h) - f(x0-h)] / (2h). Error O(h²). */
        static double derivadaCentral(DoubleUnaryOperator f, double x0, double h) {
            return (f.applyAsDouble(x0 + h) - f.applyAsDouble(x0 - h)) / (2.0 * h);
        }

        /** Integral definida por Simpson 1/3: (h/3)[f0 + 4f1 + 2f2 + ... + 4f(n-1) + fn]. */
        static double simpson(DoubleUnaryOperator f, double a, double b, int n) {
            if (n % 2 != 0) n++;                       // Simpson necesita n par
            double h = (b - a) / n;
            double suma = f.applyAsDouble(a) + f.applyAsDouble(b);
            for (int i = 1; i < n; i++) {
                suma += f.applyAsDouble(a + i * h) * (i % 2 == 1 ? 4.0 : 2.0);
            }
            return suma * h / 3.0;
        }

        /** Integral definida por la regla del trapecio (se usa como verificación cruzada). */
        static double trapecio(DoubleUnaryOperator f, double a, double b, int n) {
            double h = (b - a) / n;
            double suma = 0.5 * (f.applyAsDouble(a) + f.applyAsDouble(b));
            for (int i = 1; i < n; i++) {
                suma += f.applyAsDouble(a + i * h);
            }
            return suma * h;
        }
    }

    // =========================================================================
    // 2. MODELOS FÍSICOS (mapeo parámetros físicos -> función matemática)
    // =========================================================================
    /**
     * Un "perfil" describe un fenómeno físico con hasta 2 parámetros.
     *  - funcion:   convierte los parámetros p[] en la función continua v(t) o F(d)
     *  - derivada:  derivada analítica (solo para comparar; puede ser null)
     *  - primitiva: antiderivada analítica (solo para comparar; puede ser null)
     */
    record Perfil(String nombre, String formula,
                  String[] etiquetas, String[] unidades, double[] valoresIniciales,
                  Function<double[], DoubleUnaryOperator> funcion,
                  Function<double[], DoubleUnaryOperator> derivada,
                  Function<double[], DoubleUnaryOperator> primitiva) { }

    private static final List<Perfil> VELOCIDADES = List.of(
        new Perfil("Aceleración constante (MRUA)", "v(t) = v₀ + a·t",
            new String[]{"Velocidad inicial (v₀)", "Aceleración (a)"},
            new String[]{"m/s", "m/s²"},
            new double[]{5, 2},
            p -> t -> p[0] + p[1] * t,
            p -> t -> p[1],
            p -> t -> p[0] * t + 0.5 * p[1] * t * t),
        new Perfil("Movimiento oscilante (resorte / péndulo)", "v(t) = v_máx · sen(ω·t)",
            new String[]{"Velocidad máxima (v_máx)", "Frecuencia angular (ω)"},
            new String[]{"m/s", "rad/s"},
            new double[]{10, 1},
            p -> t -> p[0] * Math.sin(p[1] * t),
            p -> t -> p[0] * p[1] * Math.cos(p[1] * t),
            p -> t -> -p[0] / p[1] * Math.cos(p[1] * t)),
        new Perfil("Frenado con resistencia (decaimiento exponencial)", "v(t) = v₀ · e^(−k·t)",
            new String[]{"Velocidad inicial (v₀)", "Coeficiente de frenado (k)"},
            new String[]{"m/s", "1/s"},
            new double[]{20, 0.5},
            p -> t -> p[0] * Math.exp(-p[1] * t),
            p -> t -> -p[0] * p[1] * Math.exp(-p[1] * t),
            p -> t -> -p[0] / p[1] * Math.exp(-p[1] * t))
    );

    private static final List<Perfil> FUERZAS = List.of(
        new Perfil("Fuerza constante", "F(d) = F",
            new String[]{"Fuerza aplicada (F)"},
            new String[]{"N"},
            new double[]{50},
            p -> d -> p[0],
            null,
            p -> d -> p[0] * d),
        new Perfil("Resorte (Ley de Hooke)", "F(d) = k · d",
            new String[]{"Constante del resorte (k)"},
            new String[]{"N/m"},
            new double[]{200},
            p -> d -> p[0] * d,
            null,
            p -> d -> 0.5 * p[0] * d * d),
        new Perfil("Fuerza que decae con la distancia", "F(d) = F₀ · e^(−d/L)",
            new String[]{"Fuerza inicial (F₀)", "Longitud característica (L)"},
            new String[]{"N", "m"},
            new double[]{100, 2},
            p -> d -> p[0] * Math.exp(-d / p[1]),
            null,
            p -> d -> -p[0] * p[1] * Math.exp(-d / p[1]))
    );

    enum Problema {
        POSICION("1. Posición a partir de la velocidad  (s = ∫ v dt)"),
        ACELERACION("2. Aceleración a partir de la velocidad  (a = dv/dt)"),
        TRABAJO("3. Trabajo mecánico  (W = ∫ F dd)");

        private final String texto;
        Problema(String texto) { this.texto = texto; }
        @Override public String toString() { return texto; }
    }

    // =========================================================================
    // 3. GRÁFICO PROPIO (Canvas)
    // =========================================================================
    static class GraficoCanvas extends Pane {
        enum Resalte { NINGUNO, AREA, TANGENTE }

        private final Canvas canvas = new Canvas();
        private DoubleUnaryOperator f;
        private String titulo = "", ejeX = "", ejeY = "";
        private double xMin = 0, xMax = 1;
        private Resalte resalte = Resalte.NINGUNO;
        private double p1, p2;   // AREA: [p1,p2] límites | TANGENTE: p1 = punto, p2 = pendiente

        // Geometría de la zona de dibujo (se recalcula en cada dibujo)
        private double ml, mt, pw, ph, yMin, yMax;

        GraficoCanvas() {
            getChildren().add(canvas);
            setMinHeight(260);
            setPrefHeight(430);
        }

        @Override
        protected void layoutChildren() {
            canvas.setWidth(getWidth());
            canvas.setHeight(getHeight());
            dibujar();
        }

        void actualizar(String titulo, String ejeX, String ejeY, DoubleUnaryOperator f,
                        double xMin, double xMax, Resalte resalte, double p1, double p2) {
            this.titulo = titulo; this.ejeX = ejeX; this.ejeY = ejeY;
            this.f = f; this.xMin = xMin; this.xMax = xMax;
            this.resalte = resalte; this.p1 = p1; this.p2 = p2;
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
            if (f == null || w < 150 || h < 120) return;

            ml = 66; mt = 36;
            double mr = 20, mb = 50;
            pw = w - ml - mr;
            ph = h - mt - mb;

            // --- Muestreo de la curva y rango vertical (siempre incluye y = 0) ---
            int n = Math.max(2, (int) pw);
            double[] ys = new double[n + 1];
            yMin = 0; yMax = 0;
            for (int i = 0; i <= n; i++) {
                ys[i] = f.applyAsDouble(xMin + (xMax - xMin) * i / n);
                if (Double.isFinite(ys[i])) {
                    yMin = Math.min(yMin, ys[i]);
                    yMax = Math.max(yMax, ys[i]);
                }
            }
            if (yMax - yMin < 1e-12) yMax = yMin + 1;
            double pad = 0.1 * (yMax - yMin);
            yMax += pad;
            if (yMin < 0) yMin -= pad;

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

            // Ejes en cero
            g.setStroke(Color.web("#6b7280"));
            g.setLineWidth(1.5);
            if (yMin < 0 && yMax > 0) g.strokeLine(ml, py(0), ml + pw, py(0));
            if (xMin < 0 && xMax > 0) g.strokeLine(px(0), mt, px(0), mt + ph);
            g.setLineWidth(1);
            g.setStroke(Color.web("#9ca3af"));
            g.strokeRect(ml, mt, pw, ph);

            // --- Área sombreada bajo la curva (integral) ---
            String leyenda = "";
            if (resalte == Resalte.AREA) {
                g.setFill(Color.rgb(37, 99, 235, 0.25));
                g.beginPath();
                g.moveTo(px(p1), py(0));
                int m = 400;
                for (int i = 0; i <= m; i++) {
                    double x = p1 + (p2 - p1) * i / m;
                    double y = f.applyAsDouble(x);
                    if (!Double.isFinite(y)) y = 0;
                    g.lineTo(px(x), py(y));
                }
                g.lineTo(px(p2), py(0));
                g.closePath();
                g.fill();
                g.setStroke(Color.rgb(37, 99, 235, 0.7));
                g.setLineDashes(5, 4);
                g.strokeLine(px(p1), py(0), px(p1), py(f.applyAsDouble(p1)));
                g.strokeLine(px(p2), py(0), px(p2), py(f.applyAsDouble(p2)));
                g.setLineDashes((double[]) null);
                leyenda = "Área sombreada = integral";
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

            // --- Recta tangente (derivada) ---
            if (resalte == Resalte.TANGENTE) {
                double y0 = f.applyAsDouble(p1);
                g.save();
                g.beginPath();
                g.rect(ml, mt, pw, ph);
                g.clip();
                g.setStroke(Color.web("#dc2626"));
                g.setLineWidth(2);
                g.setLineDashes(8, 5);
                g.strokeLine(px(xMin), py(y0 + p2 * (xMin - p1)),
                             px(xMax), py(y0 + p2 * (xMax - p1)));
                g.restore();
                g.setFill(Color.web("#dc2626"));
                g.fillOval(px(p1) - 5, py(y0) - 5, 10, 10);
                leyenda = String.format(Locale.US, "Recta tangente: pendiente = %.4f", p2);
            }

            // --- Títulos y ejes ---
            g.setFill(Color.web("#111827"));
            g.setFont(Font.font("System", FontWeight.BOLD, 13));
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(titulo, ml, 22);
            g.setFont(Font.font("System", 12));
            g.setTextAlign(TextAlignment.RIGHT);
            g.setFill(resalte == Resalte.TANGENTE ? Color.web("#dc2626") : Color.web("#1d4ed8"));
            g.fillText(leyenda, ml + pw, 22);
            g.setFill(Color.web("#111827"));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(ejeX, ml + pw / 2, h - 10);
            g.save();
            g.translate(16, mt + ph / 2);
            g.rotate(-90);
            g.fillText(ejeY, 0, 0);
            g.restore();
        }

        /** Paso "bonito" (1, 2, 5 × 10^n) para ~6 divisiones. */
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
            String s = String.format(Locale.US, "%.3f", v);
            return s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }

    // =========================================================================
    // 4. INTERFAZ Y LÓGICA
    // =========================================================================
    private final ComboBox<Problema> cmbProblema = new ComboBox<>();
    private final ComboBox<Perfil> cmbPerfil = new ComboBox<>();
    private final Label lblTipo = new Label("Tipo de movimiento");
    private final Label lblFormula = new Label();
    private final VBox cajaCampos = new VBox(6);
    private final List<TextField> camposParam = new ArrayList<>();
    private TextField txtA, txtB;   // Problema 1: t₁,t₂ | Problema 2: t_exacto | Problema 3: d₁,d₂

    private final Label lblResultado = new Label("—");
    private final Label lblSecundario = new Label();
    private final TextArea txtPasos = new TextArea();
    private final GraficoCanvas grafico = new GraficoCanvas();

    private boolean construyendo = false;   // evita recalcular mientras se reconstruye el formulario
    private boolean sincronizando = false;  // evita bucles slider <-> campo de texto

    @Override
    public void start(Stage stage) {
        Label titulo = new Label("Calculadora de Física y Cálculo Aplicado");
        titulo.setWrapText(true);
        titulo.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label lblProblema = etiqueta("Problema a resolver");
        lblTipo.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        cmbProblema.getItems().addAll(Problema.values());
        cmbProblema.setMaxWidth(Double.MAX_VALUE);
        cmbPerfil.setMaxWidth(Double.MAX_VALUE);
        cmbPerfil.setConverter(new StringConverter<Perfil>() {
            @Override public String toString(Perfil p) { return p == null ? "" : p.nombre(); }
            @Override public Perfil fromString(String s) { return null; }
        });
        lblFormula.setStyle("-fx-font-family: 'Consolas','Menlo','Monospaced'; "
                          + "-fx-text-fill: #1d4ed8; -fx-font-size: 14px;");

        Button btnCalcular = new Button("Calcular");
        btnCalcular.setMaxWidth(Double.MAX_VALUE);
        btnCalcular.setDefaultButton(true);
        btnCalcular.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; "
                           + "-fx-font-weight: bold; -fx-padding: 8 16;");
        btnCalcular.setOnAction(e -> calcular());

        VBox izquierda = new VBox(10, titulo, new Separator(), lblProblema, cmbProblema,
                lblTipo, cmbPerfil, lblFormula, new Separator(), cajaCampos, btnCalcular);
        izquierda.setPadding(new Insets(16));
        izquierda.setStyle("-fx-background-color: white;");
        ScrollPane scroll = new ScrollPane(izquierda);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(370);

        // Panel de resultados
        lblResultado.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        lblResultado.setWrapText(true);
        lblSecundario.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        lblSecundario.setWrapText(true);
        txtPasos.setEditable(false);
        txtPasos.setWrapText(true);
        txtPasos.setPrefRowCount(9);
        VBox resultados = new VBox(6, lblResultado, lblSecundario, txtPasos);
        resultados.setPadding(new Insets(12));
        resultados.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;");

        grafico.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;");
        VBox centro = new VBox(10, grafico, resultados);
        centro.setPadding(new Insets(12));
        VBox.setVgrow(grafico, Priority.ALWAYS);

        BorderPane raiz = new BorderPane(centro);
        raiz.setLeft(scroll);
        raiz.setStyle("-fx-background-color: #f3f4f6;");

        // Listeners (se registran antes de seleccionar el primer elemento)
        cmbProblema.valueProperty().addListener((o, a, b) -> { if (b != null) cambiarProblema(); });
        cmbPerfil.valueProperty().addListener((o, a, b) -> {
            if (!construyendo && b != null) {
                construyendo = true;
                construirCampos();
                construyendo = false;
                calcular();
            }
        });

        stage.setTitle("Calculadora de Física y Cálculo Aplicado");
        // --- Conector: la app base y los módulos de Razones de Cambio como pestañas ---
        Tab pestanaBase = new Tab("Cálculo aplicado", raiz);
        pestanaBase.setClosable(false);
        TabPane pestanas = new TabPane(pestanaBase);
        RazonesCambioPanel.integrarEn(pestanas);      // <- una sola línea para acoplar el módulo
        ProblemasRazonesPanel.integrarEn(pestanas);   // <- módulo de problemas de razones relacionadas (usa BancoProblemas)
        stage.setScene(new Scene(pestanas, 1200, 800));
        stage.show();

        cmbProblema.getSelectionModel().selectFirst();
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");
        return l;
    }

    private void cambiarProblema() {
        construyendo = true;
        Problema pr = cmbProblema.getValue();
        lblTipo.setText(pr == Problema.TRABAJO ? "Tipo de fuerza" : "Tipo de movimiento");
        cmbPerfil.getItems().setAll(pr == Problema.TRABAJO ? FUERZAS : VELOCIDADES);
        cmbPerfil.getSelectionModel().selectFirst();
        construirCampos();
        construyendo = false;
        calcular();
    }

    /** Reconstruye el formulario según el problema y el perfil seleccionados. */
    private void construirCampos() {
        cajaCampos.getChildren().clear();
        camposParam.clear();
        Problema pr = cmbProblema.getValue();
        Perfil pf = cmbPerfil.getValue();
        if (pr == null || pf == null) return;

        lblFormula.setText(pf.formula());

        cajaCampos.getChildren().add(etiqueta("Parámetros del modelo"));
        for (int i = 0; i < pf.etiquetas().length; i++) {
            camposParam.add(crearCampo(pf.etiquetas()[i], pf.unidades()[i], num(pf.valoresIniciales()[i])));
        }

        cajaCampos.getChildren().add(new Separator());
        cajaCampos.getChildren().add(etiqueta(pr == Problema.ACELERACION ? "Instante a analizar" : "Intervalo"));
        switch (pr) {
            case POSICION -> {
                txtA = crearCampo("Tiempo inicial (t₁)", "s", "0");
                txtB = crearCampo("Tiempo final (t₂)", "s", "10");
            }
            case TRABAJO -> {
                txtA = crearCampo("Distancia inicial (d₁)", "m", "0");
                txtB = crearCampo("Distancia final (d₂)", "m", "5");
            }
            case ACELERACION -> {
                txtA = crearCampo("Momento exacto (t_exacto)", "s", "3");
                txtB = null;
                Slider sld = new Slider(0, 20, 3);
                sld.setShowTickMarks(true);
                sld.setShowTickLabels(true);
                sld.setMajorTickUnit(5);
                sld.setMinorTickCount(4);
                cajaCampos.getChildren().add(sld);
                final TextField campo = txtA;
                // Slider -> campo de texto (el campo dispara el recálculo)
                sld.valueProperty().addListener((o, a, b) -> {
                    if (sincronizando) return;
                    sincronizando = true;
                    campo.setText(String.format(Locale.US, "%.2f", b.doubleValue()));
                    sincronizando = false;
                });
                // Campo de texto -> slider
                campo.textProperty().addListener((o, a, b) -> {
                    if (sincronizando) return;
                    try {
                        double v = Double.parseDouble(b.trim().replace(',', '.'));
                        sincronizando = true;
                        sld.setValue(v);
                    } catch (NumberFormatException ignorado) {
                        // mientras escribe, el texto puede ser inválido: no pasa nada
                    } finally {
                        sincronizando = false;
                    }
                });
            }
        }
    }

    private TextField crearCampo(String etiqueta, String unidad, String valorInicial) {
        Label l = new Label(etiqueta + "  [" + unidad + "]");
        l.setStyle("-fx-text-fill: #374151;");
        TextField tf = new TextField(valorInicial);
        tf.textProperty().addListener((o, a, b) -> { if (!construyendo) calcular(); });
        cajaCampos.getChildren().addAll(l, tf);
        return tf;
    }

    // ------------------------------------------------------------------------
    //  Lectura y validación de datos
    // ------------------------------------------------------------------------
    private double leer(TextField tf, String nombre) {
        try {
            double v = Double.parseDouble(tf.getText().trim().replace(',', '.'));
            if (!Double.isFinite(v)) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Escribe un número válido en «" + nombre + "».");
        }
    }

    private double[] leerParametros(Perfil pf) {
        double[] p = new double[camposParam.size()];
        for (int i = 0; i < p.length; i++) p[i] = leer(camposParam.get(i), pf.etiquetas()[i]);
        return p;
    }

    // ------------------------------------------------------------------------
    //  Cálculo
    // ------------------------------------------------------------------------
    private void calcular() {
        Problema pr = cmbProblema.getValue();
        Perfil pf = cmbPerfil.getValue();
        if (pr == null || pf == null || camposParam.isEmpty() || txtA == null) return;
        try {
            double[] p = leerParametros(pf);
            switch (pr) {
                case POSICION -> calcularPosicion(pf, p);
                case ACELERACION -> calcularAceleracion(pf, p);
                case TRABAJO -> calcularTrabajo(pf, p);
            }
        } catch (IllegalArgumentException ex) {
            lblResultado.setText("—");
            lblSecundario.setText("");
            txtPasos.setText("⚠ " + ex.getMessage());
            grafico.limpiar();
        }
    }

    /** Problema 1: Δs = ∫ v(t) dt  (Simpson 1/3) */
    private void calcularPosicion(Perfil pf, double[] p) {
        double t1 = leer(txtA, "Tiempo inicial (t₁)");
        double t2 = leer(txtB, "Tiempo final (t₂)");
        if (t2 <= t1) throw new IllegalArgumentException("El tiempo final (t₂) debe ser mayor que el inicial (t₁).");

        // MAPEO FÍSICO -> MATEMÁTICO: los parámetros (v₀, a, ...) se convierten en la función v(t).
        DoubleUnaryOperator v = pf.funcion().apply(p);

        double ds = MotorCalculo.simpson(v, t1, t2, N_SIMPSON);
        double dsTrap = MotorCalculo.trapecio(v, t1, t2, N_SIMPSON);
        double distancia = MotorCalculo.simpson(t -> Math.abs(v.applyAsDouble(t)), t1, t2, N_ABS);
        double exacto = valorAnalitico(pf, p, t1, t2);
        verificarFinito(ds, dsTrap, distancia);

        StringBuilder sb = new StringBuilder();
        sb.append("1) Modelo físico: ").append(pf.formula()).append("\n   con ")
          .append(describir(pf, p)).append(".\n");
        sb.append("2) El desplazamiento es el área neta bajo la curva v–t:  Δs = ∫ v(t) dt,  de t₁ = ")
          .append(f4(t1)).append(" s a t₂ = ").append(f4(t2)).append(" s.\n");
        sb.append("3) Regla de Simpson 1/3 con n = ").append(N_SIMPSON).append(" subintervalos (h = ")
          .append(f6((t2 - t1) / N_SIMPSON)).append(" s):  Δs = ").append(f4(ds)).append(" m.\n");
        sb.append("4) Verificación con la regla del trapecio: ").append(f4(dsTrap))
          .append(" m  (diferencia ").append(sci(Math.abs(ds - dsTrap))).append(" m).\n");
        if (Double.isFinite(exacto)) {
            sb.append("5) Valor analítico de referencia: ").append(f4(exacto))
              .append(" m  (error de Simpson ").append(sci(Math.abs(ds - exacto))).append(" m).\n");
        }
        sb.append("6) Distancia total recorrida = ∫|v| dt = ").append(f4(distancia)).append(" m. ")
          .append("Si v cambia de signo, la distancia recorrida es mayor que el desplazamiento neto Δs.");

        lblResultado.setText("Δs = " + f4(ds) + " m");
        lblSecundario.setText("Distancia total recorrida (∫|v| dt) = " + f4(distancia) + " m");
        txtPasos.setText(sb.toString());

        double xMin = Math.min(0, t1);
        double xMax = t2 + 0.1 * (t2 - xMin);
        grafico.actualizar("Velocidad en función del tiempo", "Tiempo t [s]", "Velocidad v [m/s]",
                v, xMin, xMax, GraficoCanvas.Resalte.AREA, t1, t2);
    }

    /** Problema 2: a = dv/dt  (diferencia central) */
    private void calcularAceleracion(Perfil pf, double[] p) {
        double t0 = leer(txtA, "Momento exacto (t_exacto)");
        DoubleUnaryOperator v = pf.funcion().apply(p);   // mapeo físico -> v(t)

        // Paso h proporcional a la escala de t0 (equilibrio entre error de truncamiento y de redondeo).
        double h = 1e-5 * Math.max(1.0, Math.abs(t0));
        double vMas = v.applyAsDouble(t0 + h), vMenos = v.applyAsDouble(t0 - h);
        double a = MotorCalculo.derivadaCentral(v, t0, h);
        double v0 = v.applyAsDouble(t0);
        double exacta = pf.derivada() == null ? Double.NaN : pf.derivada().apply(p).applyAsDouble(t0);
        verificarFinito(a, v0);

        String interp = Math.abs(a) < 1e-6
            ? "a ≈ 0: la velocidad es momentáneamente constante (la tangente es horizontal)."
            : a > 0 ? "a > 0: en ese instante la velocidad está aumentando."
                    : "a < 0: en ese instante la velocidad está disminuyendo (frenado si v > 0).";

        StringBuilder sb = new StringBuilder();
        sb.append("1) Modelo físico: ").append(pf.formula()).append("\n   con ")
          .append(describir(pf, p)).append(".\n");
        sb.append("2) La aceleración instantánea es a = dv/dt = pendiente de la recta tangente a la curva v–t en t_exacto = ")
          .append(f4(t0)).append(" s.\n");
        sb.append("3) Diferencia finita central con h = ").append(sci(h)).append(" s:\n   a ≈ [v(t+h) − v(t−h)] / (2h) = [")
          .append(f6(vMas)).append(" − ").append(f6(vMenos)).append("] / ").append(sci(2 * h))
          .append(" = ").append(f4(a)).append(" m/s².\n");
        sb.append("4) Velocidad en ese instante: v(t_exacto) = ").append(f4(v0)).append(" m/s.\n");
        sb.append("5) Recta tangente:  v ≈ ").append(f4(v0)).append(" + ").append(f4(a)).append("·(t − ")
          .append(f4(t0)).append(").\n");
        if (Double.isFinite(exacta)) {
            sb.append("6) Derivada analítica de referencia: ").append(f4(exacta))
              .append(" m/s²  (error numérico ").append(sci(Math.abs(a - exacta))).append(").\n");
        }
        sb.append("7) Interpretación: ").append(interp);

        lblResultado.setText("a = " + f4(a) + " m/s²");
        lblSecundario.setText("Pendiente de la recta tangente = " + f4(a) + " (m/s)/s   |   v(t_exacto) = " + f4(v0) + " m/s");
        txtPasos.setText(sb.toString());

        double xMin = Math.min(0, t0 - 1);
        double xMax = Math.max(10, 1.3 * t0);
        grafico.actualizar("Velocidad en función del tiempo", "Tiempo t [s]", "Velocidad v [m/s]",
                v, xMin, xMax, GraficoCanvas.Resalte.TANGENTE, t0, a);
    }

    /** Problema 3: W = ∫ F(d) dd  (Simpson 1/3) */
    private void calcularTrabajo(Perfil pf, double[] p) {
        double d1 = leer(txtA, "Distancia inicial (d₁)");
        double d2 = leer(txtB, "Distancia final (d₂)");
        if (d2 <= d1) throw new IllegalArgumentException("La distancia final (d₂) debe ser mayor que la inicial (d₁).");

        DoubleUnaryOperator F = pf.funcion().apply(p);   // mapeo físico -> F(d)
        double w = MotorCalculo.simpson(F, d1, d2, N_SIMPSON);
        double wTrap = MotorCalculo.trapecio(F, d1, d2, N_SIMPSON);
        double exacto = valorAnalitico(pf, p, d1, d2);
        verificarFinito(w, wTrap);

        String interp = w > 0 ? "W > 0: la fuerza aporta energía al cuerpo (actúa a favor del desplazamiento)."
                      : w < 0 ? "W < 0: la fuerza se opone al desplazamiento y le quita energía al cuerpo."
                              : "W = 0: la fuerza no transfiere energía neta en este tramo.";

        StringBuilder sb = new StringBuilder();
        sb.append("1) Modelo físico: ").append(pf.formula()).append("\n   con ")
          .append(describir(pf, p)).append(".\n");
        sb.append("2) El trabajo es el área bajo la curva F–d:  W = ∫ F(d) dd,  de d₁ = ")
          .append(f4(d1)).append(" m a d₂ = ").append(f4(d2)).append(" m.\n");
        sb.append("3) Regla de Simpson 1/3 con n = ").append(N_SIMPSON).append(" subintervalos (h = ")
          .append(f6((d2 - d1) / N_SIMPSON)).append(" m):  W = ").append(f4(w)).append(" J.\n");
        sb.append("4) Verificación con la regla del trapecio: ").append(f4(wTrap))
          .append(" J  (diferencia ").append(sci(Math.abs(w - wTrap))).append(" J).\n");
        if (Double.isFinite(exacto)) {
            sb.append("5) Valor analítico de referencia: ").append(f4(exacto))
              .append(" J  (error de Simpson ").append(sci(Math.abs(w - exacto))).append(" J).\n");
        }
        sb.append("6) Unidades: 1 N·m = 1 J.  ").append(interp);

        lblResultado.setText("W = " + f4(w) + " J");
        lblSecundario.setText("Trabajo mecánico total realizado por la fuerza entre d₁ y d₂ (1 J = 1 N·m)");
        txtPasos.setText(sb.toString());

        double xMin = Math.min(0, d1);
        double xMax = d2 + 0.1 * (d2 - xMin);
        grafico.actualizar("Fuerza en función de la distancia", "Distancia d [m]", "Fuerza F [N]",
                F, xMin, xMax, GraficoCanvas.Resalte.AREA, d1, d2);
    }

    // ------------------------------------------------------------------------
    //  Utilidades
    // ------------------------------------------------------------------------
    /** P(b) − P(a) con la primitiva analítica; NaN si no existe o no es finita (p. ej. ω = 0). */
    private static double valorAnalitico(Perfil pf, double[] p, double a, double b) {
        if (pf.primitiva() == null) return Double.NaN;
        DoubleUnaryOperator P = pf.primitiva().apply(p);
        return P.applyAsDouble(b) - P.applyAsDouble(a);
    }

    private static void verificarFinito(double... valores) {
        for (double v : valores) {
            if (!Double.isFinite(v)) {
                throw new IllegalArgumentException(
                    "El resultado no es un número finito. Revisa los parámetros (¿división por cero?).");
            }
        }
    }

    private static String describir(Perfil pf, double[] p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(pf.etiquetas()[i]).append(" = ").append(num(p[i])).append(" ").append(pf.unidades()[i]);
        }
        return sb.toString();
    }

    private static String num(double v) { return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString(); }
    private static String f4(double v) { return String.format(Locale.US, "%.4f", v); }
    private static String f6(double v) { return String.format(Locale.US, "%.6f", v); }
    private static String sci(double v) { return String.format(Locale.US, "%.2e", v); }

    public static void main(String[] args) {
        launch(args);
    }
}
