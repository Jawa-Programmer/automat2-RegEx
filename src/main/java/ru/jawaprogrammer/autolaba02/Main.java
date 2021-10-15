package ru.jawaprogrammer.autolaba02;

import ru.jawaprogrammer.autolaba02.utils.RegExp;

import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        RegExp test = RegExp.compile("(ab(c?))?(ab(c?))...");
        try {
            test.saveGraphviz("test.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(test.checkString("abcab"));
        test.minimize();
        try {
            test.saveGraphviz("test2.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(test.checkString("abcab"));
        test.minimize();
        try {
            test.saveGraphviz("test3.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(test.checkString("abcab"));
        test.clear();
    }
}
