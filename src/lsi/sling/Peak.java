package lsi.sling;

import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.spectrum.ISpectrum;
import umich.ms.fileio.exceptions.FileParsingException;

import java.util.ArrayList;

/**
 * Represents a group of LocalPeaks which together form a peak which is significant across the entire dataset.
 * (This is the peak which will later be integrated and analysed)
 * <p>
 * All of the class member fields are private to help ensure reproducibility (They can't be changed which ensures
 * that you can read them later to find the same values which were used to create the object)
 *
 * @author Adithya Diddapur
 */
public class Peak {

    private ArrayList<LocalPeak> intensityScanPairs;
    private ArrayList<LocalPeak> intensityScanPairsBelow;
    private ArrayList<Integer> pointsOfInflection;
    private double meanMZ;
    private double tolerance;
    private double threshold; //used to define noise to signal ratio
    private int startingPointIndex; //index of the max peak within the ArrayList (max intensity)
    private double startingPointRT;
    private double startingPointIntensity;

    /**
     * Constructor which creates a new Peak. This class is designed so that in normal use, a user only every needs
     * to call the constructor which acts as a wrapper for everything.
     * i.e. Once called, the constructor initialises all the relevant variables and runs the recursive algorithm to
     * find the edges of the peak (where it stops being significant) Note: at the moment significance is determined by
     * a constant (thresh).
     *
     * @param scanList      An ArrayList of all the scans (as IScan objects)
     * @param startingPoint The LocalPeak to use as the starting point for the larger peak
     * @param tol           The tolerance (in ppm) to account for the jitter
     * @param thresh        The threshold to determine the end points of the peak
     * @throws FileParsingException Thrown when the recursive loops try to access the scan data
     */
    public Peak(ArrayList<IScan> scanList, LocalPeak startingPoint, double tol, double thresh) throws FileParsingException {
        startingPointRT = startingPoint.getRT();
        startingPointIntensity = startingPoint.getIntensity();
        intensityScanPairs = new ArrayList<>();
        intensityScanPairsBelow = new ArrayList<>();
        pointsOfInflection = new ArrayList<>();
        tolerance = tol;
        threshold = thresh;
        meanMZ = startingPoint.getMZ();
        System.out.println(createPeakBelow(scanList, meanMZ, 400, startingPoint.getScanNumber() - 1));
        for (int i = intensityScanPairsBelow.size(); i > 0; i--) {
            intensityScanPairs.add(intensityScanPairsBelow.get(i - 1));
        }
        intensityScanPairs.add(startingPoint);
        if (startingPoint.getScanNumber() + 1 < scanList.size()) {
            System.out.println(createPeakAbove(scanList, averageMZ(), 400, startingPoint.getScanNumber() + 1));
        }
        startingPointIndex = intensityScanPairsBelow.size();
        findLocalMinima();
    }

    /**
     * Recursive loop which 'looks' at the scans above the starting points (higher RT) to find where the peak ends.
     *
     * @param scanList  An ArrayList of all the scans (as IScan objects)
     * @param average   The average m/z value of the peak. This is used to account for the jitter and is re-calculated
     *                  once for each recursive iteration.
     * @param toler     The tolerance (in ppm) to account for the jitter
     * @param increment Used to iterate through one of the loops is a semi-recursive manner
     * @return the integer 1 if the operation was carried out successfully. The integer 2 is returned if the scans
     * reach the end of the file
     * @throws FileParsingException Thrown when the recursive loops try to access the scan data
     */
    private int createPeakAbove(ArrayList<IScan> scanList, double average, double toler, int increment) throws FileParsingException {
        ISpectrum temp = scanList.get(increment).fetchSpectrum();
        if (temp.findMzIdxsWithinPpm(average, toler) != null) {
            LocalPeak tempPeak = maxIntWithinTol(temp, average, toler, increment, scanList.get(increment).getRt());
            if (tempPeak.getIntensity() > threshold) {
                int tempInt = Main.peakList.indexOf(tempPeak);
                if (tempInt == -1) {
                    tempPeak.setIsUsed();
                    tempInt = Main.peakList.indexOf(tempPeak);
                }
                tempPeak.setIsUsed();
                intensityScanPairs.add(tempPeak);
                Main.peakList.get(tempInt).setIsUsed();
                System.out.println(increment);
                System.out.println(increment < scanList.size() - 2);
                if (increment < scanList.size() - 2) {
                    return createPeakAbove(scanList, averageMZ(), toler, increment + 1);
                } else {
                    return 2;
                }
            }
        }
        return 1;
    }

    /**
     * Recursive loop which 'looks' at the scans below the starting points (lower RT) to find where the peak ends.
     *
     * @param scanList  An ArrayList of all the scans (as IScan objects)
     * @param average   The average m/z value of the peak. This is used to account for the jitter and is re-calculated
     *                  once for each recursive iteration.
     * @param toler     The tolerance (in ppm) to account for the jitter
     * @param increment Used to iterate through one of the loops is a semi-recursive manner
     * @return the integer 1 if the operation was carried out successfully
     * @throws FileParsingException Thrown when the recursive loops try to access the scan data
     */
    private int createPeakBelow(ArrayList<IScan> scanList, double average, double toler, int increment) throws FileParsingException {
        ISpectrum temp = scanList.get(increment).fetchSpectrum();
        if(temp.findMzIdxsWithinPpm(average,toler)!=null) {
            LocalPeak tempPeak = maxIntWithinTol(temp, average, toler, increment, scanList.get(increment).getRt());
            if (tempPeak.getIntensity() > threshold) {
                int tempInt = Main.peakList.indexOf(tempPeak);
                if (tempInt == -1) {
                    tempPeak.setIsUsed();
                    tempInt = Main.peakList.indexOf(tempPeak);
                }
                tempPeak.setIsUsed();
                intensityScanPairsBelow.add(tempPeak);
                Main.peakList.get(tempInt).setIsUsed();
                System.out.println(increment);
                return createPeakBelow(scanList, averageMZBelow(), toler, increment - 1);
            }
        }
        return 1;
    }

    /**
     * Finds the highest single peak within a given tolerance in a individual spectrum(to account for jitter).
     * <p>
     * NOTE: This function is crucial to the operation of the Peak data structure and is
     * called by several other functions at higher levels so be careful when modifying it
     *
     * @param spec      The single spectrum from which to extract a single Peak
     * @param mean      The value around which the tolerance is centered (this partly defines where the single peak will
     *                  be extracted from
     * @param tol       The tolerance to jitter
     * @param increment The scan number (from the parent IScan object) which is then used to create a LocalPeak object
     *                  containing the relevant information
     * @param RT        The RT of the scan which is used to create a LocalPeak object containing the relevant information
     * @return A LocalPeak object containing all of the relvant information
     */
    static LocalPeak maxIntWithinTol(ISpectrum spec, double mean, double tol, int increment, double RT) {
        int[] temp = spec.findMzIdxsWithinPpm(mean, tol);   //according to source code tolerance is calculated as (mean/1e6)*tol
        double[] inten = spec.getIntensities();
        int maxIndex = 0;
        double maxIntensity = 0;
        for (int i = temp[0]; i <= temp[1]; i++) {
            if (inten[i] > maxIntensity) {
                maxIntensity = inten[i];
                maxIndex = i;
            }
        }
        return new LocalPeak(increment, maxIntensity, spec.getMZs()[maxIndex], RT);
    }

    /**
     * Finds the local minima within the peak (based on intensities). This information is directly related to separating
     * isobars.
     */
    private void findLocalMinima(){
        double[] intensityArray = new double[intensityScanPairs.size()];
        for(int i=0; i<intensityArray.length; i++)
            intensityArray[i] = intensityScanPairs.get(i).getIntensity();
        for(int i=1; i<intensityArray.length-1;i++){
            if(intensityArray[i-1]>intensityArray[i]&&intensityArray[i]<intensityArray[i+1])
                pointsOfInflection.add(i);
        }
    }

    /**
     * Calculates the average m/z value of all the LocalPeak objects in the intensityScanPairs ArrayList. This method
     * returns the value as a double for use in the recursive loops and saves the value to the class field meanMZ
     *
     * @return The average m/z value as a double
     */
    private double averageMZ() {
        int i = 0;
        double total = 0;
        for (LocalPeak intensityScanPair : intensityScanPairs) {
            total = total + intensityScanPair.getMZ();
            i++;
        }
        double average = total / i;
        meanMZ = average;
        return average;
    }

    /**
     * The average m/z value of all the LocalPeak objects in the intensityScanPairsBelow ArrayList. Note: This method
     * is very similar to the averageMZ() method except that it iis specifically intended for use in the
     * createPeakBelow() method. The calculated value also gets saved to the class field meanMZ
     *
     * @return the average m/z value from intensityScanPairsBelow
     */
    private double averageMZBelow() {
        int i = 0;
        double total = 0;
        for (LocalPeak anIntensityScanPairsBelow : intensityScanPairsBelow) {
            total = total + anIntensityScanPairsBelow.getMZ();
            i++;
        }
        double average = total / i;
        meanMZ = average;
        return average;
    }

    /**
     * Returns the intensityScanPairs ArrayList
     *
     * @return An ArrayList containing the LocalPeak objects from intensityScanPairs
     */
    ArrayList<LocalPeak> getIntensityScanPairs() {
        return intensityScanPairs;
    }

    /**
     * Returns the mean m/z for the Peak
     *
     * @return the mean m/z value as a double
     */
    double getMeanMZ() {
        return meanMZ;
    }

    /**
     * Returns the tolerance used to create the peak
     *
     * @return the tolerance as a double
     */
    double getTolerance() {
        return tolerance;
    }

    /**
     * Returns the threshold used to generate the peak
     *
     * @return The threshold as a double
     */
    double getThreshold() {
        return threshold;
    }

    /**
     * Returns the index of the LocalPeak which was used as a starting point
     *
     * @return the index of the starting point as a integer
     */
    int getStartingPointIndex() {
        return startingPointIndex;
    }

    /**
     * Returns the RT of the LocalPeak which was used as a starting point
     *
     * @return the RT of the starting point as a double
     */
    double getStartingPointRT() { return startingPointRT;}

    /**
     * Returns the intensity of the LocalPeak which was used as a starting point
     *
     * @return the intensity of the starting point as a double
     */
    double getStartingPointIntensity() { return startingPointIntensity;}
}