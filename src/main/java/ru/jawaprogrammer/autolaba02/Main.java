package ru.jawaprogrammer.autolaba02;

import ru.jawaprogrammer.autolaba02.utils.RegExp;


public class Main {
    public static void main(String[] args) {
        RegExp test = RegExp.compile("(<lol>a...b)cd");
        System.out.println(test.checkString("aaaaaaaaaaabcd"));
        test.clear();
    }
}
