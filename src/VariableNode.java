public class VariableNode {
    private final String Name; //Node main name

    private final String[] Outcomes; //Possible outcomes of the node.

    private final String[] parents; //The parents of the node. Kept as string names.

    private final double[] probabilities; //Array of probability values.

    private final int outcomeCount, parentCount; //Counters of outcomes and parents, respectively.

    public VariableNode(String nodeName, String[] possibleOutcomes, String[] parents, String stringValues){
        this.Name = nodeName;
        this.Outcomes = possibleOutcomes;
        this.parents = parents;
        this.probabilities = parseProbabilities(stringValues);
        this.outcomeCount = possibleOutcomes.length;
        this.parentCount = parents.length;
    }

    private double[] parseProbabilities(String stringValues){
        String[] Array = stringValues.split(" ");
        double[] values = new double[Array.length];
        for(int i = 0; i < values.length; i++){
            values[i] = Double.parseDouble(Array[i]);
        }
        return values;
    }

    public String getName() {
        return Name;
    }

    public String[] getOutcomes(){
        return Outcomes;
    }

    public String[] getParents(){
        return this.parents;
    }

    public String[] getVars(){
        String[] variabless = new String[getVarCount()];
        variabless[0] = Name;
        System.arraycopy(parents, 0, variabless, 1, variabless.length - 1);
        return variabless;
    }

    public int getOutcomeCount() {
        return outcomeCount;
    }

    public int getVarCount(){
        return parentCount + 1;
    }

    public double[] getProbabilities(){
        return probabilities;
    }

}