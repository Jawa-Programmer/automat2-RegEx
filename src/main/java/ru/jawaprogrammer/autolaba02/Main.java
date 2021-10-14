package ru.jawaprogrammer.autolaba02;

import ru.jawaprogrammer.autolaba02.utils.RegExp;


public class Main {
    public static void main(String[] args) {
        RegExp test = RegExp.compile("(<lol>a)?ab");
        System.out.println(test.checkString("ab"));
        test.clear();
    }
}
