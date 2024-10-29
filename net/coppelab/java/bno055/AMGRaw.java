package net.coppelab.java.bno055;

import net.coppelab.java.geo.Point3D;

public class AMGRaw {
    public Point3D acc;
    public Point3D mag;
    public Point3D gyr;

    public AMGRaw(){
        acc = new Point3D();
        mag = new Point3D();
        gyr = new Point3D();
    }
}
