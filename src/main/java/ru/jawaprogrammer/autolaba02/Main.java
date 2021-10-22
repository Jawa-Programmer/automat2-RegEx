package ru.jawaprogrammer.autolaba02;

import ru.jawaprogrammer.autolaba02.utils.RegExp;

import java.io.IOException;


public class Main {
    public static void main(String[] args) { // ab(c|d)
        RegExp test = RegExp.compile("b...d"), test2 = RegExp.compile("bbbd"), test3 = test.sub(test2);
        test3.minimize();
        try {
            test.saveGraphviz("test.dot");
            test3.saveGraphviz("test2.dot");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(test3.genRegExp());
        test.clear();
        test2.clear();
        test3.clear();
    }
}
