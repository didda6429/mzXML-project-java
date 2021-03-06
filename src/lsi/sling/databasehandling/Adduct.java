package lsi.sling.databasehandling;

import java.io.Serializable;

/**
 * This class represents a possible adduct (which a peakcluster could be).
 * @author Adithya Diddapur
 */
public class Adduct implements Comparable<Adduct>, Serializable{

    private String ionName;
    private String ionMassFunction;
    private double ionMass;
    private int ionCharge;
    private double compoundExactMass;
    private double resultMZ;
    private String compoundFormula;
    private String compoundCommonName;
    private String compoundSystemicName;

    /**
     * This constructor creates an object given all of the relevant information. The intented use is for all computation to be
     * performed elsewhere and then this object is used to store the calculated data.
     * @param iName The name of the ion
     * @param iMassFunction The formula to calculate the mass of the entire adduct for this specific ion
     * @param iMass The mass of the ion
     * @param iCharge The charge of the ion
     * @param cExactmass The exact mass of the compound (without the ion component)
     * @param rMZ The combined mass of the compound and ion (the entire adduct)
     * @param cFormula The formula of the compound (without the ion component)
     * @param cCommonName The common name of the compound (without the ion component)
     * @param cSystemicName The systemic name of the compound (without the ion component)
     */
    Adduct(String iName, String iMassFunction, double iMass, int iCharge, double cExactmass, double rMZ, String cFormula, String cCommonName, String cSystemicName){
        ionName = iName;
        ionMassFunction = iMassFunction;
        ionMass = iMass;
        ionCharge = iCharge;
        compoundExactMass = cExactmass;
        resultMZ = rMZ;
        compoundFormula = cFormula;
        compoundCommonName = cCommonName;
        compoundSystemicName = cSystemicName;
    }

    /**
     * Creates a String[] containing all of the data from a particular object. This method is used when writing the List
     * of objects to a file in AdductDatabase.createDatabase(String).<br/>
     * Note that this method has been replaced by writing the list itself (as an object) to the files
     * @return A String[] of length 9 containing the information
     */
    @Deprecated
    String[] toStringArray(){
        String[] array = new String[9];
        array[0] = ionName;
        array[1] = ionMassFunction;
        array[2] = Double.toString(ionMass);
        array[3] = Integer.toString(ionCharge);
        array[4] = Double.toString(compoundExactMass);
        array[5] = Double.toString(resultMZ);
        array[6] = compoundFormula;
        array[7] = compoundCommonName;
        array[8] = compoundSystemicName;
        return array;
    }

    public String getIonName() {
        return ionName;
    }

    public String getIonMassFunction() {
        return ionMassFunction;
    }

    public double getIonMass() {
        return ionMass;
    }

    public int getIonCharge() {
        return ionCharge;
    }

    public double getCompoundExactMass() {
        return compoundExactMass;
    }

    public double getResultMZ() {
        return resultMZ;
    }

    public String getCompoundFormula() {
        return compoundFormula;
    }

    public String getCompoundCommonName() {
        return compoundCommonName;
    }

    public String getCompoundSystemicName() {
        return compoundSystemicName;
    }

    /**
     * Compares 2 adducts in order to implement the Comparable interface. They are compared in terms of result m/z value
     * @param o An adduct to compare with
     * @return an integer representing the result. <br/> 0 implies equality, 1 implies this object is "less than" (lower m/z value) o, and -1 implies this object is "greater than" o
     */
    @Override
    public int compareTo(Adduct o) {
        int val = 0;
        if(this.getResultMZ()>o.getResultMZ()){
            val = -1;
        } else if (this.getResultMZ()<o.getResultMZ()){
            val = 1;
        }
        return val;
    }
}
