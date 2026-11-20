package astro.galaxy;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.StringJoiner;
import javax.swing.SwingUtilities;

public final class JavaReference extends ClusterCanvas {
    @Override public void paint(Graphics graphics) {}

    private static void record(String[] args, Random random) {
        random.setSeed(524L);
        Cluster app = new Cluster();
        app.init();
        app.cc = new JavaReference();
        app.SELECTION.setSelectedItem(args[0]);
        app.INTEGRATOR.setSelectedItem(args[1]);
        app.N.setText(args[2]);
        app.actionPerformed(new ActionEvent(app.START, ActionEvent.ACTION_PERFORMED, "Start"));
        ClusterCanvas canvas = app.cc;
        canvas.timer.stop();

        int previous = 0;
        StringJoiner trace = new StringJoiner(",", "[", "]");
        for (int index = 3; index < args.length; index++) {
            int step = Integer.parseInt(args[index]);
            if (step > previous) {
                canvas.warp = step - previous;
                canvas.actionPerformed(null);
            }
            StringJoiner state = new StringJoiner(",", "[", "]");
            for (Body3d body : canvas.p) {
                int rgb = body.col == null ? -1 : body.col.getRGB() & 0xFFFFFF;
                state.add("[" + body.m + "," + rgb + "," + body.x + "," + body.y + "," + body.z
                    + "," + body.vx + "," + body.vy + "," + body.vz
                    + "," + body.ax + "," + body.ay + "," + body.az
                    + "," + body.jx + "," + body.jy + "," + body.jz + "]");
            }
            trace.add("[" + step + "," + canvas.t + "," + canvas.theta + "," + state + "]");
            previous = step;
        }
        System.out.println(trace);
    }

    public static void main(String[] args) throws ReflectiveOperationException, InterruptedException {
        Field field = Class.forName("java.lang.Math$RandomNumberGeneratorHolder")
            .getDeclaredField("randomNumberGenerator");
        field.setAccessible(true);
        Random random = (Random) field.get(null);
        SwingUtilities.invokeAndWait(() -> record(args, random));
        System.exit(0);
    }
}
