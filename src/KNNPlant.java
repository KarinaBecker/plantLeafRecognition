/**
 * KNNPlant This class implements a function to find k nearest neighbours (KNN)
 * of a new instance of a plant's leaf
 * It uses majority vote for classification
 *
 * @author Karina Becker
 * <p>
 * Strongly modified and extended version of Dr Noureddin Sadawis KNN class, Copyright (C) 2014
 * Source: https://raw.githubusercontent.com/nsadawi/KNN/master/KNN.java
 * Webpage: http://people.brunel.ac.uk/~csstnns/
 * Youtube: https://www.youtube.com/user/DrNoureddinSadawi
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.opencsv.CSVReader;

public class KNNPlant {
    private List<Result> resultListEfd;
    private List<Result> resultListHu;
    private List<Result> resultList;
    private double[] efdUser;
    private double[] huUser;
    public String[] namesDatabase;
    public double[][] efdDatabase;
    public double[][] huDatabase;

    /**
     * Constructor  KNNPlant to find nearest neighbour of input leaf
     * and get majority vote to determinate species
     *
     * @param efd       EFD double array derived from user input leaf
     * @param huMoments double array with huMoments
     * @param k         number of closest matches to consider
     */
    public KNNPlant(double[] efdUser, double[] huUser, int k) {
        this.efdUser = efdUser;
        this.huUser = huUser;
        resultListEfd = new ArrayList<Result>();
        resultListHu = new ArrayList<Result>();
        resultList = new ArrayList<Result>();

        //read from database
        readDatabase();

        //calculate Euclidean distance between efd or Hu moments and database
        double[] efdResults = calculateShepardsDistance(calculateEuclideanDistance(efdUser, efdDatabase, 1));
        double[] huResults = calculateShepardsDistance(calculateEuclideanDistance(huUser, huDatabase, 0.00000002));

        //array for combination of methods
        double[] combinedResults = new double[efdResults.length];
        for (int i = 0; i < combinedResults.length; i++) {
            combinedResults[i] = efdResults[i] + huResults[0];
        }

        //add results to result list
        for (int i = 0; i < namesDatabase.length; i++) {
            resultListEfd.add(new Result(namesDatabase[i], efdResults[i]));
            resultListHu.add(new Result(namesDatabase[i], huResults[i]));
            resultList.add(new Result(namesDatabase[i], combinedResults[i]));
        }

        //sort results
        Collections.sort(resultListEfd, new DistanceComparator());
        Collections.sort(resultListHu, new DistanceComparator());
        Collections.sort(resultList, new DistanceComparator());

        //Get classes of k nearest instances (species names) from the list into an array
        String[] speciesClosestMatchEfd = new String[k];
        String[] speciesClosestMatchHu = new String[k];
        String[] speciesClosestMatch = new String[k];
        System.out.println("\n_________K = " + k + " CLOSTEST MATCHES__________");
        for (int i = 0; i < k; i++) {
            //speciesClosestMatchEfd[i] = resultListEfd.get(i).speciesName;
            //System.out.println(resultListEfd.get(i).speciesName+ "\t" + resultListEfd.get(i).distance);
            //speciesClosestMatchHu[i] = resultListHu.get(i).speciesName;
            //System.out.println(resultListHu.get(i).speciesName+ "\t" + resultListHu.get(i).distance);
            speciesClosestMatch[i] = resultList.get(i).speciesName;
            System.out.println(resultList.get(i).speciesName + "\t" + resultList.get(i).distance);
        }

        //Get majority vote
        //String majClassEfd = findMajorityClass(speciesClosestMatchEfd, resultListEfd, k);
        //String majClassHu = findMajorityClass(speciesClosestMatchHu, resultListHu, k);
        String majClass = findMajorityClass(speciesClosestMatch, resultList, k);
        System.out.println("\n_________CLASSIFICATION RESULT___________");
        //System.out.println("EFD Class of new instance is: " + majClassEfd);
        //System.out.println("Hu Class of new instance is: " + majClassHu);
        System.out.println("Combined Class of new instance is: " + majClass);
    }


    /**
     * Read input from database
     */
    private void readDatabase() {
        String filePath = "file/data.csv";
        CSVReader reader;
        int lineCount = 27;
        namesDatabase = new String[lineCount];
        double[] databaseCircularity = new double[lineCount];
        double[] databaseAspectRatio = new double[lineCount];
        double[] databaseRoundness = new double[lineCount];
        double[] databaseSolidity = new double[lineCount];
        efdDatabase = new double[lineCount][efdUser.length];
        huDatabase = new double[lineCount][huUser.length];
        try {
            reader = new CSVReader(new FileReader(filePath), ',', '"', 1);
            String[] nextLine;
            int row = 0;
            while ((nextLine = reader.readNext()) != null) {
                namesDatabase[row] = nextLine[0];
                databaseCircularity[row] = Double.parseDouble(nextLine[3]);
                databaseAspectRatio[row] = Double.parseDouble(nextLine[4]);
                databaseRoundness[row] = Double.parseDouble(nextLine[5]);
                databaseSolidity[row] = Double.parseDouble(nextLine[6]);
                for (int i = 7; i < (nextLine.length - 7); ++i) {
                    efdDatabase[row][(i - 7)] = Double.parseDouble(nextLine[i]);
                }
                for (int i = 34; i < nextLine.length; ++i) {
                    huDatabase[row][(i - 34)] = Double.parseDouble(nextLine[i]);
                }
                row++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Calculates the Euclidean distance of user input to database entries, adds to results List
     *
     * @param userInput     descriptors from user input leaf
     * @param databaseInput descriptors from database
     * @return distances    distances between descriptors
     */
    private double[] calculateEuclideanDistance(double[] userInput, double[][] databaseInput, double factor) {
        double[] distances = new double[namesDatabase.length];

        for (int i = 0; i < namesDatabase.length; ++i) {
            double dist = 0.0;
            for (int j = 0; j < databaseInput[i].length; ++j) {
                dist += Math.pow(databaseInput[i][j] - userInput[j], 2);
            }
            double distance = (Math.sqrt(dist)) * factor; //factor to equalise
            distances[i] = distance;
            //System.out.println("Euclidean distance " + namesDatabase[i] +" at "+i+": "+distance);
        }
        return distances;
    }

    /**
     * Calculates Shepard's interpolation for user input and database leaf
     * loops through all database samples
     * adds results to resultsList
     *
     * @param userInput     descriptors from user input leaf
     * @param databaseInput descriptors from database
     * @return distances    distances between descriptors
     */
    private double[] calculateShepardsDistance(double[] eucDistance) {
        double[] distances = new double[eucDistance.length];

        for (int j = 0; j < eucDistance.length; j++) {
            double weight = 0.0, sum = 0.0;
            double distance = 0;
            for (int i = 0; i < eucDistance.length; i++) {
                double r = Math.abs(eucDistance[i]);    //d(x, xi)
                if (r == 0.0) {
                    distance = eucDistance[i];
                }
                double wi = 1 / (Math.pow(r, 2));
                weight += wi;
                sum += wi * eucDistance[i];
            }
            distance = sum / weight;
            distances[j] = distance;
            //System.out.println("Interpolated distance at "+j+": "+distance);
        }
        return distances;
    }

    /**
     * Returns the majority value in an array of strings
     * majority value is the most frequent value (the mode)
     * multiple majority values: which species has smaller distances in selection
     *
     * @param namesKList an array of strings of species names
     * @param k          the number of selected samples
     * @return the String with the smallest distance
     */
    private String findMajorityClass(String[] namesKList, List<Result> resultList, int k) {
        //add the String array to a HashSet to get unique String values and convert the HashSet back to array
        Set<String> h = new HashSet<String>(Arrays.asList(namesKList));
        String[] uniqueValues = h.toArray(new String[0]);

        //loop through unique strings and count how many times they appear in original array
        int[] counts = new int[uniqueValues.length];
        for (int i = 0; i < uniqueValues.length; i++) {
            for (int j = 0; j < namesKList.length; j++) {
                if (namesKList[j].equals(uniqueValues[i])) {
                    counts[i]++;
                }
            }
        }

        //maximum number of occurrence of a species
        int max = counts[0];
        for (int counter = 1; counter < counts.length; counter++) {
            if (counts[counter] > max) {
                max = counts[counter];
            }
        }

        //freq (frequency) of max (maximum number of occurrences)
        //max will appear at least once in counts, so freq = 1 at minimum after loop
        int freq = 0;
        for (int counter = 0; counter < counts.length; counter++) {
            if (counts[counter] == max) {
                freq++;
            }
        }

        //index of most freq value if only one mode
        int index = -1;
        if (freq == 1) {
            for (int counter = 0; counter < counts.length; counter++) {
                if (counts[counter] == max) {
                    index = counter;
                    break;
                }
            }
            System.out.println("One majority class, index is: " + index);
            return uniqueValues[index];

        } else {
            //find index of species with max occurrence
            int[] ix = new int[freq];//array of indices of modes
            System.out.println("multiple majority classes: " + freq + " classes");
            int ixi = 0;
            for (int counter = 0; counter < counts.length; counter++) {
                if (counts[counter] == max) {
                    ix[ixi] = counter;        //save index of each max count value
                    ixi++;                    // increase index of ix array
                }
            }

            //get average distance for each species with max occurrence
            double[] averageDist = new double[freq];
            String[] averageDistSpecies = new String[freq];

            for (int j = 0; j < averageDist.length; ++j) {
                averageDistSpecies[j] = uniqueValues[ix[j]];
                int count = 0;
                for (int i = 0; i < k; ++i) {    //first k = 5 of sorted result list
                    if (resultList.get(i).speciesName.equals(averageDistSpecies[j])) {
                        averageDist[j] += resultList.get(i).distance;
                        count++;
                    }
                }
                averageDist[j] = (averageDist[j] / count);
            }

            //determine species with minimum average distance
            double minAverageDist = Double.MAX_VALUE;
            int minAverageDistIndex = 0;
            for (int i = 0; i < averageDist.length; i++) {
                if (averageDist[i] < minAverageDist) {
                    minAverageDist = averageDist[i];
                    minAverageDistIndex = i;
                }
            }

            System.out.println("class with index " + minAverageDistIndex + ": " + averageDistSpecies[minAverageDistIndex]);
            return averageDistSpecies[minAverageDistIndex];
        }//else
    }//method majority class


    /**
     * simple helper classes for results and comparison
     */

    //simple class to model results (name and results)
    static class Result {
        String speciesName;
        double distance;

        public Result(String speciesName, double distance) {
            this.speciesName = speciesName;
            this.distance = distance;
        }
    }

    //simple comparator class used to compare results via distances
    static class DistanceComparator implements Comparator<Result> {
        @Override
        public int compare(Result a, Result b) {
            return a.distance < b.distance ? -1 : a.distance == b.distance ? 0 : 1;
        }
    }
}