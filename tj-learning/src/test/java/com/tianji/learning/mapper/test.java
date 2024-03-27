package com.tianji.learning.mapper;

import com.tianji.learning.LearningApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;

//@SpringBootTest(classes = LearningApplication.class)
class test {

//    @Autowired
//    private StringRedisTemplate redisTemplate;

    public static void main(String[] args) {
//        int[] nums = {100,1,4,2,3,200};
//        int i = longestConsecutive(nums);
//        System.out.println(i);
    }
    public static int longestConsecutive(int[] nums) {
        Arrays.sort(nums);
        int n = nums.length;
        int count = 1;
        int max = 0;
        Map<Integer,Integer> map = new HashMap<>();
        for(int i = 0; i < n; i++){
            count = map.getOrDefault(nums[i]-1,1);
            map.put(nums[i],count);
            if(count > max){
                max = count;
            }
        }
        return max;
    }
}