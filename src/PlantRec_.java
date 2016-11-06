/**
 * PlantRec_ This is the main class for the plant leaf recognition plugin for ImageJ.
 * It does pre-configuration of RGB input image and
 * extracts contour for EFD with help of Particle Analyser.
 *
 * @author Karina Becker
 */

import fiji.threshold.Auto_Local_Threshold;
import ij.*;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.process.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class PlantRec_ {
    private ImagePlus imp;
    private ResultsTable rt;

    /**
     * Main class starts PlantRec_ constructor
     */
    public static void main(String[] args) {
        new PlantRec_();
    }

    /**
     * Constructor for new PlantRec_ ImageJ plugin
     */
    public PlantRec_() {
        String filePath = "file/leaf.JPG";
        System.out.println("Fielpath: " + filePath);
        IJ.open(filePath);
        imp = WindowManager.getCurrentImage();  // Open image

        double[] huMoments = getHuMoments();    // Get array with Hu moments
        preparation();                          // Turn image to binary
        Polygon polygon = findLargestArea();    // Find polygon for largest area
        double[] efd = getEFD(polygon);         // Get array with EFDs
        new KNNPlant(efd, huMoments, 5);        // Put data into comparator with number of k nearest neighbours
        //writeToDatabase(efd, huMoments);		// Write to database
    }

    /**
     * Preparation of input image,
     * Get image, set up processors, turn to binary image with clear outline
     */
    private void preparation() {

        ImageConverter ic = new ImageConverter(imp);
        Auto_Local_Threshold autoLocThres = new Auto_Local_Threshold();

        //Preparation of image
        ic.convertToGray8();                            //Grayscale
        IJ.run(imp, "Gaussian Blur...", "sigma=1.5");    //Blurring
        autoLocThres.exec(imp, "Bernsen", 7, 0, 0, true); //Thresholding with Bernsen
        IJ.run("Convert to Mask");                        //Converting to mask
        IJ.run(imp, "Dilate", "");                        //Dilation (adding on top)
        IJ.run(imp, "Close-", "");                        //Closing operation (join narrow isthumes)
        IJ.run(imp, "Fill Holes", "");                    //Fill holes
    }


    /**
     * Find largest contour to create polygon from it
     * finds contours with ParticleAnalyzer and gets results table.
     * gets largest contour and then polygon of it.
     *
     * @return polygon to get outline contour from
     */
    private Polygon findLargestArea() {
        //Find contours with Particle Analyser and get results table
        rt = ResultsTable.getResultsTable();
        if (rt == null) {
            rt = new ResultsTable();
        }
        RoiManager manager = RoiManager.getInstance();
        if (manager == null) {
            manager = new RoiManager();
        }
        ParticleAnalyzer analyser = new ParticleAnalyzer(
                ParticleAnalyzer.ADD_TO_MANAGER //add to RoiManager
                        + ParticleAnalyzer.SHOW_OUTLINES + ParticleAnalyzer.IN_SITU_SHOW, //show outlines and replace original image
                Measurements.AREA + Measurements.SHAPE_DESCRIPTORS, //get area and shape descriptors
                rt, 20000, Double.MAX_VALUE, 0, 1);
        analyser.analyze(imp);
        rt.updateResults();
        rt.show("Results");
        imp.show();

        //Loop through indices of individual areas to find largest area
        Roi[] contour = manager.getRoisAsArray();
        double largest_area = 0;
        int largest_area_index = 0;
        if (contour.length > 0) {
            for (int i = 0; i < contour.length; i++) {
                double a = rt.getValue("Area", i);
                if (a > largest_area) {
                    largest_area = a;
                    largest_area_index = i;
                }
            }
            //Test if image quality sufficient/ correct shape detected
            if (rt.getValue("AR", largest_area_index) > 3.0) {
                System.err.println("Please try again with another image. Leaf could not be detected.");
            }
        } else {
            System.err.println("Error. Empty contour array");
        }

        //Get polygon of largest shape
        Polygon polygon = contour[largest_area_index].getPolygon();
        return polygon;
    }


    /**
     * get EFDs
     * draw outline of it in new JFrame
     *
     * @param polygon to get outline contour from
     * @return efd double array of EFDs
     */
    private double[] getEFD(Polygon polygon) {
        //Get outline points from polygon
        int[] xInt = polygon.xpoints;
        int[] yInt = polygon.ypoints;

        //Change to double
        double[] xDouble = new double[xInt.length];
        for (int i = 0; i < xDouble.length; ++i) {
            xDouble[i] = (double) xInt[i];
        }

        double[] yDouble = new double[yInt.length];
        for (int i = 0; i < yDouble.length; ++i) {
            yDouble[i] = (double) yInt[i];
        }

        //Elliptic Fourier transform
        EFD efdFunct = new EFD(xDouble, yDouble, 30);
        double[] efd = efdFunct.getEfdNormalised();

        //Display EFD outline in a new JFrame
        int[][] polygonDraw = efdFunct.createPolygonInt();
        for (int i = 0; i < polygonDraw.length; ++i) {
            xInt[i] = polygonDraw[i][0];
            yInt[i] = polygonDraw[i][1];
        }
        drawPolygon(xInt, yInt);

        return efd;
    }

    /**
     * call ImageMoments class, get Hu moments
     *
     * @return huMoments double array with the seven Hu moments
     */
    private double[] getHuMoments() {
        BufferedImage bufImg = imp.getBufferedImage();
        ImageMoments imgMoments = new ImageMoments(bufImg);
        double[] huMoments = imgMoments.getAllMoments();

        return huMoments;
    }

    /**
     * Draw Polygon received from EFDs
     *
     * @param xInt xVariables of contour
     * @param yInt yVariables of contour
     */
    private void drawPolygon(int[] xInt, int[] yInt) {
        JFrame showEFDpoly;
        final Polygon poly;

        showEFDpoly = new JFrame();
        showEFDpoly.setResizable(true);
        showEFDpoly.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        poly = new Polygon(xInt, yInt, xInt.length);

        JPanel p = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLUE);
                g.drawPolygon(poly);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(600, 800);
            }
        };
        showEFDpoly.add(p);
        showEFDpoly.pack();
        showEFDpoly.setVisible(true);
    }

    /**
     * Write to database
     *
     * @param efd
     * @param huMoments
     */
    private void writeToDatabase(double[] efd, double[] huMoments) {
        int largest_area_index = 0; //Get real index for use
        String COMMA_DELIMITER = ",";
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("file/data.csv", true);

            fileWriter.append("9");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("1");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(rt.getValue("Area", largest_area_index)));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(rt.getValue("Circ.", largest_area_index)));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(rt.getValue("Round", largest_area_index)));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(rt.getValue("Solidity", largest_area_index)));
            fileWriter.append(COMMA_DELIMITER);
            for (double value : efd) {
                fileWriter.append(String.valueOf(value));
                fileWriter.append(COMMA_DELIMITER);
            }
            for (double value : huMoments) {
                fileWriter.append(String.valueOf(value));
                fileWriter.append(COMMA_DELIMITER);
            }
            fileWriter.append("\n");

        } catch (Exception e) {
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();

        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }
}