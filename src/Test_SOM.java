import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.*;
import java.awt.*;

public class Test_SOM extends JFrame {

	private MyComponent komponent;
	private SOM som;
	private Timer timer;
	
	class MyComponent extends JComponent {
	    private String shapeMode = "Prostokąt";
	    private SOM som;
	    
	    // Zapis wartości startowych do rozgrzewki
	    private double startEta = 0.1;
	    private double startS;

	    public MyComponent(SOM som) {
	        this.som = som;
	        this.startS = som.S; // Pobranie początkowego promienia
	    }

	    public void setShapeMode(String mode) {
	        this.shapeMode = mode;
	        // Rozgrzewka przy zmianie kształtu, anty stagnacja
	        som.warmup(startEta, startS * 0.6);
	    }

	    @Override
	    protected void paintComponent(Graphics g) {
	        super.paintComponent(g);

	        int w = getWidth();
	        int h = getHeight();
	        int x0 = w / 4;
	        int y0 = h / 4;
	        int szer = w / 2;
	        int wys = h / 2;

	        Random r = new Random();
	        double a_in = 0, b_in = 0;

	        if (shapeMode.equals("Prostokąt")) {
	            g.setColor(Color.LIGHT_GRAY);
	            g.drawRect(x0, y0, szer, wys);

	            a_in = (r.nextDouble() - 0.5) / 0.5;
	            b_in = (r.nextDouble() - 0.5) / 0.5;

	        } else if (shapeMode.equals("Trójkąt")) {
	            // Rysowanie trójkąta (czubek na górze)
	            int[] px = {x0, x0 + szer, x0 + szer / 2};
	            int[] py = {y0 + wys, y0 + wys, y0};
	            g.setColor(Color.LIGHT_GRAY);
	            g.drawPolygon(px, py, 3);

	            // Losowanie w trójkącie równoramiennym
	            double r1 = Math.sqrt(r.nextDouble()); // Dystans od wierzchołka
	            double r2 = r.nextDouble();            // Pozycja na podstawie
	            
	            double x = (1 - r1) * 0.5 + r1 * r2;
	            double y = r1; 

	            a_in = (x * 2.0) - 1.0;
	            b_in = (y * 2.0) - 1.0; 
	            
	        } else if (shapeMode.equals("Koło")) {
	            // Rysowanie koła pomocniczego
	            g.setColor(Color.LIGHT_GRAY);
	            g.drawOval(x0, y0, szer, wys);
	            
	            // Losowanie punktu wewnątrz koła (jednorodnie)
	            double radius = Math.sqrt(r.nextDouble());
	            double angle = 2.0 * Math.PI * r.nextDouble();
	            
	            // Wyliczenie współrzędnych a_in i b_in w zakresie [-1, 1]
	            a_in = radius * Math.cos(angle);
	            b_in = radius * Math.sin(angle);
	        }

	        // Uczenie i rysowanie
	        Vec2D wejscia = new Vec2D(a_in, b_in);
	        som.ucz(wejscia);
	        som.draw(g, x0, y0, szer, wys);

	        // Informacja o plastyczności sieci
	        g.setColor(Color.BLACK);
	        g.drawString(String.format("Tryb: %s | Plastyczność (eta): %.5f", shapeMode, som.eta), 10, 20);
	    }
	}

	public Test_SOM(String string) {
		super(string);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension d = kit.getScreenSize();
		setBounds(d.width/4, d.height/4, d.width/2, d.height/2);
		setLayout(new BorderLayout());

		som = new SOM(10, 10, 0.1, 0.9999, 0.999);
		
		komponent = new MyComponent(som);
		add(komponent, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		JRadioButton rb1 = new JRadioButton("Prostokąt", true);
		JRadioButton rb2 = new JRadioButton("Trójkąt");
		JRadioButton rb3 = new JRadioButton("Koło");
		
		ButtonGroup group = new ButtonGroup();
		group.add(rb1); 
		group.add(rb2);
		group.add(rb3);

		rb1.addActionListener(e -> komponent.setShapeMode("Prostokąt"));
		rb2.addActionListener(e -> komponent.setShapeMode("Trójkąt"));
		rb3.addActionListener(e -> komponent.setShapeMode("Koło"));

		panel.add(rb1);
		panel.add(rb2);
		panel.add(rb3);
		
		// Przycisk ręcznej pobudki
		JButton btn = new JButton("Pobudź sieć");
		btn.addActionListener(e -> som.warmup(0.1, Math.sqrt(10*10)));
		panel.add(btn);
		
		add(panel, BorderLayout.SOUTH);
		
		// Timer odświeżający widok
		timer = new Timer(5, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				komponent.repaint();
			}
		});
		
		timer.start();
		setVisible(true);
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Test_SOM("Test SOM");
			}
		});
	}
}