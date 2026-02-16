public class Main {

    String msg = "Hello World";

    public static void main(String[] args) {
        System.exit(new Main().main());
    }

    int main() {
        System.out.println(msg);
        return 0;
    }

}