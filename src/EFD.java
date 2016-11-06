/**
 * EFD This class implements the Elliptic Fourier Descriptor EFD that is
 * described (and implemented in Matlab code) in REF1 (see chapter 7). The EFD
 * provides a normalised set of coefficients that are rotation, translation and
 * scale invariant. The first coefficient relates to the centroid of the input
 * shape before the EFD is computed and can be ignored. The second FD
 * coefficient relates to a circle circumscribed about the centroid before the
 * EFD computation. After the EDF computation the second EFD is always 2 and can
 * be ignored. That leaves the remaining EFD coefficients for use in comparing
 * shapes.
 * <p>
 * The core of the class is modelled closely after Thomas Boudier's Fourier_ and
 * Fourier classes. See REF2.
 * <p>
 * REF1 adds a very simple set of descriptors derived from those computed in
 * Boudier's - the so called "Elliptical Fourier Descriptors". Kathy Clawson
 * privately shared similar code and gently pushed the author toward this type
 * of normalisation.
 * <p>
 * REF1: Feature Extraction and Image Processing, 2nd Ed, by Mark Nixon and
 * Alberto Aguado, Academic Press 2008, ISBN 978-0-1237-2538-7
 * <p>
 * REF2: Thomas Boudier's Fourier_ and Fourier classes can be found following
 * the links at see http://rsb.info.nih.gov/ij/plugins/index.html#more. They are
 * also found on the ImageJ docuwiki. http://imagejdocu.tudor.lu/doku.php
 * <p>
 * modified version taken from Author: Stelian Nastase
 * Source: https://github.com/steliann/objectj/blob/56e928f2890d92cbe3177fd352c7889b8d7dc8ab/src/ij/plugin/filter/EllipticFD.java
 */

public class EFD {
    private double[] x;        // The x coordinates
    private double[] y;            // The y coordinates
    private int m;                // The number of points on input contour
    public int nFD;                // The number of FD coefficients
    public double[] ax, ay, bx, by; // The Fourier Descriptors
    public double[] efd;        // The Elliptic Fourier Descriptors
    public double[] efdNormalised; // The normalised Elliptic Fourier Descriptors

    /**
     * Constructor with the number of descriptors
     *
     * @param x the x coordinates of the contour
     * @param y the y coordinates of the contour
     * @param m the number of descriptors to compute, in not provided then the
     *          number of descriptors is set to half the number of contour
     *          points
     */
    public EFD(double[] x, double[] y, int n) {
        this.x = x;
        this.y = y;
        this.nFD = n;
        this.m = x.length;
        computeEllipticFD();
    }

    /**
     * Computes the Fourier and Elliptic Fourier Descriptors
     */
    private void computeEllipticFD() {
        //The fourier descriptors
        ax = new double[nFD];
        ay = new double[nFD];
        bx = new double[nFD];
        by = new double[nFD];

        //Preconfigure some values
        double t = 2.0 * Math.PI / m;
        double p = 0.0;
        double twoOverM = 2.0 / m;
        //Step through each FD
        for (int k = 0; k < nFD; k++) {
            //And for each point
            for (int i = 0; i < m; i++) {
                p = k * t * i;
                ax[k] += x[i] * Math.cos(p);
                bx[k] += x[i] * Math.sin(p);
                ay[k] += y[i] * Math.cos(p);
                by[k] += y[i] * Math.sin(p);
            }//i-loop through the number of points

            ax[k] *= twoOverM;
            bx[k] *= twoOverM;
            ay[k] *= twoOverM;
            by[k] *= twoOverM;
        }//k-loop through the number of coeffs

        //Compute the elliptic fourier descriptors as per REF2
        efd = new double[nFD];
        int first = 1; // index of the normalization values

        //Precompute the denominators/magnitude of semi major axis
        double denomA = (ax[first] * ax[first]) + (ay[first] * ay[first]);
        double denomB = (bx[first] * bx[first]) + (by[first] * by[first]);
        for (int k = 0; k < nFD; k++) {
            efd[k] = Math.sqrt((ax[k] * ax[k] + ay[k] * ay[k]) / denomA)
                    + Math.sqrt((bx[k] * bx[k] + by[k] * by[k]) / denomB);
        }// k-loop for efd

        //Leave out first two descriptors for invariance
        efdNormalised = new double[nFD - 2];
        for (int i = 0; i < efdNormalised.length; i++) {
            efdNormalised[i] = efd[i + 2];
        }

        /**Print EFD results for user input
         System.out.println("");
         System.out.println("______VALUES FOR USER INPUT PLANT_ "+efdNormalised.length+"___________");
         for (int i = 0; i < efdNormalised.length; i++) {
         System.out.print(efdNormalised[i] + "\t");
         }//print results
         */
    }// computeEllipticFD


    /**
     * Returns the polygon computed using the FD coefficients
     *
     * @return a nx2 element array of x,y pairs that is the same length as the
     * input polygon
     */
    public int[][] createPolygonInt() {
        double p = 0.0;
        double[][] xy = new double[m][2];
        int[][] ixy = new int[m][2];
        double t = 2.0 * Math.PI / m;
        for (int i = 0; i < m; i++) {
            xy[i][0] = ax[0] / 2.0;
            xy[i][1] = ay[0] / 2.0;

            for (int k = 1; k < nFD; k++) {
                p = t * k * i;
                xy[i][0] += ax[k] * Math.cos(p) + bx[k] * Math.sin(p);
                xy[i][1] += ay[k] * Math.cos(p) + by[k] * Math.sin(p);
                ixy[i][0] = (int) xy[i][0];
                ixy[i][1] = (int) xy[i][1];

            } // k-loop through the FDs
        }// i-loop through the points
        return ixy;
    }// createPolygon

    public double[] getEfdNormalised() {
        return efdNormalised;
    }
}