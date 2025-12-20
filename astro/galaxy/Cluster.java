package astro.galaxy;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
// import java.awt.image.*;
// import java.util.*;
// import java.io.*;
// import java.net.*;
// import java.text.*;
import javax.swing.*;

import myutil.*;

public class Cluster extends JApplet implements ActionListener, WindowListener,
						ItemListener
{	
        JTextField ROTRATE;
	JTextField DT;
	JTextField WARP;
	JTextField N;

	JButton START;
	JButton PAUSEPLAY;
	JButton TRAIL;
	JButton APPLY;
	JButton ZOOMIN;
	JButton ZOOMOUT;
	JComboBox SELECTION;
	JComboBox INTEGRATOR;

	JPanel cp;
	JPanel tp;
	JPanel bp;
	ClusterCanvas cc;

	double rotrate;
	double dt;
	int warp, n;
	int integrator;
	double zoom = 8;
	final int ANIMATE=0;
	final int TRAPNEWT=1;
	final int MIDPOINT=2;
	final int RUNGEKUTTA=3;
	final int HERMITE=4;
	final int EULER=5;
	final int GASMODEL=6;

        public static void main(String[] args) {
                new AppletFrame(new Cluster(), 1000, 1000);
        }

	public void init()
	{	
	    int j;

	    setFont(new Font("Courier", Font.BOLD, 12));
	    setBackground(Color.white);

	    ROTRATE = new JTextField(6);
	    DT = new JTextField(6);
	    WARP = new JTextField(6);
	    N = new JTextField(6);
	    ROTRATE.setText("0.0");
	    DT.setText("1.0e-6");
	    WARP.setText("500");
	    N.setText("7");

	    START   = new JButton("Start");
	    PAUSEPLAY = new JButton("Pause");
	    TRAIL = new JButton("Show Trails");
	    APPLY = new JButton("Apply");
	    ZOOMIN = new JButton("+");
	    ZOOMOUT = new JButton("-");
	    SELECTION = new JComboBox();
	    SELECTION.addItem("--Select One--");
	    SELECTION.addItem("LagrangeN");
	    SELECTION.addItem("FigureEight3");
	    SELECTION.addItem("Gaussian");
	    SELECTION.addItem("Random");
	    SELECTION.addItem("Epimetheus/Janus");
	    SELECTION.addItem("Saturn/Janus");

	    INTEGRATOR = new JComboBox();
	    INTEGRATOR.addItem("Midpoint (Leapfrog)");
	    INTEGRATOR.addItem("Hermite");
	    INTEGRATOR.addItem("Gas Model");
	    INTEGRATOR.addItem("Euler");
	    INTEGRATOR.addItem("RungeKutta4");
	    INTEGRATOR.addItem("TrapNewt");
	    INTEGRATOR.addItem("Animate Only");

	    START.addActionListener( this );
	    PAUSEPLAY.addActionListener( this );
	    TRAIL.addActionListener( this );
	    APPLY.addActionListener( this );
	    ZOOMIN.addActionListener( this );
	    ZOOMOUT.addActionListener( this );
	    SELECTION.addItemListener( this );
	    INTEGRATOR.addItemListener( this );

	    START.setToolTipText("Start the simulation");
	    PAUSEPLAY.setToolTipText("Pause the simulation");
	    TRAIL.setToolTipText("Show historical trails");
	    APPLY.setToolTipText("Apply the parameters dt and warp");
	    ZOOMIN.setToolTipText("Zoom in");
	    ZOOMOUT.setToolTipText("Zoom out");

	    cp = new JPanel();
	    cp.setLayout(new BoxLayout(cp, BoxLayout.X_AXIS));
	    cp.add(new JLabel("Instance:"));
	    cp.add(SELECTION);
	    cp.add(Box.createRigidArea(new Dimension(5,5)));
	    cp.add(new JLabel("Integrator:"));
	    cp.add(INTEGRATOR);

	    tp = new JPanel();
	    tp.setLayout(new BoxLayout(tp, BoxLayout.X_AXIS));
	    tp.add(Box.createRigidArea(new Dimension(5,5)));
	    tp.add(new JLabel("FrameRotationRate="));
	    tp.add(ROTRATE);
	    tp.add(Box.createRigidArea(new Dimension(5,5)));
	    tp.add(new JLabel("dt="));
	    tp.add(DT);
	    tp.add(Box.createRigidArea(new Dimension(5,5)));
	    tp.add(new JLabel("warp="));
	    tp.add(WARP);
	    tp.add(Box.createRigidArea(new Dimension(5,5)));
	    tp.add(new JLabel("n="));
	    tp.add(N);

	    bp = new JPanel();
	    bp.setLayout(new BoxLayout(bp, BoxLayout.X_AXIS));
	    bp.add(START);
	    bp.add(PAUSEPLAY);
	    bp.add(TRAIL);
	    bp.add(APPLY);
	    bp.add(ZOOMIN);
	    bp.add(ZOOMOUT);

	    cc = new ClusterCanvas();
	    // cc.addWindowListener(this);
	    cc.setSize(400,400);
	    cc.setVisible(true);

	    Container c = getContentPane();
	    c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
	    c.add(cc);
	    c.add(cp);
	    //c.add(Box.createRigidArea(new Dimension(50,50)));
	    c.add(tp);
	    c.add(bp);
	    // add(cc);
	    // cc.show();
	}

	public void itemStateChanged(ItemEvent e) {
	  zoom = 8;
	  if (e.getItem().equals("LagrangeN")) {rotrate=0; dt=0.001; warp=5; n=100; N.enable();} else 
	  if (e.getItem().equals("FigureEight3")) {rotrate=0; dt=0.001; warp=5; n=3; N.disable();} else 
	  if (e.getItem().equals("Gaussian")) {rotrate=0; dt=0.001; warp=5; n=100; N.enable();} else 
	  if (e.getItem().equals("Random")) {rotrate=0; dt=0.001; warp=5; n=100; N.enable();} else 
	  if (e.getItem().equals("Epimetheus/Janus")) {rotrate=0; dt=0.00001; warp=500; n=3; N.enable(); zoom/=2*2*2*2*2*2*2*2*2*2*2*2; rotrate=3286.8;} else 
	  if (e.getItem().equals("Saturn/Janus")) {rotrate=0; dt=0.00001; warp=500; n=1000; N.enable(); zoom/=2*2*2*2*2*2*2*2*2*2*2*2; rotrate=3284.4;} else 
	  {}
	  ROTRATE.setText(""+rotrate); 
	  DT.setText(""+dt); WARP.setText(""+warp); N.setText(""+n);
	}

        public void actionPerformed(ActionEvent e)
        {
	    int j;
	    double mv;
	    double r=0, th=0, v=0;
	    // double v_escape=0;
	    double G, r0;

	    G=1.177e-4;
	    r0 = 0.0;

	    n = SL.atoi(N.getText());
	    int n7 = (n<7)?7:n;

	    double m[];
	    double x[];
	    double y[];
	    double z[];
	    double vx[];
	    double vy[];
	    double vz[];
	    double rad[];
	    Color  col[];
	    double pathx[][] = null;
	    double pathy[][] = null;
	    double[][] as = null;
	    double[][] ac = null;
	    double[][] bs = null;
	    double[][] bc = null;
	    int i, k, M=0, N=0, K=0;

	    if (INTEGRATOR.getSelectedItem().equals("Euler")) {
		integrator = EULER;
	    } else
	    if (INTEGRATOR.getSelectedItem().equals("Midpoint (Leapfrog)")) {
		integrator = MIDPOINT;
	    } else
	    if (INTEGRATOR.getSelectedItem().equals("Hermite")) {
		integrator = HERMITE;
	    } else
	    if (INTEGRATOR.getSelectedItem().equals("Gas Model")) {
		integrator = GASMODEL;
	    } else
	    if (INTEGRATOR.getSelectedItem().equals("RungeKutta")) {
		integrator = RUNGEKUTTA;
	    } else
	    if (INTEGRATOR.getSelectedItem().equals("TrapNewt")) {
		integrator = TRAPNEWT;
	    } else
	    if (INTEGRATOR.getSelectedItem().equals("Animate Only")) {
		integrator = ANIMATE;
	    } 

	    if (e.getSource() == START) {
		if (SELECTION.getSelectedItem().equals(
			    "--Select One--")) {
		  return;
		} else 
		Data3d.init(SELECTION.getSelectedItem().toString(), n, G);
		M=Data3d.M;
		N=Data3d.N;
		K=Data3d.K;
		m=Data3d.m;
		System.err.println("3: m = "+(m[0]));
		rad=Data3d.rad;
		col=Data3d.col;
		x=Data3d.x;
		y=Data3d.y;
		z=Data3d.z;
		vx=Data3d.vx;
		System.err.println("3: vx = "+(vx[0]));
		vy=Data3d.vy;
		vz=Data3d.vz;

		rotrate = SL.atof(ROTRATE.getText());
		dt = SL.atof(DT.getText());
		warp = SL.atoi(WARP.getText());

		System.err.println("2: vx = "+(vx[0]));
		mv = 0;
		for (j=1; j<n; j++) {
		    mv += m[j]*vx[j];
		}
		vx[0] = -mv/m[0];
		mv = 0;
		for (j=1; j<n; j++) {
		    mv += m[j]*vy[j];
		}
		vy[0] = -mv/m[0];
		mv = 0;
		for (j=1; j<n; j++) {
		    mv += m[j]*vz[j];
		}
		vz[0] = -mv/m[0];

		System.err.println("1: vx = "+(vx[0]));
		cc.setParams( n,m,x,y,z,vx,vy,vz,rad,col,
			      G,r0,rotrate,dt,warp,zoom,integrator);
		cc.timer.start();
	    } else if (e.getSource() == PAUSEPLAY) {
		if (PAUSEPLAY.getText().equals("Pause")) {
		    PAUSEPLAY.setText("Play");
		    PAUSEPLAY.setToolTipText("Restart the simulation");
		    cc.timer.stop();
		} else
		if (PAUSEPLAY.getText().equals("Play")) {
		    PAUSEPLAY.setText("Pause");
		    PAUSEPLAY.setToolTipText("Pause the simulation");
		    cc.timer.start();
		} 
	    } else if (e.getSource() == TRAIL) {
		if (TRAIL.getText().equals("Show Trails")) {
		    TRAIL.setText("Hide Trails");
		    TRAIL.setToolTipText("Show historical trails");
		    cc.setTrails(true);
		} else
		if (TRAIL.getText().equals("Hide Trails")) {
		    TRAIL.setText("Show Trails");
		    TRAIL.setToolTipText("Hide historical trails");
		    cc.setTrails(false);
		} 
	    } else if (e.getSource() == APPLY) {
		rotrate = SL.atof(ROTRATE.getText());
		dt = SL.atof(DT.getText());
		warp = SL.atoi(WARP.getText());
		if (cc != null) {
		    cc.setAnimEffects(rotrate,dt,warp,zoom);
		    cc.timer.start();
		}
	    } else if (e.getSource() == ZOOMIN) {
		zoom /= 2;
		if (cc != null) {
		    cc.setAnimEffects(rotrate,dt,warp,zoom);
		    cc.timer.start();
		}
	    } else if (e.getSource() == ZOOMOUT) {
		zoom *= 2;
		if (cc != null) {
		    cc.setAnimEffects(rotrate,dt,warp,zoom);
		    cc.timer.start();
		}
	    } else if (e.getSource() == ROTRATE) {
		rotrate = SL.atof(ROTRATE.getText());
		if (cc != null) {
		    cc.setAnimEffects(rotrate,dt,warp,zoom);
		    cc.timer.start();
		}
	    } else if (e.getSource() == DT) {
		dt = SL.atof(DT.getText());
		if (cc != null) {
		    cc.setAnimEffects(rotrate,dt,warp,zoom);
		    cc.timer.start();
		}
	    } else if (e.getSource() == WARP) {
		warp = SL.atoi(WARP.getText());
		if (cc != null) {
		    cc.setAnimEffects(rotrate,dt,warp,zoom);
		    cc.timer.start();
		}
	    }
        }

	public void stop()
	{
	    if (cc != null) if (cc.timer != null) cc.timer.stop();
	}

	public void windowClosing(WindowEvent e)
	{
	    cc.timer.stop();
	    cc.setVisible(false);
	}
	public void windowClosed(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { cc.timer.stop(); }
	public void windowDeiconified(WindowEvent e) { cc.timer.start(); }
	public void windowActivated(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }
}
