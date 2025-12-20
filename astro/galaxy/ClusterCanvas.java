package astro.galaxy;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;
import javax.swing.*;

import myutil.*;
import myutil.Format;

class ClusterCanvas extends Canvas implements ActionListener
{
	int n;
	int n0;
	Body3d[] p;
	double G, r0;
	double t, dt;
	int warp;
	double zoom;
	double v_escape;
	double theta, rotrate;
	Body3d pi, pj;
	double[] cum_mass;
	int integrator;
	final int ANIMATE=0;
	final int TRAPNEWT=1;
	final int MIDPOINT=2;
	final int RUNGEKUTTA=3;
	final int HERMITE=4;
	final int EULER=5;
	final int GASMODEL=6;

	javax.swing.Timer timer=null;

	double cmax0, cmax;

	double old_x[], old_y[], old_z[], old_vx[], old_vy[], old_vz[],
	       old_ax[], old_ay[], old_az[], old_jx[], old_jy[], old_jz[];

	public void setParams(
		int n,
		double m[],
		double x[],
		double y[],
		double z[],
		double vx[],
		double vy[],
		double vz[],
		double rad[],
		Color  col[],
		double G,
		double r0,
		double rotrate,
		double dt,
		int warp,
		double zoom,
		int integrator)
	{
	    setBackground(Color.black);
	    if (timer==null) {timer = new javax.swing.Timer (25, this);}

	    int i, j;

	    this.n = n;
	    p = new Body3d[n];
	    for (j=0; j<n; j++) p[j] = new Body3d();

	    for (j=0; j<n; j++) {
		p[j].m = m[j];
		p[j].x = x[j];
		p[j].y = y[j];
		p[j].z = z[j];
		p[j].vx = vx[j];
		p[j].vy = vy[j];
		p[j].vz = vz[j];
		p[j].rad = rad[j];
		p[j].col = col[j];
		p[j].escaped = false;
	    }

	    for (j=n; j>0; j--) {
		if (p[j-1].m != 0) break;
	    }
	    n0 = j;

	    cmax = 0.1;
	    this.G = G;
	    this.r0 = r0;
	    this.rotrate = rotrate;
	    this.dt = dt;
	    this.warp = warp;
	    this.zoom = zoom;
	    this.integrator = integrator;
	    cum_mass = new double[n];

	    // if (n<=7) {cmax = 2;} // for the Lagrange case

	    pi = new Body3d(); pj = new Body3d();

	    v_escape = 10*Math.sqrt(3*G*(n-1)/r0); 
	    theta = 0;
	    t=0;
            if (integrator == MIDPOINT) {
		double r03 = r0*r0*r0;
		for (j=0; j<n; j++) {
		    p[j].ax = 0; p[j].ay = 0; p[j].az = 0;
		    for (i=0; i<n0; i++) {
			if (i != j) {
			    double r3 = dist3(p[i], p[j]);
			    if (r3<r03) r3=r03;
			    p[j].ax -= G * p[i].m * (p[j].x - p[i].x)/r3;
			    p[j].ay -= G * p[i].m * (p[j].y - p[i].y)/r3;
			    p[j].az -= G * p[i].m * (p[j].z - p[i].z)/r3;
			}
		    }
		}
		for (j=0; j<n; j++) {
		    p[j].vx += p[j].ax * dt/2;
		    p[j].vy += p[j].ay * dt/2;
		    p[j].vz += p[j].az * dt/2;
		}
	    } else
            if (integrator == HERMITE) {
		double r02 = r0*r0;
		for (j=0; j<n; j++) {
		    p[j].ax = p[j].ay = p[j].az = 0;
		    p[j].jx = p[j].jy = p[j].jz = 0;
		}
		for (i=0; i<n; i++) {
		    for (j=i+1; j<n; j++) {
			double rjix = p[j].x-p[i].x, 
			       rjiy = p[j].y-p[i].y, 
			       rjiz = p[j].z-p[i].z, 
			       vjix = p[j].vx-p[i].vx, 
			       vjiy = p[j].vy-p[i].vy,
			       vjiz = p[j].vz-p[i].vz;
			double r2 = rjix*rjix + rjiy*rjiy + rjiz*rjiz;
			if (r2<r02) r2=r02;
			double r3 = r2*Math.sqrt(r2);
			double rv = (rjix*vjix + rjiy*vjiy + rjiz*vjiz)/r2;
			p[i].ax += G*p[j].m * rjix / r3;
			p[i].ay += G*p[j].m * rjiy / r3;
			p[i].az += G*p[j].m * rjiz / r3;
			p[j].ax -= G*p[i].m * rjix / r3;
			p[j].ay -= G*p[i].m * rjiy / r3;
			p[j].az -= G*p[i].m * rjiz / r3;
			p[i].jx += G*p[j].m * (vjix - 3*rv*rjix)/r3;
			p[i].jy += G*p[j].m * (vjiy - 3*rv*rjiy)/r3;
			p[i].jz += G*p[j].m * (vjiz - 3*rv*rjiz)/r3;
			p[j].jx -= G*p[i].m * (vjix - 3*rv*rjix)/r3;
			p[j].jy -= G*p[i].m * (vjiy - 3*rv*rjiy)/r3;
			p[j].jz -= G*p[i].m * (vjiz - 3*rv*rjiz)/r3;
		    }
		}
		old_x  = new double[n];
		old_y  = new double[n];
		old_z  = new double[n];
		old_vx = new double[n];
		old_vy = new double[n];
		old_vz = new double[n];
		old_ax = new double[n];
		old_ay = new double[n];
		old_az = new double[n];
		old_jx = new double[n];
		old_jy = new double[n];
		old_jz = new double[n];
	    }
	}

	public void destroy()
	{
	    timer.stop();
	    timer=null;
	}

	public void actionPerformed (ActionEvent event)
	{
	    int i, j, k;
	    double r03 = r0*r0*r0;
	    for (k=0; k<warp; k++) {
	      t += dt;
	      theta += dt*rotrate;
              if (integrator == EULER) {
		for (j=0; j<n; j++) {
		    p[j].ax = 0; p[j].ay = 0; p[j].az = 0;
		    for (i=0; i<n0; i++) {
			if (i != j) {
			    double r3 = dist3(p[i], p[j]);
			    if (r3<r03) r3=r03;
			    p[j].ax -= G * p[i].m * (p[j].x - p[i].x)/r3;
			    p[j].ay -= G * p[i].m * (p[j].y - p[i].y)/r3;
			    p[j].az -= G * p[i].m * (p[j].z - p[i].z)/r3;
			}
		    }
		}
		for (j=0; j<n; j++) {
		    p[j].x += p[j].vx * dt;
		    p[j].y += p[j].vy * dt;
		    p[j].z += p[j].vz * dt;
		}
		for (j=0; j<n; j++) {
		    p[j].vx += p[j].ax * dt;
		    p[j].vy += p[j].ay * dt;
		    p[j].vz += p[j].az * dt;
		}
              } else
              if (integrator == MIDPOINT) {
		for (j=0; j<n; j++) {
		    p[j].x += p[j].vx * dt;
		    p[j].y += p[j].vy * dt;
		    p[j].z += p[j].vz * dt;
		}
		for (j=0; j<n; j++) {
		    p[j].ax = 0; p[j].ay = 0; p[j].az = 0;
		    for (i=0; i<n0; i++) {
			if (i != j) {
			    double r3 = dist3(p[i], p[j]);
			    if (r3<r03) r3=r03;
			    p[j].ax -= G * p[i].m * (p[j].x - p[i].x)/r3;
			    p[j].ay -= G * p[i].m * (p[j].y - p[i].y)/r3;
			    p[j].az -= G * p[i].m * (p[j].z - p[i].z)/r3;
			}
		    }
		}
		for (j=0; j<n; j++) {
		    p[j].vx += p[j].ax * dt;
		    p[j].vy += p[j].ay * dt;
		    p[j].vz += p[j].az * dt;
		}
              } else
              if (integrator == GASMODEL) {
		for (j=0; j<n; j++) {
		    p[j].x += p[j].vx * dt;
		    p[j].y += p[j].vy * dt;
		    p[j].z += p[j].vz * dt;
		}
		double x_cm=0, y_cm=0, z_cm=0;
		double mass;
		double tot_mass = 0;
		// double[] cum_mass = new double[n];
		Body3d cm = new Body3d();
		for (j=0; j<n; j++) {
		    mass = p[j].m;
		    x_cm += mass * p[j].x;
		    y_cm += mass * p[j].y;
		    z_cm += mass * p[j].z;
		    tot_mass += mass;
		}
		for (j=0; j<n; j++) {
		    cm.x /= tot_mass;
		    cm.y /= tot_mass;
		    cm.z /= tot_mass;
		}
		for (j=0; j<n; j++) {
		    p[j].cm_dist = dist(p[j],cm);
		}
		qkBodySort(p, 0, n-1);
		cum_mass[0] = 0;
		for (j=1; j<n; j++) {
		    cum_mass[j] = cum_mass[j-1] + p[j-1].m;
		}

		for (j=0; j<n; j++) {
		    double r3 = dist3(p[j], cm);
		    if (r3<r03) r3=r03;
		    p[j].ax = -G * cum_mass[j] * (p[j].x - cm.x)/r3;
		    p[j].ay = -G * cum_mass[j] * (p[j].y - cm.y)/r3;
		    p[j].az = -G * cum_mass[j] * (p[j].z - cm.z)/r3;
		}
		//System.err.print("r0 = "+r0);
		//System.err.print("min = "+p[0].cm_dist);
		//System.err.println("med = "+p[n/2].cm_dist);
		for (j=0; j<n; j++) {
		    p[j].vx += p[j].ax * dt;
		    p[j].vy += p[j].ay * dt;
		    p[j].vz += p[j].az * dt;
		}
              } else
              if (integrator == HERMITE) {
		double r02 = r0*r0;
		double far;

		for (i=0; i<n; i++) {
		  old_x[i]  = p[i].x;  old_y[i]  = p[i].y;  old_z[i]  = p[i].z;
		  old_vx[i] = p[i].vx; old_vy[i] = p[i].vy; old_vz[i] = p[i].vz;
		  old_ax[i] = p[i].ax; old_ay[i] = p[i].ay; old_az[i] = p[i].az;
		  old_jx[i] = p[i].jx; old_jy[i] = p[i].jy; old_jz[i] = p[i].jz;
		  p[i].x += p[i].vx*dt + p[i].ax*dt*dt/2 + p[i].jx*dt*dt*dt/6;
		  p[i].y += p[i].vy*dt + p[i].ay*dt*dt/2 + p[i].jy*dt*dt*dt/6;
		  p[i].z += p[i].vz*dt + p[i].az*dt*dt/2 + p[i].jz*dt*dt*dt/6;
		  p[i].vx += p[i].ax*dt + p[i].jx*dt*dt/2;
		  p[i].vy += p[i].ay*dt + p[i].jy*dt*dt/2;
		  p[i].vz += p[i].az*dt + p[i].jz*dt*dt/2;
		}
		for (i=0; i<n; i++) {
		    p[i].ax = p[i].ay = p[i].az = 0;
		    p[i].jx = p[i].jy = p[i].jz = 0;
		}
		for (i=0; i<n; i++) {
		    for (j=i+1; j<n; j++) {
			double rjix = p[j].x - p[i].x;
			double rjiy = p[j].y - p[i].y;
			double rjiz = p[j].z - p[i].z;
			double vjix = p[j].vx - p[i].vx;
			double vjiy = p[j].vy - p[i].vy;
			double vjiz = p[j].vz - p[i].vz;

			double r2 = rjix*rjix + rjiy*rjiy + rjiz*rjiz;
			if (r2<r02) {r2=r02; far=1;} else {far=0;}
			double r3 = r2*Math.sqrt(r2);
			double rv = (rjix*vjix + rjiy*vjiy + rjiz*vjiz)/r2;

			p[i].ax += G*p[j].m * rjix/r3;
			p[i].ay += G*p[j].m * rjiy/r3;
			p[i].az += G*p[j].m * rjiz/r3;
			p[j].ax -= G*p[i].m * rjix/r3;
			p[j].ay -= G*p[i].m * rjiy/r3;
			p[j].az -= G*p[i].m * rjiz/r3;
			p[i].jx += G*p[j].m * (vjix - 3*far*rv*rjix)/r3;
			p[i].jy += G*p[j].m * (vjiy - 3*far*rv*rjiy)/r3;
			p[i].jz += G*p[j].m * (vjiz - 3*far*rv*rjiz)/r3;
			p[j].jx -= G*p[i].m * (vjix - 3*far*rv*rjix)/r3;
			p[j].jy -= G*p[i].m * (vjiy - 3*far*rv*rjiy)/r3;
			p[j].jz -= G*p[i].m * (vjiz - 3*far*rv*rjiz)/r3;
		    }
		}
		for (i=0; i<n; i++) {
		    p[i].vx = old_vx[i] + (old_ax[i] + p[i].ax)*dt/2
			                + (old_jx[i] - p[i].jx)*dt*dt/12;
		    p[i].vy = old_vy[i] + (old_ay[i] + p[i].ay)*dt/2
			                + (old_jy[i] - p[i].jy)*dt*dt/12;
		    p[i].vz = old_vz[i] + (old_az[i] + p[i].az)*dt/2
			                + (old_jz[i] - p[i].jz)*dt*dt/12;
		    p[i].x = old_x[i] + (old_vx[i] + p[i].vx)*dt/2
			              + (old_ax[i] - p[i].ax)*dt*dt/12;
		    p[i].y = old_y[i] + (old_vy[i] + p[i].vy)*dt/2
			              + (old_ay[i] - p[i].ay)*dt*dt/12;
		    p[i].z = old_z[i] + (old_vz[i] + p[i].vz)*dt/2
			              + (old_az[i] - p[i].az)*dt*dt/12;
		}
	      }
	    }
	    double kinetic=0, potential=0;
	    for (j=0; j<n; j++) {
		kinetic += p[j].m*
		          (p[j].vx*p[j].vx+p[j].vy*p[j].vy+p[j].vz*p[j].vz);
		for (i=0; i<n; i++) {
		    if (i != j) {
			double r1 = dist(p[i], p[j]);
			potential -= G*p[i].m*p[j].m/r1;
		    }
		}
	    }
	    paint(getGraphics());
	}

	public void setAnimEffects(double rotrate, double dt, 
				   int warp, double zoom)
	{
	    this.rotrate = rotrate;
	    this.dt = dt;
	    this.warp = warp;
	    this.zoom = zoom;
	}

	public void setTrails(boolean trails)
	{
	    this.trails = trails;
	    if (trails) firstpass = true;
	}

	double dist( Body3d pi, Body3d pj )
	{
	    double r;
	    double xdiff = pi.x - pj.x;
	    double ydiff = pi.y - pj.y;
	    double zdiff = pi.z - pj.z;
	    r = Math.sqrt(xdiff*xdiff + ydiff*ydiff + zdiff*zdiff);
	    return r;
	}

	double dist3( Body3d pi, Body3d pj )
	{
	    double r = dist(pi, pj);
	    return r*r*r;
	}

	void qkBodySort(Body3d[] p, int left, int right) 
	{
	    int i, last;

	    if (left >= right) return;

	    BodySwap(p, left, (left+right)/2);
	    last = left;
	    for (i = left+1; i <= right; i++) 
		if (p[i].cm_dist < p[left].cm_dist)
		    BodySwap(p, ++last, i);
	    BodySwap(p, left, last);
	    qkBodySort(p, left, last-1);
	    qkBodySort(p, last+1, right);
	}

	void BodySwap(Body3d[] p, int i, int j)
	{
	    Body3d temp;

	    temp= p[i];
	    p[i] = p[j];
	    p[j] = temp;
	}

	/*
	public void repaint () { paint(); }
	public void update (Graphics g) { paint(); }
	public void paint (Graphics g) {paint();}
	*/

	boolean trails = false;
	boolean firstpass = true;
	public void paint (Graphics g) 
	{
	    int i, j;
	    double r;
	    double costh=Math.cos(theta), sinth=Math.sin(theta);
	    double kin=0, pot=0;
	    DecimalFormat fmt = new DecimalFormat("###,##0.###");

System.err.println("trails = "+trails);
	  if (trails) {
System.err.println("firstpass = "+firstpass);
	   if (firstpass) {
	    firstpass = false;
	    GL.ginit(getGraphics(), getSize().width, getSize().height, this,false);
	    GL.ortho2( -zoom, zoom, -zoom, zoom);
	    GL.color(Color.black);
	    GL.clear();
	    // GL.swapbuffers();
	   } else {
	    // GL.ginit(g, getSize().width, getSize().height, this,true);
	    GL.ortho2( -zoom, zoom, -zoom, zoom);
	    for (j=0; j<n; j++) {
		GL.color(p[j].col);
		// g.setXORMode(p[j].col);
		r = p[j].rad;
		GL.pnt2(costh*p[j].x+sinth*p[j].y, 
			-sinth*p[j].x+costh*p[j].y);
	    }
	    // GL.swapbuffers();
	   }
	  } else {
	    GL.ginit(g, getSize().width, getSize().height, 
			    this,true);
	    GL.ortho2( -zoom, zoom, -zoom, zoom);

	    GL.color(Color.black);
	    GL.clear();

	    GL.color(Color.white);

	    for (j=0; j<n; j++) {
		GL.color(p[j].col);
		GL.pnt2(costh*p[j].x+sinth*p[j].y, 
			-sinth*p[j].x+costh*p[j].y);
		/* GL.circf(costh*p[j].x+sinth*p[j].y, 
			-sinth*p[j].x+costh*p[j].y, p[j].rad); */
	    }

	    /*
	    double r03 = r0*r0*r0;
	    kin = pot = 0;
	    for (j=0; j<n; j++) {
		kin += p[j].m*(p[j].vx*p[j].vx 
			     + p[j].vy*p[j].vy
			     + p[j].vz*p[j].vz)/2;
		for (i=j+1; i<n; i++) {
		    double r1 = dist(p[i], p[j]);
		    if (r1 > r0) {
			pot -= G*p[i].m*p[j].m/r1;
		    } else {
			pot -= 3*G*p[i].m*p[j].m/(2*r0)
			       + G*p[i].m*p[j].m*r1*r1/(2*r03);
		    }
		}
	    }
	    */

	    GL.color(Color.white);
	    GL.drawString("Time   = "+fmt.format(t), 
		    		-0.98*zoom,  0.98*zoom, "LEFT", "TOP");

	    if (p[1].m == 9.377e-8 && p[2].m == 3.366e-7) {
	        GL.drawString("Epimetheus - Janus = "
		                +(dist(p[0],p[1])-dist(p[0],p[2])), 
		    		-0.98*zoom, -0.98*zoom, "LEFT", "BOTTOM");
	    }
	    GL.swapbuffers();
	  }
	}
}
