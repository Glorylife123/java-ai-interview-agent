package com.biliying.study.day1;

import java.util.Scanner;

public class JavaBasicDemo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Please enter a score from 0 to 100: ");
        int score = scanner.nextInt();
        System.out.println("Grade: " + getGrade(score));

        int[] scores = {88, 92, 79, 100, 66};
        System.out.println("Average score: " + calculateAverage(scores));
    }

    public static String getGrade(int score) {
        if (score < 0 || score > 100) {
            return "Invalid score";
        }
        if (score >= 90) {
            return "A";
        }
        if (score >= 80) {
            return "B";
        }
        if (score >= 70) {
            return "C";
        }
        if (score >= 60) {
            return "D";
        }
        return "F";
    }

    public static double calculateAverage(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            return 0;
        }

        int sum = 0;
        for (int number : numbers) {
            sum += number;
        }
        return (double) sum / numbers.length;
    }
}

