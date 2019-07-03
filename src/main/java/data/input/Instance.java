/*
@author Arthur Godet <arth.godet@gmail.com>
@since 03/07/2019
*/
package data.input;

public class Instance {
    public int horizon;
    public int[][] roadsCost;
    public int[] workCenters;
    public Worksheet[] worksheets;
    public RoadMaxBlock[] roadsBlocked;
    public int[][] precedences;
    public int nbActivities;
}
