/*
 * ============================================================================
 *  ProblemasRazonesPanel.java
 *  Pestaña "Problemas de razones relacionadas" (Java 17+ / JavaFX 21).
 * ============================================================================
 *  QUÉ HACE
 *    1) CONTROL TOTAL DE VARIABLES: el usuario edita los datos del problema (en t₀) y los
 *       tres instantes  t₀ (inicial), t₁ (evaluación), t₂ (final). Cada cambio recalcula
 *       la resolución, el esquema geométrico y la gráfica al instante.
 *    2) GRAFICACIÓN CONTEXTUAL: LienzoGeometria dibuja el triángulo (Pitágoras) o el
 *       cono / tanque con sus longitudes y las flechas de las razones de cambio, y
 *       GraficoTangente dibuja f(t) con su recta tangente en t₁.
 *       La casilla "Mostrar representación geométrica / gráfica" oculta ambos paneles
 *       para dejar todo el espacio al procedimiento.
 *    3) PASO A PASO FORMAL (formato estricto):
 *          1. Fórmula original   2. Derivación respecto al tiempo
 *          3. Sustitución de valores   4. Respuesta en notación de Leibniz (ds/dt = ...)
 *       Debajo se conserva la redacción con valores concretos (sin variables abstractas).
 *    4) VERIFICACIÓN NUMÉRICA: la curva de la magnitud se deriva con la diferencia central
 *       (RazonesCambioPanel.MotorDerivacion) y se compara con el resultado analítico.
 *
 *  INTEGRACIÓN (conector)
 *    ProblemasRazonesPanel.integrarEn(tabPane);      // añade la pestaña a un TabPane
 *    Tab t = ProblemasRazonesPanel.comoPestana();    // o recibe la pestaña lista
 *    BorderPane p = new ProblemasRazonesPanel();     // o usa el panel directamente
 *
 *  DEPENDENCIAS (mismo paquete): BancoProblemas, LienzoGeometria y RazonesCambioPanel
 * ============================================================================
 */

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProblemasRazonesPanel extends BorderPane {

    // --- Entrada de datos ---
    private final ComboBox<BancoProblemas.Problema> cmbProblema = new ComboBox<>();
    private final Label lblEnunciado = new Label();
    private final VBox cajaCampos = new VBox(6);
    private final List<TextField> camposParam = new ArrayList<>();
    private TextField txtT0, txtT1, txtT2, txtH;
    private String hTexto = "0.00001";

    // --- Visualización ---
    private final CheckBox chkGeo = new CheckBox("Mostrar representación geométrica / gráfica");
    private final LienzoGeometria lienzo = new LienzoGeometria();
    private final RazonesCambioPanel.GraficoTangente grafico = new RazonesCambioPanel.GraficoTangente();
    private final HBox areaVisual = new HBox(10, lienzo, grafico);

    // --- Resolución ---
    private final VBox cajaProcedimiento = new VBox(10);
    private final Label lblVerificacion = new Label();
    private final Label lblInterpretacion = new Label();
    private final TextArea txtPasos = new TextArea();
    private String textoPlano = "";

    private boolean construyendo = false;

    public ProblemasRazonesPanel() {
        Label titulo = new Label("Problemas de razones relacionadas (con valores concretos)");
        titulo.setWrapText(true);
        titulo.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        cmbProblema.getItems().addAll(BancoProblemas.TODOS);
        cmbProblema.setMaxWidth(Double.MAX_VALUE);
        cmbProblema.setVisibleRowCount(14);

        lblEnunciado.setWrapText(true);
        lblEnunciado.setStyle("-fx-text-fill: #1d4ed8; -fx-font-size: 13px; -fx-background-color: #eff6ff; "
                            + "-fx-padding: 8; -fx-background-radius: 6;");

        Button btnCalcular = new Button("Calcular");
        btnCalcular.setMaxWidth(Double.MAX_VALUE);
        btnCalcular.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        btnCalcular.setOnAction(e -> calcular());

        Button btnCopiar = new Button("Copiar enunciado y procedimiento");
        btnCopiar.setMaxWidth(Double.MAX_VALUE);
        btnCopiar.setOnAction(e -> copiar());

        VBox izquierda = new VBox(10, titulo, new Separator(), etiqueta("Problema"), cmbProblema, lblEnunciado,
                new Separator(), cajaCampos, btnCalcular, btnCopiar);
        izquierda.setPadding(new Insets(16));
        izquierda.setStyle("-fx-background-color: white;");
        ScrollPane scroll = new ScrollPane(izquierda);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(400);

        // --- Panel visual (geometría + gráfica) con interruptor ---
        chkGeo.setSelected(true);
        chkGeo.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");
        lienzo.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;");
        grafico.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;");
        HBox.setHgrow(lienzo, Priority.ALWAYS);
        HBox.setHgrow(grafico, Priority.ALWAYS);
        areaVisual.setPrefHeight(390);
        areaVisual.setMinHeight(280);
        // Si se desmarca, el panel se oculta Y deja de ocupar espacio: el procedimiento usa toda la pantalla
        chkGeo.selectedProperty().addListener((o, a, visible) -> {
            areaVisual.setVisible(visible);
            areaVisual.setManaged(visible);
        });

        // --- Procedimiento ---
        lblVerificacion.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        lblVerificacion.setWrapText(true);
        lblInterpretacion.setWrapText(true);
        VBox cajaInterp = new VBox(6, etiqueta("Interpretación física"), lblInterpretacion);
        cajaInterp.setPadding(new Insets(10));
        cajaInterp.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; "
                          + "-fx-background-radius: 6; -fx-border-radius: 6;");
        txtPasos.setEditable(false);
        txtPasos.setWrapText(true);
        txtPasos.setPrefRowCount(10);
        TitledPane redaccion = new TitledPane("Redacción con valores concretos (paso a paso)", txtPasos);
        redaccion.setExpanded(true);
        redaccion.setAnimated(false);

        VBox resultados = new VBox(12, cajaProcedimiento, lblVerificacion, cajaInterp, redaccion);
        resultados.setPadding(new Insets(12));
        resultados.setStyle("-fx-background-color: white;");
        ScrollPane scrollRes = new ScrollPane(resultados);
        scrollRes.setFitToWidth(true);
        scrollRes.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;");

        VBox centro = new VBox(8, chkGeo, areaVisual, scrollRes);
        centro.setPadding(new Insets(12));
        VBox.setVgrow(scrollRes, Priority.ALWAYS);

        setCenter(centro);
        setLeft(scroll);
        setStyle("-fx-background-color: #f3f4f6;");

        cmbProblema.valueProperty().addListener((o, a, b) -> { if (!construyendo && b != null) reconstruir(); });
        cmbProblema.getSelectionModel().selectFirst();   // dispara la primera construcción del formulario
    }

    // ------------------------------------------------------------------------
    //  Conectores de integración
    // ------------------------------------------------------------------------
    /** Devuelve una pestaña lista para insertar en un TabPane. */
    public static Tab comoPestana() {
        Tab tab = new Tab("Problemas de razones relacionadas", new ProblemasRazonesPanel());
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
        BancoProblemas.Problema pr = cmbProblema.getValue();
        if (pr == null) return;

        lblEnunciado.setText(pr.enunciado());
        cajaCampos.getChildren().add(etiqueta("Datos del problema (valen en el instante inicial t₀)"));
        for (int i = 0; i < pr.etiquetas().length; i++) {
            camposParam.add(crearCampo(pr.etiquetas()[i], pr.unidades()[i], BancoProblemas.campo(pr.iniciales()[i])));
        }

        // Instantes de tiempo: t₀ (inicial), t₁ (evaluación), t₂ (final). Por defecto t₁ reproduce el enunciado.
        cajaCampos.getChildren().add(new Separator());
        cajaCampos.getChildren().add(etiqueta("Instantes de tiempo"));
        String uT = pr.unidadTiempo();
        txtT0 = crearCampo("t₀ — instante inicial (aquí valen los datos)", uT, BancoProblemas.campo(pr.tiempos()[0]));
        txtT1 = crearCampo("t₁ — instante de evaluación (se calcula la razón de cambio)", uT, BancoProblemas.campo(pr.tiempos()[1]));
        txtT2 = crearCampo("t₂ — instante final (límite de la gráfica)", uT, BancoProblemas.campo(pr.tiempos()[2]));

        cajaCampos.getChildren().add(new Separator());
        cajaCampos.getChildren().add(etiqueta("Verificación numérica"));
        txtH = crearCampo("Delta de aproximación h (fracción del intervalo t₂ − t₀)", "", hTexto);
        txtH.textProperty().addListener((o, a, b) -> hTexto = b);
    }

    private TextField crearCampo(String etiqueta, String unidad, String valorInicial) {
        Label l = new Label(unidad == null || unidad.isEmpty() ? etiqueta : etiqueta + "  [" + unidad + "]");
        l.setStyle("-fx-text-fill: #374151;");
        l.setWrapText(true);
        TextField tf = new TextField(valorInicial);
        // Modificación en tiempo real: cualquier cambio de un campo recalcula todo
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
        BancoProblemas.Problema pr = cmbProblema.getValue();
        if (pr == null || txtH == null || txtT2 == null) return;
        try {
            double[] datos = new double[camposParam.size()];
            for (int i = 0; i < datos.length; i++) datos[i] = leer(camposParam.get(i), pr.etiquetas()[i]);
            double t0 = leer(txtT0, "t₀"), t1 = leer(txtT1, "t₁"), t2 = leer(txtT2, "t₂");
            double hRel = leer(txtH, "Delta de aproximación (h)");
            if (hRel <= 0 || hRel >= 0.1) throw new IllegalArgumentException("El delta de aproximación (h) debe estar entre 0 y 0.1 (por ejemplo 0.00001).");

            // Resolución analítica (regla de la cadena) en t₁ + curva de la magnitud
            BancoProblemas.Resultado r = BancoProblemas.resolver(pr, datos, t0, t1, t2);

            // Aproximación NUMÉRICA: diferencia central sobre la curva; se divide entre la escala del reloj
            // para expresar el resultado en las mismas unidades que el analítico.
            double h = hRel * (t2 - t0);
            double numerico = RazonesCambioPanel.MotorDerivacion.derivadaCentral(r.curva(), t1, h) / r.escala();
            boolean numOk = Double.isFinite(numerico);
            double error = Math.abs(numerico - r.valor());

            mostrarProcedimiento(r);
            lblVerificacion.setText(numOk
                ? "Comprobación numérica (diferencia central, h = " + sci(h) + " " + r.unidadTiempo() + "): "
                  + BancoProblemas.g(numerico) + " " + r.unidad() + "   |   error = " + sci(error)
                : "Comprobación numérica no disponible con estos datos.");
            lblInterpretacion.setText(r.interpretacion());

            StringBuilder sb = new StringBuilder();
            int n = 1;
            for (String paso : r.pasos()) sb.append(n++).append(") ").append(paso).append("\n");
            if (numOk) {
                sb.append(n).append(") Comprobación numérica: la magnitud «").append(r.tituloGrafico().toLowerCase(Locale.ROOT))
                  .append("» se deriva con la diferencia central [f(t₁+h) − f(t₁−h)] / (2h) en t₁ = ")
                  .append(BancoProblemas.g(t1)).append(" ").append(r.unidadTiempo()).append(" con h = ").append(sci(h))
                  .append(": ").append(BancoProblemas.g(numerico)).append(" frente a ").append(BancoProblemas.g(r.valor()))
                  .append(" analítico (error ").append(sci(error)).append(").");
            }
            txtPasos.setText(sb.toString());

            // Panel visual: esquema geométrico + gráfica f(t) con la recta tangente en t₁
            lienzo.actualizar(r.geo());
            grafico.actualizar(r.tituloGrafico() + " en función del tiempo", "Tiempo t [" + r.unidadTiempo() + "]", r.ejeY(),
                    r.curva(), t0, t2, true, t1, r.valor() * r.escala(), false, 0);

            textoPlano = pr.nombre() + "\n" + pr.enunciado() + "\n\n" + textoPlano + "\n" + txtPasos.getText();
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /** Arma las 4 partes del procedimiento formal y la respuesta en notación de Leibniz. */
    private void mostrarProcedimiento(BancoProblemas.Resultado r) {
        BancoProblemas.Formal f = r.formal();
        String ut = " " + r.unidadTiempo();
        String instantes = "t₀ = " + BancoProblemas.g(r.t0()) + ut + ",  t₁ = " + BancoProblemas.g(r.t1()) + ut + ",  t₂ = " + BancoProblemas.g(r.t2()) + ut;
        String valorTxt = BancoProblemas.g(r.valor()) + " " + r.unidad();

        Label lblEstado = new Label("Estado en t₁ (" + instantes + "):\n" + r.estado());
        lblEstado.setWrapText(true);
        lblEstado.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        cajaProcedimiento.getChildren().setAll(
            lblEstado,
            seccion("1. Fórmula original del problema", f.formula(), f.leyenda()),
            seccion("2. Derivación implícita respecto al tiempo", f.derivada(), null),
            seccion("3. Sustitución de valores en t₁ = " + BancoProblemas.g(r.t1()) + ut, f.sustitucion(), null),
            respuestaLeibniz(r.resultado(), f.simbolo(), valorTxt, f.nota()));

        StringBuilder sb = new StringBuilder();
        sb.append("Instantes: ").append(instantes).append("\nEstado en t₁: ").append(r.estado()).append("\n\n")
          .append("1. Fórmula original: ").append(f.formula()).append("   (").append(f.leyenda()).append(")\n")
          .append("2. Derivación: ").append(f.derivada()).append("\n")
          .append("3. Sustitución: ").append(f.sustitucion()).append("\n")
          .append("4. Respuesta: ").append(f.simbolo()).append(" = ").append(valorTxt);
        if (f.nota() != null && !f.nota().isEmpty()) sb.append("   ").append(f.nota());
        sb.append("\n");
        textoPlano = sb.toString();
    }

    private VBox seccion(String titulo, String cuerpo, String leyenda) {
        Label t = etiqueta(titulo);
        Label c = new Label(cuerpo);
        c.setWrapText(true);
        c.setStyle("-fx-font-family: 'Consolas','Menlo','Monospaced'; -fx-font-size: 14px; -fx-text-fill: #1d4ed8;");
        VBox caja = new VBox(3, t, c);
        if (leyenda != null && !leyenda.isEmpty()) {
            Label l = new Label(leyenda);
            l.setWrapText(true);
            l.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
            caja.getChildren().add(l);
        }
        return caja;
    }

    /** Parte 4: resultado en notación de Leibniz, con la fracción dibujada (d… sobre dt). */
    private VBox respuestaLeibniz(String nombre, String simbolo, String valorTxt, String nota) {
        String[] partes = simbolo.split("/", 2);
        Label num = new Label(partes[0]), den = new Label(partes.length > 1 ? partes[1] : "dt");
        String estiloFraccion = "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #111827;";
        num.setStyle(estiloFraccion);
        den.setStyle(estiloFraccion);
        VBox fraccion = new VBox(2, num, new Separator(), den);
        fraccion.setAlignment(Pos.CENTER);
        fraccion.setMaxWidth(Region.USE_PREF_SIZE);

        Label igual = new Label("=  " + valorTxt);
        igual.setWrapText(true);
        igual.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        HBox fila = new HBox(12, fraccion, igual);
        fila.setAlignment(Pos.CENTER_LEFT);

        VBox caja = new VBox(6, etiqueta("4. Respuesta formal en notación de Leibniz"), new Label(nombre), fila);
        if (nota != null && !nota.isEmpty()) {
            Label n = new Label(nota);
            n.setWrapText(true);
            n.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
            caja.getChildren().add(n);
        }
        caja.setPadding(new Insets(10));
        caja.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; -fx-background-radius: 6; -fx-border-radius: 6;");
        return caja;
    }

    private void mostrarError(String mensaje) {
        Label e = new Label("⚠ " + mensaje);
        e.setWrapText(true);
        e.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;");
        cajaProcedimiento.getChildren().setAll(e);
        lblVerificacion.setText("");
        lblInterpretacion.setText("");
        txtPasos.setText("");
        textoPlano = "";
        lienzo.actualizar(null);
        grafico.limpiar();
    }

    private void copiar() {
        if (textoPlano.isEmpty()) return;
        ClipboardContent contenido = new ClipboardContent();
        contenido.putString(textoPlano);
        Clipboard.getSystemClipboard().setContent(contenido);
    }

    private static String sci(double v) { return String.format(Locale.US, "%.2e", v); }
}
