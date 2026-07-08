package com.stat.web.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectToUtils {

    public static Map<String, Object>  objectToMap(Object obj) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();

        // 获取obj的Class对象
        Class<?> clazz = obj.getClass();

        // 获取类的所有字段
        Field[] fields = clazz.getDeclaredFields();

        // 遍历字段数组
        for (Field field : fields) {
            // 设置字段可访问（如果为private等修饰符）
            field.setAccessible(true);
            // 将字段名作为键，字段值作为值，存入map中
            map.put(field.getName(), field.get(obj));
        }

        return map;
    }

    public static List<Map<String, Object>> objectToMaps(List<Object> objs) throws IllegalAccessException {
        List<Map<String, Object>> objectToMaps = new ArrayList<>();
        objs.forEach(obj->{
            try {
                objectToMaps.add(objectToMap(obj));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        return objectToMaps;
    }
}
