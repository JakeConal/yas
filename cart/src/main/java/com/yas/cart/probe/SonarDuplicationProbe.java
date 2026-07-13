package com.yas.cart.probe;

/**
 * Intentional SonarCloud probe. This class must never be merged into a production branch.
 */
public final class SonarDuplicationProbe {

    private SonarDuplicationProbe() {
    }

    public static int firstDuplicatedSequence() {
        int value = 0;
        value += 1;
        value += 2;
        value += 3;
        value += 4;
        value += 5;
        value += 6;
        value += 7;
        value += 8;
        value += 9;
        value += 10;
        value += 11;
        value += 12;
        return value;
    }

    public static int secondDuplicatedSequence() {
        int value = 0;
        value += 1;
        value += 2;
        value += 3;
        value += 4;
        value += 5;
        value += 6;
        value += 7;
        value += 8;
        value += 9;
        value += 10;
        value += 11;
        value += 12;
        System.out.println("Intentional SonarCloud code-smell probe");
        return value;
    }
}
