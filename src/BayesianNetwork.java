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

        for(int temp = 0; temp < varTag.getLength(); temp++){
            Node defNode = defTag.item(temp);
            Node varNode = varTag.item(temp);

            if((defNode.getNodeType() == Node.ELEMENT_NODE) && (varNode.getNodeType() == Node.ELEMENT_NODE)){
                Element defElement = (Element) defNode;
                Element valElement = (Element) varNode;
                int PTagCount = defElement.getElementsByTagName("GIVEN").getLength();
                int OTagCount = valElement.getElementsByTagName("OUTCOME").getLength();
                String name = defElement.getElementsByTagName("FOR").item(0).getTextContent();
                String table = defElement.getElementsByTagName("TABLE").item(0).getTextContent();
                String[] possiOutcomes = new String[OTagCount];
                String[] possiParents = new String[PTagCount];
                for(int i = 0; i < PTagCount; i++){
                    possiParents[i] = defElement.getElementsByTagName("GIVEN").item(i).getTextContent();
                }
                for(int j = 0; j < OTagCount; j++){
                    possiOutcomes[j] = valElement.getElementsByTagName("OUTCOME").item(j).getTextContent();
                }
                networkList.add(new VariableNode(name, possiOutcomes, possiParents, table));
            }
        }
        return networkList;
    }

    private ArrayList<Key> addNodesToNet() {
        ArrayList<Key> Nodes = new ArrayList<>();
        for(int i = 0; i < count; i++){
            Hashtable<Table, Double> table= new Hashtable<>();
            VariableNode currentVar = variableNodes.get(i);

            int count = currentVar.getVarCount();
            String[] variables = currentVar.getVars();
            double[] probab = currentVar.getProbabilities();
            int[] indexArray = new int[count];
            int[] OCountArr = new int[count];
            for(int j = 0; j < OCountArr.length; j++){
                int currentOCount = getNodeByName(variables[j]).getOutcomeCount();
                OCountArr[j] = currentOCount;
            }
            for (double probability : probab) {
                table.put(IndexToValues(indexArray, variables), probability);
                permutateOneLeft(indexArray, OCountArr);
            }
            Nodes.add(new Key(currentVar.getName(), currentVar.getParents(), table));
        }
        return Nodes;
    }

    private void permutateOneLeft(int[] indexArray, int[] OCounts){
        int[] nameHelper  = Arrays.copyOfRange(indexArray, 0, 1);
        int[] parentHelper = Arrays.copyOfRange(indexArray, 1, indexArray.length);
        if(parentHelper.length == 0){
            indexArray[0]++;
        }
        else{
            int prev = nameHelper[0];
            nameHelper[0] += 1;
            nameHelper[0] %= OCounts[0];
            if(prev != 0 && prev == OCounts[0] - 1){
                permutateOne(parentHelper, Arrays.copyOfRange(OCounts, 1, OCounts.length));
            }
            indexArray[0] = nameHelper[0];
            System.arraycopy(parentHelper, 0, indexArray, 1, indexArray.length - 1);
        }
    }

    private Table IndexToValues(int[] indexArr, String[] vars){
        String[] vals = new String[vars.length];
        for(int i = 0; i < vals.length; i++){
            VariableNode current = getNodeByName(vars[i]);
            vals[i] = current.getOutcomes()[indexArr[i]];
        }
        return new Table(vals);
    }

    private boolean IsProbabilityDirect(String[] names){
        Key key = getFactorByName(names[0]);
        String[] keyParent = key.getFactorParents();
        ArrayList<String> parents = new ArrayList<>();
        Collections.addAll(parents, keyParent);

        String[] queryP = new String[names.length - 1];
        System.arraycopy(names, 1, queryP, 0, names.length - 1);

        boolean found = true;
        for(String givenParent: queryP){
            found = parents.contains(givenParent);
            if(!found)
                break;
        }

        return found && queryP.length == keyParent.length;
    }

    private double getDirectProbability(String[] names, String[] truthVal){
        Key key = getFactorByName(names[0]);
        Hashtable<Table, Double> factorTable = getFactorByName(names[0]).getFactorTable();

        String[] keyVariables = key.getFactorVars();
        String[] newValues = new String[truthVal.length];

        for(int i = 0; i < keyVariables.length; i++){
            for(int j = 0; j < names.length; j++){
                if(names[j].equals(keyVariables[i])){
                    newValues[i] = truthVal[j];
                }
            }
        }
        return factorTable.get(new Table(newValues));
    }

    public String Variable_Elimination(String[] names, String[] truthValsArr, String [] hidden){
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
        if (IsProbabilityDirect(names)) {
            double probability = getDirectProbability(names, truthValsArr);
            return decimalFormat.format(probability) + ",0,0";
        }
        else{
            ArrayList<String> relevant = findRelevantKeys(names);
            ArrayList<Key> tempKeys = new ArrayList<>();
            for(Key key : keyNodes){
                try {
                    if(relevant.contains(key.getFactorName()))
                        tempKeys.add((Key) key.clone());
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
            String[] evid = new String[names.length - 1];
            System.arraycopy(names, 1, evid, 0, evid.length);
            String[] query = new String[1];
            query[0] = names[0];

            for(int temp = 0; temp < evid.length; temp++){
                String checkedVariable = evid[temp];
                String checkedVarValue = truthValsArr[temp + 1];
                for (Key currKey : tempKeys) {
                    if (currKey.IsvarInFactor(checkedVariable))
                        currKey.MakeInstance(checkedVariable, checkedVarValue);
                }
            }
            discardOne(tempKeys);

            int addCounter = 0;
            int mulCount = 0;
            for(String hiddenString: hidden) {
                ArrayList<Key> hiddenKeys = new ArrayList<>();
                for (Key currKey : tempKeys) {
                    if (currKey.IsvarInFactor(hiddenString))
                        hiddenKeys.add(currKey);
                }

                if(hiddenKeys.size() == 0)
                    continue;
                    hiddenKeys.sort(Key.factorComparator);
                if(hiddenKeys.size() > 1){
                    for(int i = 1; i < hiddenKeys.size(); i++){
                        mulCount += join(hiddenKeys.get(i -1), hiddenKeys.get(i));
                        tempKeys.remove(hiddenKeys.get(i - 1));
                    }
                }
                Key elimKey = hiddenKeys.get(hiddenKeys.size() - 1);

                addCounter += eliminate(elimKey, hiddenString);
                discardOne(tempKeys);
            }
            ArrayList<Key> queryKeys = new ArrayList<>();
            for(Key tempKey : tempKeys){
                if(tempKey.IsvarInFactor(query[0]))
                    queryKeys.add(tempKey);
            }
            queryKeys.sort(Key.factorComparator);
            for(int i = 1; i < queryKeys.size(); i++){
                mulCount += join(queryKeys.get(i -1), queryKeys.get(i));
            }

            Key finalKey = queryKeys.get(queryKeys.size() - 1);
            Hashtable<Table, Double> finalTable = finalKey.getFactorTable();
            double normalizationSum = 0.0;

            Enumeration<Table> SetFinal = finalKey.getFactorTable().keys();
            while(SetFinal.hasMoreElements()){
                normalizationSum += finalTable.get(SetFinal.nextElement());
                addCounter++;
            }
            Enumeration<Table> SetFinalNormalization = finalKey.getFactorTable().keys();
            while(SetFinalNormalization.hasMoreElements()){
                Table currKey = SetFinalNormalization.nextElement();
                double rowValue = finalTable.get(currKey);
                finalTable.put(currKey, rowValue / normalizationSum);
            }
            String[] queryValue = new String[1];
            queryValue[0] = truthValsArr[0]; //The desired query value.
            double end = finalTable.get(new Table(queryValue)); //Answer according to query value.
            return decimalFormat.format(end)+","+(addCounter - 1)+","+mulCount;
        }
    }

    private ArrayList<String> getAncestors(VariableNode node){
        ArrayList<String> ancestors = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(node.getName());
        while(!queue.isEmpty()){
            String ancestor = queue.remove();
            ancestors.add(ancestor);
            String[] currentNodeP = getNodeByName(ancestor).getParents();
            Collections.addAll(queue, currentNodeP);
        }
        return ancestors;
    }

    private ArrayList<String> findRelevantKeys(String[] names){
        ArrayList<String> relevantkeys = new ArrayList<>();
        for(String name: names){
            ArrayList<String> maybes = getAncestors(getNodeByName(name));
            for(String potential: maybes){
                if(!relevantkeys.contains(potential))
                    relevantkeys.add(potential);
            }
        }
        return relevantkeys;
    }

    private void discardOne(ArrayList<Key> tempKeys){
        tempKeys.removeIf(key -> key.getFactorSize() == 1);
    }

    private int join(Key prevKey, Key currKey){
        String[] currentVariables = currKey.getFactorVars();
        String[] previousVariables = prevKey.getFactorVars();
        Hashtable<String, String[]> variableOutComes = new Hashtable<>();
        ArrayList<String> newTableVariables = new ArrayList<>();
        for(String var: previousVariables){
            if(!variableOutComes.containsKey(var)){
                newTableVariables.add(var);
                variableOutComes.put(var, getOGivenkeyColumn(prevKey,previousVariables,var));
            }
        }
        for(String var: currentVariables){
            if(!variableOutComes.containsKey(var)){
                newTableVariables.add(var);
                variableOutComes.put(var, getOGivenkeyColumn(currKey,currentVariables,var));
            }
        }
        int[] OCounts = new int[newTableVariables.size()];
        for(int i = 0; i < variableOutComes.size(); i++){
            OCounts[i] = variableOutComes.get(newTableVariables.get(i)).length;
        }
        int[] idxArray = new int[OCounts.length];
        int rows = 1;
        for (int outcomeCount : OCounts) {
            rows *= outcomeCount;
        }
        Hashtable<Table, Double> joined = new Hashtable<>();
        for(int i = 0; i < rows; i++){
            Table newKey = IndexToValues(idxArray, newTableVariables, variableOutComes);
            joined.put(newKey, 1.0);
            permutateOne(idxArray, OCounts);
        }
        int muls = getProductAfterJoin(prevKey, currKey, joined, newTableVariables);
        currKey.setFactorTable(joined);
        currKey.setVars(newTableVariables.toArray(new String[0]));
        return muls;
    }

    private int eliminate(Key key, String hidden){
        int variableIdx = 0;
        String[] KEYVariables = key.getFactorVars();
        String[] newKEYVars = new String[KEYVariables.length - 1];
        int insertTemp = 0;
        for(int i = 0; i < KEYVariables.length; i++){
            if(KEYVariables[i].equals(hidden)){
                variableIdx = i;
            }else{
                newKEYVars[insertTemp++] = KEYVariables[i];
            }
        }
        Hashtable<Table, Double> currTable = key.getFactorTable();
        Hashtable<Table, Double> newEliminatedTable = new Hashtable<>();
        int additionCounter = 0;
        Enumeration<Table> keySet = currTable.keys();
        while(keySet.hasMoreElements()){
            Table currTableKey = keySet.nextElement();
            String[] currKeyArr = currTableKey.getKeys();
            String[] newValues = new String[newKEYVars.length];
            int insertNewValsTemp = 0;
            for(int i = 0; i < currKeyArr.length; i++){
                if(i != variableIdx)
                    newValues[insertNewValsTemp++] = currKeyArr[i];
            }
            if(newEliminatedTable.containsKey(new Table(newValues))){
                continue;
            }

            Enumeration<Table> keySetTemp = currTable.keys();
            double sum = 0.0;
            int additions = 0;
            while(keySetTemp.hasMoreElements()){
                Table currTableTemp = keySetTemp.nextElement();
                String[] currKeyArrTemp = currTableTemp.getKeys();
                String[] newValueesTemp = new String[newKEYVars.length];
                int insertNewValsTemp2 = 0;

                for(int i = 0; i < currKeyArrTemp.length; i++){
                    if(i != variableIdx)
                        newValueesTemp[insertNewValsTemp2++] = currKeyArrTemp[i];
                }
                if(Arrays.equals(newValueesTemp, newValues)){
                    sum += currTable.get(currTableTemp);
                    additions += 1;
                }
            }
            newEliminatedTable.put(new Table(newValues), sum);
            additionCounter += additions - 1;
        }
        key.setVars(newKEYVars);
        key.setFactorTable(newEliminatedTable);
        return additionCounter;
    }

    private int getProductAfterJoin(Key prevKey, Key currKeyFactors, Hashtable<Table, Double> joinedTable, ArrayList<String> newTableVars) {
        String[] prevKeyVarieables = prevKey.getFactorVars();
        String[] currKeyVariables = currKeyFactors.getFactorVars();
        String[] joinedTableVars = newTableVars.toArray(new String[0]);

        Hashtable<Table, Double> prevKeyTable = prevKey.getFactorTable();
        Hashtable<Table, Double> currKeyTable = currKeyFactors.getFactorTable();
        int subArrEndIdx = prevKeyVarieables.length;

        Enumeration<Table> prevKeys = prevKeyTable.keys();
        Enumeration<Table> currKeys = currKeyTable.keys();

        int multiCount = 0;
        while(prevKeys.hasMoreElements()){
            Table key = prevKeys.nextElement();
            String[] keyArr = key.getKeys();
            double probab = prevKeyTable.get(key);
            Enumeration<Table> joinedKeys = joinedTable.keys();
            while(joinedKeys.hasMoreElements()){
                Table joinedKey = joinedKeys.nextElement();
                double joinedTableProb = joinedTable.get(joinedKey);
                String[] joinedKeysSubArray = Arrays.copyOfRange(joinedKey.getKeys(),0, subArrEndIdx);
                if(Arrays.equals(keyArr, joinedKeysSubArray)){
                    joinedTable.put(joinedKey, joinedTableProb * probab);
                }
            }
        }
        while(currKeys.hasMoreElements()){
            Table currKey = currKeys.nextElement();
            String[] currKeyArr = currKey.getKeys();
            double prob = currKeyTable.get(currKey);

            Enumeration<Table> joinedKeys = joinedTable.keys();
            while(joinedKeys.hasMoreElements()){
                Table joinedKey = joinedKeys.nextElement();
                double joinedTableProb = joinedTable.get(joinedKey);
                String[] joinedKeyVals = joinedKey.getKeys();
                String[] newOrderedVals = new String[currKeyVariables.length];
                int tempCounter = 0;
                for (String currFactorVar : currKeyVariables) {
                    for (int joinedColumn = 0; joinedColumn < joinedTableVars.length; joinedColumn++) {
                        if (currFactorVar.equals(joinedTableVars[joinedColumn])) {
                            newOrderedVals[tempCounter++] = joinedKeyVals[joinedColumn];
                        }
                    }
                }
                if(Arrays.equals(newOrderedVals, currKeyArr)){
                    joinedTable.put(joinedKey, joinedTableProb * prob);
                    multiCount++;
                }
            }
        }
        return multiCount;
    }

    private Table IndexToValues(int[] idxArray, ArrayList<String> varList, Hashtable<String, String[]> varTable){
        String[] values = new String[idxArray.length];
        for(int i = 0; i < varList.size(); i++){
            values[i] = varTable.get(varList.get(i))[idxArray[i]];
        }
        return new Table(values);
    }

    private String[] getOGivenkeyColumn(Key key, String[] vars, String var){
        int variablrIdx = -1;
        for(int i = 0; i < vars.length; i++){
            if(vars[i].equals(var)){
                variablrIdx = i;
                break;
            }
        }
        ArrayList<String> uniqueValue = new ArrayList<>();
        Enumeration<Table> keySet = key.getFactorTable().keys();
        while(keySet.hasMoreElements()){
            Table currKey = keySet.nextElement();
            String value = currKey.getKeys()[variablrIdx];
            if(!uniqueValue.contains(value))
                uniqueValue.add(value);
        }
        return uniqueValue.toArray(new String[0]);
    }

    private void permutateOne(int[] idxArray, int[] outcomeCounts){
        int idxArrayLength = idxArray.length;
        if(idxArrayLength != 0){
            int prevValue = idxArray[idxArrayLength - 1];
            idxArray[idxArrayLength - 1] += 1;
            idxArray[idxArrayLength - 1] %= outcomeCounts[idxArrayLength - 1];
            boolean next = true;
            for(int j = idxArray.length - 1; j >=1; j--){
                if((next &&(idxArray[j] == 0 && prevValue == outcomeCounts[j] - 1))){
                    prevValue = idxArray[j - 1];
                    idxArray[j - 1] += 1;
                    idxArray[j - 1] %= outcomeCounts[j - 1];
                    next = idxArray[j - 1] == 0 && prevValue == outcomeCounts[j - 1] - 1 && prevValue != 0;
                }
                else{
                    next = (idxArray[j - 1] + 1 == outcomeCounts[j - 1]);
                }
            }
        }
    }

    public VariableNode getNodefromIdx(int idx) throws IndexOutOfBoundsException{
        try{
            return this.variableNodes.get(idx);
        } catch (IndexOutOfBoundsException e){
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }

    public VariableNode getNodeByName(String str){
        for(int i = 0; i < count; i++){
            if(getNodefromIdx(i).getName().equals(str))
                return getNodefromIdx(i);
        }
        return null;
    }

    public Key getKeyfromIdx(int idx) throws IndexOutOfBoundsException{
        try{
            return keyNodes.get(idx);
        } catch (IndexOutOfBoundsException e){
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }

    public Key getFactorByName(String str) {
        str = str.replace(" ", "");
        for (int i = 0; i < keyNodes.size(); i++) {
            if (str.equals(keyNodes.get(i).getFactorName()))
                return getKeyfromIdx(i);
        }
        return null;
    }

    public String Bayes_ball(String[] vars, String[] evidence){
        String first  = vars[0];
        ArrayList<String> Children = new ArrayList<String>();
        for (VariableNode Nod : this.variableNodes){
            //System.out.println("name = "+ Nod.getName() + "parents - ");
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