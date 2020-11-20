package org.baylor.ecs.cloudhubs.sourcecrawler.pkg;

public class SimpleFunc {
    public static void main(String[] args) {
        int x = Integer.parseInt(args[0]);

        if(x < 2) {
            System.out.println("low");
        }

        int y = 3;
        x += foo(y);

        if(x > 4){
            System.out.println("high");
        }
    }

    public static int foo(int y) {
        return y + 2;
    }
}
