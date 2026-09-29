import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * Poke-Gacha: Menu, PC, Pokedex e Gacha de Pokemon.
 * Compilar: javac PokemonApp.java
 * Rodar:    java PokemonApp
 */
public class PokemonApp extends JFrame {

    // ======================= DADOS =======================
    static class Pokemon {
        final int id; final String name, t1, t2; final int rarity;
        Pokemon(int id, String name, String t1, String t2, int rarity) {
            this.id = id; this.name = name; this.t1 = t1; this.t2 = t2; this.rarity = rarity;
        }
    }

    static final String[] RARITY = {"Comum", "Raro", "Épico", "Lendário"};
    static final Color[] RARITY_COLOR = {
            new Color(0x8A8A8A), new Color(0x2E86DE), new Color(0x8E44AD), new Color(0xE1A800)};
    static final int[] WEIGHT = {55, 30, 12, 3};       // chance de cada raridade (%)
    static final int[] RELEASE_VALUE = {10, 30, 100, 500};
    static final int ROLL_COST = 100;

    static final Color RED = new Color(0xDC0A2D);
    static final Color DARK_RED = new Color(0x8F0620);
    static final Color SCREEN = new Color(0xF4F1DE);
    static final Color INK = new Color(0x222831);

    static final Map<String, Color> TYPE = new HashMap<>();
    static final List<Pokemon> DEX = new ArrayList<>();
    static final Map<Integer, Pokemon> BY_ID = new HashMap<>();

    static {
        String[][] t = {
                {"Normal", "A8A878"}, {"Fogo", "F08030"}, {"Água", "6890F0"}, {"Elétrico", "F8D030"},
                {"Grama", "78C850"}, {"Gelo", "98D8D8"}, {"Lutador", "C03028"}, {"Veneno", "A040A0"},
                {"Terra", "E0C068"}, {"Voador", "A890F0"}, {"Psíquico", "F85888"}, {"Inseto", "A8B820"},
                {"Pedra", "B8A038"}, {"Fantasma", "705898"}, {"Dragão", "7038F8"}, {"Fada", "EE99AC"}};
        for (String[] x : t) TYPE.put(x[0], new Color(Integer.parseInt(x[1], 16)));

        Object[][] d = {
                {1, "Bulbasaur", "Grama", "Veneno", 1}, {3, "Venusaur", "Grama", "Veneno", 2},
                {4, "Charmander", "Fogo", null, 1}, {6, "Charizard", "Fogo", "Voador", 2},
                {7, "Squirtle", "Água", null, 1}, {9, "Blastoise", "Água", null, 2},
                {16, "Pidgey", "Normal", "Voador", 0}, {19, "Rattata", "Normal", null, 0},
                {25, "Pikachu", "Elétrico", null, 1}, {35, "Clefairy", "Fada", null, 1},
                {37, "Vulpix", "Fogo", null, 1}, {39, "Jigglypuff", "Normal", "Fada", 0},
                {52, "Meowth", "Normal", null, 0}, {54, "Psyduck", "Água", null, 0},
                {58, "Growlithe", "Fogo", null, 1}, {63, "Abra", "Psíquico", null, 0},
                {66, "Machop", "Lutador", null, 0}, {74, "Geodude", "Pedra", "Terra", 0},
                {79, "Slowpoke", "Água", "Psíquico", 0}, {92, "Gastly", "Fantasma", "Veneno", 0},
                {94, "Gengar", "Fantasma", "Veneno", 2}, {95, "Onix", "Pedra", "Terra", 1},
                {104, "Cubone", "Terra", null, 1}, {109, "Koffing", "Veneno", null, 0},
                {123, "Scyther", "Inseto", "Voador", 1}, {129, "Magikarp", "Água", null, 0},
                {130, "Gyarados", "Água", "Voador", 2}, {131, "Lapras", "Água", "Gelo", 2},
                {133, "Eevee", "Normal", null, 1}, {137, "Porygon", "Normal", null, 1},
                {138, "Omanyte", "Pedra", "Água", 1}, {142, "Aerodactyl", "Pedra", "Voador", 2},
                {143, "Snorlax", "Normal", null, 2}, {144, "Articuno", "Gelo", "Voador", 3},
                {145, "Zapdos", "Elétrico", "Voador", 3}, {146, "Moltres", "Fogo", "Voador", 3},
                {147, "Dratini", "Dragão", null, 1}, {149, "Dragonite", "Dragão", "Voador", 2},
                {150, "Mewtwo", "Psíquico", null, 3}, {151, "Mew", "Psíquico", null, 3}};
        for (Object[] o : d) {
            Pokemon p = new Pokemon((Integer) o[0], (String) o[1], (String) o[2], (String) o[3], (Integer) o[4]);
            DEX.add(p);
            BY_ID.put(p.id, p);
        }
        DEX.sort(Comparator.comparingInt(p -> p.id));
    }

    // ======================= ESTADO DO JOGO =======================
    final List<Integer> pc = new ArrayList<>();      // pokemons no PC (com repetidos)
    final Set<Integer> caught = new TreeSet<>();     // registrados na Pokedex
    int coins = 1000;
    final Random rng = new Random();
    final File saveFile = new File(System.getProperty("user.home"), "pokegacha_save.txt");

    void save() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(coins).append("\n");
            for (int i = 0; i < pc.size(); i++) sb.append(i > 0 ? "," : "").append(pc.get(i));
            sb.append("\n");
            int k = 0;
            for (int id : caught) sb.append(k++ > 0 ? "," : "").append(id);
            Files.write(saveFile.toPath(), sb.toString().getBytes("UTF-8"));
        } catch (IOException ignored) { }
    }

    void load() {
        try {
            if (!saveFile.exists()) return;
            List<String> lines = Files.readAllLines(saveFile.toPath());
            coins = Integer.parseInt(lines.get(0).trim());
            if (lines.size() > 1 && !lines.get(1).isBlank())
                for (String s : lines.get(1).split(",")) pc.add(Integer.parseInt(s.trim()));
            if (lines.size() > 2 && !lines.get(2).isBlank())
                for (String s : lines.get(2).split(",")) caught.add(Integer.parseInt(s.trim()));
        } catch (Exception e) {
            pc.clear(); caught.clear(); coins = 1000;
        }
    }

    Pokemon rollPokemon() {
        int r = rng.nextInt(100), acc = 0, rarity = 0;
        for (int i = 0; i < WEIGHT.length; i++) {
            acc += WEIGHT[i];
            if (r < acc) { rarity = i; break; }
        }
        List<Pokemon> pool = new ArrayList<>();
        for (Pokemon p : DEX) if (p.rarity == rarity) pool.add(p);
        return pool.get(rng.nextInt(pool.size()));
    }

    // ======================= DESENHO =======================
    static Color mix(Color a, Color b, float t) {
        return new Color((int) (a.getRed() * (1 - t) + b.getRed() * t),
                (int) (a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) (a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    /** Desenha um "Pokemon" estilizado (gerado por codigo) com as cores dos seus tipos. */
    static void drawPokemon(Graphics2D g, Pokemon p, int x, int y, int s, boolean silhouette) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color c1 = silhouette ? new Color(0x30343B) : TYPE.get(p.t1);
        Color c2 = silhouette ? new Color(0x30343B) : (p.t2 != null ? TYPE.get(p.t2) : mix(c1, Color.WHITE, 0.45f));

        // orelhas / pontas
        int e = s / 4;
        g.setColor(mix(c1, Color.BLACK, 0.15f));
        g.fillPolygon(new int[]{x + s / 6, x + s / 6 + e, x + s / 6 - e / 3},
                new int[]{y + s / 6, y + s / 3, y - e / 2}, 3);
        g.fillPolygon(new int[]{x + s - s / 6, x + s - s / 6 - e, x + s - s / 6 + e / 3},
                new int[]{y + s / 6, y + s / 3, y - e / 2}, 3);
        // corpo
        g.setPaint(new GradientPaint(x, y, c1, x, y + s, c2));
        g.fillOval(x, y + s / 10, s, s - s / 10);
        g.setColor(mix(c1, Color.BLACK, 0.35f));
        g.setStroke(new BasicStroke(Math.max(2, s / 32f)));
        g.drawOval(x, y + s / 10, s, s - s / 10);

        if (silhouette) {
            g.setColor(new Color(0xF4F1DE));
            g.setFont(new Font("SansSerif", Font.BOLD, s / 2));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("?", x + (s - fm.stringWidth("?")) / 2, y + s / 2 + fm.getAscent() / 2);
            return;
        }
        // barriga
        g.setColor(new Color(255, 255, 255, 110));
        g.fillOval(x + s / 4, y + s / 2, s / 2, s / 3);
        // olhos
        int ew = s / 6;
        int ey = y + s / 3;
        g.setColor(Color.WHITE);
        g.fillOval(x + s / 4, ey, ew, ew + 2);
        g.fillOval(x + s - s / 4 - ew, ey, ew, ew + 2);
        g.setColor(INK);
        g.fillOval(x + s / 4 + ew / 3, ey + ew / 4, ew / 2, ew / 2 + 2);
        g.fillOval(x + s - s / 4 - ew + ew / 3, ey + ew / 4, ew / 2, ew / 2 + 2);
        // boca
        g.setStroke(new BasicStroke(Math.max(2, s / 40f)));
        g.drawArc(x + s / 2 - s / 10, y + s / 2 + s / 12, s / 5, s / 8, 200, 140);
    }

    static void drawPokeball(Graphics2D g, int cx, int cy, int r) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Shape circle = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        Shape old = g.getClip();
        g.setClip(circle);
        g.setColor(RED);
        g.fillRect(cx - r, cy - r, 2 * r, r);
        g.setColor(Color.WHITE);
        g.fillRect(cx - r, cy, 2 * r, r);
        g.setClip(old);
        g.setColor(INK);
        g.setStroke(new BasicStroke(Math.max(3, r / 12f)));
        g.draw(circle);
        g.drawLine(cx - r, cy, cx + r, cy);
        g.setColor(Color.WHITE);
        g.fillOval(cx - r / 4, cy - r / 4, r / 2, r / 2);
        g.setColor(INK);
        g.drawOval(cx - r / 4, cy - r / 4, r / 2, r / 2);
    }

    // ======================= COMPONENTES =======================
    static class SpritePanel extends JPanel {
        Pokemon p; boolean silhouette;
        SpritePanel(int size) {
            setPreferredSize(new Dimension(size, size));
            setOpaque(false);
        }
        void set(Pokemon p, boolean silhouette) { this.p = p; this.silhouette = silhouette; repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (p == null) return;
            int s = Math.min(getWidth(), getHeight()) * 3 / 4;
            drawPokemon((Graphics2D) g0.create(), p, (getWidth() - s) / 2, (getHeight() - s) / 2, s, silhouette);
        }
    }

    static JLabel chip(String text, Color bg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setBorder(new EmptyBorder(3, 12, 3, 12));
        return l;
    }

    static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(INK, 3, true), new EmptyBorder(10, 22, 10, 22)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Painel com detalhes de um Pokemon (usado no PC e na Pokedex). */
    static class DetailPanel extends JPanel {
        final SpritePanel sprite = new SpritePanel(200);
        final JLabel number = new JLabel(" ", SwingConstants.CENTER);
        final JLabel name = new JLabel("—", SwingConstants.CENTER);
        final JPanel types = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        final JLabel rarity = new JLabel(" ", SwingConstants.CENTER);
        final JLabel extra = new JLabel(" ", SwingConstants.CENTER);
        final JPanel bottom = new JPanel();

        DetailPanel() {
            setLayout(new BorderLayout(0, 6));
            setBackground(SCREEN);
            setBorder(new CompoundBorder(new LineBorder(INK, 4, true), new EmptyBorder(12, 12, 12, 12)));
            setPreferredSize(new Dimension(280, 0));
            number.setFont(new Font("Monospaced", Font.BOLD, 16));
            name.setFont(new Font("SansSerif", Font.BOLD, 26));
            rarity.setFont(new Font("SansSerif", Font.BOLD, 15));
            extra.setFont(new Font("SansSerif", Font.PLAIN, 13));
            types.setOpaque(false);
            bottom.setOpaque(false);
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
            for (JComponent c : new JComponent[]{number, name, rarity, extra}) c.setAlignmentX(CENTER_ALIGNMENT);

            JPanel top = new JPanel(new GridLayout(2, 1));
            top.setOpaque(false);
            top.add(number);
            top.add(name);
            JPanel info = new JPanel();
            info.setOpaque(false);
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.add(types);
            info.add(rarity);
            info.add(Box.createVerticalStrut(6));
            info.add(extra);
            info.add(Box.createVerticalStrut(10));
            info.add(bottom);
            add(top, BorderLayout.NORTH);
            add(sprite, BorderLayout.CENTER);
            add(info, BorderLayout.SOUTH);
        }

        void show(Pokemon p, boolean known, String extraText) {
            types.removeAll();
            if (p == null) {
                sprite.set(null, false);
                number.setText(" "); name.setText("—"); rarity.setText(" "); extra.setText(" ");
            } else {
                number.setText(String.format("Nº %03d", p.id));
                sprite.set(p, !known);
                if (known) {
                    name.setText(p.name);
                    types.add(chip(p.t1, TYPE.get(p.t1)));
                    if (p.t2 != null) types.add(chip(p.t2, TYPE.get(p.t2)));
                    rarity.setText(RARITY[p.rarity]);
                    rarity.setForeground(RARITY_COLOR[p.rarity]);
                } else {
                    name.setText("???");
                    rarity.setText("Não descoberto");
                    rarity.setForeground(Color.GRAY);
                }
                extra.setText(extraText);
            }
            types.revalidate();
            types.repaint();
        }
    }

    JPanel header(String title, Runnable back) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(RED);
        h.setBorder(new CompoundBorder(new MatteBorder(0, 0, 5, 0, INK), new EmptyBorder(10, 14, 10, 14)));
        JLabel t = new JLabel(title);
        t.setForeground(Color.WHITE);
        t.setFont(new Font("SansSerif", Font.BOLD, 28));
        JButton b = button("← Menu", DARK_RED);
        b.setBorder(BorderFactory.createCompoundBorder(new LineBorder(INK, 2, true), new EmptyBorder(5, 14, 5, 14)));
        b.addActionListener(e -> back.run());
        h.add(b, BorderLayout.WEST);
        JPanel c = new JPanel(new FlowLayout(FlowLayout.CENTER));
        c.setOpaque(false);
        c.add(t);
        h.add(c, BorderLayout.CENTER);
        return h;
    }

    // ======================= TELAS =======================
    final CardLayout cards = new CardLayout();
    final JPanel root = new JPanel(cards);
    final JLabel coinLabel = new JLabel();
    PcScreen pcScreen;
    DexScreen dexScreen;
    GachaScreen gachaScreen;

    void go(String name) {
        if (name.equals("pc")) pcScreen.refresh();
        if (name.equals("dex")) dexScreen.refresh();
        if (name.equals("gacha")) gachaScreen.refresh();
        if (name.equals("menu")) updateCoins();
        cards.show(root, name);
    }

    void updateCoins() {
        coinLabel.setText("Pokécoins: " + coins + "   |   Pokédex: " + caught.size() + "/" + DEX.size());
    }

    // ---- MENU ----
    JPanel buildMenu() {
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setPaint(new GradientPaint(0, 0, RED, 0, getHeight(), DARK_RED));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(255, 255, 255, 40));
                drawPokeball(g, getWidth() - 20, getHeight() - 20, 220);
                drawPokeball(g, 40, 30, 110);
            }
        };
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.insets = new Insets(8, 0, 8, 0); c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("POKÉ-GACHA", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 54));
        title.setForeground(new Color(0xFFCB05));
        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0);
        p.add(title, c);

        coinLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        coinLabel.setForeground(Color.WHITE);
        coinLabel.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 1; c.insets = new Insets(0, 0, 24, 0);
        p.add(coinLabel, c);

        c.insets = new Insets(8, 120, 8, 120);
        String[][] items = {{"Ver PC", "pc"}, {"Ver Pokédex", "dex"}, {"Gachar Pokémon", "gacha"}};
        Color[] cols = {new Color(0x2E86DE), new Color(0xE67E22), new Color(0x27AE60)};
        int y = 2;
        for (int i = 0; i < items.length; i++) {
            JButton b = button(items[i][0], cols[i]);
            b.setFont(new Font("SansSerif", Font.BOLD, 22));
            String target = items[i][1];
            b.addActionListener(e -> go(target));
            c.gridy = y++;
            p.add(b, c);
        }
        JButton exit = button("Sair", INK);
        exit.addActionListener(e -> { save(); System.exit(0); });
        c.gridy = y;
        p.add(exit, c);
        return p;
    }

    // ---- PC ----
    class PcScreen extends JPanel {
        static final int PER_BOX = 30;
        int box = 0, selected = -1;
        final JPanel grid = new JPanel(new GridLayout(5, 6, 6, 6));
        final DetailPanel detail = new DetailPanel();
        final JLabel boxLabel = new JLabel("", SwingConstants.CENTER);
        final JButton release = button("Soltar Pokémon", new Color(0xC0392B));
        final Slot[] slots = new Slot[PER_BOX];

        class Slot extends JPanel {
            int index = -1; Pokemon p;
            Slot() {
                setPreferredSize(new Dimension(84, 84));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        if (p != null) { selected = index; updateDetail(); repaintSlots(); }
                    }
                });
            }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(p == null ? new Color(0xC9D6A8) : new Color(0xE8F0C8));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                boolean sel = p != null && index == selected;
                g.setColor(sel ? RED : new Color(0x6B7A45));
                g.setStroke(new BasicStroke(sel ? 4 : 2));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                if (p != null) {
                    int s = getWidth() - 34;
                    drawPokemon(g, p, 17, 12, s, false);
                    g.setColor(RARITY_COLOR[p.rarity]);
                    g.fillOval(getWidth() - 16, 6, 9, 9);
                }
            }
        }

        PcScreen() {
            setLayout(new BorderLayout());
            setBackground(new Color(0x3B7A9E));
            add(header("PC do Treinador", () -> go("menu")), BorderLayout.NORTH);

            for (int i = 0; i < PER_BOX; i++) { slots[i] = new Slot(); grid.add(slots[i]); }
            grid.setOpaque(false);

            JPanel boxFrame = new JPanel(new BorderLayout(0, 8));
            boxFrame.setBackground(new Color(0xF7E7A0));
            boxFrame.setBorder(new CompoundBorder(new LineBorder(INK, 4, true), new EmptyBorder(10, 10, 10, 10)));
            JButton prev = button("◀", new Color(0x2E86DE)), next = button("▶", new Color(0x2E86DE));
            prev.setBorder(new CompoundBorder(new LineBorder(INK, 2, true), new EmptyBorder(4, 14, 4, 14)));
            next.setBorder(prev.getBorder());
            prev.addActionListener(e -> { box--; selected = -1; refresh(); });
            next.addActionListener(e -> { box++; selected = -1; refresh(); });
            boxLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
            JPanel nav = new JPanel(new BorderLayout());
            nav.setOpaque(false);
            nav.add(prev, BorderLayout.WEST);
            nav.add(boxLabel, BorderLayout.CENTER);
            nav.add(next, BorderLayout.EAST);
            boxFrame.add(nav, BorderLayout.NORTH);
            boxFrame.add(grid, BorderLayout.CENTER);

            release.addActionListener(e -> releaseSelected());
            detail.bottom.add(release);
            release.setAlignmentX(CENTER_ALIGNMENT);

            JPanel center = new JPanel(new BorderLayout(14, 0));
            center.setOpaque(false);
            center.setBorder(new EmptyBorder(14, 14, 14, 14));
            center.add(boxFrame, BorderLayout.CENTER);
            center.add(detail, BorderLayout.EAST);
            add(center, BorderLayout.CENTER);
        }

        int boxCount() { return Math.max(1, (pc.size() + PER_BOX - 1) / PER_BOX); }

        void refresh() {
            box = Math.max(0, Math.min(box, boxCount() - 1));
            if (selected >= pc.size()) selected = -1;
            boxLabel.setText("BOX " + (box + 1) + " / " + boxCount() + "   (" + pc.size() + " Pokémon)");
            for (int i = 0; i < PER_BOX; i++) {
                int idx = box * PER_BOX + i;
                slots[i].index = idx;
                slots[i].p = idx < pc.size() ? BY_ID.get(pc.get(idx)) : null;
            }
            updateDetail();
            repaintSlots();
        }

        void repaintSlots() { for (Slot s : slots) s.repaint(); }

        void updateDetail() {
            if (selected < 0 || selected >= pc.size()) {
                detail.show(null, true, " ");
                release.setVisible(false);
            } else {
                Pokemon p = BY_ID.get(pc.get(selected));
                detail.show(p, true, "Valor ao soltar: " + RELEASE_VALUE[p.rarity] + " Pokécoins");
                release.setVisible(true);
            }
        }

        void releaseSelected() {
            if (selected < 0 || selected >= pc.size()) return;
            Pokemon p = BY_ID.get(pc.get(selected));
            int ok = JOptionPane.showConfirmDialog(PokemonApp.this,
                    "Soltar " + p.name + " e ganhar " + RELEASE_VALUE[p.rarity] + " Pokécoins?",
                    "Soltar Pokémon", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) return;
            pc.remove(selected);
            coins += RELEASE_VALUE[p.rarity];
            selected = -1;
            save();
            refresh();
        }
    }

    // ---- POKEDEX ----
    class DexScreen extends JPanel {
        final DefaultListModel<Pokemon> model = new DefaultListModel<>();
        final JList<Pokemon> list = new JList<>(model);
        final DetailPanel detail = new DetailPanel();
        final JLabel progress = new JLabel();

        DexScreen() {
            setLayout(new BorderLayout());
            setBackground(DARK_RED);
            add(header("Pokédex", () -> go("menu")), BorderLayout.NORTH);
            for (Pokemon p : DEX) model.addElement(p);

            list.setFont(new Font("Monospaced", Font.BOLD, 17));
            list.setBackground(SCREEN);
            list.setFixedCellHeight(34);
            list.setSelectionBackground(new Color(0xFFCB05));
            list.setSelectionForeground(INK);
            list.setCellRenderer((l, p, i, sel, foc) -> {
                boolean known = caught.contains(p.id);
                JLabel lb = new JLabel(String.format("  %s  %03d  %s", known ? "●" : "○", p.id, known ? p.name : "???"));
                lb.setFont(l.getFont());
                lb.setOpaque(true);
                lb.setBackground(sel ? l.getSelectionBackground() : (i % 2 == 0 ? SCREEN : new Color(0xE9E5CC)));
                lb.setForeground(known ? (sel ? INK : RED.darker()) : Color.GRAY);
                return lb;
            });
            list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateDetail(); });

            JScrollPane sp = new JScrollPane(list);
            sp.setBorder(new LineBorder(INK, 4, true));

            progress.setFont(new Font("SansSerif", Font.BOLD, 16));
            progress.setForeground(Color.WHITE);
            progress.setHorizontalAlignment(SwingConstants.CENTER);
            progress.setBorder(new EmptyBorder(0, 0, 8, 0));

            JPanel left = new JPanel(new BorderLayout());
            left.setOpaque(false);
            left.add(progress, BorderLayout.NORTH);
            left.add(sp, BorderLayout.CENTER);

            JPanel center = new JPanel(new BorderLayout(14, 0));
            center.setOpaque(false);
            center.setBorder(new EmptyBorder(14, 14, 14, 14));
            center.add(left, BorderLayout.CENTER);
            center.add(detail, BorderLayout.EAST);
            add(center, BorderLayout.CENTER);
            list.setSelectedIndex(0);
        }

        void refresh() {
            progress.setText("Capturados: " + caught.size() + " de " + DEX.size());
            updateDetail();
            list.repaint();
        }

        void updateDetail() {
            Pokemon p = list.getSelectedValue();
            if (p == null) { detail.show(null, true, " "); return; }
            boolean known = caught.contains(p.id);
            int owned = Collections.frequency(pc, p.id);
            detail.show(p, known, known ? "No PC: " + owned + "x" : "Capture para registrar!");
        }
    }

    // ---- GACHA ----
    class GachaScreen extends JPanel {
        Pokemon result; boolean isNew;
        int phase = 0, tick = 0;          // 0 = ocioso, 1 = balancando, 2 = revelado
        javax.swing.Timer timer;
        final JButton roll = button("Lançar Pokébola (" + ROLL_COST + " Pokécoins)", new Color(0x27AE60));
        final JLabel status = new JLabel(" ", SwingConstants.CENTER);
        final JLabel wallet = new JLabel(" ", SwingConstants.CENTER);
        final JPanel stage;

        GachaScreen() {
            setLayout(new BorderLayout());
            add(header("Gacha Pokémon", () -> { if (phase != 1) go("menu"); }), BorderLayout.NORTH);

            stage = new JPanel() {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0;
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    Color glow = phase == 2 ? RARITY_COLOR[result.rarity] : new Color(0x4A6FA5);
                    g.setPaint(new RadialGradientPaint(w / 2f, h / 2f, Math.max(w, h) / 1.6f,
                            new float[]{0f, 1f}, new Color[]{mix(glow, Color.WHITE, 0.5f), new Color(0x1B2735)}));
                    g.fillRect(0, 0, w, h);
                    int cx = w / 2, cy = h / 2, r = Math.min(w, h) / 5;
                    if (phase == 2) {
                        int s = r * 3;
                        // brilho ao redor do pokemon
                        g.setColor(new Color(255, 255, 255, Math.max(0, 200 - tick * 5)));
                        g.fillOval(cx - s, cy - s, 2 * s, 2 * s);
                        drawPokemon(g, result, cx - s / 2 - r / 2, cy - s / 2 - r / 2, s + r, false);
                    } else {
                        Graphics2D g2 = (Graphics2D) g.create();
                        double angle = phase == 1 ? Math.sin(tick / 2.5) * (0.15 + 0.35 * Math.min(1, tick / 25.0)) : 0;
                        g2.rotate(angle, cx, cy + r);
                        int bounce = phase == 1 ? (int) (Math.abs(Math.sin(tick / 4.0)) * 14) : 0;
                        drawPokeball(g2, cx, cy - bounce, r);
                        g2.dispose();
                        g.setColor(new Color(0, 0, 0, 70));
                        g.fillOval(cx - r, cy + r + 8, 2 * r, r / 3);
                    }
                }
            };
            stage.setBorder(new LineBorder(INK, 5, true));

            status.setFont(new Font("SansSerif", Font.BOLD, 24));
            status.setForeground(Color.WHITE);
            wallet.setFont(new Font("SansSerif", Font.BOLD, 15));
            wallet.setForeground(new Color(0xFFCB05));

            JPanel south = new JPanel(new GridLayout(3, 1, 0, 6));
            south.setOpaque(false);
            south.add(status);
            south.add(wallet);
            JPanel bp = new JPanel();
            bp.setOpaque(false);
            bp.add(roll);
            south.add(bp);

            setBackground(new Color(0x1B2735));
            JPanel center = new JPanel(new BorderLayout(0, 10));
            center.setOpaque(false);
            center.setBorder(new EmptyBorder(14, 14, 14, 14));
            center.add(stage, BorderLayout.CENTER);
            center.add(south, BorderLayout.SOUTH);
            add(center, BorderLayout.CENTER);

            roll.addActionListener(e -> startRoll());
        }

        void refresh() {
            phase = 0; tick = 0; result = null;
            status.setText("Pronto para gachar!");
            wallet.setText("Pokécoins: " + coins);
            roll.setEnabled(true);
            stage.repaint();
        }

        void startRoll() {
            if (coins < ROLL_COST) {
                JOptionPane.showMessageDialog(this,
                        "Pokécoins insuficientes!\nSolte Pokémon no PC para ganhar mais.",
                        "Sem moedas", JOptionPane.WARNING_MESSAGE);
                return;
            }
            coins -= ROLL_COST;
            result = rollPokemon();
            isNew = !caught.contains(result.id);
            phase = 1; tick = 0;
            roll.setEnabled(false);
            status.setText("Balançando...");
            wallet.setText("Pokécoins: " + coins);
            if (timer != null) timer.stop();
            timer = new javax.swing.Timer(40, e -> {
                tick++;
                if (phase == 1 && tick >= 50) {
                    phase = 2; tick = 0;
                    pc.add(result.id);
                    caught.add(result.id);
                    save();
                    status.setText(result.name + " (" + RARITY[result.rarity] + ")" + (isNew ? "  —  NOVO!" : ""));
                    status.setForeground(mix(RARITY_COLOR[result.rarity], Color.WHITE, 0.4f));
                    roll.setText("Gachar de novo (" + ROLL_COST + " Pokécoins)");
                    roll.setEnabled(true);
                } else if (phase == 2 && tick > 40) {
                    ((javax.swing.Timer) e.getSource()).stop();
                }
                stage.repaint();
            });
            status.setForeground(Color.WHITE);
            timer.start();
        }
    }

    // ======================= MAIN =======================
    public PokemonApp() {
        super("Poké-Gacha");
        load();
        pcScreen = new PcScreen();
        dexScreen = new DexScreen();
        gachaScreen = new GachaScreen();
        root.add(buildMenu(), "menu");
        root.add(pcScreen, "pc");
        root.add(dexScreen, "dex");
        root.add(gachaScreen, "gacha");
        setContentPane(root);
        updateCoins();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { save(); }
        });
        setSize(960, 680);
        setMinimumSize(new Dimension(860, 620));
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new PokemonApp().setVisible(true));
    }
}