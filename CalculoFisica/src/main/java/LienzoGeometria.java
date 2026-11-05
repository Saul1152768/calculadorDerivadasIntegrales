import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class LienzoGeometria extends Pane {

    private static final Color AZUL = Color.web("#1d4ed8");
    private static final Color AZUL_CLARO = Color.web("#dbeafe");
    private static final Color AGUA = Color.web("#93c5fd");
    private static final Color ARENA = Color.web("#fde68a");
    private static final Color ROJO = Color.web("#dc2626");
    private static final Color GRIS = Color.web("#374151");

    private final Canvas canvas = new Canvas();
    private BancoProblemas.Geometria geo;

    LienzoGeometria() {
        getChildren().add(canvas);
        setMinSize(300, 270);
        setPrefHeight(380);
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        dibujar();
    }

    void actualizar(BancoProblemas.Geometria g) {
        this.geo = g;
        dibujar();
    }

    // ------------------------------------------------------------------------
    //  Dibujo principal
    // ------------------------------------------------------------------------
    private void dibujar() {
        double w = canvas.getWidth(), h = canvas.getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, w, h);
        if (w < 220 || h < 180) return;

        if (geo == null || geo.tipo() == BancoProblemas.TipoGeo.NINGUNA) {
            texto(g, "Este problema no tiene esquema geométrico.", w / 2, h / 2 - 8, TextAlignment.CENTER, GRIS, false, 14);
            texto(g, "Observa la gráfica de la magnitud en función del tiempo.", w / 2, h / 2 + 14, TextAlignment.CENTER, GRIS, false, 12);
            return;
        }
        texto(g, geo.titulo(), 12, 22, TextAlignment.LEFT, GRIS, true, 14);
        if (geo.tipo() == BancoProblemas.TipoGeo.TRIANGULO) dibujarTriangulo(g, w, h);
        else dibujarCono(g, w, h);
    }

    private void dibujarTriangulo(GraphicsContext g, double w, double h) {
        double a = Math.abs(geo.a()), b = Math.abs(geo.b());
        if (a == 0 && b == 0) {
            texto(g, "Los dos catetos valen 0 en este instante.", w / 2, h / 2, TextAlignment.CENTER, GRIS, false, 13);
            return;
        }
        double c = Math.hypot(a, b);
        // Razón de la hipotenusa por derivación de c² = a² + b²  ⇒  dc/dt = (a·da/dt + b·db/dt) / c
        double dc = (geo.a() * geo.da() + geo.b() * geo.db()) / c;

        // Escala: se respeta la proporción real, pero ningún cateto se dibuja menor al 12 % del mayor
        double mayor = Math.max(a, b);
        double la = Math.max(a, 0.12 * mayor), lb = Math.max(b, 0.12 * mayor);
        double margenIzq = 100, margenDer = 60, margenSup = 50, margenInf = 120;   // abajo queda la leyenda
        double aw = w - margenIzq - margenDer, ah = h - margenSup - margenInf;
        double s = Math.min(aw / la, ah / lb);
        double pa = la * s, pb = lb * s;

        double xr = margenIzq + (aw - pa) / 2;                 // vértice del ángulo recto
        double yr = margenSup + (ah + pb) / 2;
        double xp = xr + pa, yq = yr - pb;                     // extremo del cateto horizontal / vertical

        // Triángulo
        g.setFill(AZUL_CLARO);
        g.fillPolygon(new double[]{xr, xp, xr}, new double[]{yr, yr, yq}, 3);
        g.setStroke(AZUL);
        g.setLineWidth(2.5);
        g.strokePolygon(new double[]{xr, xp, xr}, new double[]{yr, yr, yq}, 3);
        g.setLineWidth(1.2);
        g.strokeRect(xr, yr - 12, 12, 12);                     // marca del ángulo recto

        // Longitudes sobre los lados
        texto(g, BancoProblemas.g(a) + " " + geo.unidad(), (xr + xp) / 2, yr + 34, TextAlignment.CENTER, GRIS, true, 12);
        texto(g, BancoProblemas.g(b) + " " + geo.unidad(), xr - 22, (yr + yq) / 2 + 4, TextAlignment.RIGHT, GRIS, true, 12);
        texto(g, BancoProblemas.g(c) + " " + geo.unidad(), (xr + xp) / 2 + 12, (yr + yq) / 2 - 10, TextAlignment.LEFT, AZUL, true, 12);

        // Vectores de la razón de cambio (rojo): hacia donde crece o decrece cada cateto
        if (Math.abs(geo.da()) > 1e-12) {
            double dir = Math.signum(geo.da());
            flecha(g, xp - dir * 10, yr + 14, xp + dir * 38, yr + 14, ROJO);
        }
        if (Math.abs(geo.db()) > 1e-12) {
            double dir = Math.signum(geo.db());                // positivo = hacia arriba en pantalla
            flecha(g, xr - 14, yq + dir * 10, xr - 14, yq - dir * 38, ROJO);
        }

        // Leyenda inferior: nombre, valor y razón de cambio de cada lado
        double ly = h - 78;
        leyenda(g, geo.nomA(), a, geo.da(), ly);
        leyenda(g, geo.nomB(), b, geo.db(), ly + 20);
        leyenda(g, geo.nomC(), c, dc, ly + 40);
    }

    private void leyenda(GraphicsContext g, String nombre, double valor, double tasa, double y) {
        String t = nombre + " = " + BancoProblemas.g(valor) + " " + geo.unidad() + "   |   "
                 + (Math.abs(tasa) < 1e-12 ? "constante"
                    : (tasa > 0 ? "aumenta " : "disminuye ") + BancoProblemas.g(Math.abs(tasa)) + " " + geo.unidadTasa());
        texto(g, t, 12, y, TextAlignment.LEFT, GRIS, false, 12);
    }

    // ------------------------------------------------------------------------
    //  Cono (vértice abajo), cono truncado o montón de arena (vértice arriba)
    // ------------------------------------------------------------------------
    private void dibujarCono(GraphicsContext g, double w, double h) {
        double alto = geo.alto(), rt = geo.radioTope(), rb = geo.radioFondo(), nivel = geo.nivel();
        if (!(alto > 0) || !(Math.max(rt, rb) > 0)) return;
        double rMax = Math.max(rt, rb);
        double aw = w - 170, ah = h - 150;
        double s = Math.min(aw / (2 * rMax), ah / alto);
        double cx = w / 2, yTop = 44 + (ah - alto * s) / 2, yBot = yTop + alto * s;
        double lim = Math.max(0, Math.min(nivel, alto));
        double radioNivel, dRadio;

        if (geo.pila()) {
            // Montón de arena: vértice arriba, la arena ocupa todo el cono
            g.setFill(ARENA);
            g.fillPolygon(new double[]{cx, cx + rt * s, cx - rt * s}, new double[]{yTop, yBot, yBot}, 3);
            g.setStroke(Color.web("#b45309"));
            g.setLineWidth(2.5);
            g.strokePolygon(new double[]{cx, cx + rt * s, cx - rt * s}, new double[]{yTop, yBot, yBot}, 3);
            g.setLineWidth(1);
            g.strokeOval(cx - rt * s, yBot - 7, 2 * rt * s, 14);
            radioNivel = rt;
            dRadio = nivel > 0 ? geo.dNivel() * rt / nivel : 0;
            segmentoRadio(g, cx, yBot, cx + rt * s, yBot);
            segmentoAltura(g, cx, yBot, yTop);
            texto(g, "r", cx + rt * s / 2, yBot + 20, TextAlignment.CENTER, ROJO, true, 13);
            texto(g, "h", cx - 10, (yTop + yBot) / 2, TextAlignment.RIGHT, ROJO, true, 13);
            double dirPila = Math.signum(geo.dNivel()), xfp = cx + rt * s * 0.45 + 40, ym = (yTop + yBot) / 2;
            if (dirPila != 0) flecha(g, xfp, ym + dirPila * 18, xfp, ym - dirPila * 18, ROJO);   // la altura sube o baja
        } else {
            // Recipiente (vértice abajo o truncado) con agua hasta el nivel
            double[] xs = {cx - rt * s, cx + rt * s, cx + rb * s, cx - rb * s};
            double[] ys = {yTop, yTop, yBot, yBot};
            g.setFill(Color.web("#f9fafb"));
            g.fillPolygon(xs, ys, 4);
            radioNivel = rb + (rt - rb) * lim / alto;
            double yN = yBot - lim * s;
            g.setFill(AGUA);
            g.fillPolygon(new double[]{cx - radioNivel * s, cx + radioNivel * s, cx + rb * s, cx - rb * s},
                          new double[]{yN, yN, yBot, yBot}, 4);
            g.setStroke(Color.web("#2563eb"));
            g.setLineWidth(1.5);
            g.strokeOval(cx - radioNivel * s, yN - 6, 2 * radioNivel * s, 12);        // superficie del agua
            g.setStroke(GRIS);
            g.setLineWidth(2.5);
            g.strokePolygon(xs, ys, 4);                                               // contorno del recipiente
            g.setLineWidth(1);
            dRadio = geo.dNivel() * (rt - rb) / alto;                                 // dr/dt = (dr/dh)·(dh/dt)
            segmentoRadio(g, cx, yN, cx + radioNivel * s, yN);
            segmentoAltura(g, cx, yBot, yN);
            texto(g, "r", cx + radioNivel * s / 2, yN - 10, TextAlignment.CENTER, ROJO, true, 13);
            texto(g, "h", cx - 8, (yN + yBot) / 2, TextAlignment.RIGHT, ROJO, true, 13);
            double xf = cx + rMax * s + 28;                                           // flecha: el nivel sube o baja
            double dir = Math.signum(geo.dNivel());
            if (dir != 0) flecha(g, xf, yN + dir * 20, xf, yN - dir * 20, ROJO);
        }

        // Leyenda inferior
        double ly = h - 54;
        String hNombre = geo.pila() ? "Altura del cono (h)" : "Profundidad del agua (h)";
        String rNombre = geo.pila() ? "Radio de la base (r)" : "Radio de la superficie (r)";
        texto(g, hNombre + " = " + BancoProblemas.g(nivel) + " " + geo.unidad() + "   |   dh/dt = "
                + BancoProblemas.g(geo.dNivel()) + " " + geo.unidadTasa(), 12, ly, TextAlignment.LEFT, GRIS, false, 12);
        texto(g, rNombre + " = " + BancoProblemas.g(radioNivel) + " " + geo.unidad() + "   |   dr/dt = "
                + BancoProblemas.g(dRadio) + " " + geo.unidadTasa(), 12, ly + 20, TextAlignment.LEFT, GRIS, false, 12);
    }

    private void segmentoRadio(GraphicsContext g, double x1, double y1, double x2, double y2) {
        g.setStroke(ROJO);
        g.setLineWidth(2);
        g.setLineDashes(6, 4);
        g.strokeLine(x1, y1, x2, y2);
        g.setLineDashes((double[]) null);
        g.setLineWidth(1);
    }

    private void segmentoAltura(GraphicsContext g, double x, double yAbajo, double yArriba) {
        g.setStroke(ROJO);
        g.setLineWidth(2);
        g.setLineDashes(6, 4);
        g.strokeLine(x, yAbajo, x, yArriba);
        g.setLineDashes((double[]) null);
        g.setLineWidth(1);
    }

    // ------------------------------------------------------------------------
    //  Utilidades de dibujo
    // ------------------------------------------------------------------------
    private void texto(GraphicsContext g, String s, double x, double y, TextAlignment al, Color c, boolean negrita, double tam) {
        g.setFill(c);
        g.setFont(Font.font("System", negrita ? FontWeight.BOLD : FontWeight.NORMAL, tam));
        g.setTextAlign(al);
        g.fillText(s, x, y);
    }

    /** Flecha (vector) de (x1,y1) a (x2,y2) con punta triangular. */
    private void flecha(GraphicsContext g, double x1, double y1, double x2, double y2, Color c) {
        g.setStroke(c);
        g.setFill(c);
        g.setLineWidth(2.5);
        g.strokeLine(x1, y1, x2, y2);
        double ang = Math.atan2(y2 - y1, x2 - x1), punta = 10;
        double[] px = {x2, x2 - punta * Math.cos(ang - 0.45), x2 - punta * Math.cos(ang + 0.45)};
        double[] py = {y2, y2 - punta * Math.sin(ang - 0.45), y2 - punta * Math.sin(ang + 0.45)};
        g.fillPolygon(px, py, 3);
        g.setLineWidth(1);
    }
}
