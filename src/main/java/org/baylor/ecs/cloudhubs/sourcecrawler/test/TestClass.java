package org.baylor.ecs.cloudhubs.sourcecrawler.test;

public class TestClass {
    public static void main(String[] args) {
        int x = 10;
        x = addTen(x);
        System.out.println("Value of x is " + x);

    }

    private static int addTen(int x){
        return x + 10;
    }

    private static void branch(int x){
        if(x == 10){
            System.out.println("branch 1");
        }else{
            System.out.println("branch2");
        }
    }
}
