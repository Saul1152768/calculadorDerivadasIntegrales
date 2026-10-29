/*
 * ============================================================================
 *  ProblemasRazonesPanel.java
 *  Pestaña "Problemas de razones relacionadas" (Java 17+ / JavaFX 21).
 * ============================================================================
 *  QUÉ HACE
 *    - Muestra el banco de problemas de BancoProblemas.java (ejemplos, problemas
 *      propuestos y una plantilla general).
 *    - El usuario puede CAMBIAR los datos numéricos de cada problema.
 *    - Resuelve con la regla de la cadena y redacta el procedimiento paso a paso
 *      con valores concretos (sin variables abstractas x, y, s, z).
 *    - VERIFICA el resultado de forma numérica: deriva la curva de la magnitud
 *      con la diferencia central (la misma de RazonesCambioPanel.MotorDerivacion)
 *      y muestra el error frente al resultado analítico.
 *    - Dibuja la magnitud en el tiempo con su recta tangente (pendiente = respuesta).
 *
 *  INTEGRACIÓN (conector)
 *    ProblemasRazonesPanel.integrarEn(tabPane);      // añade la pestaña a un TabPane
 *    Tab t = ProblemasRazonesPanel.comoPestana();    // o recibe la pestaña lista
 *    BorderPane p = new ProblemasRazonesPanel();     // o usa el panel directamente
 *
 *  DEPENDENCIAS (mismo paquete): BancoProblemas.java y RazonesCambioPanel.java
 * ============================================================================
 */

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProblemasRazonesPanel extends BorderPane {

    private final ComboBox<BancoProblemas.Problema> cmbProblema = new ComboBox<>();
    private final Label lblEnunciado = new Label();
    private final VBox cajaCampos = new VBox(6);
    private final List<TextField> camposParam = new ArrayList<>();
    private TextField txtH;
    private String hTexto = "0.00001";

    private final Label lblResultado = new Label("—");
    private final Label lblVerificacion = new Label();
    private final Label lblInterpretacion = new Label();
    private final TextArea txtPasos = new TextArea();
    private final RazonesCambioPanel.GraficoTangente grafico = new RazonesCambioPanel.GraficoTangente();

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
        scroll.setPrefWidth(390);

        lblResultado.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        lblResultado.setWrapText(true);
        lblVerificacion.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        lblVerificacion.setWrapText(true);
        lblInterpretacion.setWrapText(true);
        VBox cajaInterp = new VBox(6, etiqueta("Interpretación física"), lblInterpretacion);
        cajaInterp.setPadding(new Insets(10));
        cajaInterp.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; "
                          + "-fx-background-radius: 6; -fx-border-radius: 6;");
        txtPasos.setEditable(false);
        txtPasos.setWrapText(true);
        txtPasos.setPrefRowCount(11);
        VBox resultados = new VBox(8, lblResultado, lblVerificacion, cajaInterp, etiqueta("Procedimiento paso a paso"), txtPasos);
        resultados.setPadding(new Insets(12));
        resultados.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;");

        grafico.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db;");
        VBox centro = new VBox(10, grafico, resultados);
        centro.setPadding(new Insets(12));
        VBox.setVgrow(grafico, Priority.ALWAYS);

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
        cajaCampos.getChildren().add(etiqueta("Datos del problema (puedes cambiarlos)"));
        for (int i = 0; i < pr.etiquetas().length; i++) {
            camposParam.add(crearCampo(pr.etiquetas()[i], pr.unidades()[i], BancoProblemas.g(pr.iniciales()[i])));
        }

        cajaCampos.getChildren().add(new Separator());
        cajaCampos.getChildren().add(etiqueta("Verificación numérica"));
        txtH = crearCampo("Delta de aproximación h (fracción del intervalo graficado)", "", hTexto);
        txtH.textProperty().addListener((o, a, b) -> hTexto = b);
    }

    private TextField crearCampo(String etiqueta, String unidad, String valorInicial) {
        Label l = new Label(unidad == null || unidad.isEmpty() ? etiqueta : etiqueta + "  [" + unidad + "]");
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
        BancoProblemas.Problema pr = cmbProblema.getValue();
        if (pr == null || txtH == null) return;
        try {
            double[] datos = new double[camposParam.size()];
            for (int i = 0; i < datos.length; i++) datos[i] = leer(camposParam.get(i), pr.etiquetas()[i]);
            double hRel = leer(txtH, "Delta de aproximación (h)");
            if (hRel <= 0 || hRel >= 0.1) throw new IllegalArgumentException("El delta de aproximación (h) debe estar entre 0 y 0.1 (por ejemplo 0.00001).");

            BancoProblemas.Solucion sol = pr.resolver().apply(datos);

            // Verificación numérica: diferencia central sobre la curva de la magnitud (método de RazonesCambioPanel)
            double h = hRel * (sol.tMax() - sol.tMin());
            double numerico = RazonesCambioPanel.MotorDerivacion.derivadaCentral(sol.curva(), sol.t0(), h);
            if (!Double.isFinite(numerico)) throw new IllegalArgumentException("La verificación numérica no es finita con estos datos.");
            double error = Math.abs(numerico - sol.valor());

            double mostrado = sol.magnitud() ? Math.abs(sol.valor()) : sol.valor();
            lblResultado.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
            lblResultado.setText(sol.resultado() + " = " + BancoProblemas.g(mostrado) + " " + sol.unidad());
            lblVerificacion.setText("Comprobación numérica (diferencia central, h = " + sci(h) + " " + sol.unidadTiempo() + "): "
                    + BancoProblemas.g(sol.magnitud() ? Math.abs(numerico) : numerico) + " " + sol.unidad() + "   |   error = " + sci(error));
            lblInterpretacion.setText(sol.interpretacion());

            StringBuilder sb = new StringBuilder();
            int n = 1;
            for (String paso : sol.pasos()) sb.append(n++).append(") ").append(paso).append("\n");
            sb.append(n).append(") Comprobación numérica: la magnitud «").append(sol.tituloGrafico().toLowerCase(Locale.ROOT))
              .append("» se modela como función del tiempo y se deriva con la diferencia central [f(t₀+h) − f(t₀−h)] / (2h) en t₀ = ")
              .append(BancoProblemas.g(sol.t0())).append(" ").append(sol.unidadTiempo()).append(" con h = ").append(sci(h)).append(": resultado ")
              .append(BancoProblemas.g(numerico)).append(" frente a ").append(BancoProblemas.g(sol.valor())).append(" analítico (error ")
              .append(sci(error)).append(").");
            txtPasos.setText(sb.toString());

            grafico.actualizar(sol.tituloGrafico() + " en función del tiempo", "Tiempo t [" + sol.unidadTiempo() + "]", sol.ejeY(),
                    sol.curva(), sol.tMin(), sol.tMax(), true, sol.t0(), sol.valor(), false, 0);
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        lblResultado.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;");
        lblResultado.setText("⚠ " + mensaje);
        lblVerificacion.setText("");
        lblInterpretacion.setText("");
        txtPasos.setText("");
        grafico.limpiar();
    }

    private void copiar() {
        BancoProblemas.Problema pr = cmbProblema.getValue();
        if (pr == null) return;
        ClipboardContent contenido = new ClipboardContent();
        contenido.putString(pr.nombre() + "\n" + pr.enunciado() + "\n\n" + lblResultado.getText() + "\n\n" + txtPasos.getText());
        Clipboard.getSystemClipboard().setContent(contenido);
    }

    private static String sci(double v) { return String.format(Locale.US, "%.2e", v); }
}
