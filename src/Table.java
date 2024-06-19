import java.util.Arrays;

public class Table {
    private final String[] keys;

    public Table(String[] keys){

        this.keys = keys;
    }

    @Override
    public String toString() {

        return Arrays.toString(keys);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Table otherTable = (Table) obj;
        return Arrays.equals(keys, otherTable.keys);
    }


    @Override
    public int hashCode() {

        return Arrays.hashCode(keys);
    }

    public String[] getKeys(){

        return this.keys;
    }
}