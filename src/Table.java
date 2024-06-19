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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return Arrays.equals(keys, table.keys);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(keys);
    }

    public String[] getKeys(){
        return this.keys;
    }
}