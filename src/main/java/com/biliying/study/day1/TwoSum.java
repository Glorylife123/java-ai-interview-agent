package com.biliying.study.day1;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TwoSum {
    public static void main(String[] args) {
        int[] nums = {2, 7, 11, 15};
        int target = 9;

        System.out.println("Brute force: " + Arrays.toString(twoSumBruteForce(nums, target)));
        System.out.println("HashMap: " + Arrays.toString(twoSumHashMap(nums, target)));
    }

    public static int[] twoSumBruteForce(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[0];
    }

    public static int[] twoSumHashMap(int[] nums, int target) {
        Map<Integer, Integer> numToIndex = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int need = target - nums[i];
            if (numToIndex.containsKey(need)) {
                return new int[]{numToIndex.get(need), i};
            }
            numToIndex.put(nums[i], i);
        }
        return new int[0];
    }
}

