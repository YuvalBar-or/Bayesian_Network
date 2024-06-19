import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class BayesianNetwork {
    public ArrayList<VariableNode> variableNodes;
    private final ArrayList<Key> keyNodes;
    private final int count;

    public BayesianNetwork(String xmlFileName){
        variableNodes = addNodesToNet(parseXML(xmlFileName));
        count = variableNodes.size();
        keyNodes = addNodesToNet();
    }


    @Override
    public String toString() {
        return keyNodes.toString();
    }

    private Document parseXML(String xmlName){
        File file = new File(xmlName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        try {
            return builder.parse(file);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private ArrayList<VariableNode> addNodesToNet(Document doc) {
        NodeList defTag = doc.getElementsByTagName("DEFINITION");
        NodeList varTag = doc.getElementsByTagName("VARIABLE");
        ArrayList<VariableNode> networkList = new ArrayList<>();

        int temp = 0;
        while (temp < varTag.getLength()) {
            Node defNode = defTag.item(temp);
            Node varNode = varTag.item(temp);

            if ((defNode.getNodeType() == Node.ELEMENT_NODE) && (varNode.getNodeType() == Node.ELEMENT_NODE)) {
                Element defElement = (Element) defNode;
                Element valElement = (Element) varNode;
                int PTagCount = defElement.getElementsByTagName("GIVEN").getLength();
                int OTagCount = valElement.getElementsByTagName("OUTCOME").getLength();
                String name = defElement.getElementsByTagName("FOR").item(0).getTextContent();
                String table = defElement.getElementsByTagName("TABLE").item(0).getTextContent();
                String[] possiOutcomes = new String[OTagCount];
                String[] possiParents = new String[PTagCount];
                int i = 0;
                while (i < PTagCount) {
                    possiParents[i] = defElement.getElementsByTagName("GIVEN").item(i).getTextContent();
                    i++;
                }
                for (int j = 0; j < OTagCount; j++) {
                    possiOutcomes[j] = valElement.getElementsByTagName("OUTCOME").item(j).getTextContent();
                }
                networkList.add(new VariableNode(name, possiOutcomes, possiParents, table));
            }
            temp++;
        }
        return networkList;
    }

    private ArrayList<Key> addNodesToNet() {
        ArrayList<Key> Nodes = new ArrayList<>();
        int i = 0;
        while (i < count) {
            Hashtable<Table, Double> table = new Hashtable<>();
            VariableNode currentVar = variableNodes.get(i);

            int count = currentVar.getVarCount();
            String[] variables = currentVar.getVars();
            double[] probab = currentVar.getProbabilities();
            int[] indexArray = new int[count];
            int[] OCountArr = new int[count];
            int j = 0;
            while (j < OCountArr.length) {
                int currentOCount = getNodeByName(variables[j]).getOutcomeCount();
                OCountArr[j] = currentOCount;
                j++;
            }
            for (double probability : probab) {
                table.put(IndexToValues(indexArray, variables), probability);
                permutateOneLeft(indexArray, OCountArr);
            }
            Nodes.add(new Key(currentVar.getName(), currentVar.getParents(), table));
            i++;
        }
        return Nodes;
    }

    private void permutateOneLeft(int[] indexArray, int[] OCounts) {
        int[] nameHelper = Arrays.copyOfRange(indexArray, 0, 1);
        int[] parentHelper = Arrays.copyOfRange(indexArray, 1, indexArray.length);
        if (parentHelper.length > 0) {
            int prev = nameHelper[0];
            nameHelper[0] += 1;
            nameHelper[0] %= OCounts[0];
            if (prev != 0 && prev == OCounts[0] - 1) {
                permutateOne(parentHelper, Arrays.copyOfRange(OCounts, 1, OCounts.length));
            }
            indexArray[0] = nameHelper[0];
            System.arraycopy(parentHelper, 0, indexArray, 1, indexArray.length - 1);
        } else {
            indexArray[0]++;
        }
    }

    private Table IndexToValues(int[] indexArr, String[] vars) {
        String[] values = new String[vars.length];
        for (int i = 0; i < values.length; i++) {
            VariableNode currentNode = getNodeByName(vars[i]);
            values[i] = currentNode.getOutcomes()[indexArr[i]];
        }
        return new Table(values);
    }

    private boolean IsProbabilityDirect(String[] names) {
        Key key = getFactorByName(names[0]);
        String[] keyParents = key.getFactorParents();
        ArrayList<String> parentsList = new ArrayList<>(Arrays.asList(keyParents));

        String[] queryParents = Arrays.copyOfRange(names, 1, names.length);
        for (String givenParent : queryParents) {
            if (!parentsList.contains(givenParent)) {
                return false;
            }
        }

        return queryParents.length == keyParents.length;
    }

    private double getDirectProbability(String[] names, String[] truthValues) {
        Key key = getFactorByName(names[0]);
        Hashtable<Table, Double> factorTable = key.getFactorTable();

        String[] keyVariables = key.getFactorVars();
        String[] newValues = new String[truthValues.length];

        for (int i = 0; i < keyVariables.length; i++) {
            for (int j = 0; j < names.length; j++) {
                if (names[j].equals(keyVariables[i])) {
                    newValues[i] = truthValues[j];
                    break; // Once we find the match, no need to continue the inner loop
                }
            }
        }

        return factorTable.get(new Table(newValues));
    }


    public String Variable_Elimination(String[] names, String[] truthValsArr, String[] hidden) {
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");

        if (IsProbabilityDirect(names)) {
            double probability = getDirectProbability(names, truthValsArr);
            return decimalFormat.format(probability) + ",0,0";
        } else {
            ArrayList<String> relevant = findRelevantKeys(names);
            ArrayList<Key> tempKeys = new ArrayList<>();

            for (Key keyNode : keyNodes) {
                if (relevant.contains(keyNode.getFactorName())) {
                    try {
                        tempKeys.add((Key) keyNode.clone());
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            String[] evid = Arrays.copyOfRange(names, 1, names.length);
            String query = names[0];

            int temp = 0;
            while (temp < evid.length) {
                String checkedVariable = evid[temp];
                String checkedVarValue = truthValsArr[temp + 1];
                for (Key currKey : tempKeys) {
                    if (currKey.instanceOfK(checkedVariable)) {
                        currKey.MakeInstance(checkedVariable, checkedVarValue);
                    }
                }
                temp++;
            }
            discardOne(tempKeys);

            int addCounter = 0;
            int mulCount = 0;

            int hiddenIndex = 0;
            while (hiddenIndex < hidden.length) {
                String hiddenString = hidden[hiddenIndex];
                ArrayList<Key> hiddenKeys = new ArrayList<>();
                for (Key currKey : tempKeys) {
                    if (currKey.instanceOfK(hiddenString)) {
                        hiddenKeys.add(currKey);
                    }
                }

                if (hiddenKeys.isEmpty()) {
                    hiddenIndex++;
                    continue;
                }

                hiddenKeys.sort(Key.factorComparator);

                int i = 1;
                while (i < hiddenKeys.size()) {
                    int mulIncrement = join(hiddenKeys.get(i - 1), hiddenKeys.get(i));
                    mulCount += mulIncrement;
                    tempKeys.remove(hiddenKeys.get(i - 1));
                    i++;
                }

                Key elimKey = hiddenKeys.get(hiddenKeys.size() - 1);
                int addIncrement = eliminate(elimKey, hiddenString);
                addCounter += addIncrement;
                discardOne(tempKeys);

                hiddenIndex++;
            }

            ArrayList<Key> queryKeys = new ArrayList<>();
            for (Key tempKey : tempKeys) {
                if (tempKey.instanceOfK(query)) {
                    queryKeys.add(tempKey);
                }
            }

            queryKeys.sort(Key.factorComparator);
            int i = 1;
            while (i < queryKeys.size()) {
                int mulIncrement = join(queryKeys.get(i - 1), queryKeys.get(i));
                mulCount += mulIncrement;
                i++;
            }

            Key finalKey = queryKeys.get(queryKeys.size() - 1);
            Hashtable<Table, Double> finalTable = finalKey.getFactorTable();
            double normalizationSum = 0.0;

            Enumeration<Table> setFinal = finalKey.getFactorTable().keys();
            while (setFinal.hasMoreElements()) {
                normalizationSum += finalTable.get(setFinal.nextElement());
                addCounter++;
            }

            Enumeration<Table> setFinalNormalization = finalKey.getFactorTable().keys();
            while (setFinalNormalization.hasMoreElements()) {
                Table currKey = setFinalNormalization.nextElement();
                double rowValue = finalTable.get(currKey);
                finalTable.put(currKey, rowValue / normalizationSum);
            }

            String queryValue = truthValsArr[0]; // The desired query value.
            Double end = finalTable.get(new Table(new String[]{queryValue})); // Answer according to query value.

            if (end == null) {
                end = 0.0;
            }

            return decimalFormat.format(end) + "," + (addCounter - 1) + "," + mulCount;
        }
    }


    private ArrayList<String> getAncestors(VariableNode node) {
        ArrayList<String> ancestors = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(node.getName());

        for (; !queue.isEmpty(); ) {
            String ancestor = queue.remove();
            ancestors.add(ancestor);
            String[] currentNodeParents = getNodeByName(ancestor).getParents();
            Collections.addAll(queue, currentNodeParents);
        }

        return ancestors;
    }

    private ArrayList<String> findRelevantKeys(String[] names) {
        ArrayList<String> relevantKeys = new ArrayList<>();
        int nameIndex = 0;

        while (nameIndex < names.length) {
            String name = names[nameIndex];
            ArrayList<String> potentialAncestors = getAncestors(getNodeByName(name));
            for (String potential : potentialAncestors) {
                if (!relevantKeys.contains(potential)) {
                    relevantKeys.add(potential);
                }
            }
            nameIndex++;
        }

        return relevantKeys;
    }


    private void discardOne(ArrayList<Key> tempKeys){
        tempKeys.removeIf(key -> key.getFactorSize() == 1);
    }

    private int join(Key prevKey, Key currKey) {
        String[] currentVariables = currKey.getFactorVars();
        String[] previousVariables = prevKey.getFactorVars();
        Hashtable<String, String[]> variableOutcomes = new Hashtable<>();
        ArrayList<String> newTableVariables = new ArrayList<>();

        int prevVarIndex = 0;
        while (prevVarIndex < previousVariables.length) {
            String var = previousVariables[prevVarIndex];
            if (!variableOutcomes.containsKey(var)) {
                newTableVariables.add(var);
                variableOutcomes.put(var, getOGivenkeyColumn(prevKey, previousVariables, var));
            }
            prevVarIndex++;
        }

        for (String var : currentVariables) {
            if (!variableOutcomes.containsKey(var)) {
                newTableVariables.add(var);
                variableOutcomes.put(var, getOGivenkeyColumn(currKey, currentVariables, var));
            }
        }

        int[] outcomeCounts = new int[newTableVariables.size()];
        int i = 0;
        while (i < variableOutcomes.size()) {
            outcomeCounts[i] = variableOutcomes.get(newTableVariables.get(i)).length;
            i++;
        }

        int[] indexArray = new int[outcomeCounts.length];
        int totalRows = 1;
        for (int outcomeCount : outcomeCounts) {
            totalRows *= outcomeCount;
        }

        Hashtable<Table, Double> joinedTable = new Hashtable<>();
        i = 0;
        while (i < totalRows) {
            Table newKey = IndexToValues(indexArray, newTableVariables, variableOutcomes);
            joinedTable.put(newKey, 1.0);
            permutateOne(indexArray, outcomeCounts);
            i++;
        }

        int multiplications = getProductAfterJoin(prevKey, currKey, joinedTable, newTableVariables);
        currKey.setFactorTable(joinedTable);
        currKey.setVars(newTableVariables.toArray(new String[0]));

        return multiplications;
    }


    private int eliminate(Key key, String hidden) {
        int variableIdx = 0;
        String[] keyVariables = key.getFactorVars();
        String[] newKeyVars = new String[keyVariables.length - 1];
        int insertTemp = 0;

        int i = 0;
        while (i < keyVariables.length) {
            if (keyVariables[i].equals(hidden)) {
                variableIdx = i;
            } else {
                newKeyVars[insertTemp++] = keyVariables[i];
            }
            i++;
        }

        Hashtable<Table, Double> currTable = key.getFactorTable();
        Hashtable<Table, Double> newEliminatedTable = new Hashtable<>();
        int additionCounter = 0;
        Enumeration<Table> keySet = currTable.keys();

        while (keySet.hasMoreElements()) {
            Table currTableKey = keySet.nextElement();
            String[] currKeyArr = currTableKey.getKeys();
            String[] newValues = new String[newKeyVars.length];
            int insertNewValsTemp = 0;

            i = 0;
            while (i < currKeyArr.length) {
                if (i != variableIdx) {
                    newValues[insertNewValsTemp++] = currKeyArr[i];
                }
                i++;
            }

            if (newEliminatedTable.containsKey(new Table(newValues))) {
                continue;
            }

            Enumeration<Table> keySetTemp = currTable.keys();
            double sum = 0.0;
            int additions = 0;

            while (keySetTemp.hasMoreElements()) {
                Table currTableTemp = keySetTemp.nextElement();
                String[] currKeyArrTemp = currTableTemp.getKeys();
                String[] newValueesTemp = new String[newKeyVars.length];
                int insertNewValsTemp2 = 0;

                for (int j = 0; j < currKeyArrTemp.length; j++) {
                    if (j != variableIdx) {
                        newValueesTemp[insertNewValsTemp2++] = currKeyArrTemp[j];
                    }
                }

                if (Arrays.equals(newValueesTemp, newValues)) {
                    sum += currTable.get(currTableTemp);
                    additions += 1;
                }
            }

            newEliminatedTable.put(new Table(newValues), sum);
            additionCounter += additions - 1;
        }

        key.setVars(newKeyVars);
        key.setFactorTable(newEliminatedTable);
        return additionCounter;
    }


    private int getProductAfterJoin(Key prevKey, Key currKeyFactors, Hashtable<Table, Double> joinedTable, ArrayList<String> newTableVars) {
        String[] prevKeyVariables = prevKey.getFactorVars();
        String[] currKeyVariables = currKeyFactors.getFactorVars();
        String[] joinedTableVars = newTableVars.toArray(new String[0]);

        Hashtable<Table, Double> prevKeyTable = prevKey.getFactorTable();
        Hashtable<Table, Double> currKeyTable = currKeyFactors.getFactorTable();
        int subArrEndIdx = prevKeyVariables.length;

        Enumeration<Table> prevKeys = prevKeyTable.keys();
        Enumeration<Table> currKeys = currKeyTable.keys();

        int multiCount = 0;

        for (; prevKeys.hasMoreElements(); ) {
            Table key = prevKeys.nextElement();
            String[] keyArr = key.getKeys();
            double probab = prevKeyTable.get(key);

            Enumeration<Table> joinedKeys = joinedTable.keys();
            while (joinedKeys.hasMoreElements()) {
                Table joinedKey = joinedKeys.nextElement();
                double joinedTableProb = joinedTable.get(joinedKey);
                String[] joinedKeysSubArray = Arrays.copyOfRange(joinedKey.getKeys(), 0, subArrEndIdx);
                if (Arrays.equals(keyArr, joinedKeysSubArray)) {
                    joinedTable.put(joinedKey, joinedTableProb * probab);
                }
            }
        }

        while (currKeys.hasMoreElements()) {
            Table currKey = currKeys.nextElement();
            String[] currKeyArr = currKey.getKeys();
            double prob = currKeyTable.get(currKey);

            Enumeration<Table> joinedKeys = joinedTable.keys();
            while (joinedKeys.hasMoreElements()) {
                Table joinedKey = joinedKeys.nextElement();
                double joinedTableProb = joinedTable.get(joinedKey);
                String[] joinedKeyVals = joinedKey.getKeys();
                String[] newOrderedVals = new String[currKeyVariables.length];
                int tempCounter = 0;

                for (int j = 0; j < currKeyVariables.length; j++) {
                    String currFactorVar = currKeyVariables[j];
                    for (int joinedColumn = 0; joinedColumn < joinedTableVars.length; joinedColumn++) {
                        if (currFactorVar.equals(joinedTableVars[joinedColumn])) {
                            newOrderedVals[tempCounter++] = joinedKeyVals[joinedColumn];
                        }
                    }
                }

                if (Arrays.equals(newOrderedVals, currKeyArr)) {
                    joinedTable.put(joinedKey, joinedTableProb * prob);
                    multiCount++;
                }
            }
        }
        return multiCount;
    }

    private Table IndexToValues(int[] idxArray, ArrayList<String> varList, Hashtable<String, String[]> varTable) {
        String[] values = new String[idxArray.length];
        int i = 0;
        while (i < varList.size()) {
            values[i] = varTable.get(varList.get(i))[idxArray[i]];
            i++;
        }
        return new Table(values);
    }

    private String[] getOGivenkeyColumn(Key key, String[] vars, String var) {
        int variableIdx = -1;
        int i = 0;
        while (i < vars.length) {
            if (vars[i].equals(var)) {
                variableIdx = i;
                break;
            }
            i++;
        }
        ArrayList<String> uniqueValues = new ArrayList<>();
        Enumeration<Table> keySet = key.getFactorTable().keys();
        for (; keySet.hasMoreElements(); ) {
            Table currKey = keySet.nextElement();
            String value = currKey.getKeys()[variableIdx];
            if (!uniqueValues.contains(value)) {
                uniqueValues.add(value);
            }
        }
        return uniqueValues.toArray(new String[0]);
    }

    private void permutateOne(int[] idxArray, int[] outcomeCounts) {
        int idxArrayLength = idxArray.length;
        if (idxArrayLength != 0) {
            int prevValue = idxArray[idxArrayLength - 1];
            idxArray[idxArrayLength - 1] += 1;
            idxArray[idxArrayLength - 1] %= outcomeCounts[idxArrayLength - 1];
            boolean next = true;
            for (int j = idxArrayLength - 1; j >= 1; j--) {
                if (next && (idxArray[j] == 0 && prevValue == outcomeCounts[j] - 1)) {
                    prevValue = idxArray[j - 1];
                    idxArray[j - 1] += 1;
                    idxArray[j - 1] %= outcomeCounts[j - 1];
                    next = idxArray[j - 1] == 0 && prevValue == outcomeCounts[j - 1] - 1 && prevValue != 0;
                } else {
                    next = idxArray[j - 1] + 1 == outcomeCounts[j - 1];
                }
            }
        }
    }

    public VariableNode getNodefromIdx(int idx) throws IndexOutOfBoundsException {
        try {
            return this.variableNodes.get(idx);
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }

    public VariableNode getNodeByName(String name) {
        int i = 0;
        while (i < count) {
            if (getNodefromIdx(i).getName().equals(name)) {
                return getNodefromIdx(i);
            }
            i++;
        }
        return null;
    }

    public Key getKeyfromIdx(int idx) throws IndexOutOfBoundsException {
        try {
            return keyNodes.get(idx);
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }

    public Key getFactorByName(String name) {
        name = name.replace(" ", "");
        int i = 0;
        while (i < keyNodes.size()) {
            if (name.equals(keyNodes.get(i).getFactorName())) {
                return getKeyfromIdx(i);
            }
            i++;
        }
        return null;
    }


    public String Bayes_ball(String[] vars, String[] evidence){
        String first  = vars[0];
        ArrayList<String> Children = new ArrayList<String>();
        for (VariableNode Nod : this.variableNodes){
            if (Arrays.asList(Nod.getParents()).contains(first)){
                Children.add(Nod.getName());
            }
        }
        String[] parents = getNodeByName(first).getParents();
        if (Arrays.asList(parents).contains(vars[1]) || Children.contains(vars[1])){
            return "no";
        }
        ArrayList<String> observed = new ArrayList<String>();
        observed.add(first);
        boolean flag = false;
        for (String par: parents){
            flag = flag || Bayes_ball_helper(getNodeByName(par) , evidence , true , observed, vars[1]);
        }
        for (String child: Children){
            flag = flag || Bayes_ball_helper(getNodeByName(child) , evidence , false, observed, vars[1]);
        }
        if ( flag){
            return "no";
        }
        else {
            return "yes";
        }
    }

    public boolean Bayes_ball_helper (VariableNode var, String[] evidence , boolean isFromChild, ArrayList<String> observed,String Nod){
        boolean flag = false;
        observed.add(var.getName());
        if(Arrays.asList(evidence).contains(var.getName())){
            if (isFromChild) {
                return false;
            }
            if (Arrays.asList(var.getParents()).contains(Nod)){
                return true;
            }
            for (String par: var.getParents()){
                if ( Arrays.asList(evidence).contains(par)){
                    return false;
                }
                flag = flag|| Bayes_ball_helper(getNodeByName(par),evidence,true,observed,Nod);
            }
        }
        else{

            ArrayList<String> Child = new ArrayList<String>();
            for (VariableNode v : this.variableNodes){
                if (Arrays.asList(v.getParents()).contains(var.getName())){
                    Child.add(v.getName());
                    if(v.getName() == Nod){
                        return true;
                    }
                }
            }
            for (String chill: Child){
                if(!observed.contains(chill)){
                    flag = flag|| Bayes_ball_helper(getNodeByName(chill),evidence,false,observed,Nod);
                }}
            if ( isFromChild){
                if (Arrays.asList(var.getParents()).contains(Nod)){
                    return true;
                }
                for (String par: var.getParents()){
                    if ( Arrays.asList(evidence).contains(par)){
                        return false;
                    }
                    flag = flag|| Bayes_ball_helper(getNodeByName(par),evidence,true,observed,Nod);
                }

            }
        }
        return flag;
    }

}