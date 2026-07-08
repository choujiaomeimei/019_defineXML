package com.stat.common.util;


import com.github.dozermapper.core.Mapper;

import java.util.ArrayList;
import java.util.List;

public class DozerUtils {

    /**
     * 封装dozer处理集合的方法：List<S> --> List<T>
     */
    public static <T, S> List<T> mapList(final Mapper mapper, List<S> sourceList, Class<T> targetObjectClass) {
        ArrayList<T> targetList = new ArrayList<T>();
        if(sourceList == null){
            return new ArrayList<>();
        }
        for (S s : sourceList) {
            targetList.add(mapper.map(s, targetObjectClass));
        }
        return targetList;
    }
}