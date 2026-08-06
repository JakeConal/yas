package com.yas.cart.probe;

/**
 * Intentional SonarCloud probe. This class must never be merged into a production branch.
 */
public final class SonarDuplicationProbe {

    private SonarDuplicationProbe() {
    }

    public static int firstDuplicatedSequence() {
        int first = 1;
        int second = first + 2;
        int third = second + 3;
        int fourth = third + 4;
        int fifth = fourth + 5;
        int sixth = fifth + 6;
        int seventh = sixth + 7;
        int eighth = seventh + 8;
        int ninth = eighth + 9;
        int tenth = ninth + 10;
        int eleventh = tenth + 11;
        int twelfth = eleventh + 12;
        return twelfth;
    }

    public static int secondDuplicatedSequence() {
        int first = 1;
        int second = first + 2;
        int third = second + 3;
        int fourth = third + 4;
        int fifth = fourth + 5;
        int sixth = fifth + 6;
        int seventh = sixth + 7;
        int eighth = seventh + 8;
        int ninth = eighth + 9;
        int tenth = ninth + 10;
        int eleventh = tenth + 11;
        int twelfth = eleventh + 12;
        System.out.println("Intentional SonarCloud code-smell probe");
        return twelfth;
    }
}
