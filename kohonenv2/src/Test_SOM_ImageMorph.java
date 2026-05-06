import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Test_SOM_ImageMorph extends JFrame {

    private MyComponent komponent;
    private SOM som;
    private Timer timer;

    private JButton loadFirstButton;
    private JButton loadSecondButton;
    private JButton startButton;
    private JLabel statusLabel;

    private ImagePanel leftPanel;
    private ImagePanel rightPanel;

    class ImagePanel extends JPanel {
        private BufferedImage image;
        private String label;

        public ImagePanel(String label) {
            this.label = label;
            setBackground(Color.DARK_GRAY);
        }

        public void setImage(BufferedImage img) {
            this.image = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = getWidth();
            int h = getHeight();

            if (image != null) {
                double imgRatio = image.getWidth() / (double) image.getHeight();
                int drawW = w;
                int drawH = (int) (w / imgRatio);

                if (drawH > h) {
                    drawH = h;
                    drawW = (int) (h * imgRatio);
                }

                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;

                g.drawImage(image, x, y, drawW, drawH, null);
            } else {
                g.setColor(Color.WHITE);
                g.drawString(label, w / 2 - 40, h / 2);
            }
        }
    }

    class MyComponent extends JComponent {

        private SOM som;
        private Random random = new Random();

        private List<Vec2D> firstShapePoints = new ArrayList<>();
        private List<Vec2D> secondShapePoints = new ArrayList<>();

        private BufferedImage firstImage;
        private BufferedImage secondImage;

        private boolean running = false;
        private int phase = 0;

        private int samplesPerFrame = 25; // TODO do pozmieniania popróbowania

        private double startEta = 0.12;
        private double startS;

        private double etaThreshold = 0.01; // do pobawienia się
        private double sThreshold = 0.5; // do pozmieniania również

        public MyComponent(SOM som) {
            this.som = som;
            this.startS = som.S;
        }

        public void stopMorph() {
            running = false;
            phase = 0;
        }

        public void loadFirstImage(File file) {
            try {
                firstImage = ImageIO.read(file);
                firstShapePoints = imageToPoints(firstImage);
                leftPanel.setImage(firstImage);

                stopMorph();
                repaint();
                updateStatus("First image loaded: " + firstShapePoints.size());
            } catch (Exception ex) {
                updateStatus("Cannot load first image");
            }
        }

        public void loadSecondImage(File file) {
            try {
                secondImage = ImageIO.read(file);
                secondShapePoints = imageToPoints(secondImage);
                rightPanel.setImage(secondImage);

                stopMorph();
                repaint();
                updateStatus("Second image loaded: " + secondShapePoints.size());
            } catch (Exception ex) {
                updateStatus("Cannot load second image");
            }
        }

        public void startMorph() {

            stopMorph();

            if (firstShapePoints.isEmpty() || secondShapePoints.isEmpty()) {
                updateStatus("Load both images first");
                return;
            }

            som.warmup(startEta, startS);

            running = true;
            phase = 1;

            updateStatus("Learning first image...");
        }

        public void trainOneFrame() {
            if (!running) return;

            for (int i = 0; i < samplesPerFrame; i++) {

                Vec2D input;

                if (phase == 1) {
                    input = randomPoint(firstShapePoints);
                    som.ucz(input);

                    if (som.eta < etaThreshold || som.S < sThreshold) { // sprawdzić lepiej || czy && TODO
                        phase = 2;
                        som.warmup(startEta * 0.7, startS * 0.7);
                        updateStatus("Morphing into second image...");
                    }

                } else if (phase == 2) {
                    input = randomPoint(secondShapePoints);
                    som.ucz(input);

                    if (som.eta < etaThreshold || som.S < sThreshold) { // Tu też TODO
                        running = false;
                        updateStatus("Finished morphing");
                    }
                }
            }
        }

        private Vec2D randomPoint(List<Vec2D> points) {
            return points.get(random.nextInt(points.size()));
        }

        private List<Vec2D> imageToPoints(BufferedImage img) {
            List<Vec2D> points = new ArrayList<>();

            int width = img.getWidth();
            int height = img.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    int argb = img.getRGB(x, y);

                    int alpha = (argb >> 24) & 255;
                    int red = (argb >> 16) & 255;
                    int green = (argb >> 8) & 255;
                    int blue = argb & 255;

                    int brightness = (red + green + blue) / 3;

                    if (alpha > 30 && brightness < 220) {
                        double nx = (x / (double) (width - 1)) * 2.0 - 1.0;
                        double ny = (y / (double) (height - 1)) * 2.0 - 1.0;
                        points.add(new Vec2D(nx, ny));
                    }
                }
            }

            return points;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);

            BufferedImage current = null;

            if (phase == 1) current = firstImage;
            if (phase == 2) current = secondImage;

            if (current != null) {
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.4f));

                double ratio = current.getWidth() / (double) current.getHeight();

                int drawW = w;
                int drawH = (int) (w / ratio);

                if (drawH > h) {
                    drawH = h;
                    drawW = (int) (h * ratio);
                }

                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;

                g2.drawImage(current, x, y, drawW, drawH, null);

                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 1f));
            }

            trainOneFrame();

            som.drawFilled(g2, 0, 0, w, h);
        }
    }

    public Test_SOM_ImageMorph(String title) {
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setSize(1200, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        som = new SOM(25, 25, 0.12, 0.99995, 0.9995);
        komponent = new MyComponent(som);

        JPanel mainPanel = new JPanel(null);

        leftPanel = new ImagePanel("Load Image 1");
        rightPanel = new ImagePanel("Load Image 2");

        leftPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(true);
            }
        });

        rightPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(false);
            }
        });

        mainPanel.add(leftPanel);
        mainPanel.add(komponent);
        mainPanel.add(rightPanel);

        mainPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {

                int w = mainPanel.getWidth();
                int h = mainPanel.getHeight();

                int size = Math.min(h, w / 3);
                int startX = (w - size * 3) / 2;
                int startY = (h - size) / 2;

                leftPanel.setBounds(startX, startY, size, size);
                komponent.setBounds(startX + size, startY, size, size);
                rightPanel.setBounds(startX + size * 2, startY, size, size);
            }
        });

        add(mainPanel, BorderLayout.CENTER);

        JPanel panel = new JPanel();

        loadFirstButton = new JButton("Load first image");
        loadSecondButton = new JButton("Load second image");
        startButton = new JButton("Start");
        statusLabel = new JLabel("Load two images, then press Start");

        loadFirstButton.addActionListener(e -> chooseImage(true));
        loadSecondButton.addActionListener(e -> chooseImage(false));
        startButton.addActionListener(e -> komponent.startMorph());

        //panel.add(loadFirstButton);
        //panel.add(loadSecondButton);
        panel.add(startButton);
        panel.add(statusLabel);

        add(panel, BorderLayout.SOUTH);

        timer = new Timer(25, e -> komponent.repaint());
        timer.start();

        setVisible(true);
    }

    private void chooseImage(boolean first) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(
                new FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "bmp")
        );

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            if (first) {
                komponent.loadFirstImage(chooser.getSelectedFile());
            } else {
                komponent.loadSecondImage(chooser.getSelectedFile());
            }
        }
    }

    private void updateStatus(String text) {
        statusLabel.setText(text);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(
                () -> new Test_SOM_ImageMorph("SOM Image Morph")
        );
    }
}