package com.easylive.web.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单的测试类，包含两个示例算法，支持直接通过 main 方法执行。
 */
public class SampleAlgorithmDemo {

    public static void main(String[] args) {
        int[] numbers = {5, 3, 8, 2, 1, 4};
        int target = 9;

        System.out.println("原始数组: " + Arrays.toString(numbers));
        System.out.println("目标和: " + target);

        int[] twoSumResult = twoSum(numbers, target);
        if (twoSumResult.length == 2) {
            System.out.println("Two Sum 结果索引: " + Arrays.toString(twoSumResult));
            System.out.println("对应数字: " + numbers[twoSumResult[0]] + " + " + numbers[twoSumResult[1]]);
        } else {
            System.out.println("未找到符合条件的两数之和。");
        }

        int[] sorted = insertionSort(numbers.clone());
        System.out.println("插入排序结果: " + Arrays.toString(sorted));
    }

    /**
     * 两数之和算法，返回满足条件的两个元素索引。
     */
    public static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> cache = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int diff = target - nums[i];
            if (cache.containsKey(diff)) {
                return new int[]{cache.get(diff), i};
            }
            cache.put(nums[i], i);
        }
        return new int[0];
    }

    /**
     * 插入排序，返回排序后的新数组。
     */
    public static int[] insertionSort(int[] nums) {
        for (int i = 1; i < nums.length; i++) {
            int current = nums[i];
            int j = i - 1;
            while (j >= 0 && nums[j] > current) {
                nums[j + 1] = nums[j];
                j--;
            }
            nums[j + 1] = current;
        }
        return nums;
    }
}

