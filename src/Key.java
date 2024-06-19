import java.util.*;
import java.util.Comparator;


public class Key implements Cloneable{
    private final String factorName; //Factor node name.

    private String[] parents; //Factor node array of parents.

    private String[] vars;

    private Hashtable<Table, Double> factorTable; //Factor node factor table!

    public Key(String name, String[] parents, Hashtable<Table, Double> factorTable){
        this.factorName = name;
        this.parents = parents;
        this.factorTable = factorTable;
        this.vars = new String[parents.length + 1];
        this.vars[0] = name;
        System.arraycopy(parents, 0, this.vars, 1, this.vars.length - 1);
    }


    @Override
    public String toString() {
        return "P("+this.factorName + "|" + Arrays.toString(parents) + ") => " + factorTable.toString() + "\n";
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Key clonedKey = null;
        try{
            clonedKey = (Key) super.clone();
            clonedKey.setParents(this.getFactorParents().clone());
            clonedKey.setFactorTable(new Hashtable<>(this.getFactorTable()));
        } catch (CloneNotSupportedException e){
            e.printStackTrace();
        }
        return clonedKey;
    }

    public static Comparator<Key> factorComparator = (key1, key2) -> {
        int factor1 = key1.getFactorSize();
        int factor2 = key2.getFactorSize();
        if(factor1 != factor2){
            return factor1 - factor2;
        }
        else{
            int asciiFactor1 = key1.getAsciiSumOfVars();
            int asciiFactor2 = key2.getAsciiSumOfVars();
            return asciiFactor1 - asciiFactor2;
        }
    };

    public void MakeInstance(String var, String val){
        String[] factorsVariables = this.getFactorVars();
        int varIndex = 0;
        for(int i = 0; i < factorsVariables.length; i++){
            if(factorsVariables[i].equals(var)){
                varIndex = i;
                break;
            }
        }
        Enumeration<Table> setKeys = this.factorTable.keys();
        while(setKeys.hasMoreElements()){
            Table currentKey = setKeys.nextElement();
            if(!currentKey.getKeys()[varIndex].equals(val))
                this.factorTable.remove(currentKey);
        }

        Hashtable<Table, Double> temp = new Hashtable<>();
        Enumeration<Table> newSet = this.factorTable.keys();
        while(newSet.hasMoreElements()){
            Table currKey = newSet.nextElement();
            double probab = this.factorTable.get(currKey);
            String[] currVarArray = currKey.getKeys();
            String[] newKey = new String[currVarArray.length - 1];
            int insertTemp = 0;

            for(int i = 0; i < currVarArray.length; i++){
                if(i != varIndex){
                    newKey[insertTemp++] = currVarArray[i];
                }
            }
            temp.put(new Table(newKey), probab);
        }
        String[] variables = this.getFactorVars();
        String[] newArray = new String[variables.length - 1];
        int valInsertionTemp = 0;

        for (int i = 0; i < variables.length; i++) {
            if (i != varIndex)
                newArray[valInsertionTemp++] = variables[i];
        }

        this.setFactorTable(temp);
        this.setVars(newArray);
    }

    public boolean IsvarInFactor(String var){
        String[] factor = getFactorVars();
        for (String factorVar : factor) {
            if (factorVar.equals(var))
                return true;
        }
        return false;
    }

    public String getFactorName(){
        return this.factorName;
    }

    public String[] getFactorParents(){
        return this.parents;
    }

    public Hashtable<Table, Double> getFactorTable(){
        return this.factorTable;
    }

    public String[] getFactorVars(){
        return this.vars;
    }

    public int getFactorSize(){
        return this.factorTable.size();
    }

    public int getAsciiSumOfVars(){
        String[] variables = this.getFactorVars();
        int sum = 0;

        for(String var : variables){
            for(int i = 0; i < var.length(); i++){
                sum += var.charAt(i);
            }
        }
        return sum;
    }

    private void setParents(String[] parents){
        this.parents = parents;
    }

    public void setVars(String[] vars){
        this.vars = vars;
    }

    public void setFactorTable(Hashtable<Table, Double> factorTable){
        this.factorTable = factorTable;
    }
}