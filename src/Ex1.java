import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ex1 {

    private static String[] getQueryNames(String query){
        List<String> stringList = new ArrayList<>();
        Pattern pattern = Pattern.compile("([a-zA-Z\\d]*)=");
        Matcher matcher = pattern.matcher(query);
        while(matcher.find()){
            stringList.add(matcher.group(1));
        }
        String[] namesArr = new String[stringList.size()];
        return stringList.toArray(namesArr);
    }

    private static String[] getQueryIndex(String query){
        List<String> stringList = new ArrayList<>();
        Pattern pattern = Pattern.compile("=([a-zA-Z\\d]*)");
        Matcher matcher = pattern.matcher(query);
        while(matcher.find()){
            stringList.add(matcher.group(1));
        }
        String[] namesArr = new String[stringList.size()];
        return stringList.toArray(namesArr);
    }

    private static String[] getHidden (String query){
        List<String> stringList = new ArrayList<>();
        Pattern pattern2 = Pattern.compile("([a-zA-Z\\d]*)-");
        Matcher matcher2 = pattern2.matcher(query);
        matcher2.find();
        stringList.add(matcher2.group(1));
        Pattern pattern = Pattern.compile("-([a-zA-Z\\d]*)");
        Matcher matcher = pattern.matcher(query);
        while(matcher.find()){
            stringList.add(matcher.group(1));
        }
        String[] namesArr = new String[stringList.size()];
        return stringList.toArray(namesArr);
    }

    private static String[] getNodesNames (String query){
        List<String> stringList = new ArrayList<>();
        Pattern pattern = Pattern.compile("-([a-zA-Z\\d]*)");
        Matcher matcher = pattern.matcher(query);
        matcher.find();
        stringList.add(matcher.group(1));
        Pattern pattern2 = Pattern.compile("([a-zA-Z\\d]*)-");
        Matcher matcher2 = pattern2.matcher(query);
        matcher2.find();
        stringList.add(matcher2.group(1));

        String[] namesArr = new String[stringList.size()];
        return stringList.toArray(namesArr);


    }

    public static void printStringArray(String[] array) {
        for (String element : array) {
            System.out.println(element);
        }
    }

    private static void readInput(){
        BufferedReader br;
        BayesianNetwork Network;
        FileOutputStream outputStream;
        try {
            br = new BufferedReader(new FileReader("input.txt"));
            outputStream = new FileOutputStream("output.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            String line;
            try {
                line = br.readLine();
                Network = new BayesianNetwork(line);
                line = br.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            while (line != null) {
                try {
                    char first = line.charAt(0);
                    char second = line.charAt(1);
                    String query = line.substring(0,line.length()); //Query string input.
                    String answer;
                    System.out.println(query);
                            if ( first == 'P' && second == '(') {
                                for (VariableNode Nod : Network.variableNodes){
                                    printStringArray(Nod.getParents());}
                                answer = Network.Variable_Elimination(getQueryNames(query), getQueryIndex(query), getHidden(query)) + "\n";
                                outputStream.write(answer.getBytes());
                            }
                            else {
                                answer = Network.Bayes_ball(getNodesNames(query), getQueryNames(query)) + "\n";
                               outputStream.write(answer.getBytes());
                            }
                    } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                br.close();
                outputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        readInput();
    }
}